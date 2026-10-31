package com.example.minorityvault.feature.lobby

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.component.KeeperBubble
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.net.NearbyPermissions

/**
 * Shows [content] once every Nearby permission is granted, and an explanation until then. Checks again whenever
 * the app comes back to the front, in case the player granted them in Settings.
 */
@Composable
fun NearbyPermissionGate(onBack: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val required = remember { NearbyPermissions.required(Build.VERSION.SDK_INT) }
    var granted by remember { mutableStateOf(context.hasAll(required)) }
    // Decided when Android answers, so a second refusal (which looks the same as the first) still updates it.
    var ask by rememberSaveable { mutableStateOf(PermissionAsk.EXPLAIN) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = context.hasAll(required)
        val blocked = activity != null && activity.wontAskAgain(context, required)
        ask = if (blocked) PermissionAsk.BLOCKED else PermissionAsk.DENIED
    }
    LifecycleResumeEffect(required) {
        granted = context.hasAll(required)
        onPauseOrDispose {}
    }
    if (granted) {
        content()
    } else {
        PermissionScreen(
            ask = ask,
            onAllow = { launcher.launch(required.toTypedArray()) },
            onOpenSettings = { context.openAppSettings() },
            onBack = onBack,
        )
    }
}

@Composable
fun PermissionScreen(
    ask: PermissionAsk,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LobbyScaffold(modifier, title = stringResource(R.string.permission_title), bottom = {
        when (ask) {
            PermissionAsk.EXPLAIN ->
                VaultButton(stringResource(R.string.permission_allow), onAllow, Modifier.fillMaxWidth())

            PermissionAsk.DENIED ->
                VaultButton(stringResource(R.string.permission_ask_again), onAllow, Modifier.fillMaxWidth())

            PermissionAsk.BLOCKED ->
                VaultButton(stringResource(R.string.permission_open_settings), onOpenSettings, Modifier.fillMaxWidth())
        }
        VaultButton(stringResource(R.string.back), onBack, Modifier.fillMaxWidth(), style = VaultButtonStyle.Secondary)
    }) {
        KeeperBubble(stringResource(R.string.permission_explain))
        when (ask) {
            PermissionAsk.EXPLAIN -> Unit
            PermissionAsk.DENIED -> Warning(stringResource(R.string.permission_denied))
            PermissionAsk.BLOCKED -> Warning(stringResource(R.string.permission_blocked))
        }
    }
}

enum class PermissionAsk {
    /** Not asked yet: explain first, then ask. */
    EXPLAIN,

    /** Refused once; Android will still show the prompt again. */
    DENIED,

    /** Android won't show the prompt any more; only Settings can change it. */
    BLOCKED,
}

private fun Context.hasAll(permissions: List<String>) =
    permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

/** After a refusal, Android stops offering a rationale for a permission it will no longer prompt for. */
private fun Activity.wontAskAgain(context: Context, permissions: List<String>) = permissions
    .filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
    .none { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@ScreenPreviews
@Composable
private fun PermissionExplainPreview() {
    VaultPreviewTheme { PermissionScreen(PermissionAsk.EXPLAIN, onAllow = {}, onOpenSettings = {}, onBack = {}) }
}

@ScreenPreviews
@Composable
private fun PermissionBlockedPreview() {
    VaultPreviewTheme { PermissionScreen(PermissionAsk.BLOCKED, onAllow = {}, onOpenSettings = {}, onBack = {}) }
}
