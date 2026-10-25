package com.lorran.tatu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable

/** Paleta própria do jogo (tropical, tons de mangue e açaí). */
object Pal {
    val SKY = Color(0.56f, 0.82f, 0.93f, 1f)
    val SHELL = Color(0.80f, 0.60f, 0.33f, 1f)
    val BAND = Color(0.55f, 0.36f, 0.18f, 1f)
    val SKIN = Color(0.92f, 0.74f, 0.58f, 1f)
    val SKIN_DARK = Color(0.62f, 0.42f, 0.30f, 1f)
    val SNOUT = Color(0.95f, 0.62f, 0.58f, 1f)
    val WHITE = Color(0.97f, 0.97f, 0.95f, 1f)
    val BLACK = Color(0.08f, 0.07f, 0.09f, 1f)
    val CRAB = Color(0.92f, 0.32f, 0.22f, 1f)
    val CRAB_DARK = Color(0.68f, 0.18f, 0.14f, 1f)
    val ACAI = Color(0.33f, 0.10f, 0.42f, 1f)
    val LEAF = Color(0.20f, 0.56f, 0.26f, 1f)
    val LEAF2 = Color(0.12f, 0.45f, 0.30f, 1f)
    val TRUNK = Color(0.42f, 0.29f, 0.17f, 1f)
    val GRASS_A = Color(0.46f, 0.76f, 0.33f, 1f)
    val GRASS_B = Color(0.40f, 0.69f, 0.29f, 1f)
    val DIRT = Color(0.56f, 0.39f, 0.23f, 1f)
    val STONE_TOP = Color(0.68f, 0.65f, 0.56f, 1f)
    val STONE = Color(0.52f, 0.49f, 0.42f, 1f)
    val PLANK = Color(0.66f, 0.46f, 0.25f, 1f)
    val HILL = Color(0.26f, 0.52f, 0.27f, 1f)
    val HILL_SIDE = Color(0.40f, 0.30f, 0.19f, 1f)
    val WATER = Color(0.18f, 0.42f, 0.46f, 1f)
    val GOLD = Color(1f, 0.82f, 0.25f, 1f)
    val WOOD = Color(0.78f, 0.52f, 0.25f, 1f)
}

