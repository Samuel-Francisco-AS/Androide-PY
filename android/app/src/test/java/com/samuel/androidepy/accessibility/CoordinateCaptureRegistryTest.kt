package com.samuel.androidepy.accessibility

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateCaptureRegistryTest {
    @Test
    fun pointsRequireActiveSessionAndUniqueNames() {
        val registry = CoordinateCaptureRegistry()
        val id = registry.create("Principal")

        assertFalse(registry.addPoint(id, "home", 10, 20))
        assertTrue(registry.activate(id))
        assertTrue(registry.addPoint(id, "home", 10, 20))
        assertFalse(registry.addPoint(id, " HOME ", 20, 30))
        assertFalse(registry.addPoint(id, " ", 20, 30))
        assertEquals(1, registry.points(id).size)
    }

    @Test
    fun removeLastPointSupportsCorrectionBeforeCompletion() {
        val registry = CoordinateCaptureRegistry()
        val id = registry.create("Principal")
        registry.activate(id)
        registry.addPoint(id, "home", 10, 20)
        registry.addPoint(id, "settings", 30, 40)

        assertTrue(registry.removeLastPoint(id))
        assertEquals("home", registry.points(id).single().name)
        assertFalse(registry.removeLastPoint(id))
    }

    @Test
    fun completedResultIsPublishedAsSerializableJson() {
        val registry = CoordinateCaptureRegistry()
        val id = registry.create("Principal")
        registry.activate(id)
        registry.addPoint(id, "home", 10, 20)
        assertTrue(registry.markSaving(id))
        registry.complete(id, JSONObject().put("name", "Principal"))

        val envelope = JSONObject(registry.statusJson(id))
        assertTrue(envelope.getBoolean("ok"))
        assertEquals("completed", envelope.getJSONObject("result").getString("state"))
        assertEquals("Principal", envelope.getJSONObject("result")
            .getJSONObject("result").getString("name"))
    }

    @Test
    fun cancellationDiscardsUnpersistedPointsAndIsIdempotent() {
        val registry = CoordinateCaptureRegistry()
        val id = registry.create("Principal")
        registry.activate(id)
        registry.addPoint(id, "home", 10, 20)

        assertTrue(registry.cancelLocally(id))
        assertTrue(registry.cancelLocally(id))
        assertTrue(registry.points(id).isEmpty())
        assertEquals("cancelled", JSONObject(registry.statusJson(id))
            .getJSONObject("result").getString("state"))
    }

    @Test
    fun savingStateCannotBeCancelledAndCompletesOnlyAfterPersistence() {
        val registry = CoordinateCaptureRegistry()
        val id = registry.create("Principal")
        registry.activate(id)
        registry.addPoint(id, "home", 10, 20)

        assertTrue(registry.markSaving(id))
        assertEquals("saving", registry.state(id))
        assertFalse(registry.cancelLocally(id))
        registry.complete(id, JSONObject().put("name", "Principal"))
        assertEquals("completed", registry.state(id))
    }
}
