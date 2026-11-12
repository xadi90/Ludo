package com.example.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure normalized [0f..1f] coordinate geometry for both 4-Player Classic (15x15)
 * and 6-Player Hexagonal/6-Arm Star Ludo boards.
 * Ensures accurate token rendering, tap hit-testing, and distortion-free scaling
 * across any screen size or orientation.
 */
object BoardGeometry {

    // --- 4-PLAYER 15x15 CLASSIC BOARD ---
    // Clockwise 52-cell main track (row, col)
    private val fourPlayerTrackGrid: List<Pair<Int, Int>> = listOf(
        // Red arm outgoing (top of left arm -> going right)
        6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5,
        // Top arm left column going up
        5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6, 0 to 6,
        // Top bridge
        0 to 7, 0 to 8,
        // Green arm going down
        1 to 8, 2 to 8, 3 to 8, 4 to 8, 5 to 8,
        // Right arm top row going right
        6 to 9, 6 to 10, 6 to 11, 6 to 12, 6 to 13, 6 to 14,
        // Right bridge
        7 to 14, 8 to 14,
        // Yellow arm going left
        8 to 13, 8 to 12, 8 to 11, 8 to 10, 8 to 9,
        // Bottom arm right column going down
        9 to 8, 10 to 8, 11 to 8, 12 to 8, 13 to 8, 14 to 8,
        // Bottom bridge
        14 to 7, 14 to 6,
        // Blue arm going up
        13 to 6, 12 to 6, 11 to 6, 10 to 6, 9 to 6,
        // Left arm bottom row going left
        8 to 5, 8 to 4, 8 to 3, 8 to 2, 8 to 1, 8 to 0,
        // Left bridge
        7 to 0, 6 to 0
    )

    private val fourPlayerStartOffsets = mapOf(
        PlayerColor.RED to 0,
        PlayerColor.GREEN to 13,
        PlayerColor.YELLOW to 26,
        PlayerColor.BLUE to 39
    )

    private val fourPlayerStarIndices = setOf(8, 21, 34, 47)
    private val fourPlayerStartIndices = setOf(0, 13, 26, 39)

