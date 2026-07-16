package com.sailens.domain.repository

import com.sailens.domain.model.common.DistanceLevel
import com.sailens.domain.model.common.NormalizedRect

/**
 * 深度估计仓库接口
 */
interface DepthRepository {
    fun estimateDistance(boundingBox: NormalizedRect): DistanceLevel
}