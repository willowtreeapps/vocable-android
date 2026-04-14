package com.willowtree.vocable.ui.languageselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.VocableTheme

@Composable
fun LanguageSelectionScreen(
    state: LanguageSelectionState,
    onBack: () -> Unit,
    onLanguageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GazeButton(
                onClick = onBack,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_language_selection)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.language_selection_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        GazeButton(
            onClick = { onLanguageSelected(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.language_system_default),
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.languages) { language ->
                LanguageOptionRow(
                    language = language,
                    isSelected = state.selectedLanguageTag == language.tag,
                    onClick = { onLanguageSelected(language.tag) }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    GazeButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Text(
                    text = stringResource(R.string.selected),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview
@Composable
private fun LanguageSelectionScreenPreview() {
    VocableTheme {
        LanguageSelectionScreen(
            state = LanguageSelectionState(
                languages = listOf(
                    LanguageOption("en", "English"),
                    LanguageOption("de", "Deutsch"),
                    LanguageOption("es", "Español")
                ),
                selectedLanguageTag = "en"
            ),
            onBack = {},
            onLanguageSelected = {}
        )
    }
}
