package com.lorran.tatu

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

enum class EventType { JUMP, SPIN, CRATE, ACAI, BOUNCE, CHECKPOINT, LIFE, HURT, STOMP, WIN, GAME_OVER }

class GameEvent(val type: EventType, val x: Float, val y: Float, val z: Float)

class Crate(val type: Char, val x: Float, val base: Float, val z: Float) {
    var alive = true
    var squash = 0f // animação de quique
}

class Crab(var x: Float, val y: Float, val z: Float, val minX: Float, val maxX: Float) {
    var dir = 1f
    var alive = true
    var deadTime = 0f
    var flyVx = 0f
    var flyVy = 0f
    var flyY = 0f
    var walkPhase = 0f
}

class Acai(val x: Float, val y: Float, val z: Float) {
    var alive = true
}

class Player {
    var x = 0f
    var y = 0f
    var z = 0f
    var vx = 0f
    var vy = 0f
    var vz = 0f
    var onGround = false
    var coyote = 0f
    var jumpBuffer = 0f
    var jumping = false
    var spinTimer = 0f
    var spinCooldown = 0f
    var facing = 0f
    var runPhase = 0f
    val spinning get() = spinTimer > 0f
}

/** O que os controles pedem neste passo. moveX = direita, moveY = para a frente. */
class PlayerInput {
    var moveX = 0f
    var moveY = 0f
    var jumpHeld = false
    var jumpPressed = false
    var spinPressed = false
}

enum class WorldState { PLAYING, DYING, WON, GAME_OVER }

/** Toda a lógica do jogo, sem nada de desenho, para poder testar. */
class World(val level: Level) {
    val b = level.b
    val player = Player()
    val crates = ArrayList<Crate>()
    val crabs = ArrayList<Crab>()
    val acais = ArrayList<Acai>()
    val events = ArrayList<GameEvent>()

    var goalX = 0f
    var goalY = 0f
    var goalZ = 0f
    private var cpX = 0f
    private var cpY = 0f
    private var cpZ = 0f

    var lives = b.startLives
    var acai = 0
    var cratesBroken = 0
    val totalCrates = level.countCrates()
    var time = 0f
    var state = WorldState.PLAYING
    var stateTime = 0f

    init {
        for (r in 0 until level.rowCount) for (c in 0 until level.cols) {
            val ch = level.charAt(c, r)
            val x = level.cellX(c)
            val z = level.cellZ(r)
            val g = level.groundAt(c, r) ?: 0f
            when (ch) {
                'P' -> { cpX = x; cpY = g; cpZ = z }
                'G' -> { goalX = x; goalY = g; goalZ = z }
                'C', 'V', 'B', 'K' -> crates.add(Crate(ch, x, g, z))
                'F' -> acais.add(Acai(x, g + 0.9f, z))
                'f' -> acais.add(Acai(x, 1.7f, z))
                'E' -> crabs.add(makeCrab(c, r, g))
            }
        }
        placePlayer()
    }

    private fun makeCrab(col: Int, row: Int, g: Float): Crab {
        var lo = col
        var hi = col
        while (level.groundAt(lo - 1, row) == g) lo--
        while (level.groundAt(hi + 1, row) == g) hi++
        var minX = level.cellX(lo) - level.tile / 2f + b.crabRadius
        var maxX = level.cellX(hi) + level.tile / 2f - b.crabRadius
        val x = level.cellX(col)
        if (minX > maxX) { minX = x; maxX = x }
        return Crab(x, g, level.cellZ(row), minX, maxX)
    }

    private fun placePlayer() {
        val p = player
        p.x = cpX; p.y = cpY; p.z = cpZ
        p.vx = 0f; p.vy = 0f; p.vz = 0f
        p.onGround = true
        p.jumping = false
        p.spinTimer = 0f
        p.spinCooldown = 0f
        p.jumpBuffer = 0f
        p.facing = 0f
    }

    fun step(dt: Float, input: PlayerInput) {
        stateTime += dt
        updateCrabs(dt)
        for (c in crates) c.squash = max(0f, c.squash - dt * 4f)
        when (state) {
            WorldState.PLAYING -> {
                time += dt
                updatePlayer(dt, input)
            }
            WorldState.DYING -> if (stateTime >= b.dyingTime) respawnOrEnd()
            WorldState.WON, WorldState.GAME_OVER -> {}
        }
    }

    private fun emit(t: EventType, x: Float, y: Float, z: Float) {
        events.add(GameEvent(t, x, y, z))
    }

    private fun approach(v: Float, target: Float, delta: Float) =
        if (v < target) min(v + delta, target) else max(v - delta, target)

