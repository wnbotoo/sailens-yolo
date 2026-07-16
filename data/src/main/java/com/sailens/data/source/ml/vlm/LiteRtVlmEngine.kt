package com.sailens.data.source.ml.vlm

import android.content.Context
import android.os.SystemClock
import com.sailens.data.source.ml.session.AcceleratorSelection
import com.sailens.data.source.ml.session.AcceleratorSelector
import com.sailens.domain.model.perception.MlRuntimeInfo
import com.sailens.domain.repository.SceneDescriber
import com.sailens.domain.repository.SceneDescription
import com.sailens.domain.repository.SceneDescriptionRequest
import com.sailens.domain.service.LogService
import com.google.ai.edge.litert.Accelerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * VLM scene-description engine. Reuses the CNN path's accelerator-selection policy
 * ([AcceleratorSelector]) but runs through a [VlmRuntime] (LLM/GenAI API) instead of a
 * `CompiledModel`, because a VLM is autoregressive, not a single forward pass.
 *
 * The actual LLM library is injected via [runtimeFactory]; with the default
 * [UnavailableVlmRuntimeFactory] the engine reports `isReady == false` and [initialize] throws, so
 * the app can simply hide the "describe scene" action until a model is wired. See
 * docs/npu-litert-qnn.md → "VLM 引擎" for the MediaPipe/LiteRT-LM runtime wiring.
 */
class LiteRtVlmEngine(
    private val context: Context,
    private val config: VlmModelConfig,
    private val runtimeFactory: VlmRuntimeFactory = UnavailableVlmRuntimeFactory,
    private val logService: LogService? = null,
) : SceneDescriber {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val singleThreadDispatcher = Dispatchers.IO.limitedParallelism(1)

    private var runtime: VlmRuntime? = null
    private var backendLabel: String = MlRuntimeInfo.UNKNOWN

    @Volatile
    private var _isReady = false
    override val isReady: Boolean get() = _isReady

    override suspend fun initialize() {
        if (_isReady) return

        withContext(singleThreadDispatcher) {
            cleanupInternal()
            check(runtimeFactory.isAvailable(context)) {
                "VLM runtime unavailable (no GenAI library/model wired)."
            }
            // Same accelerator selection + fallback as the CNN models; only `build` differs (creates
            // a VlmRuntime instead of a CompiledModel session).
            val resolved = AcceleratorSelector.select(
                selection = AcceleratorSelection(
                    mode = config.acceleratorSelectionMode,
                    preferredBackend = config.acceleratorBackend,
                    fallbackOrder = ACCELERATOR_FALLBACK_ORDER,
                ),
                logTag = TAG,
                modelLabel = "VLM",
                logService = logService,
            ) { accelerator ->
                runtimeFactory.create(context, config, accelerator)
            }
            runtime = resolved.value
            backendLabel = resolved.selectionLabel
            _isReady = true
            logService?.info(TAG, "VLM engine initialized with ${resolved.accelerator}")
        }
    }

    override suspend fun describe(request: SceneDescriptionRequest): Result<SceneDescription> {
        val activeRuntime = runtime
            ?: return Result.failure(IllegalStateException("VLM engine not initialized"))

        return withContext(singleThreadDispatcher) {
            if (!isActive) return@withContext Result.failure(CancellationException("Coroutine cancelled"))
            runCatching {
                val start = SystemClock.uptimeMillis()
                val text = activeRuntime.generate(buildPrompt(request.userPrompt), request.frame)
                SceneDescription(
                    text = text.trim(),
                    backend = backendLabel,
                    latencyMs = SystemClock.uptimeMillis() - start,
                )
            }
        }
    }

    override suspend fun release() {
        withContext(singleThreadDispatcher) { cleanupInternal() }
    }

    private fun cleanupInternal() {
        runCatching { runtime?.close() }
        runtime = null
        backendLabel = MlRuntimeInfo.UNKNOWN
        _isReady = false
    }

    private fun buildPrompt(userPrompt: String?): String =
        if (userPrompt.isNullOrBlank()) config.systemPrompt
        else "${config.systemPrompt}\n\n$userPrompt"

    private companion object {
        const val TAG = "LiteRtVlmEngine"

        // VLM benefits most from the NPU; degrade to GPU then CPU on devices without it.
        val ACCELERATOR_FALLBACK_ORDER = listOf(
            Accelerator.NPU,
            Accelerator.GPU,
            Accelerator.CPU,
        )
    }
}
