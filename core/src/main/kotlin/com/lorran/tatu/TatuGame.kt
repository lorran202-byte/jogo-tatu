package com.lorran.tatu

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen

class TatuGame : Game() {
    lateinit var settings: Settings
        private set
    lateinit var audio: Audio
        private set
    lateinit var ui: Ui
        private set
    lateinit var models: Models
        private set
    lateinit var balance: Balance
        private set

    override fun create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        settings = Settings()
        audio = Audio(settings)
        ui = Ui()
        models = Models()
        balance = Balance(Gdx.files.internal("data/balance.json").readString("UTF-8"))
        setScreen(MenuScreen(this))
    }

    fun loadLevel(file: String): Level =
        Level.parse(Gdx.files.internal("levels/$file.json").readString("UTF-8"), balance)

    /** Troca de tela e libera a anterior. */
    fun goTo(next: Screen) {
        val old = screen
        setScreen(next)
        old?.dispose()
    }

    override fun dispose() {
        screen?.dispose()
        models.dispose()
        ui.dispose()
        audio.dispose()
    }

    companion object {
        const val TITLE = "TATU-BOLA"
        const val FIRST_LEVEL = "fase1"
    }
}
