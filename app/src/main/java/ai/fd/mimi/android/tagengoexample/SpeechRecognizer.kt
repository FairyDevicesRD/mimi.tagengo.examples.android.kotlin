package ai.fd.mimi.android.tagengoexample

import android.util.Log
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.net.URISyntaxException

const val SR_URL = "wss://sandbox-sr.mimi.fd.ai"
const val SR_TAG = "SR"

data class Response(
    val result: String,
    val words: List<String>,
    val determined: String,
    val time: Long
)

data class SRResponse(
    val type: String,
    val session_id: String,
    val status: String,
    val response: List<Response>
)

class SpeechRecognizer(
    val token: String,
    val lang: String,
    val mimiOption: String,
    val isProgressive: Boolean,
    val isTemporary: Boolean
) {
    private var listener: OnMessageListener? = null
    private var wsc: WsClient? = null

    interface OnMessageListener {
        fun onOpen(status: Short)
        fun onMessage(message: String, determined: Boolean)
        fun onClose(code: Int, reason: String)
        fun onError(ex: Exception)
    }

    inner class WsClient(uri: URI, draft: Draft) : WebSocketClient(uri, draft) {
        override fun onOpen(handshakeData: ServerHandshake?) {
            if (handshakeData != null) {
                listener?.onOpen(handshakeData.httpStatus)
            }
        }

        override fun onMessage(message: String?) {
            if (message != null) {
                val gson = Gson()
                val resp = gson.fromJson(message, SRResponse::class.java)
                val resultWord = mutableListOf<String>()
                if (resp != null) {
                    if (resp.type == "asr#nictlvcsr") {
                        for (r in resp.response) {
                            resultWord.add(r.result.split("\\|".toRegex())[0])
                        }
                    }
                    else {
                        resultWord.add(resp.response[0].result)
                    }
                }
                val text = if (lang == "ja") {
                    resultWord.joinToString("")
                } else {
                    resultWord.joinToString(" ")
                }
                val isDetermined = if (resp.type == "asr#nictlvcsr") true else resp.response[0].determined.toBoolean()

                listener?.onMessage(text, isDetermined)
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            val r = reason ?: ""
            listener?.onClose(code, r)
        }

        override fun onError(ex: java.lang.Exception?) {
            if (ex != null) {
                listener?.onError(ex)
            }
        }
    }

    fun open() {
        try {
            val nictAsrOptions: String = if (this.mimiOption == "v2") {
                "response_format=v2;progressive=${this.isProgressive};temporary=${this.isTemporary}"
            } else {
                "response_format=v1"
            }
            val uri =
                URI(SR_URL + "?process=nict-asr&input-language=${this.lang}&nict-asr-options=${nictAsrOptions}&access-token=${this.token}&content-type=audio/x-pcm;bit=16;rate=16000")
            wsc = WsClient(uri, Draft_6455())
            wsc?.connectBlocking()
            Log.d(SR_TAG, "connect success")
        } catch (e: URISyntaxException) {
            Log.d(SR_TAG, e.message.toString())
        } catch (e: InterruptedException) {
            Log.d(SR_TAG, e.message.toString())
        }
    }

    fun sendByteData(b: ByteArray?) {
        //Log.d(SR_TAG, b?.size.toString())
        try {
            wsc?.send(b)
        } catch (e: WebsocketNotConnectedException) {
            Log.e(SR_TAG, "ERROR", e)
        }
    }

    fun sendRecogBreak() {
        try {
            wsc?.send("{\"command\": \"recog-break\"}")
        } catch (e: WebsocketNotConnectedException) {
            Log.e(SR_TAG, "ERROR", e)
        }
    }

    fun setOnMessageListener(listener: OnMessageListener) {
        this.listener = listener
    }

    fun removeOnMessageListener() {
        this.listener = null
    }


}