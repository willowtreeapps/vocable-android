package com.willowtree.vocable.ui.sensitivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.ColorPrimaryDark
import com.willowtree.vocable.ui.theme.SelectedColor
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel
import java.text.DecimalFormat

@Composable
fun SensitivityScreen(
    onBack: () -> Unit,
    viewModel: SensitivityViewModel = koinViewModel()
) {
    val sensitivity by viewModel.sensitivity.collectAsStateWithLifecycle()
    val dwellTime by viewModel.dwellTime.collectAsStateWithLifecycle()

    SensitivityContent(
        sensitivity = sensitivity,
        dwellTime = dwellTime,
        onBack = onBack,
        onSetSensitivity = viewModel::setSensitivity,
        onIncreaseDwellTime = viewModel::increaseDwellTime,
        onDecreaseDwellTime = viewModel::decreaseDwellTime
    )
}

@Composable
private fun SensitivityContent(
    sensitivity: Float,
    dwellTime: Long,
    onBack: () -> Unit,
    onSetSensitivity: (Float) -> Unit,
    onIncreaseDwellTime: () -> Unit,
    onDecreaseDwellTime: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.settings_margin_default))
    ) {
        val (titleRef, backButtonRef, hoverTitleRef, hoverControlsRef, cursorTitleRef, cursorControlsRef) = createRefs()
        val backButtonSize = dimensionResource(id = R.dimen.settings_close_button_width)

        // Title
        BasicText(
            text = stringResource(id = R.string.timing_sensitivity_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            ),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 15.sp,
                maxFontSize = 28.sp,
                stepSize = 0.5.sp
            ),
            maxLines = 2,
            modifier = Modifier.constrainAs(titleRef) {
                start.linkTo(backButtonRef.end , margin = backButtonSize + 16.dp)
                end.linkTo(parent.end, margin = backButtonSize + 16.dp)
            }
        )

        // Back Button
        GazeButton(
            onClick = onBack,
            modifier = Modifier
                .size(backButtonSize)
                .constrainAs(backButtonRef) {
                    start.linkTo(parent.start)
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back_40dp),
                contentDescription = stringResource(R.string.close_settings),
                tint = Color.Unspecified
            )
        }

        // Hover Time Section
        Text(
            text = stringResource(id = R.string.hover_time_text),
            style = MaterialTheme.typography.titleLarge.copy(color = TextColor),
            modifier = Modifier.constrainAs(hoverTitleRef) {
                top.linkTo(backButtonRef.bottom, margin = 32.dp)
                start.linkTo(parent.start)
            }
        )

        Row(
            modifier = Modifier.constrainAs(hoverControlsRef) {
                top.linkTo(hoverTitleRef.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            GazeButton(
                onClick = onDecreaseDwellTime,
                enabled = dwellTime > 500L, // Example min

            ) {
                Icon(painterResource(id = R.drawable.ic_decrease_40dp), null, tint = Color.White)
            }

            val dwellTimeText = if (dwellTime == 1000L) {
                stringResource(R.string.hover_time_one_text)
            } else {
                val df = DecimalFormat("#.#")
                stringResource(R.string.hover_time_amount_text, df.format(dwellTime.toDouble() / 1000L))
            }

            Text(
                text = dwellTimeText,
                style = MaterialTheme.typography.headlineMedium.copy(color = TextColor),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            GazeButton(
                onClick = onIncreaseDwellTime,
                enabled = dwellTime < 4000L, // Example max
            ) {
                Icon(painterResource(id = R.drawable.ic_add_40dp), null, tint = Color.Unspecified)
            }
        }

        // Cursor Sensitivity Section
        Text(
            text = stringResource(id = R.string.cursor_sensitivity_text),
            style = MaterialTheme.typography.titleLarge.copy(color = TextColor),
            modifier = Modifier.constrainAs(cursorTitleRef) {
                top.linkTo(hoverControlsRef.bottom, margin = 32.dp)
                start.linkTo(parent.start)
            }
        )

        Row(
            modifier = Modifier.constrainAs(cursorControlsRef) {
                top.linkTo(cursorTitleRef.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
                height = Dimension.value(88.dp) // Match dimension resource if possible
            },
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val lowSelected = sensitivity == SensitivityViewModel.LOW_SENSITIVITY
            val mediumSelected = sensitivity == SensitivityViewModel.MEDIUM_SENSITIVITY
            val highSelected = sensitivity == SensitivityViewModel.HIGH_SENSITIVITY

            SensitivityButton(
                text = stringResource(R.string.cursor_sensitivity_low),
                selected = lowSelected,
                onClick = { onSetSensitivity(SensitivityViewModel.LOW_SENSITIVITY) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            SensitivityButton(
                text = stringResource(R.string.cursor_sensitivity_medium),
                selected = mediumSelected,
                onClick = { onSetSensitivity(SensitivityViewModel.MEDIUM_SENSITIVITY) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            SensitivityButton(
                text = stringResource(R.string.cursor_sensitivity_high),
                selected = highSelected,
                onClick = { onSetSensitivity(SensitivityViewModel.HIGH_SENSITIVITY) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
fun SensitivityButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GazeButton(
        onClick = onClick,
        modifier = modifier,
        backgroundColor = if (selected) SelectedColor else ColorPrimary,
        textColor = if (selected) ColorPrimaryDark else TextColor,
        enabled = !selected // Disable if already selected
    ) {
        BasicText(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            ),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = 22.sp,
                stepSize = 0.5.sp
            ),
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, widthDp = 768, heightDp = 480)
@Composable
fun SensitivityScreenPreview() {
    VocableTheme {
        SensitivityContent(
            sensitivity = SensitivityViewModel.MEDIUM_SENSITIVITY,
            dwellTime = 1000L,
            onBack = {},
            onSetSensitivity = {},
            onIncreaseDwellTime = {},
            onDecreaseDwellTime = {}
        )
    }
}
