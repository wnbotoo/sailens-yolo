package com.sailens.domain.processor.decision

import com.sailens.domain.model.common.DirectionZone
import com.sailens.domain.model.common.EventCategory
import com.sailens.domain.model.common.EventPriority
import com.sailens.domain.model.scene.SceneEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventMergerTest {
    private val merger = EventMerger()

    @Test
    fun `merged center obstacle uses stable primary center cooldown key`() {
        val merged = merger.merge(
            listOf(
                obstacle(DirectionZone.LEFT),
                obstacle(DirectionZone.CENTER),
            )
        ).single()

        assertEquals("event_obstacle_left_center", merged.messageKey)
        assertEquals("obstacle_PRIMARY_CENTER", merged.dedupeKey)
        assertTrue("obstacle_CENTER" in merged.cooldownKeys)
        assertTrue("obstacle_PRIMARY_CENTER" in merged.cooldownKeys)
    }

    @Test
    fun `merged same-category obstacle keeps category-specific message`() {
        val merged = merger.merge(
            listOf(
                obstacle(DirectionZone.CENTER, suffix = "person"),
                obstacle(DirectionZone.RIGHT, suffix = "person"),
            )
        ).single()

        assertEquals("event_obstacle_center_right_person", merged.messageKey)
        assertEquals("obstacle_PRIMARY_CENTER", merged.dedupeKey)
    }

    @Test
    fun `merged mixed non-vehicle category obstacle falls back to zone-only message`() {
        val merged = merger.merge(
            listOf(
                obstacle(DirectionZone.CENTER, suffix = "person"),
                obstacle(DirectionZone.RIGHT, suffix = "static"),
            )
        ).single()

        assertEquals("event_obstacle_center_right", merged.messageKey)
    }

    @Test
    fun `mixed vehicle category obstacles remain category specific`() {
        val merged = merger.merge(
            listOf(
                obstacle(DirectionZone.CENTER, suffix = "person"),
                obstacle(DirectionZone.RIGHT, suffix = "vehicle"),
            )
        )

        assertEquals(
            setOf("event_obstacle_center_person", "event_obstacle_right_vehicle"),
            merged.map { it.messageKey }.toSet(),
        )
    }

    @Test
    fun `unsupported five-zone combinations fall back to multiple message`() {
        val merged = merger.merge(
            listOf(
                obstacle(DirectionZone.FRONT_LEFT),
                obstacle(DirectionZone.CENTER),
            )
        ).single()

        assertEquals("event_obstacle_multiple", merged.messageKey)
    }

    private fun obstacle(
        zone: DirectionZone,
        suffix: String? = null,
    ): SceneEvent = SceneEvent(
        timestamp = 1L,
        category = EventCategory.OBSTACLE,
        priority = EventPriority.MEDIUM,
        messageKey = buildString {
            append("event_obstacle_${zone.name.lowercase()}")
            if (suffix != null) append("_$suffix")
        },
        expiresAt = 3L,
        dedupeKey = "obstacle_${zone.name}",
        cooldownKeys = setOf("obstacle_${zone.name}"),
        relatedZones = listOf(zone),
    )
}
