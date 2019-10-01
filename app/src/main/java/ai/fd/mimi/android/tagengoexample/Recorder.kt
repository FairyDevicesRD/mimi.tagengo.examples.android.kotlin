package ai.fd.mimi.android.tagengoexample

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

const val REC_TAG = "REC"

const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO
const val SAMPLING_RATE = 16000
const val BUFFER_SIZE_IN_BYTE = 8192
const val RECORDING_BLOCK = 1024

class Recorder {
    private var speechRecognizer: SpeechRecognizer? = null
    private var record = AudioRecord.Builder()
        .setAudioSource(AUDIO_SOURCE)
        .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTE)
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AUDIO_ENCODING)
                .setSampleRate(SAMPLING_RATE)
                .setChannelMask(CHANNEL_MASK)
                .build()
        ).build()
    private var done = false

    fun setRecognizer(speechRecognizer: SpeechRecognizer) {
        this.speechRecognizer = speechRecognizer
    }

    fun removeRecognizer() {
        this.speechRecognizer = null
    }

    fun start() {
        done = false
        if (speechRecognizer != null) {
            Log.d(REC_TAG, "Recorder starts running.")
            speechRecognizer?.open()
            record.startRecording()

            var readBytes = 0
            var buf = ByteArray(RECORDING_BLOCK)
            while (!done) {
                readBytes = record.read(buf, 0, buf.size)
                if (readBytes != 0) {
                    speechRecognizer?.sendByteData(buf)
                }
            }
            record.stop()
            speechRecognizer?.sendRecogBreak()
            Log.d(REC_TAG, "Recorder stopped running.")
        }
    }

    fun stop() {
        done = true
    }

}