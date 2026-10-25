package com.lorran.tatu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ExtendViewport

/** Botão simples da interface (coordenadas virtuais, y para cima). */
class UiButton(var x: Float, var y: Float, var w: Float, var h: Float, var label: String, var color: Color = Ui.GREEN) {
    var pressed = false
    fun contains(px: Float, py: Float) = px >= x && px <= x + w && py >= y && py <= y + h
}

/** Interface 2D: tela virtual de 1280x720 que se estica para telas mais largas. */
class Ui : Disposable {
    val viewport = ExtendViewport(1280f, 720f)
    val shapes = ShapeRenderer()
    val batch = SpriteBatch()
    val font = BitmapFont(Gdx.files.internal("fonts/fonte.fnt")).apply {
        region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setUseIntegerPositions(false)
    }
    private val layout = GlyphLayout()
    private val v = Vector2()
    private var mode = 0 // 0 nada, 1 formas, 2 texto

    /** Margens para fugir do entalhe da câmera e das bordas curvas. */
    var insetLeft = 0f
        private set
    var insetRight = 0f
        private set

    val width get() = viewport.worldWidth
    val height get() = viewport.worldHeight

    fun resize(w: Int, h: Int) {
        viewport.update(w, h, true)
        val k = viewport.worldWidth / w.toFloat()
        insetLeft = maxOf(Gdx.graphics.safeInsetLeft * k, 24f)
        insetRight = maxOf(Gdx.graphics.safeInsetRight * k, 24f)
    }

    fun toUi(sx: Int, sy: Int): Vector2 = viewport.unproject(v.set(sx.toFloat(), sy.toFloat()))

    fun begin() {
        viewport.apply()
        shapes.projectionMatrix = viewport.camera.combined
        batch.projectionMatrix = viewport.camera.combined
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun end() {
        when (mode) {
            1 -> shapes.end()
            2 -> batch.end()
        }
        mode = 0
    }

    private fun sh(): ShapeRenderer {
        if (mode == 2) batch.end()
        if (mode != 1) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            mode = 1
        }
        return shapes
    }

    private fun sb(): SpriteBatch {
        if (mode == 1) shapes.end()
        if (mode != 2) {
            batch.begin()
            mode = 2
        }
        return batch
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, c: Color, a: Float = c.a) {
        sh().setColor(c.r, c.g, c.b, a)
        shapes.rect(x, y, w, h)
    }

    fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, a: Float = c.a) {
        val s = sh()
        s.setColor(c.r, c.g, c.b, a)
        val rr = minOf(r, w / 2f, h / 2f)
        s.rect(x + rr, y, w - 2 * rr, h)
        s.rect(x, y + rr, rr, h - 2 * rr)
        s.rect(x + w - rr, y + rr, rr, h - 2 * rr)
        s.circle(x + rr, y + rr, rr, 16)
        s.circle(x + w - rr, y + rr, rr, 16)
        s.circle(x + rr, y + h - rr, rr, 16)
        s.circle(x + w - rr, y + h - rr, rr, 16)
    }

    fun circle(x: Float, y: Float, r: Float, c: Color, a: Float = c.a) {
        sh().setColor(c.r, c.g, c.b, a)
        shapes.circle(x, y, r, 40)
    }

    fun ring(x: Float, y: Float, r: Float, thick: Float, c: Color, a: Float = c.a) {
        val s = sh()
        s.setColor(c.r, c.g, c.b, a)
        val n = 40
        for (i in 0 until n) {
            val a0 = i * Math.PI.toFloat() * 2f / n
            val a1 = (i + 1) * Math.PI.toFloat() * 2f / n
            s.rectLine(x + kotlin.math.cos(a0) * r, y + kotlin.math.sin(a0) * r,
                x + kotlin.math.cos(a1) * r, y + kotlin.math.sin(a1) * r, thick)
        }
    }

    fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, c: Color, a: Float = c.a) {
        sh().setColor(c.r, c.g, c.b, a)
        shapes.triangle(x1, y1, x2, y2, x3, y3)
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, c: Color, a: Float = c.a) {
        sh().setColor(c.r, c.g, c.b, a)
        shapes.rectLine(x1, y1, x2, y2, w)
    }

    /** Texto centrado verticalmente em y. align: Align.left / center / right. */
    fun text(s: String, x: Float, y: Float, scale: Float, c: Color = Color.WHITE, align: Int = Align.center, a: Float = 1f) {
        val b = sb()
        font.data.setScale(scale)
        font.setColor(c.r, c.g, c.b, a)
        layout.setText(font, s)
        val dx = when (align) {
            Align.center -> -layout.width / 2f
            Align.right -> -layout.width
            else -> 0f
        }
        font.draw(b, layout, x + dx, y + layout.height / 2f)
    }

    fun textWidth(s: String, scale: Float): Float {
        font.data.setScale(scale)
        layout.setText(font, s)
        return layout.width
    }

    fun button(b: UiButton, textScale: Float = 0.75f) {
        val c = b.color
        val press = if (b.pressed) 4f else 0f
        roundRect(b.x, b.y - 6f, b.w, b.h, 22f, SHADOW, 0.35f)
        roundRect(b.x, b.y - press, b.w, b.h, 22f, c)
        roundRect(b.x + 6f, b.y + b.h * 0.5f - press, b.w - 12f, b.h * 0.45f, 18f, Color.WHITE, 0.12f)
        text(b.label, b.x + b.w / 2f, b.y + b.h / 2f - press, textScale)
    }

    fun panel(x: Float, y: Float, w: Float, h: Float) {
        roundRect(x, y - 8f, w, h, 30f, SHADOW, 0.35f)
        roundRect(x, y, w, h, 30f, PANEL, 0.94f)
    }

    override fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }

    companion object {
        val GREEN = Color(0.30f, 0.66f, 0.30f, 1f)
        val PURPLE = Color(0.47f, 0.22f, 0.58f, 1f)
        val ORANGE = Color(0.93f, 0.52f, 0.20f, 1f)
        val BLUE = Color(0.22f, 0.50f, 0.80f, 1f)
        val GRAY = Color(0.42f, 0.42f, 0.48f, 1f)
        val PANEL = Color(0.16f, 0.12f, 0.22f, 1f)
        val SHADOW = Color(0f, 0f, 0f, 1f)
        val GOLD = Color(1f, 0.84f, 0.30f, 1f)
        val SKY_TOP = Color(0.40f, 0.72f, 0.92f, 1f)
    }
}