    private val fourPlayerHomeColumns: Map<PlayerColor, List<Pair<Int, Int>>> = mapOf(
        PlayerColor.RED to listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5),
        PlayerColor.GREEN to listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7),
        PlayerColor.YELLOW to listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9),
        PlayerColor.BLUE to listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7)
    )

    private val fourPlayerBaseCenters: Map<PlayerColor, List<Pair<Float, Float>>> = mapOf(
        PlayerColor.RED to listOf(2f to 2f, 4f to 2f, 2f to 4f, 4f to 4f),
        PlayerColor.GREEN to listOf(2f to 11f, 4f to 11f, 2f to 13f, 4f to 13f),
        PlayerColor.YELLOW to listOf(11f to 11f, 13f to 11f, 11f to 13f, 13f to 13f),
        PlayerColor.BLUE to listOf(11f to 2f, 13f to 2f, 11f to 4f, 13f to 4f)
    )

    private val fourPlayerFinishCenters: Map<PlayerColor, Offset> = mapOf(
        PlayerColor.RED to Offset(6.5f / 15f, 7.5f / 15f),
        PlayerColor.GREEN to Offset(7.5f / 15f, 6.5f / 15f),
        PlayerColor.YELLOW to Offset(8.5f / 15f, 7.5f / 15f),
        PlayerColor.BLUE to Offset(7.5f / 15f, 8.5f / 15f)
    )

    private fun gridToNormalized(row: Float, col: Float): Offset {
        return Offset((col + 0.5f) / 15f, (row + 0.5f) / 15f)
    }

    // --- 6-PLAYER HEXAGONAL / 6-ARM STAR BOARD ---
    // 6 arms at angles -90, -30, +30, +90, +150, -150 degrees (clockwise from top)
    private val sixArmAnglesDeg = listOf(-90.0, -30.0, 30.0, 90.0, 150.0, -150.0)
    private val sixRadii = listOf(0.188f, 0.242f, 0.296f, 0.350f, 0.404f)
    private const val SIX_LANE_OFFSET = 0.036f

    private fun polarArmPoint(angleDeg: Double, radial: Float, lateral: Float): Offset {
        val rad = angleDeg * PI / 180.0
        val ux = cos(rad).toFloat()
        val uy = sin(rad).toFloat()
        // Clockwise tangent vector
        val tx = -uy
        val ty = ux
        return Offset(
            x = 0.5f + ux * radial + tx * lateral,
            y = 0.5f + uy * radial + ty * lateral
        )
    }

    val sixPlayerTrackNormalized: List<Offset> by lazy {
        val list = ArrayList<Offset>(72)
        for (k in 0 until 6) {
            val angle = sixArmAnglesDeg[k]
            // 0..4: left lane going outward
            for (r in sixRadii) {
                list.add(polarArmPoint(angle, r, -SIX_LANE_OFFSET))
            }
            // 5: outer center tip
            list.add(polarArmPoint(angle, sixRadii[4] + 0.025f, 0f))
            // 6..10: right lane coming inward
            for (r in sixRadii.reversed()) {
                list.add(polarArmPoint(angle, r, SIX_LANE_OFFSET))
            }
            // 11: bridge cell between arm k and arm k+1
            val bridgeAngle = angle + 30.0
            list.add(polarArmPoint(bridgeAngle, 0.175f, 0f))
        }
        list
    }

    private val sixPlayerStartOffsets = mapOf(
        PlayerColor.RED to 7,
        PlayerColor.GREEN to 19,
        PlayerColor.YELLOW to 31,
        PlayerColor.BLUE to 43,
        PlayerColor.PURPLE to 55,
        PlayerColor.ORANGE to 67
    )

    private val sixPlayerStartIndices = setOf(7, 19, 31, 43, 55, 67)
    private val sixPlayerStarIndices = setOf(2, 14, 26, 38, 50, 62)

    private val sixPlayerHomeColumns: Map<PlayerColor, List<Offset>> by lazy {
        PlayerColor.sixPlayerOrder.mapIndexed { idx, color ->
            val angle = sixArmAnglesDeg[idx]
            val cells = listOf(
                polarArmPoint(angle, sixRadii[3], 0f),
                polarArmPoint(angle, sixRadii[2], 0f),
                polarArmPoint(angle, sixRadii[1], 0f),
                polarArmPoint(angle, sixRadii[0], 0f),
                polarArmPoint(angle, 0.132f, 0f)
            )
            color to cells
        }.toMap()
    }

    val sixPlayerBaseCenters: Map<PlayerColor, Offset> by lazy {
        PlayerColor.sixPlayerOrder.mapIndexed { idx, color ->
            val baseAngle = sixArmAnglesDeg[idx] + 30.0
            color to polarArmPoint(baseAngle, 0.365f, 0f)
        }.toMap()
    }

    private val sixPlayerBaseTokenSlots: Map<PlayerColor, List<Offset>> by lazy {
        PlayerColor.sixPlayerOrder.mapIndexed { idx, color ->
            val baseAngle = sixArmAnglesDeg[idx] + 30.0
            val center = polarArmPoint(baseAngle, 0.365f, 0f)
            val d = 0.032f
            color to listOf(
                Offset(center.x - d, center.y - d),
                Offset(center.x + d, center.y - d),
                Offset(center.x - d, center.y + d),
                Offset(center.x + d, center.y + d)
            )
        }.toMap()
    }

    private val sixPlayerFinishCenters: Map<PlayerColor, Offset> by lazy {
        PlayerColor.sixPlayerOrder.mapIndexed { idx, color ->
            val angle = sixArmAnglesDeg[idx]
            color to polarArmPoint(angle, 0.068f, 0f)
        }.toMap()
    }

    // --- PUBLIC QUERY API ---

    fun getStartOffset(boardType: BoardType, color: PlayerColor): Int {
        return when (boardType) {
            BoardType.FOUR_PLAYER -> fourPlayerStartOffsets[color] ?: 0
            BoardType.SIX_PLAYER -> sixPlayerStartOffsets[color] ?: 0
        }
    }

    fun toGlobalTrackIndex(boardType: BoardType, color: PlayerColor, progress: Int): Int? {
        if (progress < 0 || progress >= boardType.trackLength - 1) return null
        val start = getStartOffset(boardType, color)
        return (start + progress) % boardType.trackLength
    }

    fun isSafeGlobalIndex(boardType: BoardType, globalIndex: Int, safeStarsEnabled: Boolean): Boolean {
        val starts = when (boardType) {
            BoardType.FOUR_PLAYER -> fourPlayerStartIndices
            BoardType.SIX_PLAYER -> sixPlayerStartIndices
        }
        val stars = when (boardType) {
            BoardType.FOUR_PLAYER -> fourPlayerStarIndices
            BoardType.SIX_PLAYER -> sixPlayerStarIndices
        }
        return globalIndex in starts || (safeStarsEnabled && globalIndex in stars)
    }

    fun isStarGlobalIndex(boardType: BoardType, globalIndex: Int): Boolean {
        return when (boardType) {
            BoardType.FOUR_PLAYER -> globalIndex in fourPlayerStarIndices
            BoardType.SIX_PLAYER -> globalIndex in sixPlayerStarIndices
        }
    }

    fun getAllStarIndices(boardType: BoardType): List<Int> {
        return when (boardType) {
            BoardType.FOUR_PLAYER -> fourPlayerStarIndices.sorted()
            BoardType.SIX_PLAYER -> sixPlayerStarIndices.sorted()
        }
    }

    fun getAllStartIndices(boardType: BoardType): Map<PlayerColor, Int> {
        return when (boardType) {
            BoardType.FOUR_PLAYER -> fourPlayerStartOffsets
            BoardType.SIX_PLAYER -> sixPlayerStartOffsets
        }
    }

    fun getTrackNormalizedCenter(boardType: BoardType, globalIndex: Int): Offset {
        return when (boardType) {
            BoardType.FOUR_PLAYER -> {
                val (r, c) = fourPlayerTrackGrid[globalIndex % 52]
                gridToNormalized(r.toFloat(), c.toFloat())
            }
            BoardType.SIX_PLAYER -> sixPlayerTrackNormalized[globalIndex % 72]
        }
    }

    fun getHomeColumnCenters(boardType: BoardType, color: PlayerColor): List<Offset> {
        return when (boardType) {
            BoardType.FOUR_PLAYER -> (fourPlayerHomeColumns[color] ?: emptyList())
                .map { (r, c) -> gridToNormalized(r.toFloat(), c.toFloat()) }
            BoardType.SIX_PLAYER -> sixPlayerHomeColumns[color] ?: emptyList()
        }
    }

    fun getBaseSlotCenter(boardType: BoardType, color: PlayerColor, tokenIndex: Int): Offset {
        return when (boardType) {
            BoardType.FOUR_PLAYER -> {
                val slots = fourPlayerBaseCenters[color] ?: listOf(2f to 2f)
                val (r, c) = slots[tokenIndex.coerceIn(0, 3)]
                Offset(c / 15f, r / 15f)
            }
            BoardType.SIX_PLAYER -> {
                val slots = sixPlayerBaseTokenSlots[color] ?: listOf(Offset(0.5f, 0.5f))
                slots[tokenIndex.coerceIn(0, 3)]
            }
        }
    }

    fun getTokenNormalizedPosition(boardType: BoardType, token: TokenState): Offset {
        if (token.isInBase()) {
            return getBaseSlotCenter(boardType, token.ownerColor, token.tokenIndex)
        }
        if (token.isFinished(boardType)) {
            return when (boardType) {
                BoardType.FOUR_PLAYER -> fourPlayerFinishCenters[token.ownerColor] ?: Offset(0.5f, 0.5f)
                BoardType.SIX_PLAYER -> sixPlayerFinishCenters[token.ownerColor] ?: Offset(0.5f, 0.5f)
            }
        }
        if (token.isInHomeColumn(boardType)) {
            val colIndex = token.progress - (boardType.trackLength - 1)
            val cols = getHomeColumnCenters(boardType, token.ownerColor)
            return cols.getOrElse(colIndex) { Offset(0.5f, 0.5f) }
        }
        val globalIdx = toGlobalTrackIndex(boardType, token.ownerColor, token.progress) ?: 0
        return getTrackNormalizedCenter(boardType, globalIdx)
    }

    fun getFourPlayerGridTrack(): List<Pair<Int, Int>> = fourPlayerTrackGrid
    fun getFourPlayerGridHomeColumns(): Map<PlayerColor, List<Pair<Int, Int>>> = fourPlayerHomeColumns
}
