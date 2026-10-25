package com.lorran.tatu

import com.badlogic.gdx.utils.JsonReader

/** Todos os números de ajuste do jogo, lidos de assets/data/balance.json. */
class Balance(json: String) {
    private val v = JsonReader().parse(json)

    val tile = v.getFloat("tile")
    val raisedHeight = v.getFloat("raisedHeight")
    val runSpeed = v.getFloat("runSpeed")
    val groundAccel = v.getFloat("groundAccel")
    val airAccel = v.getFloat("airAccel")
    val gravity = v.getFloat("gravity")
    val maxFallSpeed = v.getFloat("maxFallSpeed")
    val jumpVelocity = v.getFloat("jumpVelocity")
    val jumpCutGravityMultiplier = v.getFloat("jumpCutGravityMultiplier")
    val coyoteTime = v.getFloat("coyoteTime")
    val jumpBufferTime = v.getFloat("jumpBufferTime")
    val spinDuration = v.getFloat("spinDuration")
    val spinCooldown = v.getFloat("spinCooldown")
    val spinRadius = v.getFloat("spinRadius")
    val playerRadius = v.getFloat("playerRadius")
    val playerHeight = v.getFloat("playerHeight")
    val hurtRadius = v.getFloat("hurtRadius")
    val pickupRadius = v.getFloat("pickupRadius")
    val crateSize = v.getFloat("crateSize")
    val crateBounce = v.getFloat("crateBounce")
    val bounceCrateVelocity = v.getFloat("bounceCrateVelocity")
    val stompBounce = v.getFloat("stompBounce")
    val crabSpeed = v.getFloat("crabSpeed")
    val crabRadius = v.getFloat("crabRadius")
    val crabHeight = v.getFloat("crabHeight")
    val deathY = v.getFloat("deathY")
    val dyingTime = v.getFloat("dyingTime")
    val startLives = v.getInt("startLives")
    val acaiPerLife = v.getInt("acaiPerLife")
    val acaiPerCrate = v.getInt("acaiPerCrate")
}
