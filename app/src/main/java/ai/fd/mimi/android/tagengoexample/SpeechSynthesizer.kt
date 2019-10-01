package ai.fd.mimi.android.tagengoexample

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

const val SS_URL = "https://sandbox-ss.mimi.fd.ai/speech_synthesis"
const val SS_TAG = "SS"

class SpeechSynthesizer(val token: String) {

    fun synthesize(text: String, inputLang: String, gender: String): Response {
        Log.d(SS_TAG, "$text, $inputLang, $gender")
        val body = FormBody.Builder()
            .add("engine", "nict")
            .add("text", text)
            .add("lang", inputLang)
            .add("gender", gender)
            .build()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(SS_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        val resp = client.newCall(request).execute()

        Log.d(SS_TAG, resp.code.toString())
        return resp
    }
}