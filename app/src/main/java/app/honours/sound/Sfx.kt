package app.honours.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * The ceremony's sounds, synthesised once into small WAV files: a short brass fanfare, the typewriter of
 * the order, the stamp coming down, a pin going into felt, a bell for a higher class and a tick for a box
 * ticked. The heavy moments are also felt through the vibration motor.
 */
class Sfx(private val context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(8)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = IntArray(Snd.entries.size)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    @Volatile var on = true

    fun load() {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        for (s in Snd.entries) {
            val f = File(dir, "${s.name.lowercase()}1.wav")
            if (!f.exists()) Wav.write(f, s.make())
            ids[s.ordinal] = pool.load(f.absolutePath, 1)
        }
    }

    fun play(s: Snd, vol: Float = 1f, rate: Float = 1f) {
        if (on && ids[s.ordinal] != 0) pool.play(ids[s.ordinal], vol, vol, 1, 0, rate)
    }

    /** A short buzz of [ms] at [amp] (1–255). */
    fun feel(ms: Long, amp: Int = 80) {
        runCatching { if (vibrator?.hasVibrator() == true) vibrator.vibrate(VibrationEffect.createOneShot(ms, amp.coerceIn(1, 255))) }
    }
}

enum class Snd(val make: () -> ShortArray) {
    FANFARE(Synth::fanfare), TYPE(Synth::type), STAMP(Synth::stamp), PIN(Synth::pin), BELL(Synth::bell), TICK(Synth::tick)
}

object Synth {
    const val RATE = 22050

    private fun pcm(seconds: Double, f: (t: Double, i: Int) -> Double): ShortArray {
        val n = (RATE * seconds).toInt()
        return ShortArray(n) { i -> (f(i / RATE.toDouble(), i) * 32000).toInt().coerceIn(-32767, 32767).toShort() }
    }

    /** A brass-like tone: a stack of harmonics that brightens as it swells, with a little vibrato. */
    private fun brass(f: Double, t: Double, len: Double): Double {
        if (t < 0 || t > len + .25) return 0.0
        val attack = min(1.0, t / .035)
        val release = if (t > len) exp(-(t - len) * 18) else 1.0
        val bright = .35 + .65 * attack
        val vib = 1 + .006 * sin(2 * PI * 5.5 * t) * min(1.0, t / .25)
        var s = 0.0
        for (h in 1..7) s += sin(2 * PI * f * h * vib * t) * bright.pow(h - 1.0) / h
        return s * attack * release
    }

    /** Ta-ta-ta-taaa: G, C, E, then a held G, on two brass voices a fifth apart at the end. */
    fun fanfare(): ShortArray = pcm(1.6) { t, _ ->
        val notes = listOf(0.0 to 392.0, .14 to 523.25, .28 to 659.25)
        var s = 0.0
        for ((at, f) in notes) s += brass(f, t - at, .11)
        s += brass(783.99, t - .42, .85) + .55 * brass(523.25, t - .42, .85)
        s * .2
    }

    /** One typewriter key: a sharp clack with a short metal ring. */
    fun type(): ShortArray {
        val r = Random(5)
        return pcm(.06) { t, _ -> ((r.nextDouble() * 2 - 1) * exp(-t * 160) * .6 + sin(2 * PI * 1800 * t) * exp(-t * 90) * .2) }
    }

    /** A rubber stamp on paper over a wooden desk: a low knock and a slap. */
    fun stamp(): ShortArray {
        val r = Random(9)
        var lp = 0.0
        return pcm(.35) { t, _ ->
            val n = r.nextDouble() * 2 - 1
            lp += (n - lp) * .2
            sin(2 * PI * (110 - 60 * t) * t) * exp(-t * 22) * .8 + lp * exp(-t * 40) * 1.2
        }
    }

    /** A pin going into felt: a tiny metal tick, then a soft push. */
    fun pin(): ShortArray {
        val r = Random(13)
        var lp = 0.0
        return pcm(.2) { t, _ ->
            val n = r.nextDouble() * 2 - 1
            lp += (n - lp) * .08
            sin(2 * PI * 3400 * t) * exp(-t * 120) * .25 + (if (t > .03) lp * exp(-(t - .03) * 30) * 1.6 else 0.0)
        }
    }

    /** A higher class: a bright bell, struck twice. */
    fun bell(): ShortArray = pcm(1.5) { t, _ ->
        fun b(f: Double, at: Double) = if (t < at) 0.0 else (t - at).let { d -> (sin(2 * PI * f * d) + .4 * sin(2 * PI * f * 2.76 * d) + .2 * sin(2 * PI * f * 5.4 * d)) * exp(-d * 2.6) }
        (b(1318.5, 0.0) + b(1975.5, .16)) * .2
    }

    /** A box ticked on the agenda. */
    fun tick(): ShortArray = pcm(.04) { t, _ -> sin(2 * PI * 2200 * t) * exp(-t * 140) * .5 }
}

object Wav {
    fun write(f: File, pcm: ShortArray) {
        val data = ByteArrayOutputStream()
        for (s in pcm) { data.write(s.toInt() and 0xFF); data.write((s.toInt() shr 8) and 0xFF) }
        val bytes = data.toByteArray()
        DataOutputStream(f.outputStream().buffered()).use { o ->
            fun le32(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF); o.write((v shr 16) and 0xFF); o.write((v shr 24) and 0xFF) }
            fun le16(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF) }
            o.writeBytes("RIFF"); le32(36 + bytes.size); o.writeBytes("WAVE")
            o.writeBytes("fmt "); le32(16); le16(1); le16(1); le32(Synth.RATE); le32(Synth.RATE * 2); le16(2); le16(16)
            o.writeBytes("data"); le32(bytes.size); o.write(bytes)
        }
    }
}
