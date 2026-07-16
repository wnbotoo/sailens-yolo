package com.sailens.domain.processor.decision

import com.sailens.domain.config.AnalysisConfig
import com.sailens.domain.model.analysis.GroundTypeChange
import com.sailens.domain.model.analysis.RoadSafetyState
import com.sailens.domain.model.analysis.SceneSnapshot
import com.sailens.domain.model.common.DirectionBias
import com.sailens.domain.model.common.DirectionZone
import com.sailens.domain.model.common.DistanceLevel
import com.sailens.domain.model.common.EventCategory
import com.sailens.domain.model.common.EventPriority
import com.sailens.domain.model.common.GroundType
import com.sailens.domain.model.common.ObstacleCategory
import com.sailens.domain.model.common.Severity
import com.sailens.domain.model.common.UrgencyLevel
import com.sailens.domain.model.scene.SceneEvent
import com.sailens.domain.model.perception.DetectedObstacle

/**
 * 事件生成器
 */
class EventGenerator(
    private val config: AnalysisConfig = AnalysisConfig(),
) {
    private var wasOnRoad = false

    private companion object {
        private const val MIN_HARD_BLOCKED_CONFIDENCE = 0.75f
        private const val MAX_HARD_BLOCKED_VERTICAL_REACH = 0.55f
        private const val MAX_HARD_BLOCKED_FLOOD_REACH = 0.12f
        private const val MAX_HARD_BLOCKED_WIDTH_RETENTION = 0.35f
        private const val MIN_OBSTACLE_CONFIDENCE_TO_ANNOUNCE = 0.55f
        private const val MIN_PERSON_CENTER_AREA_RATIO = 0.012f
        private const val MIN_BICYCLE_CENTER_AREA_RATIO = 0.012f
        private const val MIN_VEHICLE_CENTER_AREA_RATIO = 0.016f
        private const val MIN_STATIC_CENTER_AREA_RATIO = 0.030f
        private const val MIN_ROADSIDE_STATIC_CENTER_AREA_RATIO = 0.050f
        private const val MIN_PERSON_SIDE_AREA_RATIO = 0.016f
        private const val MIN_SIDE_OBSTACLE_AREA_RATIO = 0.035f
        private const val MIN_ROADSIDE_STATIC_SIDE_AREA_RATIO = 0.060f
        private const val MIN_PERSON_CENTER_BOTTOM_Y = 0.56f
        private const val MIN_DYNAMIC_CENTER_BOTTOM_Y = 0.55f
        private const val MIN_STATIC_CENTER_BOTTOM_Y = 0.65f
        private const val MIN_ROADSIDE_STATIC_BOTTOM_Y = 0.72f
        private const val MIN_PERSON_SIDE_BOTTOM_Y = 0.54f
        private const val MIN_SIDE_OBSTACLE_BOTTOM_Y = 0.65f
        private val ROADSIDE_STATIC_CLASS_NAMES = setOf(
            "potted_plant",
            "stop_sign",
            "bench",
            "chair",
        )
    }

    fun generate(snapshot: SceneSnapshot, now: Long): List<SceneEvent> {
        val events = mutableListOf<SceneEvent>()

        // 1. 阻塞 / 复杂路况事件
        if (snapshot.connectivity.isBlocked) {
            if (isHardBlocked(snapshot)) {
                events.add(createBlockedEvent(snapshot, now))
            } else {
                events.add(createPathComplexEvent(snapshot, now))
            }
        }

        // 2. 收窄事件
        if (config.enableNarrowingEvents && snapshot.connectivity.isNarrowing && !snapshot.connectivity.isBlocked) {
            events.add(createNarrowingEvent(snapshot, now))
        }

        // 3. 方向建议
        if (config.enableDirectionAdviceEvents) {
            snapshot.connectivity.suggestedBias?.let {
                events.add(createDirectionAdviceEvent(it, now))
            }
        }

        // 4.  障碍物事件
        val significantObstacles = snapshot.obstacles.filter(::shouldAnnounceObstacle)
        if (significantObstacles.isNotEmpty()) {
            events.addAll(createObstacleEvents(significantObstacles, now))
        }

        // 5. 路口事件
        val emitsIntersection = config.enableIntersectionEvents && snapshot.sceneElements.hasIntersection
        if (emitsIntersection) {
            events.add(createIntersectionEvent(now))
        } else if (config.enableTrafficLightEvents && snapshot.sceneElements.hasTrafficLight) {
            events.add(createTrafficLightEvent(now))
        }

        // 6. 道路安全事件默认不播，避免把不稳定的车道/机动车道识别当作确定提示。
        if (config.enableRoadWarningEvents && snapshot.roadSafety.isDangerous) {
            events.add(createRoadWarningEvent(snapshot.roadSafety, now))
        }

        if (config.enableRoadExitEvents && wasOnRoad && !snapshot.roadSafety.isOnRoad) {
            events.add(createRoadExitEvent(now))
        }
        wasOnRoad = snapshot.roadSafety.isOnRoad

        // 7. 地面变化事件默认不播，当前只作为分析/debug 信号保留。
        if (config.enableGroundChangeEvents) {
            snapshot.groundTypeChange?.let {
                events.add(createGroundChangeEvent(it, now))
            }
        }

        return events
    }

    private fun createBlockedEvent(snapshot: SceneSnapshot, now: Long): SceneEvent {
        val priority = when (snapshot.connectivity.blockageSeverity) {
            Severity.SEVERE -> EventPriority.CRITICAL
            Severity.MODERATE -> EventPriority.HIGH
            else -> EventPriority.MEDIUM
        }

        return SceneEvent(
            timestamp = now,
            category = EventCategory.BLOCKED,
            priority = priority,
            messageKey = "event_blocked",
            expiresAt = now + 5000,
            dedupeKey = "blocked",
            confidence = snapshot.connectivity.blockageConfidence,
            severity = snapshot.connectivity.blockageSeverity
        )
    }

    private fun isHardBlocked(snapshot: SceneSnapshot): Boolean {
        val connectivity = snapshot.connectivity
        if (connectivity.blockageSeverity != Severity.SEVERE) return false
        if (connectivity.blockageConfidence < MIN_HARD_BLOCKED_CONFIDENCE) return false
        return connectivity.verticalReachRatio <= MAX_HARD_BLOCKED_VERTICAL_REACH &&
            connectivity.floodReachRatio <= MAX_HARD_BLOCKED_FLOOD_REACH &&
            connectivity.widthRetentionP25 <= MAX_HARD_BLOCKED_WIDTH_RETENTION
    }

    private fun createPathComplexEvent(snapshot: SceneSnapshot, now: Long): SceneEvent {
        return SceneEvent(
            timestamp = now,
            category = EventCategory.PATH_COMPLEX,
            priority = EventPriority.MEDIUM,
            messageKey = "event_path_complex",
            expiresAt = now + 5000,
            dedupeKey = "path_complex",
            confidence = snapshot.connectivity.blockageConfidence,
            severity = snapshot.connectivity.blockageSeverity
        )
    }

    private fun shouldAnnounceObstacle(obstacle: DetectedObstacle): Boolean {
        if (!obstacle.isStable(minFrames = 3)) return false
        if (obstacle.confidence < MIN_OBSTACLE_CONFIDENCE_TO_ANNOUNCE) return false
        if (obstacle.distance == DistanceLevel.FAR && obstacle.urgency < UrgencyLevel.CRITICAL) return false

        return when (obstacle.zone) {
            DirectionZone.CENTER -> {
                if (!isCloseEnoughForCenterAnnouncement(obstacle)) return false
                obstacle.urgency >= UrgencyLevel.CRITICAL ||
                    obstacle.distance == DistanceLevel.NEAR ||
                    obstacle.areaRatio >= centerAreaThreshold(obstacle)
            }

            DirectionZone.FRONT_LEFT,
            DirectionZone.FRONT_RIGHT -> {
                if (obstacle.boundingBox.maxY < sideBottomThreshold(obstacle)) return false
                hasEnoughSideObstacleEvidence(obstacle)
            }

            DirectionZone.LEFT,
            DirectionZone.RIGHT -> {
                if (obstacle.boundingBox.maxY < sideBottomThreshold(obstacle)) return false
                hasEnoughSideObstacleEvidence(obstacle)
            }
        }
    }

    private fun isCloseEnoughForCenterAnnouncement(obstacle: DetectedObstacle): Boolean {
        if (obstacle.distance == DistanceLevel.NEAR) return true
        val minBottomY = when (obstacle.category) {
            ObstacleCategory.PERSON -> MIN_PERSON_CENTER_BOTTOM_Y
            ObstacleCategory.BICYCLE,
            ObstacleCategory.VEHICLE -> MIN_DYNAMIC_CENTER_BOTTOM_Y
            ObstacleCategory.STATIC_OBSTACLE,
            ObstacleCategory.UNKNOWN -> staticGate(obstacle.className).minBottomY
        }
        return obstacle.boundingBox.maxY >= minBottomY
    }

    private fun centerAreaThreshold(obstacle: DetectedObstacle): Float {
        return when (obstacle.category) {
            ObstacleCategory.PERSON -> MIN_PERSON_CENTER_AREA_RATIO
            ObstacleCategory.BICYCLE -> MIN_BICYCLE_CENTER_AREA_RATIO
            ObstacleCategory.VEHICLE -> MIN_VEHICLE_CENTER_AREA_RATIO
            ObstacleCategory.STATIC_OBSTACLE,
            ObstacleCategory.UNKNOWN -> staticGate(obstacle.className).centerAreaRatio
        }
    }

    private fun sideAreaThreshold(obstacle: DetectedObstacle): Float {
        return when (obstacle.category) {
            ObstacleCategory.PERSON -> MIN_PERSON_SIDE_AREA_RATIO
            ObstacleCategory.STATIC_OBSTACLE,
            ObstacleCategory.UNKNOWN -> staticGate(obstacle.className).sideAreaRatio
            else -> MIN_SIDE_OBSTACLE_AREA_RATIO
        }
    }

    private fun sideBottomThreshold(obstacle: DetectedObstacle): Float {
        return when (obstacle.category) {
            ObstacleCategory.PERSON -> MIN_PERSON_SIDE_BOTTOM_Y
            ObstacleCategory.STATIC_OBSTACLE,
            ObstacleCategory.UNKNOWN -> staticGate(obstacle.className).minBottomY
            else -> MIN_SIDE_OBSTACLE_BOTTOM_Y
        }
    }

    private fun hasEnoughSideObstacleEvidence(obstacle: DetectedObstacle): Boolean {
        val minUrgency = if (obstacle.category == ObstacleCategory.PERSON) {
            UrgencyLevel.MEDIUM
        } else {
            UrgencyLevel.HIGH
        }
        return obstacle.urgency >= minUrgency &&
            obstacle.areaRatio >= sideAreaThreshold(obstacle)
    }

    private fun staticGate(className: String): StaticObstacleGate {
        val normalized = className.lowercase()
        return if (normalized in ROADSIDE_STATIC_CLASS_NAMES) {
            StaticObstacleGate(
                minBottomY = MIN_ROADSIDE_STATIC_BOTTOM_Y,
                centerAreaRatio = MIN_ROADSIDE_STATIC_CENTER_AREA_RATIO,
                sideAreaRatio = MIN_ROADSIDE_STATIC_SIDE_AREA_RATIO,
            )
        } else {
            StaticObstacleGate(
                minBottomY = MIN_STATIC_CENTER_BOTTOM_Y,
                centerAreaRatio = MIN_STATIC_CENTER_AREA_RATIO,
                sideAreaRatio = MIN_SIDE_OBSTACLE_AREA_RATIO,
            )
        }
    }

    private data class StaticObstacleGate(
        val minBottomY: Float,
        val centerAreaRatio: Float,
        val sideAreaRatio: Float,
    )

    private fun createNarrowingEvent(snapshot: SceneSnapshot, now: Long): SceneEvent {
        return SceneEvent(
            timestamp = now,
            category = EventCategory.NARROWING,
            priority = EventPriority.MEDIUM,
            messageKey = "event_narrowing",
            expiresAt = now + 4000,
            dedupeKey = "narrowing",
            confidence = snapshot.connectivity.narrowingConfidence,
            severity = snapshot.connectivity.narrowingSeverity
        )
    }

    private fun createDirectionAdviceEvent(bias: DirectionBias, now: Long): SceneEvent {
        val messageKey = when (bias) {
            DirectionBias.LEFT -> "event_suggest_left"
            DirectionBias.RIGHT -> "event_suggest_right"
        }

        return SceneEvent(
            timestamp = now,
            category = EventCategory.DIRECTION_ADVICE,
            priority = EventPriority.MEDIUM,
            messageKey = messageKey,
            expiresAt = now + 5000,
            dedupeKey = "direction_$bias"
        )
    }

    private fun createObstacleEvents(
        obstacles: List<DetectedObstacle>,
        now: Long,
    ): List<SceneEvent> {
        val byZone = obstacles.groupBy { it.zone }

        return byZone.map { (zone, zoneObstacles) ->
            val primaryObstacle = zoneObstacles.maxWithOrNull(
                compareBy<DetectedObstacle> { it.urgency.value }
                    .thenBy { obstacleCategoryPriority(it.category) }
                    .thenBy { it.confidence }
                    .thenBy { it.areaRatio }
            ) ?: zoneObstacles.first()
            val maxUrgency = zoneObstacles.maxOf { it.urgency }
            val priority = obstacleEventPriority(primaryObstacle, maxUrgency)

            SceneEvent(
                timestamp = now,
                category = EventCategory.OBSTACLE,
                priority = priority,
                messageKey = obstacleMessageKey(zone, primaryObstacle.category),
                messageParams = mapOf("count" to zoneObstacles.size.toString()),
                expiresAt = now + 3000,
                dedupeKey = "obstacle_${zone.name}",
                cooldownKeys = obstacleCooldownKeys(listOf(zone)),
                relatedZones = listOf(zone)
            )
        }
    }

    private fun obstacleMessageKey(
        zone: DirectionZone,
        category: ObstacleCategory,
    ): String {
        return "event_obstacle_${zone.name.lowercase()}_${obstacleCategorySuffix(category)}"
    }

    private fun obstacleCategorySuffix(category: ObstacleCategory): String {
        return when (category) {
            ObstacleCategory.PERSON -> "person"
            ObstacleCategory.BICYCLE -> "bicycle"
            ObstacleCategory.VEHICLE -> "vehicle"
            ObstacleCategory.STATIC_OBSTACLE,
            ObstacleCategory.UNKNOWN -> "static"
        }
    }

    private fun obstacleCategoryPriority(category: ObstacleCategory): Int {
        return when (category) {
            ObstacleCategory.VEHICLE -> 3
            ObstacleCategory.PERSON,
            ObstacleCategory.BICYCLE -> 2
            ObstacleCategory.STATIC_OBSTACLE,
            ObstacleCategory.UNKNOWN -> 1
        }
    }

    private fun obstacleEventPriority(
        obstacle: DetectedObstacle,
        maxUrgency: UrgencyLevel,
    ): EventPriority {
        val basePriority = when (maxUrgency) {
            UrgencyLevel.CRITICAL -> EventPriority.CRITICAL
            UrgencyLevel.HIGH -> EventPriority.HIGH
            else -> EventPriority.MEDIUM
        }
        if (isForwardVehicleObstacle(obstacle)) {
            return EventPriority.CRITICAL
        }
        return if (isForwardDynamicObstacle(obstacle) && basePriority.value < EventPriority.HIGH.value) {
            EventPriority.HIGH
        } else {
            basePriority
        }
    }

    private fun isForwardDynamicObstacle(obstacle: DetectedObstacle): Boolean {
        val isDynamic = obstacle.category == ObstacleCategory.PERSON ||
            obstacle.category == ObstacleCategory.BICYCLE ||
            obstacle.category == ObstacleCategory.VEHICLE
        return isDynamic && isForwardZone(obstacle.zone)
    }

    private fun isForwardVehicleObstacle(obstacle: DetectedObstacle): Boolean {
        return obstacle.category == ObstacleCategory.VEHICLE && isForwardZone(obstacle.zone)
    }

    private fun isForwardZone(zone: DirectionZone): Boolean {
        return zone == DirectionZone.CENTER ||
            zone == DirectionZone.FRONT_LEFT ||
            zone == DirectionZone.FRONT_RIGHT
    }

    private fun createIntersectionEvent(now: Long): SceneEvent {
        return SceneEvent(
            timestamp = now,
            category = EventCategory.INTERSECTION,
            priority = EventPriority.LOW,
            messageKey = "event_intersection",
            expiresAt = now + 5000,
            dedupeKey = "intersection"
        )
    }

    private fun createTrafficLightEvent(now: Long): SceneEvent {
        return SceneEvent(
            timestamp = now,
            category = EventCategory.TRAFFIC_LIGHT,
            priority = EventPriority.LOW,
            messageKey = "event_traffic_light",
            expiresAt = now + 5000,
            dedupeKey = "traffic_light"
        )
    }

    private fun createRoadWarningEvent(roadSafety: RoadSafetyState, now: Long): SceneEvent {
        val messageKey = if (roadSafety.hasVehicleOnRoad) {
            "event_road_warning_vehicle"
        } else {
            "event_road_warning"
        }

        return SceneEvent(
            timestamp = now,
            category = EventCategory.ROAD_WARNING,
            priority = EventPriority.HIGH,
            messageKey = messageKey,
            expiresAt = now + 5000,
            dedupeKey = if (roadSafety.hasVehicleOnRoad) {
                "road_warning_vehicle"
            } else {
                "road_warning_area"
            },
            confidence = roadSafety.dangerConfidence
        )
    }

    private fun createRoadExitEvent(now: Long): SceneEvent {
        return SceneEvent(
            timestamp = now,
            category = EventCategory.ROAD_EXIT,
            priority = EventPriority.LOW,
            messageKey = "event_road_exit",
            expiresAt = now + 3000,
            dedupeKey = "road_exit"
        )
    }

    private fun createGroundChangeEvent(change: GroundTypeChange, now: Long): SceneEvent {
        val messageKey = when (change.to) {
            GroundType.ROAD -> "event_ground_to_road"
            GroundType.TERRAIN -> "event_ground_to_terrain"
            GroundType.SIDEWALK -> "event_ground_to_sidewalk"
            GroundType.INDOOR -> "event_ground_to_indoor"
            else -> "event_ground_change"
        }

        return SceneEvent(
            timestamp = now,
            category = EventCategory.GROUND_CHANGE,
            priority = EventPriority.MEDIUM,
            messageKey = messageKey,
            messageParams = mapOf(
                "from" to change.from.name.lowercase(),
                "to" to change.to.name.lowercase()
            ),
            expiresAt = now + 4000,
            dedupeKey = "ground_change_${change.to.name}"
        )
    }

    fun reset() {
        wasOnRoad = false
    }

    private fun obstacleCooldownKeys(zones: List<DirectionZone>): Set<String> {
        val keys = zones.mapTo(mutableSetOf()) { "obstacle_${it.name}" }
        if (zones.any { it == DirectionZone.CENTER }) {
            keys.add("obstacle_PRIMARY_CENTER")
        }
        if (zones.any {
                it == DirectionZone.FRONT_LEFT ||
                    it == DirectionZone.FRONT_RIGHT
            }
        ) {
            keys.add("obstacle_PRIMARY_FRONT")
        }
        if (zones.any {
                it == DirectionZone.LEFT ||
                    it == DirectionZone.RIGHT
            }
        ) {
            keys.add("obstacle_PRIMARY_SIDE")
        }
        return keys
    }
}
