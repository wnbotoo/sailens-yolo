package com.sailens.domain.processor.decision

import com.sailens.domain.model.common.DirectionZone
import com.sailens.domain.model.common.EventCategory
import com.sailens.domain.model.common.EventPriority
import com.sailens.domain.model.common.Severity
import com.sailens.domain.model.scene.SceneEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventConflictResolverTest {
    private val resolver = EventConflictResolver()

    @Test
    fun `blocked suppresses all center obstacles but keeps non center obstacles`() {
        val events = listOf(
            sceneEvent(category = EventCategory.BLOCKED, messageKey = "event_blocked"),
            sceneEvent(
                category = EventCategory.OBSTACLE,
                messageKey = "event_obstacle_center",
                relatedZones = listOf(DirectionZone.CENTER)
            ),
            sceneEvent(
                category = EventCategory.OBSTACLE,
                messageKey = "event_obstacle_center",
                relatedZones = listOf(DirectionZone.CENTER)
            ),
            sceneEvent(
                category = EventCategory.OBSTACLE,
                messageKey = "event_obstacle_left",
                relatedZones = listOf(DirectionZone.LEFT)
            )
        )

        val resolved = resolver.resolve(events)

        assertEquals(2, resolved.size)
        assertTrue(resolved.any { it.category == EventCategory.BLOCKED })
        assertTrue(
            resolved.any {
                it.category == EventCategory.OBSTACLE && it.relatedZones == listOf(DirectionZone.LEFT)
            }
        )
    }

    @Test
    fun `blocked suppresses narrowing and direction advice`() {
        val events = listOf(
            sceneEvent(category = EventCategory.BLOCKED, messageKey = "event_blocked"),
            sceneEvent(category = EventCategory.NARROWING, messageKey = "event_narrowing"),
            sceneEvent(category = EventCategory.DIRECTION_ADVICE, messageKey = "event_suggest_left")
        )

        val resolved = resolver.resolve(events)

        assertEquals(listOf(EventCategory.BLOCKED), resolved.map { it.category })
    }

    @Test
    fun `path complex keeps center obstacle but suppresses generic path advice`() {
        val events = listOf(
            sceneEvent(category = EventCategory.PATH_COMPLEX, messageKey = "event_path_complex"),
            sceneEvent(
                category = EventCategory.OBSTACLE,
                messageKey = "event_obstacle_center_person",
                relatedZones = listOf(DirectionZone.CENTER)
            ),
            sceneEvent(category = EventCategory.NARROWING, messageKey = "event_narrowing"),
            sceneEvent(category = EventCategory.DIRECTION_ADVICE, messageKey = "event_suggest_left")
        )

        val resolved = resolver.resolve(events)

        assertEquals(
            listOf(EventCategory.PATH_COMPLEX, EventCategory.OBSTACLE),
            resolved.map { it.category },
        )
    }

    private fun sceneEvent(
        category: EventCategory,
        messageKey: String,
        relatedZones: List<DirectionZone> = emptyList(),
    ) = SceneEvent(
        timestamp = 1L,
        category = category,
        priority = EventPriority.HIGH,
        messageKey = messageKey,
        expiresAt = 2L,
        dedupeKey = "${category.name}_$messageKey",
        severity = Severity.MODERATE,
        relatedZones = relatedZones
    )
}
