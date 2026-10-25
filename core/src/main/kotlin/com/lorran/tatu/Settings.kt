package com.lorran.tatu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/** Ajustes do jogador e recordes, salvos no aparelho. */
class Settings {
    private val prefs: Preferences = Gdx.app.getPreferences("tatu-ajustes")
    private val records: Preferences = Gdx.app.getPreferences("tatu-recordes")

    var sound = prefs.getBoolean("som", true)
        set(v) { field = v; prefs.putBoolean("som", v).flush() }
    var music = prefs.getBoolean("musica", true)
        set(v) { field = v; prefs.putBoolean("musica", v).flush() }
    var vibration = prefs.getBoolean("vibracao", true)
        set(v) { field = v; prefs.putBoolean("vibracao", v).flush() }
    var leftHanded = prefs.getBoolean("canhoto", false)
        set(v) { field = v; prefs.putBoolean("canhoto", v).flush() }
    var buttonScale = prefs.getFloat("tamanho", 1f)
        set(v) { field = v.coerceIn(0.8f, 1.4f); prefs.putFloat("tamanho", field).flush() }
    var buttonAlpha = prefs.getFloat("opacidade", 0.7f)
        set(v) { field = v.coerceIn(0.3f, 1f); prefs.putFloat("opacidade", field).flush() }

    fun bestTime(level: String) = records.getFloat("$level.tempo", 0f)
    fun bestCrates(level: String) = records.getInteger("$level.caixas", 0)
    fun bestAcai(level: String) = records.getInteger("$level.acai", 0)
    fun completed(level: String) = records.getBoolean("$level.feita", false)

    /** Salva o resultado e diz se bateu o recorde de tempo. */
    fun saveResult(level: String, time: Float, crates: Int, acai: Int): Boolean {
        val oldTime = bestTime(level)
        val newRecord = oldTime <= 0f || time < oldTime
        if (newRecord) records.putFloat("$level.tempo", time)
        if (crates > bestCrates(level)) records.putInteger("$level.caixas", crates)
        if (acai > bestAcai(level)) records.putInteger("$level.acai", acai)
        records.putBoolean("$level.feita", true)
        records.flush()
        return newRecord
    }

    fun vibrate(ms: Int) {
        if (vibration) Gdx.input.vibrate(ms)
    }
}
