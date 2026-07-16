package com.sailens.data.service

import com.sailens.domain.model.trace.FrameTrace
import com.sailens.domain.model.trace.SessionTraceMetadata
import com.sailens.domain.model.trace.SessionTraceSummary
import com.sailens.domain.service.TraceService

object NoOpTraceService : TraceService {
    override fun startSession(metadata: SessionTraceMetadata) = Unit

    override fun recordFrame(frameTrace: FrameTrace) = Unit

    override fun recordOverlayRender(
        renderedAt: Long,
        renderMs: Long,
        overlayMode: String,
        bitmapRendered: Boolean,
        sourceSequenceNumber: Long,
        sourcePipelineCompletedAt: Long,
        sourceAgeMs: Long,
    ) = Unit

    override fun recordError(sessionId: String, stage: String, throwable: Throwable) = Unit

    override fun finishSession(summary: SessionTraceSummary) = Unit
}
