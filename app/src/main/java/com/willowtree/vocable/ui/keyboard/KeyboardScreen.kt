package com.willowtree.vocable.ui.keyboard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.SpeakerButtonColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun KeyboardScreen(
    categoryId: String? = null,
    isCategoryEdit: Boolean = false,
    categoryIdToEdit: String? = null,
    phraseIdToEdit: String? = null,
    initialText: String? = null,
    onNavigateToPresets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: KeyboardViewModel = koinViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(categoryId, isCategoryEdit, categoryIdToEdit, phraseIdToEdit, initialText) {
        viewModel.setContext(
            saveCategoryId = categoryId,
            isCategoryEdit = isCategoryEdit,
            categoryId = categoryIdToEdit,
            phraseIdToEdit = phraseIdToEdit,
            initialText = initialText
        )
    }

    MviScreen(viewModel = viewModel, onEvent = { event -> 
        when (event) {
            is KeyboardEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            is KeyboardEvent.NavigateBack -> {
                onNavigateToPresets() // Use as a general back handler in this context
            }
        }
    }) { state ->
        KeyboardContent(
            inputText = state.inputText,
            headTrackingEnabled = state.headTrackingEnabled,
            isEditMode = state.isCategoryEdit || state.phraseIdToEdit != null,
            onTextChange = viewModel::onTextChange,
            onNavigateToPresets = onNavigateToPresets,
            onNavigateToSettings = onNavigateToSettings,
            onKey = viewModel::onKey,
            onClear = viewModel::onClear,
            onSpace = viewModel::onSpace,
            onBackspace = viewModel::onBackspace,
            onSpeak = viewModel::onSpeak,
            onAddPhrase = viewModel::savePhrase
        )
    }
}

