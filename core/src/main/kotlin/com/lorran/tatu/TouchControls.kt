package com.lorran.tatu

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import kotlin.math.sqrt

/**
 * Controles de toque: joystick flutuante numa metade da tela, botões Pular e Girar na outra
 * e o botão de pausa no canto. Nunca precisa de mais de dois dedos.
 */
class TouchControls(private val ui: Ui, private val settings: Settings) : InputAdapter() {
    var moveX = 0f
        private set
    var moveY = 0f
        private set
    var jumpHeld = false
        private set
    private var jumpPressed = false
    private var spinPressed = false
    private var pausePressed = false
    var enabled = true

    private var joyPointer = -1
    private var joyBaseX = 0f
    private var joyBaseY = 0f
    private var knobX = 0f
    private var knobY = 0f
    private var jumpPointer = -1
    private var spinFlash = 0f

    // posições calculadas no layout()
    var jumpX = 0f
        private set
    var jumpY = 0f
        private set
    var jumpR = 0f
        private set
    var spinX = 0f
        private set
    var spinY = 0f
        private set
    var spinR = 0f
        private set
    var pauseX = 0f
        private set
    var pauseY = 0f
        private set
    val pauseR = 42f
    private var joyR = 130f
    private var joyHomeX = 0f
    private var joyHomeY = 0f

    fun layout() {
        val s = settings.buttonScale
        val w = ui.width
        val h = ui.height
        jumpR = 95f * s
        spinR = 72f * s
        joyR = 130f * s
        val right = !settings.leftHanded
        val edgeIn = if (right) ui.insetRight else ui.insetLeft
        val jx = 60f + edgeIn + jumpR
        val sx = jx + jumpR + spinR + 40f * s
        jumpX = if (right) w - jx else jx
        jumpY = 60f + jumpR
        spinX = if (right) w - sx else sx
        spinY = 40f + spinR
        val joyEdge = if (right) ui.insetLeft else ui.insetRight
        joyHomeX = if (right) joyEdge + 70f + joyR else w - joyEdge - 70f - joyR
        joyHomeY = 70f + joyR
        pauseX = w - ui.insetRight - 30f - pauseR
        pauseY = h - 30f - pauseR
    }

    /** Lê e zera os toques de um passo. */
    fun consume(input: PlayerInput) {
        input.moveX = moveX
        input.moveY = moveY
        input.jumpHeld = jumpHeld
        input.jumpPressed = jumpPressed
        input.spinPressed = spinPressed
        jumpPressed = false
        spinPressed = false
    }

    fun takePause(): Boolean {
        val p = pausePressed
        pausePressed = false
        return p
    }

    fun reset() {
        joyPointer = -1
        jumpPointer = -1
        moveX = 0f; moveY = 0f
        jumpHeld = false
        jumpPressed = false
        spinPressed = false
        pausePressed = false
    }

    private fun dist2(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return dx * dx + dy * dy
    }

