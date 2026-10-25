package com.lorran.tatu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WorldTest {
    private val balance = Balance(File("data/balance.json").readText())
    private val dt = 1f / 60f

    private fun world(vararg rows: String) = World(Level("teste", rows.toList(), balance))

    private fun run(w: World, seconds: Float, input: PlayerInput = PlayerInput(), each: (Int) -> Unit = {}) {
        val steps = (seconds / dt).toInt()
        for (i in 0 until steps) {
            each(i)
            w.step(dt, input)
            input.jumpPressed = false
            input.spinPressed = false
        }
    }

    @Test
    fun fase1Carrega() {
        val level = Level.parse(File("levels/fase1.json").readText(), balance)
        assertEquals(5, level.cols)
        val w = World(level)
        assertEquals(1, level.find('P').size)
        assertEquals(1, level.find('G').size)
        assertTrue(w.totalCrates > 10)
        assertTrue(w.crabs.isNotEmpty())
    }

    @Test
    fun ficaParadoNoChao() {
        val w = world(".....", "..P..", ".....")
        run(w, 1f)
        assertEquals(0f, w.player.y, 0.001f)
        assertTrue(w.player.onGround)
    }

    @Test
    fun puloAtravessaBuracoDeDuasLinhas() {
        val w = world(".....", "..P..", ".....", "_____", "_____", ".....", ".....", "..G..", ".....")
        val input = PlayerInput().apply { moveY = 1f; jumpHeld = true }
        var jumped = false
        run(w, 4f, input) {
            // pula na borda da linha 2
            if (!jumped && w.player.z < w.level.cellZ(2) - 0.6f) {
                input.jumpPressed = true
                jumped = true
            }
        }
        assertEquals(WorldState.WON, w.state)
        assertEquals(balance.startLives, w.lives)
    }

    @Test
    fun cairNoBuracoTiraVida() {
        val w = world(".....", "..P..", "_____", "_____", "_____", ".....")
        val input = PlayerInput().apply { moveY = 1f }
        // depois de cair, para de andar para não cair de novo ao voltar
        run(w, 4f, input) { if (w.state == WorldState.DYING) input.moveY = 0f }
        assertEquals(balance.startLives - 1, w.lives)
        assertEquals(WorldState.PLAYING, w.state)
    }

    @Test
    fun giroQuebraCaixaEDaAcai() {
        val w = world(".....", "..P..", "..C..", ".....")
        val input = PlayerInput().apply { moveY = 1f }
        run(w, 0.3f, input)
        input.spinPressed = true
        run(w, 0.5f, input)
        assertEquals(1, w.cratesBroken)
        assertEquals(balance.acaiPerCrate, w.acai)
    }

    @Test
    fun caixaBloqueiaSemGiro() {
        val w = world(".....", "..P..", "..C..", ".....", ".....")
        val input = PlayerInput().apply { moveY = 1f }
        run(w, 2f, input)
        assertEquals(0, w.cratesBroken)
        assertTrue(w.player.z > w.level.cellZ(2))
    }

    @Test
    fun caranguejoMataSemGiroEMorreComGiro() {
        // caranguejo parado: só há chão na coluna do meio da linha dele
        val w = world(".....", "..P..", ".....", "__E__", ".....", ".....")
        val input = PlayerInput().apply { moveY = 1f }
        run(w, 2f, input)
        assertTrue(w.state == WorldState.DYING || w.lives < balance.startLives)

        val w2 = world(".....", "..P..", ".....", "__E__", ".....", ".....")
        val input2 = PlayerInput().apply { moveY = 1f }
        run(w2, 0.25f, input2)
        input2.spinPressed = true
        run(w2, 0.6f, input2)
        assertFalse(w2.crabs[0].alive)
        assertEquals(WorldState.PLAYING, w2.state)
    }

    @Test
    fun checkpointMudaOPontoDeVolta() {
        val w = world(".....", "..P..", "..K..", ".....", "_____", "_____", "_____", ".....")
        val input = PlayerInput().apply { moveY = 1f }
        run(w, 0.3f, input)
        input.spinPressed = true
        run(w, 0.4f, input)
        assertEquals(1, w.cratesBroken)
        // cai no buraco; depois de morrer para de andar e espera voltar
        run(w, 4f, input) { if (w.state == WorldState.DYING) input.moveY = 0f }
        assertEquals(balance.startLives - 1, w.lives)
        assertEquals(WorldState.PLAYING, w.state)
        assertEquals(w.level.cellZ(2), w.player.z, 0.01f)
    }

    @Test
    fun cemAcaisDaoUmaVida() {
        val rows = mutableListOf(".....", "..P..")
        repeat(40) { rows.add("..C..") }
        rows.add(".....")
        val w = World(Level("t", rows, balance))
        val before = w.lives
        // anda girando sem parar: cada caixa dá açaí
        val input = PlayerInput().apply { moveY = 1f }
        run(w, 20f, input) { if (it % 20 == 0) input.spinPressed = true }
        assertTrue(w.cratesBroken >= 34)
        assertTrue(w.lives > before)
    }

    @Test
    fun degrauPrecisaDePulo() {
        val w = world(".....", "..P..", ".....", "#####", "#####")
        val input = PlayerInput().apply { moveY = 1f }
        run(w, 2f, input)
        assertTrue(w.player.z > w.level.cellZ(3))
        input.jumpPressed = true
        input.jumpHeld = true
        run(w, 1.5f, input)
        assertEquals(balance.raisedHeight, w.player.y, 0.01f)
    }
}
