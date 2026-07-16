package com.sailens.domain.model.analysis

import com.sailens.domain.model.common.DirectionBias

/**
 * Stateless per-frame connectivity statistics.
 *
 * ConnectivityChecker owns debouncing, confidence thresholds, and severity mapping.
 */
data class ConnectivityStats(
    val validLayers: Int,
    val totalLayers: Int,
    val widthRetentionAvg: Float,
    val widthRetentionP25: Float,
    val widthSlope: Float,
    val floodReachRatio: Float,
    val floodWidthRetentionP25: Float,
    val floodVisitedRatio: Float,
    val suggestedBias: DirectionBias?,
)
