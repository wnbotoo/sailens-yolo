package com.sailens.domain.repository

import com.sailens.domain.model.perception.ImageFrame

/**
 * On-demand scene description via a vision-language model (VLM).
 *
 * This is a SEPARATE, low-frequency path from the realtime perception pipeline (sem/det). The
 * realtime models answer "is something in my way right now" every frame; the [SceneDescriber]
 * answers "what is in front of me" when the user asks — seconds-scale, not per-frame.
 *
 * The domain stays runtime-agnostic: the concrete engine (LiteRT / MediaPipe LLM Inference) and its
 * accelerator (NPU/GPU/CPU) live in the data layer.
 */
interface SceneDescriber {

    /** True once a model is loaded and [describe] can run. */
    val isReady: Boolean

    /** Loads the VLM. Throws if no model/runtime is available (caller decides if that is fatal). */
    suspend fun initialize()

    /** Describes [request]'s frame. One-shot (no streaming) for now. */
    suspend fun describe(request: SceneDescriptionRequest): Result<SceneDescription>

    /** Releases the model and frees native resources. */
    suspend fun release()
}

/**
 * @param frame the image to describe.
 * @param userPrompt optional question ("是不是有台阶?"); when null the engine uses its system prompt.
 */
data class SceneDescriptionRequest(
    val frame: ImageFrame,
    val userPrompt: String? = null,
)

/**
 * @param text the generated description.
 * @param backend the accelerator selection trace (e.g. "prefer_backend[preferred=NPU; ...]").
 * @param latencyMs wall-clock generation time.
 */
data class SceneDescription(
    val text: String,
    val backend: String,
    val latencyMs: Long,
)
