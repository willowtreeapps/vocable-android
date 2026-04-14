package com.willowtree.vocable.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.willowtree.vocable.BuildConfig
import com.willowtree.vocable.MainActivity
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.editcategories.EditCategoriesScreen
import com.willowtree.vocable.ui.editcategorymenu.EditCategoryMenuScreen
import com.willowtree.vocable.ui.editphrases.EditCategoryPhrasesScreen
import com.willowtree.vocable.ui.keyboard.KeyboardScreen
import com.willowtree.vocable.ui.presets.PresetsScreen
import com.willowtree.vocable.ui.selectionmode.SelectionModeScreen
import com.willowtree.vocable.ui.selectionmode.SelectionModeViewModel
import com.willowtree.vocable.ui.sensitivity.SensitivityScreen
import com.willowtree.vocable.ui.settings.SettingsEvent
import com.willowtree.vocable.ui.settings.SettingsScreen
import com.willowtree.vocable.ui.settings.SettingsViewModel
import com.willowtree.vocable.ui.languageselection.LanguageSelectionEvent
import com.willowtree.vocable.ui.languageselection.LanguageSelectionScreen
import com.willowtree.vocable.ui.languageselection.LanguageSelectionViewModel
import com.willowtree.vocable.ui.voiceselection.VoiceSelectionEvent
import com.willowtree.vocable.ui.voiceselection.VoiceSelectionScreen
import com.willowtree.vocable.ui.voiceselection.VoiceSelectionViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import java.net.URLDecoder
import java.net.URLEncoder

private const val ROUTE_PRESETS = "presets"
private const val ROUTE_KEYBOARD = "keyboard"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_EDIT_CATEGORIES = "editCategories"
private const val ROUTE_EDIT_CATEGORY_MENU = "editCategoryMenu"
private const val ROUTE_EDIT_CATEGORY_PHRASES = "editCategoryPhrases"
private const val ROUTE_ADD_CATEGORY = "addCategory"
private const val ROUTE_RENAME_CATEGORY = "renameCategory"
private const val ROUTE_EDIT_PHRASE = "editPhrase"
private const val ROUTE_SENSITIVITY = "sensitivity"
private const val ROUTE_SELECTION_MODE = "selectionMode"
private const val ROUTE_VOICE_SELECTION = "voiceSelection"
private const val ROUTE_LANGUAGE_SELECTION = "languageSelection"