class Models : Disposable {
    private val mb = ModelBuilder()
    private val m = Matrix4()
    private val disposables = ArrayList<Disposable>()
    private val attrs = (Usage.Position or Usage.Normal).toLong()
    private val texAttrs = (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong()

    val tatu: Model
    val ball: Model
    val crab: Model
    val acai: Model
    val goalFrame: Model
    val goalDisc: Model
    val shadow: Model
    val crates = HashMap<Char, Model>()
    val particles = HashMap<String, Model>()

    private fun mat(c: Color) = Material(ColorAttribute.createDiffuse(c))

    private fun <T : Disposable> keep(d: T): T {
        disposables.add(d)
        return d
    }

    private fun part(id: String, c: Color): MeshPartBuilder = mb.part(id, GL20.GL_TRIANGLES, attrs, mat(c))

    private fun MeshPartBuilder.at(x: Float, y: Float, z: Float, axis: Vector3? = null, deg: Float = 0f): MeshPartBuilder {
        m.idt().translate(x, y, z)
        if (axis != null) m.rotate(axis, deg)
        setVertexTransform(m)
        return this
    }

    init {
        tatu = buildTatu()
        ball = buildBall()
        crab = buildCrab()
        acai = buildAcai()
        goalFrame = buildGoalFrame()
        goalDisc = buildGoalDisc()
        shadow = keep(mb.createCylinder(1.1f, 0.02f, 1.1f, 16,
            Material(ColorAttribute.createDiffuse(Pal.BLACK), BlendingAttribute(0.35f)), attrs))
        crates['C'] = crateModel(Pal.WOOD, Color(0.46f, 0.27f, 0.10f, 1f)) { pm ->
            pm.setColor(Pal.ACAI); pm.fillCircle(32, 32, 8)
            pm.setColor(Pal.LEAF); pm.fillTriangle(32, 24, 38, 18, 34, 26)
        }
        crates['B'] = crateModel(Color(0.36f, 0.70f, 0.32f, 1f), Color(0.14f, 0.40f, 0.16f, 1f)) { pm ->
            pm.setColor(Pal.WHITE)
            pm.fillTriangle(32, 10, 13, 32, 51, 32)
            pm.fillRectangle(25, 32, 14, 20)
        }
        crates['K'] = crateModel(Color(0.28f, 0.52f, 0.88f, 1f), Color(0.10f, 0.25f, 0.52f, 1f)) { pm ->
            pm.setColor(Pal.WHITE)
            pm.fillRectangle(20, 10, 5, 44)
            pm.setColor(Pal.GOLD)
            pm.fillTriangle(25, 10, 25, 32, 48, 21)
        }
        crates['V'] = crateModel(Color(0.98f, 0.80f, 0.40f, 1f), Color(0.62f, 0.40f, 0.12f, 1f)) { pm ->
            pm.setColor(Color(0.90f, 0.20f, 0.35f, 1f))
            pm.fillCircle(24, 26, 9)
            pm.fillCircle(40, 26, 9)
            pm.fillTriangle(15, 29, 49, 29, 32, 50)
        }
        for ((name, c) in listOf("madeira" to Pal.WOOD, "acai" to Color(0.55f, 0.20f, 0.70f, 1f),
            "caranguejo" to Pal.CRAB, "brilho" to Pal.GOLD, "casco" to Pal.SHELL)) {
            particles[name] = keep(mb.createBox(0.22f, 0.22f, 0.22f,
                Material(ColorAttribute.createDiffuse(c), ColorAttribute.createEmissive(c.r * 0.3f, c.g * 0.3f, c.b * 0.3f, 1f)), attrs))
        }
    }

    private fun crateModel(base: Color, dark: Color, icon: (Pixmap) -> Unit): Model {
        val pm = Pixmap(64, 64, Pixmap.Format.RGBA8888)
        pm.setColor(base); pm.fill()
        pm.setColor(dark)
        pm.fillRectangle(0, 0, 64, 5); pm.fillRectangle(0, 59, 64, 5)
        pm.fillRectangle(0, 0, 5, 64); pm.fillRectangle(59, 0, 5, 64)
        pm.setColor(dark.r, dark.g, dark.b, 0.45f)
        pm.fillRectangle(5, 21, 54, 2); pm.fillRectangle(5, 42, 54, 2)
        icon(pm)
        val tex = keep(Texture(pm, true))
        tex.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        pm.dispose()
        return keep(mb.createBox(1.5f, 1.5f, 1.5f, Material(TextureAttribute.createDiffuse(tex)), texAttrs))
    }

    private fun buildTatu(): Model {
        mb.begin()
        part("casco", Pal.SHELL).at(0f, 0.66f, 0f).also { SphereShapeBuilder.build(it, 1.1f, 0.92f, 1.45f, 16, 12) }
        val bands = part("faixas", Pal.BAND)
        for ((z, d) in listOf(-0.32f to 1.02f, 0f to 1.16f, 0.32f to 1.04f)) {
            bands.at(0f, 0.7f, z, Vector3.X, 90f)
            CylinderShapeBuilder.build(bands, d, 0.13f, d * 0.86f, 16)
        }
        val skin = part("pele", Pal.SKIN)
        skin.at(0f, 0.62f, -0.78f); SphereShapeBuilder.build(skin, 0.56f, 0.5f, 0.62f, 12, 10)
        part("focinho", Pal.SNOUT).at(0f, 0.56f, -1.08f, Vector3.X, -90f).also { ConeShapeBuilder.build(it, 0.3f, 0.45f, 0.26f, 10) }
        val dark = part("escuro", Pal.SKIN_DARK)
        for (s in listOf(-1f, 1f)) {
            dark.at(0.17f * s, 0.9f, -0.72f); ConeShapeBuilder.build(dark, 0.15f, 0.24f, 0.09f, 6)
            for (fz in listOf(-0.38f, 0.38f)) {
                dark.at(0.34f * s, 0.12f, fz); SphereShapeBuilder.build(dark, 0.27f, 0.22f, 0.32f, 8, 6)
            }
        }
        val tail = part("rabo", Pal.BAND)
        tail.at(0f, 0.36f, 0.82f, Vector3.X, 90f); ConeShapeBuilder.build(tail, 0.18f, 0.5f, 0.18f, 8)
        val white = part("olhos", Pal.WHITE)
        val black = part("pupilas", Pal.BLACK)
        for (s in listOf(-1f, 1f)) {
            white.at(0.16f * s, 0.73f, -0.98f); SphereShapeBuilder.build(white, 0.17f, 0.19f, 0.12f, 8, 6)
        }
        for (s in listOf(-1f, 1f)) {
            black.at(0.17f * s, 0.74f, -1.04f); SphereShapeBuilder.build(black, 0.08f, 0.1f, 0.05f, 6, 5)
        }
        black.at(0f, 0.56f, -1.32f); SphereShapeBuilder.build(black, 0.1f, 0.08f, 0.08f, 6, 5)
        return keep(mb.end())
    }

    private fun buildBall(): Model {
        mb.begin()
        part("bola", Pal.SHELL).at(0f, 0f, 0f).also { SphereShapeBuilder.build(it, 1.3f, 1.3f, 1.3f, 16, 12) }
        val bands = part("faixas", Pal.BAND)
        for ((z, d) in listOf(-0.36f to 1.14f, 0f to 1.36f, 0.36f to 1.14f)) {
            bands.at(0f, 0f, z, Vector3.X, 90f)
            CylinderShapeBuilder.build(bands, d, 0.14f, d, 16)
        }
        part("ponta", Pal.SKIN_DARK).at(0f, 0f, -0.62f).also { SphereShapeBuilder.build(it, 0.22f, 0.22f, 0.12f, 8, 6) }
        return keep(mb.end())
    }

    private fun buildCrab(): Model {
        mb.begin()
        part("corpo", Pal.CRAB).at(0f, 0.4f, 0f).also { SphereShapeBuilder.build(it, 0.95f, 0.5f, 0.75f, 14, 10) }
        val dark = part("garras", Pal.CRAB_DARK)
        for (s in listOf(-1f, 1f)) {
            dark.at(0.62f * s, 0.45f, 0.3f); SphereShapeBuilder.build(dark, 0.36f, 0.3f, 0.36f, 10, 8)
            dark.at(0.72f * s, 0.5f, 0.5f); SphereShapeBuilder.build(dark, 0.18f, 0.22f, 0.24f, 8, 6)
            dark.at(0.16f * s, 0.72f, 0.18f); BoxShapeBuilder.build(dark, 0f, 0f, 0f, 0.06f, 0.26f, 0.06f)
            for (k in -1..1) {
                dark.at(0.5f * s, 0.2f, k * 0.2f, Vector3.Z, 35f * s)
                BoxShapeBuilder.build(dark, 0f, 0f, 0f, 0.42f, 0.07f, 0.07f)
            }
        }
        val white = part("olhos", Pal.WHITE)
        for (s in listOf(-1f, 1f)) {
            white.at(0.16f * s, 0.88f, 0.2f); SphereShapeBuilder.build(white, 0.16f, 0.16f, 0.16f, 8, 6)
        }
        val black = part("pupilas", Pal.BLACK)
        for (s in listOf(-1f, 1f)) {
            black.at(0.16f * s, 0.89f, 0.27f); SphereShapeBuilder.build(black, 0.07f, 0.08f, 0.04f, 6, 5)
        }
        return keep(mb.end())
    }

    private fun buildAcai(): Model {
        mb.begin()
        mb.part("fruta", GL20.GL_TRIANGLES, attrs,
            Material(ColorAttribute.createDiffuse(Pal.ACAI), ColorAttribute.createEmissive(0.16f, 0.04f, 0.22f, 1f)))
            .also { it.at(0f, 0f, 0f); SphereShapeBuilder.build(it, 0.46f, 0.46f, 0.46f, 12, 10) }
        part("brilho", Pal.WHITE).at(-0.1f, 0.11f, 0.15f).also { SphereShapeBuilder.build(it, 0.1f, 0.1f, 0.1f, 6, 5) }
        part("folha", Pal.LEAF).at(0.06f, 0.3f, 0f, Vector3.Z, -30f).also { ConeShapeBuilder.build(it, 0.14f, 0.24f, 0.06f, 6) }
        return keep(mb.end())
    }

    private fun buildGoalFrame(): Model {
        mb.begin()
        val wood = part("tronco", Pal.TRUNK)
        for (s in listOf(-1f, 1f)) {
            wood.at(1.7f * s, 1.6f, 0f); CylinderShapeBuilder.build(wood, 0.36f, 3.2f, 0.36f, 10)
        }
        wood.at(0f, 3.25f, 0f, Vector3.Z, 90f); CylinderShapeBuilder.build(wood, 0.4f, 4.2f, 0.4f, 10)
        val leaf = part("folhas", Pal.LEAF)
        for (s in listOf(-1f, 1f)) {
            leaf.at(1.7f * s, 3.55f, 0f); SphereShapeBuilder.build(leaf, 0.9f, 0.6f, 0.9f, 10, 8)
        }
        return keep(mb.end())
    }

    private fun buildGoalDisc(): Model {
        mb.begin()
        mb.part("portal", GL20.GL_TRIANGLES, attrs,
            Material(ColorAttribute.createDiffuse(Pal.GOLD), ColorAttribute.createEmissive(0.55f, 0.42f, 0.1f, 1f), BlendingAttribute(0.85f)))
            .also { it.at(0f, 0f, 0f, Vector3.X, 90f); CylinderShapeBuilder.build(it, 2.7f, 0.12f, 2.7f, 28) }
        val acai = mb.part("centro", GL20.GL_TRIANGLES, attrs,
            Material(ColorAttribute.createDiffuse(Pal.ACAI), ColorAttribute.createEmissive(0.2f, 0.05f, 0.3f, 1f)))
        acai.at(0f, 0f, 0.05f, Vector3.X, 90f); CylinderShapeBuilder.build(acai, 1.2f, 0.14f, 1.2f, 20)
        return keep(mb.end())
    }

    /** Monta o cenário da fase em blocos (12 linhas cada), um modelo por bloco. */
    fun buildLevelChunks(level: Level): List<Model> {
        val chunks = ArrayList<Model>()
        val first = -5
        val last = level.rowCount + 7
        var r0 = first
        val rnd = java.util.Random(42)
        while (r0 <= last) {
            val r1 = minOf(last, r0 + 11)
            chunks.add(keep(buildChunk(level, r0, r1, rnd)))
            r0 = r1 + 1
        }
        return chunks
    }

    private class Shape(val color: Color, val build: (MeshPartBuilder) -> Unit)

    private fun buildChunk(level: Level, r0: Int, r1: Int, rnd: java.util.Random): Model {
        val shapes = ArrayList<Shape>()
        val t = level.tile
        val half = level.cols * t / 2f

        fun box(c: Color, x: Float, y: Float, z: Float, w: Float, h: Float, d: Float) =
            shapes.add(Shape(c) { b -> b.at(0f, 0f, 0f); BoxShapeBuilder.build(b, x, y, z, w, h, d) })

        for (r in r0..r1) {
            val z = level.cellZ(r)
            val decor = r < 0 || r >= level.rowCount
            for (c in 0 until level.cols) {
                val x = level.cellX(c)
                val ch = if (decor) '.' else level.charAt(c, r)
                val g = if (decor) 0f else level.groundAt(c, r) ?: continue
                when {
                    ch == '=' -> for (k in -1..1) box(Pal.PLANK, x, g - 0.12f, z + k * 0.66f, 1.9f, 0.24f, 0.56f)
                    ch == '#' || (g > 0f) -> {
                        box(Pal.STONE_TOP, x, g - 0.15f, z, t, 0.3f, t)
                        box(Pal.STONE, x, (g - 0.3f - 5f) / 2f, z, t * 0.98f, g - 0.3f + 5f, t * 0.98f)
                    }
                    else -> {
                        box(if ((r + c) % 2 == 0) Pal.GRASS_A else Pal.GRASS_B, x, g - 0.15f, z, t, 0.3f, t)
                        box(Pal.DIRT, x, (g - 0.3f - 5f) / 2f, z, t * 0.98f, g - 0.3f + 5f, t * 0.98f)
                    }
                }
            }
            // água no fundo dos buracos
            box(Pal.WATER, 0f, -5.2f, z, half * 2f, 0.2f, t)
            // morros dos lados
            for (s in listOf(-1f, 1f)) {
                val hx = s * (half + 5f)
                val top = 2.2f + (if (r % 3 == 0) 0.3f else 0f)
                box(Pal.HILL, hx, top - 0.2f, z, 10f, 0.4f, t)
                box(Pal.HILL_SIDE, hx, (top - 0.4f - 6f) / 2f, z, 10f, top - 0.4f + 6f, t)
                if (rnd.nextFloat() < 0.6f) {
                    val tx = s * (half + 1.3f + rnd.nextFloat() * 6f)
                    val tz = z + (rnd.nextFloat() - 0.5f) * 1.2f
                    if (rnd.nextFloat() < 0.45f) palm(shapes, tx, top, tz, rnd) else tree(shapes, tx, top, tz, rnd)
                } else if (rnd.nextFloat() < 0.5f) {
                    val bx = s * (half + 0.9f + rnd.nextFloat() * 2f)
                    shapes.add(Shape(Pal.LEAF2) { b -> b.at(bx, top + 0.3f, z); SphereShapeBuilder.build(b, 1.4f, 0.9f, 1.2f, 8, 6) })
                }
            }
        }

        mb.begin()
        for ((color, group) in shapes.groupBy { it.color }) {
            val b = part("p" + color.toIntBits(), color)
            group.forEach { it.build(b) }
            b.setVertexTransform(null)
        }
        return mb.end()
    }

    private fun tree(shapes: ArrayList<Shape>, x: Float, top: Float, z: Float, rnd: java.util.Random) {
        val h = 2.2f + rnd.nextFloat() * 1.2f
        shapes.add(Shape(Pal.TRUNK) { b -> b.at(x, top + h / 2f, z); CylinderShapeBuilder.build(b, 0.42f, h, 0.42f, 7) })
        val s = 2.2f + rnd.nextFloat() * 0.8f
        val leaf = if (rnd.nextBoolean()) Pal.LEAF else Pal.LEAF2
        shapes.add(Shape(leaf) { b -> b.at(x, top + h + s * 0.3f, z); SphereShapeBuilder.build(b, s, s * 0.85f, s, 9, 7) })
        shapes.add(Shape(leaf) { b -> b.at(x + 0.5f, top + h + s * 0.6f, z - 0.3f); SphereShapeBuilder.build(b, s * 0.6f, s * 0.55f, s * 0.6f, 8, 6) })
    }

    /** Palmeira de açaí: tronco fino, folhas em leque e um cacho roxo. */
    private fun palm(shapes: ArrayList<Shape>, x: Float, top: Float, z: Float, rnd: java.util.Random) {
        val h = 4f + rnd.nextFloat() * 1.8f
        shapes.add(Shape(Pal.TRUNK) { b -> b.at(x, top + h / 2f, z); CylinderShapeBuilder.build(b, 0.24f, h, 0.24f, 6) })
        val y = top + h
        val turn = rnd.nextFloat() * 70f
        for (k in 0 until 6) {
            val ang = turn + k * 60f
            shapes.add(Shape(Pal.LEAF) { b ->
                m.idt().translate(x, y, z).rotate(Vector3.Y, ang).rotate(Vector3.Z, -28f).translate(1.05f, 0f, 0f)
                b.setVertexTransform(m)
                SphereShapeBuilder.build(b, 2.2f, 0.12f, 0.55f, 8, 4)
            })
        }
        shapes.add(Shape(Pal.ACAI) { b -> b.at(x + 0.25f, y - 0.45f, z); SphereShapeBuilder.build(b, 0.45f, 0.6f, 0.45f, 7, 6) })
    }

    override fun dispose() {
        disposables.forEach { it.dispose() }
        disposables.clear()
    }
}
