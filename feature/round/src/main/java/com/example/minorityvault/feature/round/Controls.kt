package com.example.minorityvault.feature.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle

/** A wrapping row of chips where exactly one (or none) is selected. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChoiceGroup(
    choices: List<Choice<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VaultTheme.colors
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
    ) {
        choices.forEach { choice ->
            val isSelected = choice.value == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(choice.value) },
                label = { Text(choice.label, style = MaterialTheme.typography.labelLarge) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = colors.onPanel,
                    selectedContainerColor = colors.brass,
                    selectedLabelColor = colors.ink,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = colors.steel,
                    selectedBorderColor = colors.brass,
                ),
            )
        }
    }
}

/** − value + with 48 dp buttons. */
@Composable
internal fun Stepper(value: String, onDecrease: () -> Unit, onIncrease: () -> Unit, modifier: Modifier = Modifier) {
    val decrease = stringResource(R.string.contract_compose_decrease)
    val increase = stringResource(R.string.contract_compose_increase)
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(VaultSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VaultButton(
            "−",
            onClick = onDecrease,
            style = VaultButtonStyle.Secondary,
            modifier = Modifier.semantics { contentDescription = decrease },
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = VaultTheme.colors.brass,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = STEPPER_VALUE_WIDTH),
        )
        VaultButton(
            "+",
            onClick = onIncrease,
            style = VaultButtonStyle.Secondary,
            modifier = Modifier.semantics { contentDescription = increase },
        )
    }
}

/** One option in a single-choice group. */
data class Choice<T>(val value: T, val label: String)

private val STEPPER_VALUE_WIDTH = 72.dp
