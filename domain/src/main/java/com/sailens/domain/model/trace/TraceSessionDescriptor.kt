package com.sailens.domain.model.trace

data class TraceSessionDescriptor(
    val sessionId: String,
    val fileName: String,
    val lastModifiedAt: Long,
)