    private fun updatePlayer(dt: Float, input: PlayerInput) {
        val p = player
        val r = b.playerRadius
        val h = b.playerHeight

        p.jumpBuffer = if (input.jumpPressed) b.jumpBufferTime else max(0f, p.jumpBuffer - dt)
        p.coyote = if (p.onGround) b.coyoteTime else max(0f, p.coyote - dt)

        if (p.spinTimer > 0f) {
            p.spinTimer -= dt
            if (p.spinTimer <= 0f) { p.spinTimer = 0f; p.spinCooldown = b.spinCooldown }
        } else {
            p.spinCooldown = max(0f, p.spinCooldown - dt)
        }
        if (input.spinPressed && p.spinTimer <= 0f && p.spinCooldown <= 0f) {
            p.spinTimer = b.spinDuration
            emit(EventType.SPIN, p.x, p.y, p.z)
        }

        // Andar (velocidade analógica do joystick)
        var mx = input.moveX
        var my = input.moveY
        val len = sqrt(mx * mx + my * my)
        if (len > 1f) { mx /= len; my /= len }
        val accel = (if (p.onGround) b.groundAccel else b.airAccel) * dt
        p.vx = approach(p.vx, mx * b.runSpeed, accel)
        p.vz = approach(p.vz, -my * b.runSpeed, accel)

        // Pulo com buffer e coyote time
        if (p.jumpBuffer > 0f && p.coyote > 0f) {
            p.vy = b.jumpVelocity
            p.jumpBuffer = 0f
            p.coyote = 0f
            p.onGround = false
            p.jumping = true
            emit(EventType.JUMP, p.x, p.y, p.z)
        }
        var g = b.gravity
        if (p.jumping && p.vy > 0f && !input.jumpHeld) g *= b.jumpCutGravityMultiplier
        p.vy = max(p.vy - g * dt, -b.maxFallSpeed)

        val prevY = p.y
        moveX(p.vx * dt)
        moveZ(p.vz * dt)
        p.y += p.vy * dt

        val ground = level.groundAtPos(p.x, p.z)
        if (ground != null && p.vy <= 0f && p.y <= ground && prevY >= ground - 0.6f) {
            p.y = ground
            p.vy = 0f
            p.onGround = true
            p.jumping = false
        } else {
            p.onGround = false
        }

        p.x = p.x.coerceIn(level.minX + r, level.maxX - r)
        p.z = p.z.coerceIn(level.endZ + r, level.startZ - r)

        collideCrates(prevY, r, h)
        if (state != WorldState.PLAYING) return
        collideCrabs(prevY, h)
        if (state != WorldState.PLAYING) return
        collectAcai(h)

        val speed = sqrt(p.vx * p.vx + p.vz * p.vz)
        if (speed > 0.8f) {
            val target = Math.toDegrees(atan2(-p.vx, -p.vz).toDouble()).toFloat()
            var diff = target - p.facing
            while (diff > 180f) diff -= 360f
            while (diff < -180f) diff += 360f
            p.facing += diff * min(1f, dt * 14f)
        }
        if (p.onGround) p.runPhase += speed * dt * 2.2f

        val gdx = p.x - goalX
        val gdz = p.z - goalZ
        if (gdx * gdx + gdz * gdz < 1.4f * 1.4f && abs(p.y - goalY) < 2.5f) {
            state = WorldState.WON
            stateTime = 0f
            emit(EventType.WIN, p.x, p.y, p.z)
            return
        }
        if (p.y < b.deathY) die()
    }

    private fun blocked(x: Float, z: Float): Boolean {
        val g = level.groundAtPos(x, z) ?: return false
        return g > player.y + 0.35f
    }

    private fun moveX(dx: Float) {
        if (dx == 0f) return
        val p = player
        val nx = p.x + dx
        val probe = nx + sign(dx) * b.playerRadius
        val s = b.playerRadius * 0.7f
        if (blocked(probe, p.z) || blocked(probe, p.z + s) || blocked(probe, p.z - s)) p.vx = 0f else p.x = nx
    }

    private fun moveZ(dz: Float) {
        if (dz == 0f) return
        val p = player
        val nz = p.z + dz
        val probe = nz + sign(dz) * b.playerRadius
        val s = b.playerRadius * 0.7f
        if (blocked(p.x, probe) || blocked(p.x + s, probe) || blocked(p.x - s, probe)) p.vz = 0f else p.z = nz
    }

