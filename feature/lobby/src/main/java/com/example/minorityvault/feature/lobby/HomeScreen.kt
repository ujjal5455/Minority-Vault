package com.example.minorityvault.feature.lobby

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.designsystem.sheen

/** The first screen. Hosting and joining need a name; passing the phone asks for names later. */
@Composable
fun HomeScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onHost: () -> Unit,
    onJoin: () -> Unit,
    onPassPhone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VaultTheme.colors
    val hasName = name.isNotBlank()
    LobbyScaffold(modifier, bottom = {
        if (!hasName) MutedText(stringResource(R.string.home_name_needed))
        VaultButton(stringResource(R.string.home_host), onHost, Modifier.fillMaxWidth(), enabled = hasName)
        VaultButton(
            stringResource(R.string.home_join),
            onJoin,
            Modifier.fillMaxWidth(),
            style = VaultButtonStyle.Secondary,
            enabled = hasName,
        )
        VaultButton(
            stringResource(R.string.home_pass_phone),
            onPassPhone,
            Modifier.fillMaxWidth(),
            style = VaultButtonStyle.Secondary,
        )
    }) {
        Text(
            stringResource(R.string.home_title),
            style = MaterialTheme.typography.displayMedium,
            color = colors.brass,
            modifier = Modifier.semantics { heading() }.sheen(),
        )
        Text(
            stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onPanel,
        )
        NameField(name, onNameChange, stringResource(R.string.home_name_label))
        MutedText(stringResource(R.string.home_host_hint))
        MutedText(stringResource(R.string.home_join_hint))
        MutedText(stringResource(R.string.home_pass_phone_hint))
    }
}

@Composable
internal fun NameField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        colors = vaultFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun vaultFieldColors(): TextFieldColors {
    val colors = VaultTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.brass,
        unfocusedBorderColor = colors.steel,
        focusedLabelColor = colors.brass,
        unfocusedLabelColor = colors.onPanelMuted,
        cursorColor = colors.brass,
        focusedTextColor = colors.onPanel,
        unfocusedTextColor = colors.onPanel,
    )
}

@ScreenPreviews
@Composable
private fun HomePreview() {
    VaultPreviewTheme {
        HomeScreen(name = "Asha", onNameChange = {}, onHost = {}, onJoin = {}, onPassPhone = {})
    }
}

@ScreenPreviews
@Composable
private fun HomeNoNamePreview() {
    VaultPreviewTheme {
        HomeScreen(name = "", onNameChange = {}, onHost = {}, onJoin = {}, onPassPhone = {})
    }
}
