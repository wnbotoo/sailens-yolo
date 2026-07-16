package com.sailens.domain.model.analysis

import com.sailens.domain.model.perception.DetectedObstacle

/**
 * 场景快照
 */
data class SceneSnapshot(
    val timestamp: Long,
    val obstacles: List<DetectedObstacle>,
    val bottomCoverage: Float,
    val connectivity: WalkPathConnectivity,
    val sceneElements: SceneElements,
    val roadSafety: RoadSafetyState,
    val groundTypeChange: GroundTypeChange?,
    /** det 跟踪框接地带从可行走区抠除的像素比例（用于观察 det/sem 交叉验证影响）。 */
    val occludedPassableRatio: Float = 0f,
)

/**
 * 场景元素
 */
data class SceneElements(
    val hasIntersection: Boolean = false,
    val hasCrosswalk: Boolean = false,
    val hasTactilePaving: Boolean = false,
    val hasTrafficLight: Boolean = false,
)