    private fun collideCrates(prevY: Float, r: Float, h: Float) {
        val p = player
        val half = b.crateSize / 2f
        for (c in crates) {
            if (!c.alive) continue
            val top = c.base + b.crateSize
            if (p.spinning && c.type != 'B') {
                val dx = p.x - c.x
                val dz = p.z - c.z
                if (dx * dx + dz * dz < (b.spinRadius + half * 0.5f).let { it * it } &&
                    p.y < top + 0.5f && p.y + h > c.base - 0.5f
                ) {
                    breakCrate(c)
                    continue
                }
            }
            val dx = p.x - c.x
            val dz = p.z - c.z
            val ox = r + half - abs(dx)
            val oz = r + half - abs(dz)
            if (ox <= 0f || oz <= 0f) continue
            if (p.y >= top || p.y + h <= c.base) continue
            if (prevY >= top - 0.15f && p.vy <= 0f) {
                p.y = top
                p.onGround = false
                p.jumping = false
                if (c.type == 'B') {
                    p.vy = b.bounceCrateVelocity
                    c.squash = 1f
                    emit(EventType.BOUNCE, c.x, top, c.z)
                } else {
                    p.vy = b.crateBounce
                    breakCrate(c)
                }
            } else if (p.vy > 0f && prevY + h <= c.base + 0.15f) {
                p.vy = 0f
                p.y = c.base - h
                if (c.type != 'B') breakCrate(c)
            } else {
                if (ox < oz) {
                    p.x += (if (dx >= 0f) 1f else -1f) * ox
                    p.vx = 0f
                } else {
                    p.z += (if (dz >= 0f) 1f else -1f) * oz
                    p.vz = 0f
                }
            }
        }
    }

    private fun breakCrate(c: Crate) {
        c.alive = false
        cratesBroken++
        val cy = c.base + b.crateSize / 2f
        emit(EventType.CRATE, c.x, cy, c.z)
        when (c.type) {
            'C' -> addAcai(b.acaiPerCrate, c.x, cy, c.z)
            'V' -> {
                lives++
                emit(EventType.LIFE, c.x, cy, c.z)
            }
            'K' -> {
                cpX = c.x; cpY = c.base; cpZ = c.z
                emit(EventType.CHECKPOINT, c.x, cy, c.z)
            }
        }
    }

    private fun addAcai(n: Int, x: Float, y: Float, z: Float) {
        acai += n
        emit(EventType.ACAI, x, y, z)
        while (acai >= b.acaiPerLife) {
            acai -= b.acaiPerLife
            lives++
            emit(EventType.LIFE, x, y, z)
        }
    }

    private fun updateCrabs(dt: Float) {
        for (k in crabs) {
            if (k.alive) {
                k.x += k.dir * b.crabSpeed * dt
                if (k.x > k.maxX) { k.x = k.maxX; k.dir = -1f }
                if (k.x < k.minX) { k.x = k.minX; k.dir = 1f }
                k.walkPhase += dt * 9f
            } else if (k.deadTime < 3f) {
                k.deadTime += dt
                k.flyVy -= b.gravity * dt
                k.flyY += k.flyVy * dt
                k.x += k.flyVx * dt
            }
        }
    }

    private fun killCrab(k: Crab, fly: Boolean) {
        k.alive = false
        k.deadTime = 0f
        k.flyY = 0f
        if (fly) {
            k.flyVx = if (k.x >= player.x) 6f else -6f
            k.flyVy = 9f
        } else {
            k.flyVx = 0f
            k.flyVy = 0f
        }
        emit(EventType.STOMP, k.x, k.y + 0.4f, k.z)
    }

    private fun collideCrabs(prevY: Float, h: Float) {
        val p = player
        for (k in crabs) {
            if (!k.alive) continue
            val dx = p.x - k.x
            val dz = p.z - k.z
            val dist = sqrt(dx * dx + dz * dz)
            if (p.spinning && dist < b.spinRadius && abs(p.y - k.y) < 1.2f) {
                killCrab(k, true)
                continue
            }
            val top = k.y + b.crabHeight
            if (dist < b.crabRadius + b.hurtRadius && p.y < top && p.y + h > k.y) {
                if (p.vy < 0f && (prevY >= k.y + b.crabHeight * 0.5f || p.y > k.y + b.crabHeight * 0.4f)) {
                    killCrab(k, false)
                    p.vy = b.stompBounce
                    p.jumping = false
                    p.y = top
                } else {
                    die()
                    return
                }
            }
        }
    }

    private fun collectAcai(h: Float) {
        val p = player
        val cy = p.y + h / 2f
        val rr = b.pickupRadius * b.pickupRadius
        for (a in acais) {
            if (!a.alive) continue
            val dx = p.x - a.x
            val dy = cy - a.y
            val dz = p.z - a.z
            if (dx * dx + dy * dy + dz * dz < rr) {
                a.alive = false
                addAcai(1, a.x, a.y, a.z)
            }
        }
    }

    fun die() {
        if (state != WorldState.PLAYING) return
        state = WorldState.DYING
        stateTime = 0f
        player.spinTimer = 0f
        emit(EventType.HURT, player.x, player.y, player.z)
    }

    private fun respawnOrEnd() {
        lives--
        if (lives <= 0) {
            lives = 0
            state = WorldState.GAME_OVER
            stateTime = 0f
            emit(EventType.GAME_OVER, player.x, player.y, player.z)
        } else {
            placePlayer()
            state = WorldState.PLAYING
            stateTime = 0f
        }
    }
}
