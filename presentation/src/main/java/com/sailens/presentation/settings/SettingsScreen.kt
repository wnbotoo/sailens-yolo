package com.sailens.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sailens.domain.model.common.PerceptionProfile
import com.sailens.presentation.R
import com.sailens.presentation.ext.openUrl
import com.sailens.ux.component.SailensScaffold
import com.sailens.ux.component.SelectableRow
import com.sailens.ux.component.SettingRow
import com.sailens.ux.component.SettingsGroupDivider
import com.sailens.ux.component.SettingsInlineNote
import com.sailens.ux.component.SettingsSection
import com.sailens.ux.component.ToggleRow
import com.sailens.ux.theme.SailensDimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenTraceReports: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SailensScaffold(
        title = stringResource(R.string.title_settings),
        onNavigateBack = onNavigateBack,
        navigateBackContentDescription = stringResource(R.string.cd_navigate_back),
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SailensDimens.screenPadding, vertical = SailensDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(SailensDimens.sectionSpacing),
        ) {
            SettingsSection(title = stringResource(R.string.settings_section_feedback)) {
                ToggleRow(
                    label = stringResource(R.string.label_feedback_speech),
                    supportingText = stringResource(R.string.settings_feedback_speech_supporting),
                    checked = state.feedbackSettings.speechEnabled,
                    onCheckedChange = viewModel::setSpeechEnabled,
                )
                SettingsGroupDivider()
                ToggleRow(
                    label = stringResource(R.string.label_feedback_haptics),
                    supportingText = stringResource(R.string.settings_feedback_haptics_supporting),
                    checked = state.feedbackSettings.hapticsEnabled,
                    onCheckedChange = viewModel::setHapticsEnabled,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_perception)) {
                Column(modifier = Modifier.selectableGroup()) {
                    SelectableRow(
                        label = stringResource(R.string.label_perception_profile_basic),
                        supportingText = stringResource(R.string.settings_perception_basic_supporting),
                        selected = state.perceptionProfile == PerceptionProfile.BASIC,
                        onClick = { viewModel.setPerceptionProfile(PerceptionProfile.BASIC) },
                    )
                    SettingsGroupDivider()
                    SelectableRow(
                        label = stringResource(R.string.label_perception_profile_default),
                        supportingText = stringResource(R.string.settings_perception_default_supporting),
                        selected = state.perceptionProfile == PerceptionProfile.DEFAULT,
                        onClick = { viewModel.setPerceptionProfile(PerceptionProfile.DEFAULT) },
                    )
                }
                SettingsGroupDivider()
                SettingsInlineNote(text = stringResource(R.string.settings_perception_apply_hint))
            }

            if (state.diagnostics.showDiagnostics) {
                SettingsDiagnosticsSection(
                    onOpenTraceReports = onOpenTraceReports,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                AboutSummary()
                SettingsGroupDivider()
                SettingRow(
                    title = stringResource(R.string.settings_app_version_label),
                    valueText = stringResource(
                        R.string.settings_app_version_value,
                        state.appInfo.versionName,
                        state.appInfo.versionCode,
                    ),
                )
                SettingsGroupDivider()
                SettingRow(
                    title = stringResource(R.string.settings_app_license_label),
                    valueText = state.appInfo.license,
                    supportingText = state.appInfo.sourceUrl,
                    onClick = { context.openUrl(state.appInfo.sourceUrl) },
                )
                SettingsGroupDivider()
                SettingRow(
                    title = stringResource(R.string.settings_open_source_label),
                    onClick = onOpenLicenses,
                )
            }
        }
    }
}

@Composable
private fun AboutSummary(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SailensDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(SailensDimens.spaceXs),
    ) {
        Text(
            text = stringResource(R.string.title_sailens),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
