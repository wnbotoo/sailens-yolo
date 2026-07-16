package com.sailens.domain.processor.decision

import com.sailens.domain.model.common.EventCategory
import com.sailens.domain.model.scene.SceneEvent

/**
 * 冷却管理器
 */
class CooldownManager() {
    val cooldowns: Map<EventCategory, Long> = mapOf(
        EventCategory.BLOCKED to 5000,
        EventCategory.NARROWING to 4000,
        EventCategory.OBSTACLE to 2000,
        EventCategory.DIRECTION_ADVICE to 6000,
        EventCategory.INTERSECTION to 5000,
        EventCategory.ROAD_WARNING to 12000,
        EventCategory.ROAD_EXIT to 5000,
        EventCategory.GROUND_CHANGE to 8000,
        EventCategory.PATH_COMPLEX to 6000,
        EventCategory.TRAFFIC_LIGHT to 12000,
    )

    private val keyCooldowns: Map<String, Long> = mapOf(
        "road_warning_vehicle" to 5000,
        "obstacle_PRIMARY_CENTER" to 4000,
        "obstacle_PRIMARY_FRONT" to 5000,
        "obstacle_PRIMARY_SIDE" to 6000,
    )

    private val lastEventTimes = mutableMapOf<String, Long>()

    fun filter(events: List<SceneEvent>, now: Long): List<SceneEvent> {
        return events.filter { event ->
            event.cooldownKeys().all { key ->
                val lastTime = lastEventTimes[key]
                val cooldown = cooldownFor(event, key)
                lastTime == null || now - lastTime >= cooldown
            }
        }
    }

    fun recordEvent(event: SceneEvent, now: Long) {
        event.cooldownKeys().forEach { key ->
            lastEventTimes[key] = now
        }
    }

    fun reset() {
        lastEventTimes.clear()
    }

    private fun SceneEvent.cooldownKeys(): Set<String> = cooldownKeys.ifEmpty {
        setOf(dedupeKey.ifBlank { category.name })
    }

    private fun cooldownFor(event: SceneEvent, key: String): Long {
        return keyCooldowns[key] ?: cooldowns[event.category] ?: 3000
    }
}
