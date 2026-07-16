package com.sailens.domain.processor.decision

import com.sailens.domain.config.AnalysisConfig
import com.sailens.domain.model.analysis.GroundTypeChange
import com.sailens.domain.model.analysis.RoadSafetyState
import com.sailens.domain.model.analysis.SceneElements
import com.sailens.domain.model.analysis.SceneSnapshot
import com.sailens.domain.model.analysis.WalkPathConnectivity
import com.sailens.domain.model.common.DirectionZone
import com.sailens.domain.model.common.DistanceLevel
import com.sailens.domain.model.common.EventPriority
import com.sailens.domain.model.common.GroundType
import com.sailens.domain.model.common.NormalizedRect
import com.sailens.domain.model.common.ObstacleCategory
import com.sailens.domain.model.common.Severity
import com.sailens.domain.model.common.UrgencyLevel
import com.sailens.domain.model.perception.DetectedObstacle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventGeneratorTest {

    @Test
    fun `center person is announced with typed message`() {
        val generator = EventGenerator()
        val obstacle = obstacle(
            box = NormalizedRect(x = 0.43f, y = 0.45f, width = 0.14f, height = 0.25f),
            category = ObstacleCategory.PERSON,
            zone = DirectionZone.CENTER,
            distance = DistanceLevel.MEDIUM,
            urgency = UrgencyLevel.HIGH,
        )

        val events = generator.generate(snapshot(obstacle), now = 1_000L)

        assertEquals(listOf("event_obstacle_center_person"), events.map { it.messageKey })
    }

    @Test
    fun `medium center person waits until close enough before announcement`() {
        val generator = EventGenerator()
        val obstacle = obstacle(
            box = NormalizedRect(x = 0.44f, y = 0.34f, width = 0.12f, height = 0.20f),
            category = ObstacleCategory.PERSON,
            zone = DirectionZone.CENTER,
            distance = DistanceLevel.MEDIUM,
            urgency = UrgencyLevel.HIGH,
        )

        val events = generator.generate(snapshot(obstacle), now = 1_000L)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `side person can be announced before generic side obstacle threshold`() {
        val generator = EventGenerator()
        val obstacle = obstacle(
            box = NormalizedRect(x = 0.05f, y = 0.38f, width = 0.11f, height = 0.17f),
            category = ObstacleCategory.PERSON,
            zone = DirectionZone.LEFT,
            distance = DistanceLevel.MEDIUM,
            urgency = UrgencyLevel.MEDIUM,
        )

        val events = generator.generate(snapshot(obstacle), now = 1_000L)

        assertEquals(listOf("event_obstacle_left_person"), events.map { it.messageKey })
    }

    @Test
    fun `same-zone vehicle is selected over person at equal urgency`() {
        val generator = EventGenerator()
        val person = obstacle(
            box = NormalizedRect(x = 0.40f, y = 0.48f, width = 0.20f, height = 0.25f),
            category = ObstacleCategory.PERSON,
            zone = DirectionZone.CENTER,
            distance = DistanceLevel.MEDIUM,
            urgency = UrgencyLevel.HIGH,
        )
        val vehicle = obstacle(
            box = NormalizedRect(x = 0.44f, y = 0.48f, width = 0.14f, height = 0.20f),
            category = ObstacleCategory.VEHICLE,
            className = "car",
            zone = DirectionZone.CENTER,
            distance = DistanceLevel.MEDIUM,
            urgency = UrgencyLevel.HIGH,
        )

        val events = generator.generate(snapshot(obstacles = listOf(person, vehicle)), now = 1_000L)

        assertEquals(listOf("event_obstacle_center_vehicle"), events.map { it.messageKey })
        assertEquals(EventPriority.CRITICAL, events.single().priority)
    }

    @Test
    fun `medium static center obstacle must be close enough before announcement`() {
        val generator = EventGenerator()
        val obstacle = obstacle(
            box = NormalizedRect(x = 0.42f, y = 0.36f, width = 0.16f, height = 0.20f),
            category = ObstacleCategory.STATIC_OBSTACLE,
            zone = DirectionZone.CENTER,
            distance = DistanceLevel.MEDIUM,
            urgency = UrgencyLevel.HIGH,
        )

        val events = generator.generate(snapshot(obstacle), now = 1_000L)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `near static center obstacle uses static obstacle message`() {
        val generator = EventGenerator()
        val obstacle = obstacle(
            box = NormalizedRect(x = 0.40f, y = 0.58f, width = 0.20f, height = 0.22f),
            category = ObstacleCategory.STATIC_OBSTACLE,
            zone = DirectionZone.CENTER,
            distance = DistanceLevel.NEAR,
            urgency = UrgencyLevel.HIGH,
        )

        val events = generator.generate(snapshot(obstacle), now = 1_000L)

        assertEquals(listOf("event_obstacle_center_static"), events.map { it.messageKey })
    }

    @Test
    fun `roadside static class must be closer and larger before announcement`() {
        val generator = EventGenerator()
        val obstacle = obstacle(
            box = NormalizedRect(x = 0.40f, y = 0.50f, width = 0.20f, height = 0.20f),
            category = ObstacleCategory.STATIC_OBSTACLE,
            className = "potted_plant",
            zone = DirectionZone.CENTER,
            distance = DistanceLevel.MEDIUM,
            urgency = UrgencyLevel.HIGH,
        )

        val events = generator.generate(snapshot(obstacle), now = 1_000L)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `moderate blocked path emits complex prompt instead of hard blocked prompt`() {
        val generator = EventGenerator()
        val events = generator.generate(
            snapshot(
                connectivity = connectivity().copy(
                    isBlocked = true,
                    blockageConfidence = 0.68f,
                    blockageSeverity = Severity.MODERATE,
                    verticalReachRatio = 0.50f,
                    floodReachRatio = 0.12f,
                    widthRetentionP25 = 0.32f,
                )
            ),
            now = 1_000L,
        )

        assertEquals(listOf("event_path_complex"), events.map { it.messageKey })
    }

    @Test
    fun `severe high certainty blocked path keeps hard blocked prompt`() {
        val generator = EventGenerator()
        val events = generator.generate(
            snapshot(
                connectivity = connectivity().copy(
                    isBlocked = true,
                    blockageConfidence = 0.82f,
                    blockageSeverity = Severity.SEVERE,
                    verticalReachRatio = 0.30f,
                    floodReachRatio = 0.08f,
                    widthRetentionP25 = 0.20f,
                )
            ),
            now = 1_000L,
        )

        assertEquals(listOf("event_blocked"), events.map { it.messageKey })
    }

    @Test
    fun `road warning and ground change prompts are disabled by default`() {
        val generator = EventGenerator()
        val events = generator.generate(
            snapshot(
                roadSafety = RoadSafetyState(
                    isOnRoad = true,
                    isDangerous = true,
                    roadRatio = 0.6f,
                    hasVehicleOnRoad = true,
                    hasTrafficLight = false,
                    dangerConfidence = 0.9f,
                ),
                groundTypeChange = GroundTypeChange(
                    from = GroundType.ROAD,
                    to = GroundType.SIDEWALK,
                ),
            ),
            now = 1_000L,
        )

        assertTrue(events.isEmpty())
    }

    @Test
    fun `road warning and ground change prompts remain available behind explicit flags`() {
        val generator = EventGenerator(
            AnalysisConfig(
                enableRoadWarningEvents = true,
                enableGroundChangeEvents = true,
            )
        )
        val events = generator.generate(
            snapshot(
                roadSafety = RoadSafetyState(
                    isOnRoad = true,
                    isDangerous = true,
                    roadRatio = 0.6f,
                    hasVehicleOnRoad = true,
                    hasTrafficLight = false,
                    dangerConfidence = 0.9f,
                ),
                groundTypeChange = GroundTypeChange(
                    from = GroundType.ROAD,
                    to = GroundType.SIDEWALK,
                ),
            ),
            now = 1_000L,
        )

        assertEquals(
            listOf("event_road_warning_vehicle", "event_ground_to_sidewalk"),
            events.map { it.messageKey },
        )
    }

    @Test
    fun `intersection prompt is emitted from scene element`() {
        val generator = EventGenerator()
        val events = generator.generate(
            snapshot(
                sceneElements = SceneElements(hasIntersection = true),
            ),
            now = 1_000L,
        )

        assertEquals(listOf("event_intersection"), events.map { it.messageKey })
    }

    @Test
    fun `traffic light prompt is emitted when intersection is uncertain`() {
        val generator = EventGenerator()
        val events = generator.generate(
            snapshot(
                sceneElements = SceneElements(hasTrafficLight = true),
            ),
            now = 1_000L,
        )

        assertEquals(listOf("event_traffic_light"), events.map { it.messageKey })
    }

    @Test
    fun `intersection prompt is preferred over traffic light prompt`() {
        val generator = EventGenerator()
        val events = generator.generate(
            snapshot(
                sceneElements = SceneElements(
                    hasIntersection = true,
                    hasTrafficLight = true,
                ),
            ),
            now = 1_000L,
        )

        assertEquals(listOf("event_intersection"), events.map { it.messageKey })
    }

    private fun snapshot(obstacle: DetectedObstacle): SceneSnapshot {
        return snapshot(obstacles = listOf(obstacle))
    }

    private fun snapshot(
        obstacles: List<DetectedObstacle> = emptyList(),
        connectivity: WalkPathConnectivity = connectivity(),
        sceneElements: SceneElements = SceneElements(),
        roadSafety: RoadSafetyState = RoadSafetyState(
            isOnRoad = false,
            isDangerous = false,
            roadRatio = 0f,
            hasVehicleOnRoad = false,
            hasTrafficLight = false,
            dangerConfidence = 0f,
        ),
        groundTypeChange: GroundTypeChange? = null,
    ): SceneSnapshot {
        return SceneSnapshot(
            timestamp = obstacles.firstOrNull()?.timestamp ?: 1_000L,
            obstacles = obstacles,
            bottomCoverage = 1f,
            connectivity = connectivity,
            sceneElements = sceneElements,
            roadSafety = roadSafety,
            groundTypeChange = groundTypeChange,
        )
    }

    private fun obstacle(
        box: NormalizedRect,
        category: ObstacleCategory,
        className: String = category.name.lowercase(),
        zone: DirectionZone,
        distance: DistanceLevel,
        urgency: UrgencyLevel,
    ): DetectedObstacle {
        return DetectedObstacle(
            boundingBox = box,
            category = category,
            className = className,
            zone = zone,
            distance = distance,
            urgency = urgency,
            confidence = 0.9f,
            stableFrames = 3,
            areaRatio = box.area,
            timestamp = 1_000L,
        )
    }

    private fun connectivity() = WalkPathConnectivity(
        isBlocked = false,
        isNarrowing = false,
        suggestedBias = null,
        blockageConfidence = 0f,
        narrowingConfidence = 0f,
        blockageSeverity = Severity.NONE,
        narrowingSeverity = Severity.NONE,
        verticalReachRatio = 1f,
        validLayers = 3,
        totalLayers = 3,
        widthRetentionAvg = 1f,
        widthRetentionP25 = 1f,
        widthSlope = 0f,
        floodReachRatio = 1f,
        floodWidthRetentionP25 = 1f,
        floodVisitedRatio = 1f,
    )
}
