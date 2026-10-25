package com.lorran.tatu

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import kotlin.math.exp
import kotlin.math.sin

/** Desenha o mundo 3D: cenário em blocos, objetos, tatu, partículas e câmera. */
class GameRenderer(private val models: Models, private val world: World) : Disposable {
    val camera = PerspectiveCamera(62f, 1280f, 720f)
    private val batch = ModelBatch()
    private val env = Environment()

    private val chunks = ArrayList<ModelInstance>()
    private val chunkBounds = ArrayList<BoundingBox>()
    private val tatu = ModelInstance(models.tatu)
    private val ball = ModelInstance(models.ball)
    private val shadow = ModelInstance(models.shadow)
    private val goalFrame = ModelInstance(models.goalFrame)
    private val goalDisc = ModelInstance(models.goalDisc)
    private val crateInst = HashMap<Crate, ModelInstance>()
    private val crabInst = HashMap<Crab, ModelInstance>()
    private val acaiInst = HashMap<Acai, ModelInstance>()

    private class Particle(val inst: ModelInstance) {
        var x = 0f; var y = 0f; var z = 0f
        var vx = 0f; var vy = 0f; var vz = 0f
        var life = 0f; var spin = 0f
    }

    private val particles = ArrayList<Particle>()
    private val camPos = Vector3()
    private val camTarget = Vector3()
    private val lookAt = Vector3()
    private var clock = 0f

    init {
        env.set(ColorAttribute(ColorAttribute.AmbientLight, 0.58f, 0.58f, 0.62f, 1f))
        env.add(DirectionalLight().set(0.82f, 0.78f, 0.70f, -0.35f, -1f, -0.45f))
        env.set(ColorAttribute(ColorAttribute.Fog, Pal.SKY.r, Pal.SKY.g, Pal.SKY.b, 1f))
        camera.near = 0.3f
        camera.far = 75f

        for (model in models.buildLevelChunks(world.level)) {
            val inst = ModelInstance(model)
            chunks.add(inst)
            chunkBounds.add(inst.calculateBoundingBox(BoundingBox()))
        }
        for (c in world.crates) crateInst[c] = ModelInstance(models.crates[c.type]!!)
        for (k in world.crabs) crabInst[k] = ModelInstance(models.crab)
        for (a in world.acais) acaiInst[a] = ModelInstance(models.acai)
        goalFrame.transform.setToTranslation(world.goalX, world.goalY, world.goalZ)
        snapCamera()
    }

    fun resize(w: Int, h: Int) {
        camera.viewportWidth = w.toFloat()
        camera.viewportHeight = h.toFloat()
        // em telas muito largas abre um pouco o campo de visão
        camera.fieldOfView = MathUtils.clamp(58f + (w.toFloat() / h - 1.78f) * 6f, 56f, 66f)
        camera.update()
    }

    private fun desiredCamera() {
        val p = world.player
        val baseY = maxOf(p.y, 0f)
        camPos.set(p.x * 0.55f, baseY + 4.6f, p.z + 7.2f)
        camTarget.set(p.x * 0.8f, baseY + 1.1f, p.z - 4.5f)
    }

    fun snapCamera() {
        desiredCamera()
        camera.position.set(camPos)
        lookAt.set(camTarget)
        camera.up.set(Vector3.Y)
        camera.lookAt(lookAt)
        camera.update()
    }

    fun spawnBurst(type: EventType, x: Float, y: Float, z: Float) {
        val (name, count, speed) = when (type) {
            EventType.CRATE -> Triple("madeira", 10, 6f)
            EventType.ACAI -> Triple("acai", 5, 4f)
            EventType.STOMP -> Triple("caranguejo", 6, 5f)
            EventType.CHECKPOINT, EventType.LIFE, EventType.WIN -> Triple("brilho", 12, 5f)
            EventType.HURT -> Triple("casco", 14, 7f)
            else -> return
        }
        val model = models.particles[name] ?: return
        repeat(count) {
            val p = particles.firstOrNull { it.life <= 0f && it.inst.model === model }
                ?: Particle(ModelInstance(model)).also { particles.add(it) }
            p.x = x; p.y = y; p.z = z
            val a = MathUtils.random(MathUtils.PI2)
            val s = speed * MathUtils.random(0.5f, 1f)
            p.vx = MathUtils.cos(a) * s
            p.vz = MathUtils.sin(a) * s
            p.vy = MathUtils.random(3f, 8f)
            p.life = MathUtils.random(0.5f, 0.9f)
            p.spin = MathUtils.random(360f)
        }
    }

