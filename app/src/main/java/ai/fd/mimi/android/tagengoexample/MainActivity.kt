package ai.fd.mimi.android.tagengoexample

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

const val appId = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" // FIXME
const val secret = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" // FIXME

const val MAIN_TAG = "TagengoExample"

class MainActivity : AppCompatActivity(), SpeechRecognizer.OnMessageListener {
    private var isRecording = false
    private var recorder: Recorder? = null
    private var token: String = ""

    private fun checkPermission(activity: Activity) {
        val permissions =
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET)
        for (p in permissions) {
            val permissionCheck = ContextCompat.checkSelfPermission(activity, p)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(p), 1)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission(this)

        val handler = Handler()
        val r = object : Runnable {
            override fun run() {
                updateToken()
                handler.postDelayed(this, 3000 * 1000) // 50 min
            }
        }
        handler.post(r)

        // 言語選択Spinnerの設定
        val langSpinners = mutableListOf<Spinner>()
        langSpinners.add(findViewById(R.id.sr_in_lang))
        langSpinners.add(findViewById(R.id.mt_src_lang))
        langSpinners.add(findViewById(R.id.mt_dst_lang))
        langSpinners.add(findViewById(R.id.ss_in_lang))

        langSpinners.map { spinner ->
            ArrayAdapter.createFromResource(
                this,
                R.array.lang_array,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }
        }
        //findViewById<Spinner>(R.id.sr_in_lang).setSelection(1)
        //findViewById<Spinner>(R.id.mt_src_lang).setSelection(1)
        findViewById<Spinner>(R.id.mt_dst_lang).setSelection(1)
        findViewById<Spinner>(R.id.ss_in_lang).setSelection(1)

        // 音声合成用の声の種別Spinnerの設定
        val voiceKindSpinner = findViewById<Spinner>(R.id.ss_in_voice)
        ArrayAdapter.createFromResource(
            this,
            R.array.voice_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            voiceKindSpinner.adapter = adapter
        }

        findViewById<Button>(R.id.sr_exe_btn).setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(v: View?) {
                    val btn = findViewById<Button>(R.id.sr_exe_btn)
                    if (!isRecording) {
                        onRecordStartClick()
                        isRecording = true
                        btn.text = "音声認識(タップで録音終了)"
                    } else {
                        onRecordEndClick()
                        isRecording = false
                        btn.text = "音声認識"
                    }
                }
            }
        )

        findViewById<Button>(R.id.sr_next).setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(v: View?) {
                    val text = findViewById<TextView>(R.id.sr_output).text
                    findViewById<EditText>(R.id.mt_input).setText(text)
                }
            }
        )

        findViewById<Button>(R.id.mt_exe_btn).setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(v: View?) {
                    //Toast.makeText(this@MainActivity, "Tapped MT", Toast.LENGTH_LONG).show()
                    onTranslateButtonClick()
                }
            }
        )

        findViewById<Button>(R.id.mt_next).setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(v: View?) {
                    val text = findViewById<TextView>(R.id.mt_output).text
                    findViewById<EditText>(R.id.ss_input).setText(text)
                }
            }
        )

        findViewById<Button>(R.id.ss_exe_btn).setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(v: View?) {
                    //Toast.makeText(this@MainActivity, "Tapped MT", Toast.LENGTH_LONG).show()
                    onSynthesizeButtonClick()
                }
            }
        )
    }

    fun updateToken() = GlobalScope.launch(Dispatchers.Main) {
        async(Dispatchers.Default) {
            AccessToken(appId, secret).get()
        }.await().let { t ->
            if (t != null) {
                token = t
            } else {
                token = ""
            }
            Log.d(MAIN_TAG, "update access token: $token")
        }
    }

    fun onRecordStartClick() = GlobalScope.launch(Dispatchers.Main) {
        Log.d(MAIN_TAG, "start button pressed")
        val lang = findViewById<Spinner>(R.id.sr_in_lang).selectedItem as String
        val speechRecognizer = SpeechRecognizer(token, lang)
        speechRecognizer.setOnMessageListener(this@MainActivity)
        recorder = Recorder()
        recorder?.setRecognizer(speechRecognizer)

        async(Dispatchers.Default) {
            recorder?.start()
        }.await()
    }

    fun onRecordEndClick() = GlobalScope.launch(Dispatchers.Main) {
        Log.d(MAIN_TAG, "stop button pressed")
        recorder?.stop()
    }


    fun onTranslateButtonClick() = GlobalScope.launch(Dispatchers.Main) {
        val text = findViewById<EditText>(R.id.mt_input).text.toString()
        val srcLang = findViewById<Spinner>(R.id.mt_src_lang).selectedItem as String
        val dst = findViewById<Spinner>(R.id.mt_dst_lang)
        val dstLang = dst.selectedItem as String

        async(Dispatchers.Default) {
            Translator(token).translate(text, srcLang, dstLang)
        }.await().let { resp ->
            // 結果の成否に関わらず文字列が返るので表示する
            val textView = findViewById<TextView>(R.id.mt_output)
            textView.text = resp
        }
    }

    fun onSynthesizeButtonClick() = GlobalScope.launch(Dispatchers.Main) {
        val text = findViewById<EditText>(R.id.ss_input).text.toString()
        val inputLang = findViewById<Spinner>(R.id.ss_in_lang).selectedItem as String
        val gender = findViewById<Spinner>(R.id.ss_in_voice).selectedItem as String

        async(Dispatchers.Default) {
            SpeechSynthesizer(token).synthesize(text, inputLang, gender)
        }.await().let { resp ->
            // 結果が音声データであれば再生する
            // エラー時はトーストでエラーだった旨を表示
            if (resp.code == 200 && resp.body != null) {
                val bytes = resp.body?.bytes() ?: byteArrayOf()
                val bufSize = AudioTrack.getMinBufferSize(
                    16000,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val player = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(16000)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufSize)
                    .build()

                val max = AudioTrack.getMaxVolume()
                player.setVolume(max)
                player.play()

                val wavHeaderSize = 44
                val size = if (bytes.size > 0) {
                    bytes.size
                } else {
                    wavHeaderSize
                }

                player.write(bytes, wavHeaderSize, size - wavHeaderSize)

            } else {
                Toast.makeText(this@MainActivity, "Speech Synthesis Error", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    override fun onOpen(status: Short) {
        Log.d(MAIN_TAG, "onOpen: status=${status}")
    }

    override fun onMessage(message: String) {
        Log.d(MAIN_TAG, "onMessage: message=${message}")

        findViewById<TextView>(R.id.sr_output).text = message
    }

    override fun onClose(code: Int, reason: String) {
        Log.d(MAIN_TAG, "onClose: code=${code}, reason=${reason}")
    }

    override fun onError(ex: Exception) {
        Log.d(MAIN_TAG, "onError: $ex / ${ex.message}")
    }

}
