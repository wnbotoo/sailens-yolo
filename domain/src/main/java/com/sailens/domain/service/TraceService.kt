package com.sailens.domain.service

import com.sailens.domain.model.trace.FrameTrace
import com.sailens.domain.model.trace.SessionTraceMetadata
import com.sailens.domain.model.trace.SessionTraceSummary

interface TraceService {
    fun startSession(metadata: SessionTraceMetadata)
    fun recordFrame(frameTrace: FrameTrace)
    fun recordOverlayRender(
        renderedAt: Long,
        renderMs: Long,
        overlayMode: String,
        bitmapRendered: Boolean,
        sourceSequenceNumber: Long = 0,
        sourcePipelineCompletedAt: Long = 0,
        sourceAgeMs: Long = 0,
    )
    fun recordError(sessionId: String, stage: String, throwable: Throwable)
    fun finishSession(summary: SessionTraceSummary)
}
