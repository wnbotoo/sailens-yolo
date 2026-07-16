package com.sailens.presentation.di

import com.sailens.presentation.trace.TraceReplayViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Debug-only DI: the trace-replay ViewModel exists only in the debug variant. */
val presentationDebugModule: Module = module {
    viewModel {
        TraceReplayViewModel(
            logger = get(),
            listTraceSessionsUseCase = get(),
            loadTraceReplayReportUseCase = get(),
            loadLatestTraceReplayReportUseCase = get(),
            evaluateTraceReplayBudgetUseCase = get(),
        )
    }
}
