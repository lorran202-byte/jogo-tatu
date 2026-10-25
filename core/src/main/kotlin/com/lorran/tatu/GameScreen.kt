package com.lorran.tatu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils

class GameScreen(game: TatuGame, private val levelId: String) : BaseScreen(game) {
    private val level = game.loadLevel(levelId)
    private val world = World(level)
    private val renderer = GameRenderer(game.models, world)
    private val controls = TouchControls(ui, game.settings)
    private val input = PlayerInput()
    private var accumulator = 0f
    private var paused = false
    private var resultSaved = false
    private var newRecord = false
    private var toast = ""
    private var toastTime = 0f
    private var shownLives = world.lives

    private val bResume = UiButton(0f, 0f, 420f, 84f, "CONTINUAR", Ui.GREEN)
    private val bRestart = UiButton(0f, 0f, 420f, 76f, "RECOMEÇAR FASE", Ui.ORANGE)
    private val bSettings = UiButton(0f, 0f, 420f, 76f, "AJUSTES", Ui.BLUE)
    private val bMenu = UiButton(0f, 0f, 420f, 76f, "MENU", Ui.PURPLE)
    private val bAgain = UiButton(0f, 0f, 420f, 84f, "JOGAR DE NOVO", Ui.GREEN)

    private val keys = object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                onBack()
                return true
            }
            return false
        }
    }

    override fun show() {
        super.show()
        controls.reset()
        Gdx.input.inputProcessor = InputMultiplexer(keys, buttonInput, controls)
        game.audio.updateMusic(true)
        updateOverlay()
    }

    override fun layout() {
        controls.layout()
        val cx = ui.width / 2f
        var y = 430f
        for (b in listOf(bResume, bRestart, bSettings, bMenu)) {
            b.x = cx - b.w / 2f
            b.y = y
            y -= b.h + 22f
        }
        bAgain.x = cx - bAgain.w / 2f
        bAgain.y = 200f
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        renderer.resize(width, height)
    }

    /** Liga os botões certos para cada situação (pausa, fim de fase, game over). */
    private fun updateOverlay() {
        buttons.clear()
        controls.enabled = false
        when {
            paused -> buttons.addAll(listOf(bResume, bRestart, bSettings, bMenu))
            world.state == WorldState.WON && world.stateTime > 0.8f -> {
                bMenu.y = bAgain.y - bMenu.h - 22f
                buttons.addAll(listOf(bAgain, bMenu))
            }
            world.state == WorldState.GAME_OVER && world.stateTime > 1f -> {
                bAgain.label = "TENTAR DE NOVO"
                bMenu.y = bAgain.y - bMenu.h - 22f
                buttons.addAll(listOf(bAgain, bMenu))
            }
            else -> controls.enabled = world.state == WorldState.PLAYING || world.state == WorldState.DYING
        }
        if (!controls.enabled) controls.reset()
    }

    override fun pause() {
        // app foi para segundo plano ou chegou uma ligação
        if (world.state == WorldState.PLAYING || world.state == WorldState.DYING) setPaused(true)
    }

    private fun setPaused(p: Boolean) {
        paused = p
        if (!p) layout()
        updateOverlay()
    }

    override fun onBack() {
        when {
            paused -> setPaused(false)
            world.state == WorldState.PLAYING || world.state == WorldState.DYING -> setPaused(true)
            else -> game.goTo(MenuScreen(game))
        }
    }

    override fun onButton(b: UiButton) {
        when (b) {
            bResume -> setPaused(false)
            bRestart, bAgain -> game.goTo(GameScreen(game, levelId))
            bSettings -> game.setScreen(SettingsScreen(game) { game.setScreen(this) })
            bMenu -> game.goTo(MenuScreen(game))
        }
    }

    private fun handleEvents() {
        val a = game.audio
        val s = game.settings
        for (e in world.events) {
            when (e.type) {
                EventType.JUMP -> a.play("pulo", 0.7f)
                EventType.SPIN -> a.play("giro", 0.8f)
                EventType.CRATE -> { a.play("caixa"); s.vibrate(20) }
                EventType.ACAI -> a.play("acai", 0.6f, MathUtils.random(0.95f, 1.1f))
                EventType.BOUNCE -> { a.play("quique"); s.vibrate(15) }
                EventType.CHECKPOINT -> { a.play("checkpoint"); showToast("Checkpoint!") }
                EventType.LIFE -> { a.play("vida"); showToast("+1 vida!") }
                EventType.HURT -> { a.play("dano"); s.vibrate(180) }
                EventType.STOMP -> { a.play("esmaga"); s.vibrate(25) }
                EventType.WIN -> a.play("vitoria")
                EventType.GAME_OVER -> {}
            }
            renderer.spawnBurst(e.type, e.x, e.y, e.z)
        }
        world.events.clear()
    }

    private fun showToast(t: String) {
        toast = t
        toastTime = 1.6f
    }

    override fun render(delta: Float) {
        val dt = minOf(delta, 0.1f)
        time += dt
        if (controls.takePause() && !paused && (world.state == WorldState.PLAYING || world.state == WorldState.DYING)) {
            setPaused(true)
        }

        if (!paused) {
            accumulator += dt
            val step = 1f / 60f
            var n = 0
            while (accumulator >= step && n < 6) {
                controls.consume(input)
                world.step(step, input)
                accumulator -= step
                n++
            }
            if (n == 6) accumulator = 0f
            handleEvents()
            toastTime = maxOf(0f, toastTime - dt)

            val ended = world.state == WorldState.WON || world.state == WorldState.GAME_OVER
            if (ended && buttons.isEmpty()) updateOverlay()
            if (world.state == WorldState.WON && !resultSaved) {
                resultSaved = true
                newRecord = game.settings.saveResult(levelId, world.time, world.cratesBroken, world.acai)
            }
        }

        ScreenUtils.clear(Pal.SKY, true)
        renderer.render(if (paused) 0f else dt)

        ui.begin()
        drawHud()
        if (controls.enabled && !paused) controls.draw(dt)
        if (toastTime > 0f) {
            val a = MathUtils.clamp(toastTime * 2f, 0f, 1f)
            ui.text(toast, ui.width / 2f, ui.height - 150f + (1.6f - toastTime) * 20f, 0.7f, Ui.GOLD, a = a)
        }
        when {
            paused -> drawPause()
            world.state == WorldState.WON -> drawWin()
            world.state == WorldState.GAME_OVER -> drawGameOver()
        }
        ui.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawHud() {
        val x0 = ui.insetLeft + 16f
        val top = ui.height - 56f
        ui.roundRect(x0, top - 34f, 520f, 68f, 34f, Color.BLACK, 0.3f)
        // açaí
        ui.circle(x0 + 38f, top, 20f, Pal.ACAI)
        ui.circle(x0 + 31f, top + 7f, 6f, Color.WHITE, 0.7f)
        ui.text("${world.acai}", x0 + 68f, top, 0.6f, Color.WHITE, Align.left)
        // caixas
        val cx = x0 + 170f
        ui.rect(cx, top - 18f, 36f, 36f, Pal.WOOD)
        ui.rect(cx + 4f, top - 14f, 28f, 28f, Color(0.62f, 0.40f, 0.18f, 1f))
        ui.text("${world.cratesBroken}/${world.totalCrates}", cx + 48f, top, 0.6f, Color.WHITE, Align.left)
        // vidas
        if (world.lives != shownLives) shownLives = world.lives
        val lx = x0 + 370f
        ui.text("♥", lx, top, 0.8f, Color(0.95f, 0.3f, 0.4f, 1f), Align.left)
        ui.text("× ${world.lives}", lx + 48f, top, 0.6f, Color.WHITE, Align.left)
        // tempo
        ui.text(formatTime(world.time), ui.width / 2f + 120f, top, 0.5f, Color.WHITE, a = 0.9f)
    }

    private fun dim() = ui.rect(0f, 0f, ui.width, ui.height, Color.BLACK, 0.5f)

    private fun drawPause() {
        dim()
        ui.text("PAUSA", ui.width / 2f, 560f, 1.1f, Ui.GOLD)
        for (b in listOf(bResume, bRestart, bSettings, bMenu)) ui.button(b, 0.6f)
    }

    private fun drawWin() {
        if (world.stateTime < 0.8f) return
        dim()
        val cx = ui.width / 2f
        ui.panel(cx - 360f, 330f, 720f, 330f)
        ui.text("FASE COMPLETA!", cx, 615f, 0.95f, Ui.GOLD)
        ui.text("Tempo  ${formatTime(world.time)}", cx, 545f, 0.6f)
        ui.text("Açaí  ${world.acai}", cx, 485f, 0.6f)
        val all = world.cratesBroken == world.totalCrates
        ui.text("Caixas  ${world.cratesBroken} de ${world.totalCrates}" + if (all) " – todas!" else "", cx, 425f, 0.6f, if (all) Ui.GOLD else Color.WHITE)
        if (newRecord) ui.text("Novo recorde de tempo!", cx, 370f, 0.5f, Ui.GOLD)
        ui.button(bAgain, 0.6f)
        ui.button(bMenu, 0.6f)
    }

    private fun drawGameOver() {
        if (world.stateTime < 1f) return
        dim()
        ui.text("FIM DE JOGO", ui.width / 2f, 520f, 1.2f, Ui.GOLD)
        ui.text("Caixas ${world.cratesBroken} de ${world.totalCrates}", ui.width / 2f, 420f, 0.6f)
        ui.button(bAgain, 0.6f)
        ui.button(bMenu, 0.6f)
    }

    override fun dispose() {
        renderer.dispose()
    }
}
