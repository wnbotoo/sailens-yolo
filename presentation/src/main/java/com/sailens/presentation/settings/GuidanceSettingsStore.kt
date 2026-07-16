package com.sailens.presentation.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GuidanceFeedbackSettings(
    val speechEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
)

class GuidanceSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<GuidanceFeedbackSettings> = _settings.asStateFlow()

    fun setSpeechEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SPEECH_ENABLED, enabled).apply()
        _settings.update { it.copy(speechEnabled = enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
        _settings.update { it.copy(hapticsEnabled = enabled) }
    }

    private fun readSettings(): GuidanceFeedbackSettings {
        return GuidanceFeedbackSettings(
            speechEnabled = preferences.getBoolean(KEY_SPEECH_ENABLED, true),
            hapticsEnabled = preferences.getBoolean(KEY_HAPTICS_ENABLED, true),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "guidance_settings"
        const val KEY_SPEECH_ENABLED = "speech_enabled"
        const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    }
}
