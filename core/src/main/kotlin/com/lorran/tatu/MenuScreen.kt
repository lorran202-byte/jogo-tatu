package com.lorran.tatu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import kotlin.math.abs
import kotlin.math.sin

/** Tela base: cuida do tamanho da interface e dos botões. */
abstract class BaseScreen(val game: TatuGame) : ScreenAdapter() {
    val ui get() = game.ui
    protected val buttons = ArrayList<UiButton>()
    protected var time = 0f

    protected val buttonInput = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val p = ui.toUi(screenX, screenY)
            val b = buttons.firstOrNull { it.contains(p.x, p.y) } ?: return false
            b.pressed = true
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val p = ui.toUi(screenX, screenY)
            var used = false
            for (b in buttons.toList()) {
                if (b.pressed && b.contains(p.x, p.y)) {
                    game.audio.play("clique")
                    game.settings.vibrate(10)
                    b.pressed = false
                    onButton(b)
                    used = true
                    break
                }
            }
            buttons.forEach { it.pressed = false }
            return used
        }

        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                onBack()
                return true
            }
            return false
        }
    }

    open fun onButton(b: UiButton) {}
    open fun onBack() {}
    open fun layout() {}

    override fun show() {
        ui.resize(Gdx.graphics.width, Gdx.graphics.height)
        layout()
    }

    override fun resize(width: Int, height: Int) {
        ui.resize(width, height)
        layout()
    }

    protected fun skyBackground() {
        ui.shapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled)
        ui.shapes.rect(0f, 0f, ui.width, ui.height, Pal.SKY, Pal.SKY, Ui.SKY_TOP, Ui.SKY_TOP)
        ui.shapes.end()
    }
}

class MenuScreen(game: TatuGame) : BaseScreen(game) {
    private val play = UiButton(0f, 0f, 380f, 110f, "JOGAR", Ui.GREEN)
    private val options = UiButton(0f, 0f, 380f, 90f, "AJUSTES", Ui.BLUE)
    private val cam = PerspectiveCamera(40f, 1280f, 720f)
    private val batch = ModelBatch()
    private val env = Environment()
    private val tatu = ModelInstance(game.models.tatu)
    private val ball = ModelInstance(game.models.ball)
    private val acai = ModelInstance(game.models.acai)

    init {
        buttons.add(play)
        buttons.add(options)
        env.set(ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.65f, 1f))
        env.add(DirectionalLight().set(0.85f, 0.8f, 0.72f, -0.5f, -0.8f, -0.6f))
        cam.position.set(0f, 1.6f, 5.2f)
        cam.lookAt(0f, 0.6f, 0f)
        cam.near = 0.1f
        cam.far = 50f
        cam.update()
    }

    override fun show() {
        super.show()
        Gdx.input.inputProcessor = buttonInput
        game.audio.updateMusic(true)
    }

    override fun layout() {
        val cx = ui.width * 0.68f
        play.x = cx - play.w / 2f
        play.y = 300f
        options.x = cx - options.w / 2f
        options.y = 180f
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        cam.update()
    }

    override fun onButton(b: UiButton) {
        when (b) {
            play -> game.goTo(GameScreen(game, TatuGame.FIRST_LEVEL))
            options -> game.setScreen(SettingsScreen(game) { game.setScreen(this) })
        }
    }

    override fun onBack() {
        Gdx.app.exit()
    }

    override fun render(delta: Float) {
        time += delta
        ScreenUtils.clear(Pal.SKY, true)
        ui.begin()
        skyBackground()

        // tatu girando à esquerda da tela
        val left = -1.25f * (cam.viewportWidth / cam.viewportHeight) / 1.78f
        val phase = time % 4f
        batch.begin(cam)
        if (phase < 3f) {
            val hop = abs(sin(time * 3f)) * 0.15f
            tatu.transform.setToTranslation(left, -0.3f + hop, 0f).rotate(Vector3.Y, 200f + sin(time * 0.8f) * 40f)
            batch.render(tatu, env)
        } else {
            ball.transform.setToTranslation(left, 0.35f, 0f).rotate(Vector3.Y, time * 900f).rotate(Vector3.X, 18f)
            batch.render(ball, env)
        }
        for (i in 0 until 3) {
            val a = time * 1.2f + i * 2.09f
            acai.transform.setToTranslation(left + kotlin.math.cos(a) * 1.3f, 0.9f + sin(a * 2f) * 0.2f, kotlin.math.sin(a) * 1.3f)
                .rotate(Vector3.Y, time * 100f)
            batch.render(acai, env)
        }
        batch.end()

        ui.begin()
        val cx = ui.width * 0.68f
        ui.text(TatuGame.TITLE, cx, 580f, 1.55f, Ui.GOLD)
        ui.text("Trilha do Mangue", cx, 490f, 0.6f, Color.WHITE)
        ui.button(play, 0.95f)
        ui.button(options, 0.7f)
        val lvl = TatuGame.FIRST_LEVEL
        if (game.settings.completed(lvl)) {
            val t = game.settings.bestTime(lvl)
            ui.text("Recorde: ${formatTime(t)}   •   caixas ${game.settings.bestCrates(lvl)}", cx, 120f, 0.42f, Color.WHITE)
        }
        ui.text("v0.1", ui.width - ui.insetRight - 10f, 30f, 0.3f, Color.WHITE, Align.right, 0.6f)
        ui.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    override fun dispose() {
        batch.dispose()
    }
}

fun formatTime(t: Float): String {
    val total = t.toInt()
    val cs = ((t - total) * 100).toInt()
    return "%d:%02d.%02d".format(total / 60, total % 60, cs)
}
