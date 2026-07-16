package com.sailens.presentation.scene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import android.os.SystemClock
import com.sailens.camera.ImageFrameProvider
import com.sailens.domain.model.scene.SceneEvent
import com.sailens.domain.model.scene.SceneResult
import com.sailens.domain.service.LogService
import com.sailens.domain.service.TraceService
import com.sailens.domain.usecase.scene.StartSceneAnalysisUseCase
import com.sailens.domain.usecase.scene.StopSceneAnalysisUseCase
import com.sailens.presentation.diagnostics.GuidanceDiagnosticsStore
import com.sailens.presentation.device.HapticManager
import com.sailens.presentation.device.SpeechManager
import com.sailens.presentation.ext.OverlayBitmapRenderer
import com.sailens.presentation.settings.GuidanceFeedbackSettings
import com.sailens.presentation.settings.GuidanceSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SceneAnalysisViewModel"

/**
 * Drives the live guidance screen only: starting/stopping analysis, speech/haptic feedback, and
 * overlay rendering. Navigation and trace-replay/diagnostics tooling live elsewhere
 * (Nav3 back stack + the debug-only `TraceReplayViewModel`).
 */
class SceneAnalysisViewModel(
    private val imageFrameProvider: ImageFrameProvider,
    private val startSceneAnalysisUseCase: StartSceneAnalysisUseCase,
    private val stopSceneAnalysisUseCase: StopSceneAnalysisUseCase,
    private val hapticManager: HapticManager,
    private val speechManager: SpeechManager,
    private val logger: LogService,
    private val traceService: TraceService,
    private val sceneOverlayConfig: SceneOverlayConfig,
    private val guidanceSettingsStore: GuidanceSettingsStore,
    private val guidanceDiagnosticsStore: GuidanceDiagnosticsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SceneAnalysisUiState(
            isSpeechEnabled = guidanceSettingsStore.settings.value.speechEnabled,
            isHapticsEnabled = guidanceSettingsStore.settings.value.hapticsEnabled,
            showDiagnostics = guidanceDiagnosticsStore.state.value.showDiagnostics,
            enabledOverlayModes = guidanceDiagnosticsStore.state.value.enabledOverlayModes,
            overlayMode = guidanceDiagnosticsStore.state.value.overlayMode,
        )
    )
    val uiState: StateFlow<SceneAnalysisUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SceneAnalysisUiEffect>()
    val uiEffect: SharedFlow<SceneAnalysisUiEffect> = _uiEffect.asSharedFlow()

    private var analysisJob: Job? = null
    private var releaseJob: Job? = null
    private var overlayRenderJob: Job? = null
    private val releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var frameCount: Long = 0
    private var latestSceneResult: SceneResult? = null
    private var lastOverlayRenderTimeMs: Long = 0L
    private var overlayRenderRequestId: Long = 0L
    private val overlayBitmapRenderer = OverlayBitmapRenderer()

    // State machine to prevent concurrent release/analysis initialization
    @Volatile
    private var isReleasingResources = false

    init {
        if (guidanceSettingsStore.settings.value.speechEnabled) {
            initializeSpeech()
        }
        viewModelScope.launch {
            guidanceSettingsStore.settings.collect { settings ->
                applyFeedbackSettings(settings)
            }
        }
        viewModelScope.launch {
            guidanceDiagnosticsStore.state
                .map { it.overlayMode }
                .distinctUntilChanged()
                .collect { overlayMode ->
                    applyOverlayMode(overlayMode)
                }
        }
    }

    private fun initializeSpeech() {
        speechManager.initialize {
            logger.debug(TAG, "SpeechManager initialized")
            _uiState.update { it.copy(isSpeechReady = true, errorMessage = null) }
        }
    }

    fun toggleAnalysis() {
        if (_uiState.value.isRunning) {
            stopSceneAnalysis()
        } else {
            _uiState.update { it.copy(isLoading = true) }
            startSceneAnalysis()
        }
    }

    fun setSpeechEnabled(enabled: Boolean) {
        guidanceSettingsStore.setSpeechEnabled(enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        guidanceSettingsStore.setHapticsEnabled(enabled)
    }

    private fun applyFeedbackSettings(settings: GuidanceFeedbackSettings) {
        val previous = _uiState.value
        _uiState.update {
            it.copy(
                isSpeechEnabled = settings.speechEnabled,
                isHapticsEnabled = settings.hapticsEnabled,
            )
        }

        if (!settings.speechEnabled && previous.isSpeechEnabled) {
            speechManager.stop()
        } else if (settings.speechEnabled && !previous.isSpeechEnabled) {
            initializeSpeech()
        }

        if (!settings.hapticsEnabled && previous.isHapticsEnabled) {
            hapticManager.cancel()
        }
    }

    fun setOverlayMode(overlayMode: SceneOverlayMode) {
        guidanceDiagnosticsStore.setOverlayMode(overlayMode)
    }

    private fun applyOverlayMode(overlayMode: SceneOverlayMode) {
        lastOverlayRenderTimeMs = 0L
        val effectiveOverlayMode = sceneOverlayConfig.coerceEnabledMode(overlayMode)
        if (effectiveOverlayMode == SceneOverlayMode.OFF) {
            overlayRenderRequestId++
            overlayRenderJob?.cancel()
            overlayRenderJob = null
            _uiState.update {
                it.copy(
                    overlayMode = SceneOverlayMode.OFF,
                    segMask = null,
                    maskSourceAgeMs = 0L,
                    obstacleDetections = emptyList(),
                )
            }
            return
        }

        val result = latestSceneResult
        _uiState.update {
            it.copy(
                overlayMode = effectiveOverlayMode,
                segMask = null,
                maskSourceAgeMs = 0L,
                obstacleDetections = result?.obstacleDetectionsForOverlay(effectiveOverlayMode).orEmpty(),
            )
        }
        scheduleOverlayRender(result, effectiveOverlayMode, force = true)
    }

    private fun startSceneAnalysis() {
        frameCount = 0
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            // Wait for ongoing resource release to complete (with timeout protection)
            var waitTime = 0L
            val maxWaitMs = 2000L
            while (isReleasingResources && waitTime < maxWaitMs) {
                delay(50)
                waitTime += 50
            }
            if (isReleasingResources) {
                logger.warning(TAG, "Resource release timeout; proceeding with analysis anyway")
            }

            // collectLatest is often used for high-frequency data, discarding previous incomplete processing
            startSceneAnalysisUseCase(imageFrameProvider.frames).onStart {
                _uiState.update {
                    it.copy(isInitializing = false, isRunning = true, isLoading = false)
                }
                logger.info(TAG, "Scene analysis started")
            }.catch { e ->
                logger.error(TAG, "Error in scene analysis", e)
                speechManager.stop()
                hapticManager.cancel()
                latestSceneResult = null
                // Keep the raw cause in state + logs (logger.error above), but surface only a
                // friendly, localized message to the user via the status card
                // (error_analysis_start_failed). No raw-exception toast: it leaks internals and is
                // not useful to a screen-reader user; the persistent error card already communicates it.
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        isLoading = false,
                        segMask = null,
                        obstacleDetections = emptyList(),
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
            }.collectLatest { result ->
                frameCount++
                latestSceneResult = result
                val overlayMode = _uiState.value.overlayMode
                val events = result.events
                val latestSceneDebugInfo = result.debugInfo.takeIf {
                    sceneOverlayConfig.enableDebugPanel && guidanceDiagnosticsStore.state.value.showDiagnostics
                }
                _uiState.update {
                    it.copy(
                        frameDisplayWidth = result.frameDisplayWidth,
                        frameDisplayHeight = result.frameDisplayHeight,
                        obstacleDetections = result.obstacleDetectionsForOverlay(overlayMode),
                        latestSceneDebugInfo = latestSceneDebugInfo,
                        lastEvents = events,
                    )
                }
                scheduleOverlayRender(result, overlayMode)
                if (frameCount == 1L) {
                    logger.info(
                        TAG,
                        "First frame diagnostics",
                        mapOf(
                            "semanticProvider" to (result.debugInfo?.semanticProvider ?: "unknown"),
                            "obstacleProvider" to (result.debugInfo?.obstacleProvider ?: "unknown"),
                            "perceptionProfile" to (result.debugInfo?.perceptionProfile ?: "unknown"),
                            "trackedObstacles" to result.obstacles.size,
                            "rawDetections" to result.obstacleDetections.size,
                        )
                    )
                }
                onSceneEvents(events)
            }
        }
    }

    private fun onSceneEvents(events: List<SceneEvent>) {
        if (events.isEmpty()) return
        val primaryEvent = events.first()
        val state = _uiState.value

        logger.debug(TAG, "Scene events generated, ${primaryEvent.messageKey}", mapOf("count" to events.size))

        if (state.isSpeechEnabled) {
            speechManager.speak(primaryEvent)
        }

        if (state.isHapticsEnabled) {
            hapticManager.trigger(primaryEvent)
        }
    }

    private fun stopSceneAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        overlayRenderRequestId++
        overlayRenderJob?.cancel()
        overlayRenderJob = null
        latestSceneResult = null
        stopSceneAnalysisUseCase()
        speechManager.stop()
        hapticManager.cancel()
        _uiState.update {
            it.copy(
                isRunning = false,
                isInitializing = false,
                isLoading = false,
                segMask = null,
                maskSourceAgeMs = 0L,
                frameDisplayWidth = null,
                frameDisplayHeight = null,
                obstacleDetections = emptyList(),
                latestSceneDebugInfo = null,
            )
        }
    }

    override fun onCleared() {
        analysisJob?.cancel()
        analysisJob = null
        overlayRenderJob?.cancel()
        overlayRenderJob = null
        stopSceneAnalysisUseCase()
        speechManager.stop()
        hapticManager.cancel()
        releaseSceneAnalysisResources()
        speechManager.release()
        super.onCleared()
    }

    private fun scheduleOverlayRender(
        result: SceneResult?,
        overlayMode: SceneOverlayMode,
        force: Boolean = false,
    ) {
        if (result == null || !sceneOverlayConfig.isModeEnabled(overlayMode) || !overlayMode.rendersBitmap()) {
            overlayRenderRequestId++
            overlayRenderJob?.cancel()
            overlayRenderJob = null
            if (_uiState.value.segMask != null) {
                _uiState.update {
                    it.copy(
                        segMask = null,
                        maskSourceAgeMs = 0L,
                    )
                }
            }
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastOverlayRenderTimeMs < sceneOverlayConfig.bitmapRenderIntervalMs) {
            return
        }
        lastOverlayRenderTimeMs = now

        overlayRenderJob?.cancel()
        val requestId = ++overlayRenderRequestId
        overlayRenderJob = viewModelScope.launch {
            val renderStartAt = SystemClock.elapsedRealtime()
            val bitmap = withContext(Dispatchers.Default) {
                result.toOverlayBitmap(overlayMode)
            }
            val renderCompletedAt = SystemClock.elapsedRealtime()
            val renderCompletedWallClockAt = System.currentTimeMillis()
            val sourceAgeMs = overlaySourceAgeMs(
                sourcePipelineCompletedAt = result.pipelineCompletedAt,
                renderedAt = renderCompletedWallClockAt,
            )
            traceService.recordOverlayRender(
                renderedAt = renderCompletedWallClockAt,
                renderMs = renderCompletedAt - renderStartAt,
                overlayMode = overlayMode.name,
                bitmapRendered = bitmap != null,
                sourceSequenceNumber = result.sequenceNumber,
                sourcePipelineCompletedAt = result.pipelineCompletedAt,
                sourceAgeMs = sourceAgeMs,
            )
            if (_uiState.value.overlayMode == overlayMode && requestId == overlayRenderRequestId) {
                _uiState.update {
                    it.copy(
                        segMask = bitmap,
                        maskSourceAgeMs = sourceAgeMs,
                    )
                }
            }
        }
    }

    private fun releaseSceneAnalysisResources() {
        releaseJob?.cancel()
        isReleasingResources = true
        releaseJob = releaseScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    runCatching {
                        stopSceneAnalysisUseCase.release()
                    }.onFailure { error ->
                        logger.error(TAG, "Error releasing scene analysis resources", error)
                    }
                }
            } finally {
                isReleasingResources = false
                logger.info(TAG, "Scene analysis resources released")
            }
        }.also { job ->
            job.invokeOnCompletion { cause ->
                if (cause != null) {
                    logger.error(TAG, "Release job failed", cause)
                    isReleasingResources = false
                }
                releaseScope.cancel()
            }
        }
    }

    private fun SceneResult.obstacleDetectionsForOverlay(overlayMode: SceneOverlayMode) =
        if (!sceneOverlayConfig.isModeEnabled(overlayMode)) {
            emptyList()
        } else {
            when (overlayMode) {
                SceneOverlayMode.DETECTION_BOXES -> obstacleDetections
                SceneOverlayMode.OFF,
                SceneOverlayMode.PASSABLE_AREA_MASK,
                SceneOverlayMode.SEMANTIC_CLASS_MASK -> emptyList()
            }
        }

    private fun SceneResult.toOverlayBitmap(overlayMode: SceneOverlayMode): Bitmap? {
        val sourceAspectRatio = frameDisplayWidth?.let { width ->
            frameDisplayHeight?.takeIf { it > 0 }?.let { height ->
                width.toFloat() / height
            }
        }
        return when (overlayMode) {
            SceneOverlayMode.PASSABLE_AREA_MASK ->
                passableMask?.let { overlayBitmapRenderer.renderPassableMask(it, sourceAspectRatio) }
            SceneOverlayMode.SEMANTIC_CLASS_MASK ->
                segmentationMask?.let { overlayBitmapRenderer.renderSemanticClasses(it, sourceAspectRatio) }
            SceneOverlayMode.OFF,
            SceneOverlayMode.DETECTION_BOXES -> null
        }
    }

    private fun SceneOverlayMode.rendersBitmap(): Boolean {
        return when (this) {
            SceneOverlayMode.PASSABLE_AREA_MASK,
            SceneOverlayMode.SEMANTIC_CLASS_MASK -> true
            SceneOverlayMode.OFF,
            SceneOverlayMode.DETECTION_BOXES -> false
        }
    }

    private fun overlaySourceAgeMs(sourcePipelineCompletedAt: Long, renderedAt: Long): Long {
        if (sourcePipelineCompletedAt <= 0L) return 0L
        return (renderedAt - sourcePipelineCompletedAt).coerceAtLeast(0L)
    }
}