@Composable
fun VocableNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_PRESETS,
        modifier = modifier
    ) {
        composable(ROUTE_PRESETS) {
            PresetsScreen(
                onNavigateToKeyboard = {
                    navController.navigate(ROUTE_KEYBOARD) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigateToTopLevelSettings()
                },
                onAddPhrase = { category ->
                    val encodedCategoryId = URLEncoder.encode(category.categoryId, Charsets.UTF_8.name())
                    navController.navigate("$ROUTE_KEYBOARD/$encodedCategoryId/")
                }
            )
        }

        composable(ROUTE_KEYBOARD) {
            KeyboardScreen(
                onNavigateToPresets = {
                    navController.navigate(ROUTE_PRESETS) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigateToTopLevelSettings(popUpToRoute = ROUTE_KEYBOARD)
                }
            )
        }

        composable("$ROUTE_KEYBOARD/{categoryId}/{categoryName}") { backStack ->
            val categoryId = backStack.arguments?.getString("categoryId") ?: return@composable
            val encodedCategoryName = backStack.arguments?.getString("categoryName") ?: ""
            KeyboardScreen(
                categoryId = URLDecoder.decode(categoryId, Charsets.UTF_8.name()),
                initialText = URLDecoder.decode(encodedCategoryName, Charsets.UTF_8.name()),
                onNavigateToPresets = { navController.popBackStack() },
                onNavigateToSettings = {
                    navController.navigateToTopLevelSettings()
                }
            )
        }

        composable("$ROUTE_EDIT_PHRASE/{phraseId}/{phraseText}") { backStack ->
            val phraseId = backStack.arguments?.getString("phraseId") ?: return@composable
            val encodedPhraseText = backStack.arguments?.getString("phraseText") ?: ""
            val phraseText = URLDecoder.decode(encodedPhraseText, Charsets.UTF_8.name())

            KeyboardScreen(
                phraseIdToEdit = phraseId,
                initialText = phraseText,
                onNavigateToPresets = { navController.popBackStack() },
                onNavigateToSettings = {
                    navController.navigateToTopLevelSettings()
                }
            )
        }

        composable(ROUTE_ADD_CATEGORY) {
            KeyboardScreen(
                categoryIdToEdit = null,
                isCategoryEdit = true,
                onNavigateToPresets = { navController.popBackStack() },
                onNavigateToSettings = { navController.popBackStack() }
            )
        }

        composable("$ROUTE_RENAME_CATEGORY/{categoryId}/{categoryName}") { backStack ->
            val categoryId = backStack.arguments?.getString("categoryId") ?: return@composable
            val encodedCategoryName = backStack.arguments?.getString("categoryName") ?: ""
            KeyboardScreen(
                categoryIdToEdit = categoryId,
                isCategoryEdit = true,
                initialText = URLDecoder.decode(encodedCategoryName, Charsets.UTF_8.name()),
                onNavigateToPresets = { navController.popBackStack() },
                onNavigateToSettings = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SETTINGS) {
            val settingsContext = LocalContext.current
            val viewModel: SettingsViewModel = koinViewModel()
            MviScreen(viewModel = viewModel, onEvent = { event ->
                when (event) {
                    SettingsEvent.NavigateToEditCategories -> navController.navigate(ROUTE_EDIT_CATEGORIES)
                    SettingsEvent.NavigateToTimingSensitivity -> navController.navigate(ROUTE_SENSITIVITY)
                    SettingsEvent.NavigateToSelectionMode -> navController.navigate(ROUTE_SELECTION_MODE)
                    SettingsEvent.NavigateToVoiceSelection -> navController.navigate(ROUTE_VOICE_SELECTION)
                    is SettingsEvent.OpenPrivacyPolicy -> settingsContext.startActivity(
                        Intent(Intent.ACTION_VIEW, event.url.toUri())
                    )
                    is SettingsEvent.ContactDevelopers -> settingsContext.startActivity(
                        Intent(Intent.ACTION_SENDTO).apply { data = event.mailTo.toUri() }
                    )
                }
            }) { state ->
                SettingsScreen(
                    state = state,
                    versionName = stringResource(id = R.string.version, BuildConfig.VERSION_NAME),
                    onClose = { navController.popBackStack(ROUTE_PRESETS, false) },
                    onEditCategories = viewModel::onEditCategories,
                    onTimingSensitivity = viewModel::onTimingSensitivity,
                    onSelectionMode = viewModel::onSelectionMode,
                    onPrivacyPolicy = viewModel::requestPrivacyPolicy,
                    onContactDevs = viewModel::requestContactDevs,
                    onDismissDialog = viewModel::dismissDialog,
                    onConfirmDialog = viewModel::confirmDialog
                )
            }
        }

        composable(ROUTE_EDIT_CATEGORIES) {
            EditCategoriesScreen(
                onBack = { navController.popBackStack() },
                onAddCategory = { navController.navigate(ROUTE_ADD_CATEGORY) },
                onEditCategory = { category ->
                    navController.navigate("$ROUTE_EDIT_CATEGORY_MENU/${category.categoryId}")
                }
            )
        }

        composable("$ROUTE_EDIT_CATEGORY_MENU/{categoryId}") { backStack ->
            val categoryId = backStack.arguments?.getString("categoryId") ?: return@composable
            EditCategoryMenuScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onRenameCategory = { id, categoryName ->
                    val encodedCategoryName = URLEncoder.encode(categoryName, Charsets.UTF_8.name())
                    navController.navigate("$ROUTE_RENAME_CATEGORY/$id/$encodedCategoryName")
                },
                onEditPhrases = { id -> navController.navigate("$ROUTE_EDIT_CATEGORY_PHRASES/$id") }
            )
        }

        composable("$ROUTE_EDIT_CATEGORY_PHRASES/{categoryId}") { backStack ->
            val categoryId = backStack.arguments?.getString("categoryId") ?: return@composable
            EditCategoryPhrasesScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onAddPhrase = { id ->
                    val encodedCategoryId = URLEncoder.encode(id, Charsets.UTF_8.name())
                    navController.navigate("$ROUTE_KEYBOARD/$encodedCategoryId/")
                },
                onEditPhrase = { phraseId, text ->
                    val encodedPhraseText = URLEncoder.encode(text, Charsets.UTF_8.name())
                    navController.navigate("$ROUTE_EDIT_PHRASE/$phraseId/$encodedPhraseText")
                }
            )
        }

        composable(ROUTE_SENSITIVITY) {
            SensitivityScreen(onBack = { navController.popBackStack(ROUTE_SETTINGS, false) })
        }

        composable(ROUTE_SELECTION_MODE) {
            val selectionContext = LocalContext.current
            val mainActivity = selectionContext.findActivity() as? MainActivity
                ?: error("VocableNavHost must be hosted in MainActivity")
            val vm: SelectionModeViewModel = mainActivity.getViewModel()
            SelectionModeScreen(
                onBack = { navController.popBackStack(ROUTE_SETTINGS, false) },
                onVoiceSelection = { navController.navigate(ROUTE_VOICE_SELECTION) },
                onLanguageSelection = { navController.navigate(ROUTE_LANGUAGE_SELECTION) },
                viewModel = vm
            )
        }

        composable(ROUTE_VOICE_SELECTION) {
            val voiceContext = LocalContext.current
            val viewModel: VoiceSelectionViewModel = koinViewModel()
            MviScreen(viewModel = viewModel, onEvent = { event ->
                when (event) {
                    VoiceSelectionEvent.NavigateBack -> navController.popBackStack()
                    is VoiceSelectionEvent.LaunchTtsSettings -> voiceContext.startActivity(
                        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                            event.enginePackage?.let { setPackage(it) }
                        }
                    )
                }
            }) { state ->
                VoiceSelectionScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onVoiceSelected = viewModel::onVoiceSelected,
                    onDownloadVoice = viewModel::onDownloadVoice,
                    onRefreshVoices = viewModel::refreshVoices
                )
            }
        }

        composable(ROUTE_LANGUAGE_SELECTION) {
            val viewModel: LanguageSelectionViewModel = koinViewModel()
            MviScreen(viewModel = viewModel, onEvent = { event ->
                when (event) {
                    LanguageSelectionEvent.NavigateBack -> navController.popBackStack()
                }
            }) { state ->
                LanguageSelectionScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onLanguageSelected = viewModel::onLanguageSelected
                )
            }
        }
    }
}

private fun NavHostController.navigateToTopLevelSettings(popUpToRoute: String? = null) {
    navigate(ROUTE_SETTINGS) {
        popUpTo(popUpToRoute ?: ROUTE_PRESETS) { inclusive = false }
        launchSingleTop = true
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}