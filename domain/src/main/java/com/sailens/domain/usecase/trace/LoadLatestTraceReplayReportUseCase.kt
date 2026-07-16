package com.sailens.domain.usecase.trace

import com.sailens.domain.model.trace.TraceReplayReport

class LoadLatestTraceReplayReportUseCase(
    private val listTraceSessionsUseCase: ListTraceSessionsUseCase,
    private val loadTraceReplayReportUseCase: LoadTraceReplayReportUseCase,
) {
    operator fun invoke(): TraceReplayReport? {
        val latestSession = listTraceSessionsUseCase().firstOrNull() ?: return null
        return loadTraceReplayReportUseCase(latestSession.sessionId)
    }
}

