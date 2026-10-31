package com.example.minorityvault.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.GlassSurface
import com.example.minorityvault.designsystem.GlassTint
import com.example.minorityvault.designsystem.R
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.glassRim
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/** A chat line. Promises carry a brass badge, because breaking one earns a public stamp. */
@Composable
fun ChatBubble(
    text: String,
    mine: Boolean,
    modifier: Modifier = Modifier,
    sender: String? = null,
    isPromise: Boolean = false,
) {
    val colors = VaultTheme.colors
    Box(modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        GlassSurface(
            modifier = Modifier.widthIn(max = MAX_BUBBLE_WIDTH),
            tint = if (mine) GlassTint.Raised else GlassTint.Panel,
            rim = if (isPromise) glassRim(colors.brass, PROMISE_RIM_STRENGTH) else glassRim(),
        ) {
            Column(Modifier.padding(VaultSpacing.md), verticalArrangement = Arrangement.spacedBy(VaultSpacing.xxs)) {
                if (sender != null) {
                    Text(sender, style = MaterialTheme.typography.labelMedium, color = colors.onPanelMuted)
                }
                if (isPromise) {
                    Text(
                        stringResource(R.string.promise_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.brass,
                    )
                }
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private val MAX_BUBBLE_WIDTH = 300.dp
private const val PROMISE_RIM_STRENGTH = 1.8f

@ComponentPreviews
@Composable
private fun ChatBubblePreview() {
    VaultPreviewTheme {
        Column(Modifier.padding(VaultSpacing.lg), verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
            ChatBubble("I will vote YES this round.", mine = false, sender = "Ben", isPromise = true)
            ChatBubble("I will vote NO this round.", mine = true, isPromise = true)
            ChatBubble("Nobody trust Chandrika.", mine = false, sender = "Asha")
        }
    }
}
