package com.samuel.androidepy.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import java.util.UUID

@Entity(
    tableName = "coordinate_maps",
    indices = [Index(value = ["name"], unique = true)]
)
data class CoordinateMapEntity(
    @PrimaryKey @ColumnInfo(name = "map_id") val mapId: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    @ColumnInfo(name = "target_package") val targetPackage: String?,
    @ColumnInfo(name = "display_width_px") val displayWidthPx: Int,
    @ColumnInfo(name = "display_height_px") val displayHeightPx: Int,
    @ColumnInfo(name = "density_dpi") val densityDpi: Int,
    val rotation: Int,
    @ColumnInfo(name = "inset_left_px") val insetLeftPx: Int,
    @ColumnInfo(name = "inset_top_px") val insetTopPx: Int,
    @ColumnInfo(name = "inset_right_px") val insetRightPx: Int,
    @ColumnInfo(name = "inset_bottom_px") val insetBottomPx: Int,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long
)

@Entity(
    tableName = "coordinate_map_points",
    foreignKeys = [
        ForeignKey(
            entity = CoordinateMapEntity::class,
            parentColumns = ["map_id"],
            childColumns = ["map_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["map_id"]),
        Index(value = ["map_id", "name"], unique = true)
    ]
)
data class CoordinateMapPointEntity(
    @PrimaryKey @ColumnInfo(name = "point_id") val pointId: String,
    @ColumnInfo(name = "map_id") val mapId: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    @ColumnInfo(name = "x_px") val xPx: Int,
    @ColumnInfo(name = "y_px") val yPx: Int,
    @ColumnInfo(name = "x_normalized") val xNormalized: Double,
    @ColumnInfo(name = "y_normalized") val yNormalized: Double
)

data class CoordinateMapWithPoints(
    @Embedded val map: CoordinateMapEntity,
    @Relation(
        parentColumn = "map_id",
        entityColumn = "map_id"
    )
    val points: List<CoordinateMapPointEntity>
)

data class CoordinateMapSummary(
    @ColumnInfo(name = "map_id") val mapId: String,
    val name: String,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
    @ColumnInfo(name = "point_count") val pointCount: Int
)

/** Display metadata captured before the accessibility overlay is attached. */
data class CoordinateMapDisplayContext(
    val targetPackage: String?,
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val rotation: Int,
    val insetLeftPx: Int = 0,
    val insetTopPx: Int = 0,
    val insetRightPx: Int = 0,
    val insetBottomPx: Int = 0
)

/** Pixel coordinates are absolute within the full display coordinate space. */
data class CoordinateMapPointInput(
    val name: String,
    val xPx: Int,
    val yPx: Int
)

@Dao
interface CoordinateMapDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMap(map: CoordinateMapEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPoints(points: List<CoordinateMapPointEntity>)

    @Transaction
    suspend fun insertMapWithPoints(
        map: CoordinateMapEntity,
        points: List<CoordinateMapPointEntity>
    ) {
        insertMap(map)
        insertPoints(points)
    }

    @Transaction
    @Query("SELECT * FROM coordinate_maps WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getWithPointsByName(name: String): CoordinateMapWithPoints?

    @Query(
        """
        SELECT m.map_id, m.name, m.created_at_epoch_ms, m.updated_at_epoch_ms,
               COUNT(p.point_id) AS point_count
        FROM coordinate_maps AS m
        LEFT JOIN coordinate_map_points AS p ON p.map_id = m.map_id
        GROUP BY m.map_id
        ORDER BY m.updated_at_epoch_ms DESC
        """
    )
    suspend fun listSummaries(): List<CoordinateMapSummary>

    @Query("SELECT map_id FROM coordinate_maps WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getIdByName(name: String): String?

    @Query("DELETE FROM coordinate_maps WHERE map_id = :mapId")
    suspend fun deleteById(mapId: String): Int
}

class CoordinateMapRepository(
    private val dao: CoordinateMapDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun create(
        name: String,
        context: CoordinateMapDisplayContext,
        points: List<CoordinateMapPointInput>
    ): CoordinateMapWithPoints {
        val normalizedName = validateName(name)
        validateContext(context)
        require(points.isNotEmpty()) { "Um mapa precisa conter ao menos um ponto." }
        require(dao.getIdByName(normalizedName) == null) {
            "Já existe um mapa com esse nome."
        }

        val pointNames = mutableSetOf<String>()
        val mapId = idFactory()
        val now = clock()
        val map = CoordinateMapEntity(
            mapId = mapId,
            name = normalizedName,
            targetPackage = context.targetPackage?.trim()?.takeIf { it.isNotEmpty() },
            displayWidthPx = context.widthPx,
            displayHeightPx = context.heightPx,
            densityDpi = context.densityDpi,
            rotation = context.rotation,
            insetLeftPx = context.insetLeftPx,
            insetTopPx = context.insetTopPx,
            insetRightPx = context.insetRightPx,
            insetBottomPx = context.insetBottomPx,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val storedPoints = points.map { point ->
            val pointName = validatePointName(point.name)
            require(pointNames.add(pointName.lowercase())) {
                "Os nomes dos pontos devem ser únicos dentro do mapa."
            }
            require(point.xPx in 0 until context.widthPx) {
                "A coordenada x do ponto '$pointName' está fora do display."
            }
            require(point.yPx in 0 until context.heightPx) {
                "A coordenada y do ponto '$pointName' está fora do display."
            }
            CoordinateMapPointEntity(
                pointId = idFactory(),
                mapId = mapId,
                name = pointName,
                xPx = point.xPx,
                yPx = point.yPx,
                xNormalized = point.xPx.toDouble() / context.widthPx,
                yNormalized = point.yPx.toDouble() / context.heightPx
            )
        }

        dao.insertMapWithPoints(map, storedPoints)
        return CoordinateMapWithPoints(map, storedPoints)
    }

    suspend fun get(name: String): CoordinateMapWithPoints {
        val normalizedName = validateName(name)
        return dao.getWithPointsByName(normalizedName)
            ?: throw CoordinateMapNotFoundException(normalizedName)
    }

    suspend fun list(): List<CoordinateMapSummary> = dao.listSummaries()

    suspend fun delete(name: String): Boolean {
        val normalizedName = validateName(name)
        val mapId = dao.getIdByName(normalizedName)
            ?: throw CoordinateMapNotFoundException(normalizedName)
        return dao.deleteById(mapId) == 1
    }

    private fun validateName(raw: String): String {
        val value = raw.trim()
        require(value.isNotEmpty()) { "O nome do mapa não pode ficar vazio." }
        require(value.length <= MAX_NAME_LENGTH) {
            "O nome do mapa deve ter no máximo $MAX_NAME_LENGTH caracteres."
        }
        return value
    }

    private fun validatePointName(raw: String): String {
        val value = raw.trim()
        require(value.isNotEmpty()) { "O nome do ponto não pode ficar vazio." }
        require(value.length <= MAX_NAME_LENGTH) {
            "O nome do ponto deve ter no máximo $MAX_NAME_LENGTH caracteres."
        }
        return value
    }

    private fun validateContext(context: CoordinateMapDisplayContext) {
        require(context.widthPx > 0 && context.heightPx > 0) {
            "As dimensões do display devem ser positivas."
        }
        require(context.densityDpi > 0) { "A densidade do display deve ser positiva." }
        require(context.rotation in 0..3) { "A rotação deve estar entre 0 e 3." }
        require(context.targetPackage == null || PACKAGE_NAME.matches(context.targetPackage.trim())) {
            "O pacote de destino é inválido."
        }
        val insets = listOf(
            context.insetLeftPx, context.insetTopPx,
            context.insetRightPx, context.insetBottomPx
        )
        require(insets.all { it >= 0 }) { "Os insets não podem ser negativos." }
        require(context.insetLeftPx + context.insetRightPx < context.widthPx) {
            "Os insets horizontais excedem o display."
        }
        require(context.insetTopPx + context.insetBottomPx < context.heightPx) {
            "Os insets verticais excedem o display."
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 80
        private val PACKAGE_NAME =
            Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+")
    }
}

class CoordinateMapNotFoundException(name: String) :
    NoSuchElementException("Mapa de coordenadas não encontrado: $name")
