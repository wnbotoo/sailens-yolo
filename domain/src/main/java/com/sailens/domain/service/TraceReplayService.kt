package com.sailens.domain.service

import com.sailens.domain.model.trace.TraceSessionDescriptor

interface TraceReplayService {
    fun listSessions(): List<TraceSessionDescriptor>
    fun readSessionLines(sessionId: String): List<String>
}

