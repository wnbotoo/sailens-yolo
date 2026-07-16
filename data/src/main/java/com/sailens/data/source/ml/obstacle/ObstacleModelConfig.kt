package com.sailens.data.source.ml.obstacle

import com.sailens.data.source.ml.ModelAcceleratorSelectionMode
import com.sailens.data.source.ml.ModelAcceleratorBackend
import com.sailens.data.source.ml.ModelInputDataType
import com.sailens.data.source.ml.ModelInputQuantization
import com.sailens.data.source.ml.ResizeFilter

/**
 * Obstacle detection model contract. The runtime resolves the detection head layout
 * (raw attribute-major `[1, 4+classCount, N]` vs end-to-end `[1, N, 6]`) from the output tensor
 * shape; only the class count is part of the configured contract.
 */
data class ObstacleModelConfig(
    val classCount: Int = 80,
    val inputDataType: ModelInputDataType = ModelInputDataType.AUTO,
    val inputQuantization: ModelInputQuantization = ModelInputQuantization(),
    val preferNativeYuvPreprocessing: Boolean = true,
    val resizeFilter: ResizeFilter = ResizeFilter.NEAREST,
    val acceleratorSelectionMode: ModelAcceleratorSelectionMode = ModelAcceleratorSelectionMode.EXPLICIT,
    // Bundled vision models run on the GPU; the NPU is reserved for the future VLM. ModelCatalog has no NPU asset.
    val acceleratorBackend: ModelAcceleratorBackend = ModelAcceleratorBackend.GPU,
)
