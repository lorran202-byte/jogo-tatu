package com.lorran.tatu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable

class Audio(private val settings: Settings) : Disposable {
    private val sounds = HashMap<String, Sound>()
    private val music: Music = Gdx.audio.newMusic(Gdx.files.internal("music/tema.ogg"))

    init {
        for (name in listOf("pulo", "giro", "caixa", "acai", "quique", "checkpoint", "vida", "dano", "esmaga", "vitoria", "clique")) {
            sounds[name] = Gdx.audio.newSound(Gdx.files.internal("sfx/$name.wav"))
        }
        music.isLooping = true
        music.volume = 0.45f
    }

    fun play(name: String, volume: Float = 1f, pitch: Float = 1f) {
        if (!settings.sound) return
        sounds[name]?.play(volume, pitch, 0f)
    }

    fun updateMusic(wanted: Boolean) {
        val on = wanted && settings.music
        if (on && !music.isPlaying) music.play()
        if (!on && music.isPlaying) music.pause()
    }

    override fun dispose() {
        sounds.values.forEach { it.dispose() }
        music.dispose()
    }
}