    fun render(dt: Float) {
        clock += dt
        // câmera suave
        desiredCamera()
        val k = 1f - exp(-dt * 6f)
        camera.position.lerp(camPos, k)
        lookAt.lerp(camTarget, MathUtils.clamp(k * 1.4f, 0f, 1f))
        camera.up.set(Vector3.Y)
        camera.lookAt(lookAt)
        camera.update()

        batch.begin(camera)
        for (i in chunks.indices) {
            if (camera.frustum.boundsInFrustum(chunkBounds[i])) batch.render(chunks[i], env)
        }

        val pz = world.player.z
        fun near(z: Float) = z < pz + 10f && z > pz - 70f

        for ((c, inst) in crateInst) {
            if (!c.alive || !near(c.z)) continue
            val s = 1f - c.squash * 0.25f
            val half = world.b.crateSize / 2f
            inst.transform.setToTranslation(c.x, c.base + half * s, c.z).scale(1f + c.squash * 0.15f, s, 1f + c.squash * 0.15f)
            batch.render(inst, env)
        }
        for ((k2, inst) in crabInst) {
            if (!near(k2.z)) continue
            if (k2.alive) {
                val bob = sin(k2.walkPhase) * 0.05f
                inst.transform.setToTranslation(k2.x, k2.y + bob, k2.z).rotate(Vector3.Z, sin(k2.walkPhase * 0.5f) * 6f)
            } else {
                if (k2.deadTime > 1.5f) continue
                if (k2.flyVx == 0f) {
                    // esmagado: fica achatado e some
                    inst.transform.setToTranslation(k2.x, k2.y, k2.z).scale(1.2f, 0.25f, 1.2f)
                } else {
                    inst.transform.setToTranslation(k2.x, k2.y + k2.flyY, k2.z).rotate(Vector3.Z, k2.deadTime * 720f)
                }
            }
            batch.render(inst, env)
        }
        for ((a, inst) in acaiInst) {
            if (!a.alive || !near(a.z)) continue
            inst.transform.setToTranslation(a.x, a.y + sin(clock * 3f + a.z) * 0.12f, a.z).rotate(Vector3.Y, clock * 120f + a.x * 40f)
            batch.render(inst, env)
        }
        if (near(world.goalZ)) {
            batch.render(goalFrame, env)
            goalDisc.transform.setToTranslation(world.goalX, world.goalY + 1.65f, world.goalZ).rotate(Vector3.Z, clock * 90f)
            batch.render(goalDisc, env)
        }

        renderPlayer()

        for (p in particles) {
            if (p.life <= 0f) continue
            p.life -= dt
            p.vy -= 25f * dt
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt
            p.spin += dt * 500f
            val s = MathUtils.clamp(p.life * 1.6f, 0.1f, 1f)
            p.inst.transform.setToTranslation(p.x, p.y, p.z).rotate(Vector3.Y, p.spin).rotate(Vector3.X, p.spin * 0.7f).scale(s, s, s)
            batch.render(p.inst, env)
        }
        batch.end()
    }

    private fun renderPlayer() {
        val p = world.player
        val dying = world.state == WorldState.DYING
        // sombra no chão logo abaixo
        val g = world.level.groundAtPos(p.x, p.z)
        if (g != null && p.y >= g - 0.1f && !dying) {
            val h = MathUtils.clamp(1f - (p.y - g) / 6f, 0.4f, 1f)
            shadow.transform.setToTranslation(p.x, g + 0.03f, p.z).scale(h, 1f, h)
            batch.render(shadow, env)
        }
        if (dying) {
            // pisca e encolhe ao perder vida
            val t = world.stateTime
            if (((t * 12f).toInt() % 2) == 0) return
            val s = MathUtils.clamp(1f - t / world.b.dyingTime, 0.05f, 1f)
            tatu.transform.setToTranslation(p.x, p.y, p.z).rotate(Vector3.Y, p.facing + t * 600f).scale(s, s, s)
            batch.render(tatu, env)
            return
        }
        if (p.spinning) {
            val spinAngle = (world.b.spinDuration - p.spinTimer) * 1440f
            ball.transform.setToTranslation(p.x, p.y + 0.66f, p.z).rotate(Vector3.Y, p.facing + spinAngle).rotate(Vector3.X, 18f)
            batch.render(ball, env)
        } else {
            val bob = if (p.onGround) kotlin.math.abs(sin(p.runPhase)) * 0.12f else 0f
            val tilt = if (p.onGround) sin(p.runPhase * 2f) * 4f else MathUtils.clamp(-p.vy * 1.5f, -15f, 15f)
            val stretch = if (!p.onGround && p.vy > 0f) 1.08f else 1f
            tatu.transform.setToTranslation(p.x, p.y + bob, p.z).rotate(Vector3.Y, p.facing).rotate(Vector3.X, tilt)
                .scale(1f / kotlin.math.sqrt(stretch), stretch, 1f / kotlin.math.sqrt(stretch))
            batch.render(tatu, env)
        }
    }

    override fun dispose() {
        batch.dispose()
    }
}