    private fun onJoystickSide(x: Float) = if (settings.leftHanded) x > ui.width / 2f else x < ui.width / 2f

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!enabled) return false
        val p = ui.toUi(screenX, screenY)
        val x = p.x
        val y = p.y
        val tol = 1.3f
        when {
            dist2(x, y, pauseX, pauseY) < (pauseR * 1.5f).let { it * it } -> pausePressed = true
            dist2(x, y, jumpX, jumpY) < (jumpR * tol).let { it * it } -> {
                jumpPointer = pointer
                jumpHeld = true
                jumpPressed = true
                settings.vibrate(12)
            }
            dist2(x, y, spinX, spinY) < (spinR * tol).let { it * it } -> {
                spinPressed = true
                spinFlash = 0.2f
                settings.vibrate(15)
            }
            onJoystickSide(x) && joyPointer == -1 && y < ui.height - 120f -> {
                joyPointer = pointer
                joyBaseX = x
                joyBaseY = y
                knobX = x
                knobY = y
                updateStick()
            }
            else -> return false
        }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer != joyPointer) return pointer == jumpPointer
        val p = ui.toUi(screenX, screenY)
        knobX = p.x
        knobY = p.y
        // se o dedo passar muito do limite, a base acompanha (joystick flutuante)
        val dx = knobX - joyBaseX
        val dy = knobY - joyBaseY
        val d = sqrt(dx * dx + dy * dy)
        val max = joyR * 1.25f
        if (d > max) {
            joyBaseX = knobX - dx / d * max
            joyBaseY = knobY - dy / d * max
        }
        updateStick()
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer == joyPointer) {
            joyPointer = -1
            moveX = 0f
            moveY = 0f
            return true
        }
        if (pointer == jumpPointer) {
            jumpPointer = -1
            jumpHeld = false
            return true
        }
        return false
    }

    private fun updateStick() {
        val dx = (knobX - joyBaseX) / joyR
        val dy = (knobY - joyBaseY) / joyR
        val len = sqrt(dx * dx + dy * dy)
        val dead = 0.15f
        if (len < dead) {
            moveX = 0f
            moveY = 0f
            return
        }
        val mag = MathUtils.clamp((len - dead) / (1f - dead), 0f, 1f)
        moveX = dx / len * mag
        moveY = dy / len * mag
    }

    fun draw(dt: Float) {
        val a = settings.buttonAlpha
        spinFlash = maxOf(0f, spinFlash - dt)
        // joystick
        if (joyPointer >= 0) {
            ui.circle(joyBaseX, joyBaseY, joyR, Color.BLACK, 0.22f * a)
            ui.ring(joyBaseX, joyBaseY, joyR, 4f, Color.WHITE, 0.55f * a)
            val kx = joyBaseX + moveX * joyR
            val ky = joyBaseY + moveY * joyR
            ui.circle(kx, ky, joyR * 0.42f, Color.WHITE, 0.75f * a)
        } else {
            ui.ring(joyHomeX, joyHomeY, joyR, 3f, Color.WHITE, 0.25f * a)
            ui.circle(joyHomeX, joyHomeY, joyR * 0.42f, Color.WHITE, 0.25f * a)
        }
        // botão pular
        val jp = if (jumpHeld) 0.92f else 1f
        ui.circle(jumpX, jumpY - 5f, jumpR * jp, Color.BLACK, 0.25f * a)
        ui.circle(jumpX, jumpY, jumpR * jp, Ui.GREEN, a)
        ui.triangle(jumpX - 26f, jumpY + 6f, jumpX + 26f, jumpY + 6f, jumpX, jumpY + 36f, Color.WHITE, a)
        ui.rect(jumpX - 9f, jumpY - 18f, 18f, 26f, Color.WHITE, a)
        ui.text("PULAR", jumpX, jumpY - jumpR * 0.55f, 0.38f * settings.buttonScale, Color.WHITE, a = a)
        // botão girar
        val sp = if (spinFlash > 0f) 0.9f else 1f
        ui.circle(spinX, spinY - 5f, spinR * sp, Color.BLACK, 0.25f * a)
        ui.circle(spinX, spinY, spinR * sp, Ui.ORANGE, a)
        ui.ring(spinX, spinY + 6f, 22f, 6f, Color.WHITE, a)
        ui.triangle(spinX + 14f, spinY + 34f, spinX + 30f, spinY + 22f, spinX + 12f, spinY + 16f, Color.WHITE, a)
        ui.text("GIRAR", spinX, spinY - spinR * 0.55f, 0.32f * settings.buttonScale, Color.WHITE, a = a)
        // pausa
        ui.circle(pauseX, pauseY, pauseR, Color.BLACK, 0.35f)
        ui.rect(pauseX - 13f, pauseY - 15f, 9f, 30f, Color.WHITE)
        ui.rect(pauseX + 4f, pauseY - 15f, 9f, 30f, Color.WHITE)
    }
}
