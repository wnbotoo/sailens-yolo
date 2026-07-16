package com.sailens.presentation.diagnostics

import com.sailens.presentation.config.UiFeatureFlags
import com.sailens.presentation.scene.SceneOverlayConfig
import com.sailens.presentation.scene.SceneOverlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GuidanceDiagnosticsState(
    val showDiagnostics: Boolean,
    val enabledOverlayModes: Set<SceneOverlayMode>,
    val overlayMode: SceneOverlayMode,
)

class GuidanceDiagnosticsStore(
    private val sceneOverlayConfig: SceneOverlayConfig,
    uiFeatureFlags: UiFeatureFlags,
) {
    private val _state = MutableStateFlow(
        GuidanceDiagnosticsState(
            showDiagnostics = uiFeatureFlags.showDiagnostics,
            enabledOverlayModes = sceneOverlayConfig.enabledOverlayModes,
            overlayMode = sceneOverlayConfig.effectiveInitialMode,
        )
    )
    val state: StateFlow<GuidanceDiagnosticsState> = _state.asStateFlow()

    fun setOverlayMode(overlayMode: SceneOverlayMode) {
        _state.update { it.copy(overlayMode = sceneOverlayConfig.coerceEnabledMode(overlayMode)) }
    }
}
