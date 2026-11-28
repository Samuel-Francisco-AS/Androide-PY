package com.samuel.androidepy.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateMapRepositoryTest {
    private val context = CoordinateMapDisplayContext(
        targetPackage = "com.example.target",
        widthPx = 100,
        heightPx = 200,
        densityDpi = 320,
        rotation = 0,
        insetTopPx = 24,
        insetBottomPx = 32
    )

    @Test
    fun createPersistsDisplayContextAndCalculatesNormalizedCoordinates() = runBlocking {
        val repository = CoordinateMapRepository(FakeCoordinateMapDao(), clock = { 1234L })
        val created = repository.create(
            "  Tela principal  ",
            context,
            listOf(
                CoordinateMapPointInput("  confirmar ", 25, 100),
                CoordinateMapPointInput("voltar", 0, 0)
            )
        )

        assertEquals("Tela principal", created.map.name)
        assertEquals(1234L, created.map.createdAtEpochMs)
        assertEquals("com.example.target", created.map.targetPackage)
        assertEquals(100, created.map.displayWidthPx)
        assertEquals(200, created.map.displayHeightPx)
        assertEquals(24, created.map.insetTopPx)
        assertEquals(2, created.points.size)
        assertEquals("confirmar", created.points[0].name)
        assertEquals(0.25, created.points[0].xNormalized, 0.000001)
        assertEquals(0.5, created.points[0].yNormalized, 0.000001)
        assertEquals(0.0, created.points[1].xNormalized, 0.000001)
    }

    @Test
    fun mapNamesAreUniqueIgnoringCase(): Unit = runBlocking {
        val dao = FakeCoordinateMapDao()
        val repository = CoordinateMapRepository(dao)
        repository.create("Principal", context, listOf(CoordinateMapPointInput("home", 1, 1)))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.create(" principal ", context, listOf(CoordinateMapPointInput("other", 2, 2)))
            }
        }
    }

    @Test
    fun pointNamesMustBeUniqueIgnoringCaseWithinMap() {
        val repository = CoordinateMapRepository(FakeCoordinateMapDao())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.create(
                    "Principal",
                    context,
                    listOf(
                        CoordinateMapPointInput("Confirmar", 1, 1),
                        CoordinateMapPointInput(" confirmar ", 2, 2)
                    )
                )
            }
        }
    }

    @Test
    fun pointsOutsideDisplayAreRejected() {
        val repository = CoordinateMapRepository(FakeCoordinateMapDao())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.create(
                    "Principal",
                    context,
                    listOf(CoordinateMapPointInput("fora", 100, 10))
                )
            }
        }
    }

    @Test
    fun invalidDisplayMetadataIsRejected() {
        val repository = CoordinateMapRepository(FakeCoordinateMapDao())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.create(
                    "Principal",
                    context.copy(rotation = 4),
                    listOf(CoordinateMapPointInput("home", 1, 1))
                )
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.create(
                    "Principal",
                    context.copy(insetTopPx = 200),
                    listOf(CoordinateMapPointInput("home", 1, 1))
                )
            }
        }
    }

    @Test
    fun emptyMapsAndBlankPointNamesAreRejected() {
        val repository = CoordinateMapRepository(FakeCoordinateMapDao())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.create("Vazio", context, emptyList()) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.create(
                    "Sem nome",
                    context,
                    listOf(CoordinateMapPointInput("  ", 1, 1))
                )
            }
        }
    }

    @Test
    fun getListAndDeleteOperateOnMapAndItsPoints() = runBlocking {
        val dao = FakeCoordinateMapDao()
        val repository = CoordinateMapRepository(dao, clock = { 2000L })
        val created = repository.create(
            "Mapa",
            context,
            listOf(CoordinateMapPointInput("home", 10, 20))
        )

        val loaded = repository.get("mapa")
        assertEquals(created.map.mapId, loaded.map.mapId)
        assertEquals(1, loaded.points.size)
        assertEquals(1, repository.list().single().pointCount)
        assertTrue(repository.delete("MAPA"))
        assertTrue(repository.list().isEmpty())
        assertFalse(dao.hasPoints(created.map.mapId))
    }

    @Test
    fun missingMapIsReportedExplicitly() {
        val repository = CoordinateMapRepository(FakeCoordinateMapDao())
        assertThrows(CoordinateMapNotFoundException::class.java) {
            runBlocking { repository.get("inexistente") }
        }
        assertThrows(CoordinateMapNotFoundException::class.java) {
            runBlocking { repository.delete("inexistente") }
        }
    }

    @Test
    fun failedPointInsertDoesNotLeavePartialMapInTransactionalDao() = runBlocking {
        val dao = FakeCoordinateMapDao().apply { failInsert = true }
        val repository = CoordinateMapRepository(dao)
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                repository.create(
                    "Atomicidade",
                    context,
                    listOf(CoordinateMapPointInput("home", 1, 1))
                )
            }
        }
        assertTrue(dao.maps.isEmpty())
        assertTrue(dao.points.isEmpty())
    }

    private class FakeCoordinateMapDao : CoordinateMapDao {
        val maps = linkedMapOf<String, CoordinateMapEntity>()
        val points = linkedMapOf<String, MutableList<CoordinateMapPointEntity>>()
        var failInsert: Boolean = false

        override suspend fun insertMap(map: CoordinateMapEntity) {
            check(maps.values.none { it.name.equals(map.name, ignoreCase = true) }) {
                "Duplicate map name"
            }
            maps[map.mapId] = map
        }

        override suspend fun insertPoints(points: List<CoordinateMapPointEntity>) {
            if (failInsert) throw IllegalStateException("simulated point insert failure")
            points.forEach { point ->
                this.points.getOrPut(point.mapId) { mutableListOf() }.add(point)
            }
        }

        override suspend fun insertMapWithPoints(
            map: CoordinateMapEntity,
            points: List<CoordinateMapPointEntity>
        ) {
            check(maps.values.none { it.name.equals(map.name, ignoreCase = true) }) {
                "Duplicate map name"
            }
            check(!failInsert) { "simulated point insert failure" }
            this.maps[map.mapId] = map
            this.points[map.mapId] = points.toMutableList()
        }

        override suspend fun getWithPointsByName(name: String): CoordinateMapWithPoints? {
            val map = maps.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: return null
            return CoordinateMapWithPoints(map, points[map.mapId].orEmpty())
        }

        override suspend fun listSummaries(): List<CoordinateMapSummary> =
            maps.values.sortedByDescending { it.updatedAtEpochMs }.map { map ->
                CoordinateMapSummary(
                    mapId = map.mapId,
                    name = map.name,
                    createdAtEpochMs = map.createdAtEpochMs,
                    updatedAtEpochMs = map.updatedAtEpochMs,
                    pointCount = points[map.mapId].orEmpty().size
                )
            }

        override suspend fun getIdByName(name: String): String? =
            maps.values.firstOrNull { it.name.equals(name, ignoreCase = true) }?.mapId

        override suspend fun deleteById(mapId: String): Int {
            val removed = maps.remove(mapId) ?: return 0
            points.remove(removed.mapId)
            return 1
        }

        fun hasPoints(mapId: String): Boolean = points[mapId]?.isNotEmpty() == true
    }
}
