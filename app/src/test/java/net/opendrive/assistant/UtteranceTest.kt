package net.opendrive.assistant

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class UtteranceTest {
    @Test fun wavContainsTheExactCaptureAndCorrectFormat() {
        val pcm = byteArrayOf(1, 2, 3, 4)
        val wav = Utterance.wav(pcm)
        val header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals(40, header.getInt(4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals(1, header.getShort(22).toInt())
        assertEquals(16_000, header.getInt(24))
        assertEquals(16, header.getShort(34).toInt())
        assertEquals(4, header.getInt(40))
        assertArrayEquals(pcm, wav.copyOfRange(44, wav.size))
    }

    @Test fun silenceAloneNeverCountsAsSpeechAndEndsAfterFiveSeconds() {
        val capture = Utterance()
        val silence = ByteArray(32_000)
        repeat(4) { assertFalse(capture.append(silence, 0, silence.size)) }
        assertTrue(capture.append(silence, 0, silence.size))
        assertFalse(capture.hasSpeech)
    }

    @Test fun recordingEndsAfterASecondOfSilenceFollowingSpeech() {
        val capture = Utterance()
        val speech = loud(3200)
        assertFalse(capture.append(speech, 0, speech.size))
        assertTrue(capture.hasSpeech)
        assertFalse(capture.append(ByteArray(16_000), 0, 16_000))
        assertTrue(capture.append(ByteArray(16_000), 0, 16_000))
    }

    @Test fun continuousNoiseCannotGrowTheBufferPastTwentySeconds() {
        val capture = Utterance()
        val speech = loud(32_000)
        repeat(19) { assertFalse(capture.append(speech, 0, speech.size)) }
        assertTrue(capture.append(speech, 0, speech.size))
        repeat(10) { assertTrue(capture.append(speech, 0, speech.size)) }
        assertEquals(640_044, capture.wav().size)
    }

    private fun loud(bytes: Int) = ByteArray(bytes) { if (it % 2 == 0) 0 else 20 }
}
