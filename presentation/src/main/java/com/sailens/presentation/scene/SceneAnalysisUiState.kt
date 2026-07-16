package com.sailens.presentation.scene

import android.graphics.Bitmap
import com.sailens.domain.model.perception.ObstacleDetection
import com.sailens.domain.model.scene.SceneEvent
import com.sailens.domain.model.scene.SceneDebugInfo

data class SceneAnalysisUiState(
    val isInitializing: Boolean = false,
    val isRunning: Boolean = false,
    val isLoading: Boolean = false,
    val isPaused: Boolean = false,
    val isSpeechEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isSpeechReady: Boolean = false,
    val showDiagnostics: Boolean = false,
    val enabledOverlayModes: Set<SceneOverlayMode> = emptySet(),
    val overlayMode: SceneOverlayMode = SceneOverlayMode.PASSABLE_AREA_MASK,
    val segMask: Bitmap? = null,
    val maskSourceAgeMs: Long = 0L,
    val frameDisplayWidth: Int? = null,
    val frameDisplayHeight: Int? = null,
    val obstacleDetections: List<ObstacleDetection> = emptyList(),
    val latestSceneDebugInfo: SceneDebugInfo? = null,
    val lastEvents: List<SceneEvent> = emptyList(),
    val errorMessage: String? = null,
)
