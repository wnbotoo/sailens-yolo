package com.sailens.domain.usecase.trace

import com.sailens.domain.model.trace.TraceSessionDescriptor
import com.sailens.domain.service.TraceReplayService

class ListTraceSessionsUseCase(
    private val traceReplayService: TraceReplayService,
) {
    operator fun invoke(): List<TraceSessionDescriptor> = traceReplayService.listSessions()
}

