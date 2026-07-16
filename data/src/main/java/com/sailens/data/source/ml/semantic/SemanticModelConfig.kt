package com.sailens.data.source.ml.semantic

import com.sailens.data.source.ml.ModelAcceleratorSelectionMode
import com.sailens.data.source.ml.ModelAcceleratorBackend
import com.sailens.data.source.ml.ModelInputDataType
import com.sailens.data.source.ml.ModelInputQuantization
import com.sailens.data.source.ml.ResizeFilter

/**
 * Semantic segmentation model contract. Spatial input/output dimensions are read from the
 * TFLite tensor metadata at runtime, so replacing the asset with a lower
 * resolution model does not require changing width/height constants.
 */
data class SemanticModelConfig(
    val outputChannels: Int = 19,
    val inputDataType: ModelInputDataType = ModelInputDataType.AUTO,
    val inputQuantization: ModelInputQuantization = ModelInputQuantization(),
    val preferNativeYuvPreprocessing: Boolean = true,
    val resizeFilter: ResizeFilter = ResizeFilter.NEAREST,
    val acceleratorSelectionMode: ModelAcceleratorSelectionMode = ModelAcceleratorSelectionMode.EXPLICIT,
    // Bundled vision models run on the GPU; the NPU is reserved for the future VLM. ModelCatalog has no NPU asset.
    val acceleratorBackend: ModelAcceleratorBackend = ModelAcceleratorBackend.GPU,
)
