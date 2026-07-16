package com.sailens.data.source.ml.vlm

import android.content.Context
import com.sailens.domain.model.perception.ImageFrame
import com.google.ai.edge.litert.Accelerator

/**
 * The actual VLM inference call, isolated behind a thin seam so [LiteRtVlmEngine] and the domain
 * compile WITHOUT pulling a GenAI dependency. A VLM does not use the `CompiledModel` + `run()` path
 * the CNN models use (it tokenizes, encodes the image, and decodes autoregressively), so it gets its
 * own runtime — but it reuses the same accelerator-selection policy
 * ([com.sailens.data.source.ml.session.AcceleratorSelector]).
 *
 * To enable VLM: implement [VlmRuntimeFactory] with MediaPipe LLM Inference (`LlmInference` +
 * vision modality) or LiteRT-LM, add the dependency, and inject it into [LiteRtVlmEngine]. See
 * docs/npu-litert-qnn.md → "VLM 引擎".
 */
interface VlmRuntime : AutoCloseable {
    /** One-shot generation: full text for [prompt] grounded on [image] (null = text-only). */
    fun generate(prompt: String, image: ImageFrame?): String
}

interface VlmRuntimeFactory {
    /** Whether a real runtime + model are wired (lib present, model bundle resolvable). */
    fun isAvailable(context: Context): Boolean

    /** Loads the model for [accelerator]; MUST throw if it cannot run on that backend so the
     * accelerator selector can fall back to the next one. */
    fun create(context: Context, config: VlmModelConfig, accelerator: Accelerator): VlmRuntime
}

/**
 * Default factory: no GenAI runtime wired. Keeps [LiteRtVlmEngine] gracefully unavailable (the app
 * can hide the "describe scene" action) until a real [VlmRuntimeFactory] is provided.
 */
object UnavailableVlmRuntimeFactory : VlmRuntimeFactory {
    override fun isAvailable(context: Context): Boolean = false

    override fun create(context: Context, config: VlmModelConfig, accelerator: Accelerator): VlmRuntime =
        throw IllegalStateException(
            "No VLM runtime wired. Provide a MediaPipe/LiteRT-LM VlmRuntimeFactory (see docs/npu-litert-qnn.md)."
        )
}
