package com.sailens.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sailens.domain.model.common.PerceptionProfile
import com.sailens.domain.processor.perception.PerceptionProfileManager
import com.sailens.presentation.diagnostics.GuidanceDiagnosticsState
import com.sailens.presentation.diagnostics.GuidanceDiagnosticsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SettingsUiState(
    val appInfo: AppInfo,
    val feedbackSettings: GuidanceFeedbackSettings,
    val perceptionProfile: PerceptionProfile,
    val diagnostics: GuidanceDiagnosticsState,
)

class SettingsViewModel(
    private val appInfo: AppInfo,
    private val guidanceSettingsStore: GuidanceSettingsStore,
    private val perceptionSettingsStore: PerceptionSettingsStore,
    private val perceptionProfileManager: PerceptionProfileManager,
    private val guidanceDiagnosticsStore: GuidanceDiagnosticsStore,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        guidanceSettingsStore.settings,
        perceptionSettingsStore.profile,
        guidanceDiagnosticsStore.state,
    ) { settings, perceptionProfile, diagnostics ->
        SettingsUiState(appInfo, settings, perceptionProfile, diagnostics)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(
                appInfo = appInfo,
                feedbackSettings = guidanceSettingsStore.settings.value,
                perceptionProfile = perceptionSettingsStore.profile.value,
                diagnostics = guidanceDiagnosticsStore.state.value,
            ),
        )

    fun setSpeechEnabled(enabled: Boolean) {
        guidanceSettingsStore.setSpeechEnabled(enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        guidanceSettingsStore.setHapticsEnabled(enabled)
    }

    fun setPerceptionProfile(profile: PerceptionProfile) {
        // store 负责持久化，manager 负责让下一次导航会话用上新挡位
        perceptionSettingsStore.setProfile(profile)
        perceptionProfileManager.selectProfile(profile)
    }
}
