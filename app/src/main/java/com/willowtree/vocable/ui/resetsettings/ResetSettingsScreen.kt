package com.willowtree.vocable.ui.resetsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.components.ConfirmationDialog
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.ColorPrimaryDark
import com.willowtree.vocable.ui.theme.ErrorColor
import com.willowtree.vocable.ui.theme.SelectedColor
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun ResetSettingsScreen(
    onBack: () -> Unit,
    viewModel: ResetSettingsViewModel = koinViewModel()
) {
    MviScreen(viewModel = viewModel, onEvent = { event ->
        when (event) {
            ResetSettingsEvent.NavigateBack -> onBack()
        }
    }) { state ->
        ResetSettingsContent(
            state = state,
            onBack = viewModel::onBack,
            onToggleDomain = viewModel::toggleDomain,
            onRequestResetSelected = viewModel::requestResetSelected,
            onRequestResetEverything = viewModel::requestResetEverything,
            onDismissDialog = viewModel::dismissDialog,
            onConfirmDialog = viewModel::confirmDialog
        )
    }
}

private data class ResetDomainInfo(
    val domain: ResetDomain,
    val label: Int,
    val footer: Int
)

private val RESET_DOMAINS = listOf(
    ResetDomainInfo(ResetDomain.VOICE, R.string.settings_options_voice, R.string.reset_settings_footer_voice),
    ResetDomainInfo(ResetDomain.SENSITIVITY, R.string.timing_sensitivity_title, R.string.reset_settings_footer_sensitivity),
    ResetDomainInfo(ResetDomain.SELECTION_MODE, R.string.settings_selection_mode, R.string.reset_settings_footer_selection_mode),
    ResetDomainInfo(ResetDomain.CATEGORIES, R.string.categories_edit_title, R.string.reset_settings_footer_categories),
    ResetDomainInfo(ResetDomain.PHRASES, R.string.reset_phrases_label, R.string.reset_settings_footer_phrases)
)

@Composable
private fun ResetSettingsContent(
    state: ResetSettingsState,
    onBack: () -> Unit,
    onToggleDomain: (ResetDomain) -> Unit,
    onRequestResetSelected: () -> Unit,
    onRequestResetEverything: () -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.settings_margin_default))
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
            val (titleRef, backButtonRef) = createRefs()
            val backButtonSize = dimensionResource(id = R.dimen.settings_close_button_width)

            GazeButton(
                onClick = onBack,
                modifier = Modifier
                    .size(backButtonSize)
                    .constrainAs(backButtonRef) {
                        top.linkTo(titleRef.top)
                        bottom.linkTo(titleRef.bottom)
                        start.linkTo(parent.start)
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_40dp),
                    contentDescription = stringResource(R.string.close_settings),
                    tint = Color.Unspecified
                )
            }

            Text(
                text = stringResource(id = R.string.settings_reset_app),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    fontSize = dimensionResource(id = R.dimen.settings_title_text_size).value.sp
                ),
                modifier = Modifier.constrainAs(titleRef) {
                    top.linkTo(parent.top)
                    start.linkTo(backButtonRef.end, margin = 16.dp)
                    end.linkTo(parent.end, margin = backButtonSize + 16.dp)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RESET_DOMAINS.forEach { info ->
                ResetDomainRow(
                    label = stringResource(id = info.label),
                    footer = stringResource(id = info.footer),
                    checked = info.domain in state.checkedDomains,
                    onToggle = { onToggleDomain(info.domain) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GazeButton(
            onClick = onRequestResetSelected,
            enabled = state.checkedDomains.isNotEmpty(),
            backgroundColor = ColorPrimary,
            textColor = TextColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.selection_mode_button_height))
        ) {
            Text(
                text = stringResource(R.string.reset_settings_reset_selected),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        GazeButton(
            onClick = onRequestResetEverything,
            backgroundColor = ErrorColor,
            textColor = TextColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.selection_mode_button_height))
                .testTag("reset_settings_everything_button")
        ) {
            Text(
                text = stringResource(R.string.reset_settings_reset_everything),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (state.dialogTarget != null) {
        val (title, message) = when (state.dialogTarget) {
            ResetDialogTarget.Everything ->
                stringResource(R.string.reset_settings_reset_everything) to stringResource(R.string.settings_reset_dialog_message)

            ResetDialogTarget.Selected ->
                stringResource(R.string.reset_settings_selected_dialog_title) to stringResource(R.string.reset_settings_selected_dialog_message)
        }
        ConfirmationDialog(
            title = title,
            message = message,
            confirmText = stringResource(R.string.settings_reset_dialog_confirm),
            onDismiss = onDismissDialog,
            onConfirm = onConfirmDialog,
            isDestructive = true
        )
    }
}

@Composable
private fun ResetDomainRow(
    label: String,
    footer: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    GazeButton(
        onClick = onToggle,
        backgroundColor = ColorPrimary,
        textColor = TextColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                Text(
                    text = footer,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextColor
                )
            }

            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = SelectedColor,
                    checkmarkColor = ColorPrimaryDark,
                    uncheckedColor = TextColor
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, widthDp = 768, heightDp = 480)
@Composable
private fun ResetSettingsScreenPreview() {
    VocableTheme {
        ResetSettingsContent(
            state = ResetSettingsState(checkedDomains = setOf(ResetDomain.VOICE)),
            onBack = {},
            onToggleDomain = {},
            onRequestResetSelected = {},
            onRequestResetEverything = {},
            onDismissDialog = {},
            onConfirmDialog = {}
        )
    }
}
