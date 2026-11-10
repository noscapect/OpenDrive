package net.opendrive.assistant

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Bounded 16 kHz mono PCM capture, with a conservative silence detector. No audio is written to disk. */
internal class Utterance {
    private val pcm = ByteArrayOutputStream()
    private var speechSamples = 0
    private var lastSpeechSample = 0
    private var samples = 0
    val hasSpeech: Boolean @Synchronized get() = speechSamples >= 1600

    /** True after a second of silence following speech, five silent seconds, or twenty seconds total. */
    @Synchronized
    fun append(bytes: ByteArray, offset: Int, length: Int): Boolean {
        val count = minOf(length, MAX_BYTES - pcm.size()).coerceAtLeast(0) / 2 * 2
        pcm.write(bytes, offset, count)
        var loud = 0
        for (i in offset until offset + count step 2) {
            val sample = ((bytes[i].toInt() and 255) or (bytes[i + 1].toInt() shl 8)).toShort().toInt()
            if (kotlin.math.abs(sample) >= 500) loud++
        }
        samples += count / 2
        if (loud > count / 20) { speechSamples += count / 2; lastSpeechSample = samples }
        return pcm.size() >= MAX_BYTES ||
            (hasSpeech && samples - lastSpeechSample >= 16_000) ||
            (!hasSpeech && samples >= 80_000)
    }

    @Synchronized fun wav(): ByteArray = wav(pcm.toByteArray())

    companion object {
        private const val MAX_BYTES = 20 * 16_000 * 2
        fun wav(pcm: ByteArray): ByteArray = ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + pcm.size); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(16_000); putInt(32_000)
            putShort(2); putShort(16); put("data".toByteArray()); putInt(pcm.size); put(pcm)
        }.array()
    }
}