@Composable
fun KeyboardContent(
    inputText: String,
    headTrackingEnabled: Boolean,
    isEditMode: Boolean,
    onTextChange: (String) -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onKey: (String) -> Unit,
    onClear: () -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onSpeak: () -> Unit,
    onAddPhrase: () -> Unit,
) {
    val keys = stringArrayResource(id = R.array.keyboard_keys)
    val numColumns = integerResource(id = R.integer.keyboard_columns)

    val isLandscapeOrTablet = numColumns > 5

    val chunkedKeys = remember(keys, numColumns) {
        keys.toList().chunked(numColumns)
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(headTrackingEnabled) {
        if (!headTrackingEnabled) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }else {
            keyboardController?.hide()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLandscapeOrTablet) {
                Row(
                    modifier = if(headTrackingEnabled){
                        Modifier
                            .fillMaxWidth()
                            .weight(2f)
                            .padding(bottom = 8.dp)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .weight(2f)
                    },
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (headTrackingEnabled) {
                            Text(
                                text = inputText.ifEmpty { stringResource(R.string.keyboard_select_letters) },
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (inputText.isEmpty()) Color.Gray else Color.White
                                ),
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        } else {
                            TextField(
                                value = inputText,
                                onValueChange = onTextChange,
                                placeholder = {
                                    Text(
                                        stringResource(R.string.keyboard_select_letters),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                    )
                                },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp)
                                    .focusRequester(focusRequester),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done,
                                    autoCorrectEnabled = true
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { 
                                        onSpeak() 
                                    }
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .weight(if (headTrackingEnabled) 1f else 1.5f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isEditMode) {
                            // If in edit mode, standard buttons become simple Cancel / Save
                            GazeButton(
                                onClick = onNavigateToPresets,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_back_40dp),
                                    contentDescription = stringResource(R.string.close_settings),
                                    tint = Color.Unspecified
                                )
                            }

                            GazeButton(
                                onClick = onSpeak,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                enabled = inputText.isNotBlank()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_40dp),
                                    contentDescription = stringResource(R.string.changes_saved),
                                    tint = Color.Unspecified
                                )
                            }

                        } else {
                            if (inputText.isNotEmpty()) {
                                GazeButton(
                                    onClick = onAddPhrase,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    textColor = ColorPrimary
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_star_border_40dp),
                                        contentDescription = stringResource(R.string.add_phrase),
                                        tint = Color.Unspecified
                                    )
                                }

                                if (!headTrackingEnabled) {
                                    GazeButton(
                                        onClick = onSpeak,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        backgroundColor = SpeakerButtonColor,
                                        textColor = ColorPrimary
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_speak_dark_blue),
                                            contentDescription = stringResource(R.string.keyboard_speak),
                                            tint = Color.Unspecified
                                        )
                                    }
                                }
                            }

                            GazeButton(
                                onClick = onNavigateToPresets,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_presets),
                                    contentDescription = stringResource(R.string.preset_sayings_title),
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Unspecified
                                )
                            }

                            GazeButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_settings_light_48dp),
                                    contentDescription = stringResource(R.string.settings),
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (headTrackingEnabled) 0.8f else 1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (headTrackingEnabled) {
                        Text(
                            text = inputText.ifEmpty { stringResource(R.string.keyboard_select_letters) },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (inputText.isEmpty()) Color.Gray else Color.White
                            ),
                            modifier = Modifier.padding(start = 24.dp)
                        )
                    } else {
                        TextField(
                            value = inputText,
                            onValueChange = onTextChange,
                            placeholder = {
                                Text(
                                    stringResource(R.string.keyboard_select_letters),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                )
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                                autoCorrectEnabled = true
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { 
                                    onSpeak() 
                                }
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isEditMode) {
                        GazeButton(
                            onClick = onNavigateToPresets,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back_40dp),
                                contentDescription = stringResource(R.string.close_settings),
                                tint = Color.Unspecified
                            )
                        }

                        GazeButton(
                            onClick = onSpeak,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check_40dp),
                                contentDescription = stringResource(R.string.changes_saved),
                                tint = Color.Unspecified
                            )
                        }
                    } else {
                        if (inputText.isNotEmpty()) {
                            GazeButton(
                                onClick = onAddPhrase,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                textColor = ColorPrimary
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_star_border_40dp),
                                    contentDescription = stringResource(R.string.add_phrase),
                                    tint = Color.Unspecified
                                )
                            }

                            if (!headTrackingEnabled) {
                                GazeButton(
                                    onClick = onSpeak,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    backgroundColor = SpeakerButtonColor,
                                    textColor = ColorPrimary
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_speak_dark_blue),
                                        contentDescription = stringResource(R.string.keyboard_speak),
                                        tint = Color.Unspecified
                                    )
                                }
                            }
                        }

                        GazeButton(
                            onClick = onNavigateToPresets,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_presets),
                                contentDescription = stringResource(R.string.preset_sayings_title),
                                modifier = Modifier.size(32.dp),
                                tint = Color.Unspecified
                            )
                            if (inputText.isEmpty()) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = stringResource(R.string.preset_sayings_title), style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        GazeButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings_light_48dp),
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier.size(32.dp),
                                tint = Color.Unspecified
                            )
                            if (inputText.isEmpty()) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                if (headTrackingEnabled) {
                    Spacer(modifier = Modifier.weight(0.1f))
                }
            }

            if (headTrackingEnabled) {
                Column(
                    modifier = Modifier
                        .weight(5f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunkedKeys.forEach { rowKeys ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                GazeButton(
                                    onClick = { onKey(key) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    Text(text = key, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (rowKeys.size < numColumns) {
                                repeat(numColumns - rowKeys.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GazeButton(onClick = onClear, modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()) {
                        Icon(painterResource(id = R.drawable.ic_delete), contentDescription = stringResource(R.string.keyboard_clear), tint = Color.Unspecified)
                    }

                    GazeButton(onClick = onSpace, modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()) {
                        Icon(painterResource(id = R.drawable.ic_space_bar_56dp), contentDescription = stringResource(R.string.keyboard_space), tint = Color.Unspecified)
                    }

                    GazeButton(onClick = onBackspace, modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()) {
                        Icon(painterResource(id = R.drawable.ic_backspace), contentDescription = stringResource(R.string.keyboard_backspace), tint = Color.Unspecified)
                    }

                    if (!isEditMode) {
                        GazeButton(
                            onClick = onSpeak,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            backgroundColor = SpeakerButtonColor,
                            textColor = ColorPrimary
                        ) {
                            Icon(painterResource(id = R.drawable.ic_speak_dark_blue), contentDescription = stringResource(R.string.keyboard_speak), tint = Color.Unspecified)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(5f))
                Spacer(modifier = Modifier.weight(0.2f))
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview(name = "Portrait", device = Devices.PIXEL_3, showBackground = true)
@Preview(name = "Landscape", device = "spec:width=800dp,height=400dp,dpi=240", showBackground = true)
@Composable
fun KeyboardScreenPreview() {
    VocableTheme {
        KeyboardContent(
            inputText = "",
            headTrackingEnabled = true,
            isEditMode = false,
            onTextChange = {},
            onNavigateToPresets = {},
            onNavigateToSettings = {},
            onKey = {},
            onClear = {},
            onSpace = {},
            onBackspace = {},
            onSpeak = {},
            onAddPhrase = {},
        )
    }
}

@Preview(name = "Portrait with text", device = Devices.PIXEL_3, showBackground = true)
@Composable
fun KeyboardScreenWithTextPreview() {
    VocableTheme {
        KeyboardContent(
            inputText = "Some Phrase",
            headTrackingEnabled = true,
            isEditMode = false,
            onTextChange = {},
            onNavigateToPresets = {},
            onNavigateToSettings = {},
            onKey = {},
            onClear = {},
            onSpace = {},
            onBackspace = {},
            onSpeak = {},
            onAddPhrase = {}
        )
    }
}

@Preview(name = "Landscape Head Tracking Disabled", device = "spec:width=800dp,height=400dp,dpi=240", showBackground = true)
@Composable
fun KeyboardScreenWithTextPreviewHeadTrackingDisabled() {
    VocableTheme {
        KeyboardContent(
            inputText = "Some Phrase",
            headTrackingEnabled = false,
            isEditMode = false,
            onTextChange = {},
            onNavigateToPresets = {},
            onNavigateToSettings = {},
            onKey = {},
            onClear = {},
            onSpace = {},
            onBackspace = {},
            onSpeak = {},
            onAddPhrase = {}
        )
    }
}
