package com.sailens.domain.processor.perception

import com.sailens.domain.model.perception.SegmentationAnalysis
import com.sailens.domain.model.perception.SegmentationAnalysisStats
import com.sailens.domain.model.perception.SegmentationMask

interface SegmentationAnalysisProcessor {
    fun analyze(
        segmentation: SegmentationMask,
        stats: SegmentationAnalysisStats? = null,
    ): SegmentationAnalysis

    fun reset()
}
