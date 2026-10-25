package com.lorran.tatu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils

/** Ajustes de som, vibração e dos controles de toque, com prévia dos botões na tela. */
class SettingsScreen(game: TatuGame, private val back: () -> Unit) : BaseScreen(game) {
    private val s = game.settings
    private val preview = TouchControls(ui, s)

    private val bSound = UiButton(0f, 0f, 150f, 56f, "")
    private val bMusic = UiButton(0f, 0f, 150f, 56f, "")
    private val bVibe = UiButton(0f, 0f, 150f, 56f, "")
    private val bLeft = UiButton(0f, 0f, 150f, 56f, "")
    private val bSizeMinus = UiButton(0f, 0f, 64f, 56f, "–", Ui.GRAY)
    private val bSizePlus = UiButton(0f, 0f, 64f, 56f, "+", Ui.GRAY)
    private val bAlphaMinus = UiButton(0f, 0f, 64f, 56f, "–", Ui.GRAY)
    private val bAlphaPlus = UiButton(0f, 0f, 64f, 56f, "+", Ui.GRAY)
    private val bBack = UiButton(0f, 0f, 200f, 76f, "VOLTAR", Ui.PURPLE)

    private val rowLabels = listOf("Som", "Música", "Vibração", "Modo canhoto", "Tamanho dos botões", "Transparência")
    private var panelX = 0f
    private val panelY = 236f
    private val panelW = 720f
    private val panelH = 440f

    init {
        buttons.addAll(listOf(bSound, bMusic, bVibe, bLeft, bSizeMinus, bSizePlus, bAlphaMinus, bAlphaPlus, bBack))
    }

    override fun show() {
        super.show()
        Gdx.input.inputProcessor = buttonInput
    }

    private fun rowY(i: Int) = panelY + panelH - 120f - i * 58f

    override fun layout() {
        panelX = (ui.width - panelW) / 2f
        val right = panelX + panelW - 40f
        listOf(bSound, bMusic, bVibe, bLeft).forEachIndexed { i, b ->
            b.x = right - b.w
            b.y = rowY(i) - b.h / 2f
        }
        bSizePlus.x = right - bSizePlus.w; bSizePlus.y = rowY(4) - 28f
        bSizeMinus.x = right - 230f; bSizeMinus.y = rowY(4) - 28f
        bAlphaPlus.x = right - bAlphaPlus.w; bAlphaPlus.y = rowY(5) - 28f
        bAlphaMinus.x = right - 230f; bAlphaMinus.y = rowY(5) - 28f
        bBack.x = ui.insetLeft + 16f
        bBack.y = ui.height - bBack.h - 24f
        preview.layout()
        refresh()
    }

    private fun toggle(b: UiButton, on: Boolean) {
        b.label = if (on) "LIGADO" else "DESLIGADO"
        b.color = if (on) Ui.GREEN else Ui.GRAY
    }

    private fun refresh() {
        toggle(bSound, s.sound)
        toggle(bMusic, s.music)
        toggle(bVibe, s.vibration)
        toggle(bLeft, s.leftHanded)
    }

    override fun onButton(b: UiButton) {
        when (b) {
            bSound -> s.sound = !s.sound
            bMusic -> { s.music = !s.music; game.audio.updateMusic(true) }
            bVibe -> { s.vibration = !s.vibration; s.vibrate(40) }
            bLeft -> s.leftHanded = !s.leftHanded
            bSizeMinus -> s.buttonScale = s.buttonScale - 0.1f
            bSizePlus -> s.buttonScale = s.buttonScale + 0.1f
            bAlphaMinus -> s.buttonAlpha = s.buttonAlpha - 0.1f
            bAlphaPlus -> s.buttonAlpha = s.buttonAlpha + 0.1f
            bBack -> { onBack(); return }
        }
        preview.layout()
        refresh()
    }

    override fun onBack() {
        back()
        dispose()
    }

    override fun render(delta: Float) {
        time += delta
        ScreenUtils.clear(Pal.SKY, true)
        ui.begin()
        skyBackground()
        ui.begin()
        preview.draw(delta)
        ui.panel(panelX, panelY, panelW, panelH)
        ui.text("AJUSTES", panelX + panelW / 2f, panelY + panelH - 50f, 0.85f, Ui.GOLD)
        rowLabels.forEachIndexed { i, label ->
            ui.text(label, panelX + 40f, rowY(i), 0.5f, Color.WHITE, Align.left)
        }
        for (b in listOf(bSound, bMusic, bVibe, bLeft)) ui.button(b, 0.42f)
        for (b in listOf(bSizeMinus, bSizePlus, bAlphaMinus, bAlphaPlus)) ui.button(b, 0.7f)
        val mid = (bSizeMinus.x + bSizeMinus.w + bSizePlus.x) / 2f
        ui.text("${(s.buttonScale * 100).toInt()}%", mid, rowY(4), 0.5f)
        ui.text("${(s.buttonAlpha * 100).toInt()}%", mid, rowY(5), 0.5f)
        ui.button(bBack, 0.6f)
        ui.text("Os botões aparecem como vão ficar no jogo", ui.width / 2f, panelY - 30f, 0.36f, Color.WHITE, a = 0.85f)
        ui.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }
}
