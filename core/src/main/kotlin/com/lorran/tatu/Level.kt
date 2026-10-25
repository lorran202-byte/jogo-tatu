package com.lorran.tatu

import com.badlogic.gdx.utils.JsonReader
import kotlin.math.floor

/**
 * Fase em grade. Linha 0 é o começo (perto da câmera); o caminho segue para -Z.
 * Legenda em tools/fase1.py.
 */
class Level(val name: String, val rows: List<String>, val b: Balance) {
    val cols = rows.first().length
    val tile = b.tile

    init {
        require(rows.all { it.length == cols }) { "todas as linhas precisam ter $cols colunas" }
    }

    val rowCount get() = rows.size

    fun cellX(col: Int) = (col - (cols - 1) / 2f) * tile
    fun cellZ(row: Int) = -row * tile
    fun colOf(x: Float) = floor(x / tile + (cols - 1) / 2f + 0.5f).toInt()
    fun rowOf(z: Float) = floor(-z / tile + 0.5f).toInt()

    val minX get() = cellX(0) - tile / 2f
    val maxX get() = cellX(cols - 1) + tile / 2f
    val startZ get() = tile / 2f
    val endZ get() = cellZ(rowCount - 1) - tile / 2f

    fun charAt(col: Int, row: Int): Char =
        if (row < 0 || row >= rowCount || col < 0 || col >= cols) '_' else rows[row][col]

    private fun rowBase(row: Int) = if (row in rows.indices && rows[row].contains('#')) b.raisedHeight else 0f

    /** Altura do chão na célula, ou null se for buraco. */
    fun groundAt(col: Int, row: Int): Float? = when (val c = charAt(col, row)) {
        '_', 'f' -> null
        '#' -> b.raisedHeight
        '.', '=' -> 0f
        else -> if (c in OBJECTS) rowBase(row) else 0f
    }

    fun groundAtPos(x: Float, z: Float): Float? = groundAt(colOf(x), rowOf(z))

    fun find(ch: Char): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        for (r in rows.indices) for (c in 0 until cols) if (rows[r][c] == ch) out.add(c to r)
        return out
    }

    /** Caixas que podem ser quebradas (a de pulo não conta). */
    fun countCrates() = rows.sumOf { r -> r.count { it in BREAKABLE } }

    companion object {
        const val OBJECTS = "PGCVBKFE"
        const val CRATES = "CVBK"
        const val BREAKABLE = "CVK"

        fun parse(json: String, b: Balance): Level {
            val v = JsonReader().parse(json)
            val rows = ArrayList<String>()
            for (r in v.get("rows")) rows.add(r.asString())
            return Level(v.getString("name", "Fase"), rows, b)
        }
    }
}
