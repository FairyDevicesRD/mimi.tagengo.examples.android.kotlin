package ai.fd.mimi.android.tagengoexample

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.text.StringEscapeUtils

const val MT_URL = "https://sandbox-mt.mimi.fd.ai/machine_translation"
const val MT_TAG = "MT"

class Translator(val token: String) {

    fun translate(text: String, src_lang: String, dst_lang: String): String {
        Log.d(MT_TAG, "$text, $src_lang, $dst_lang")
        val body = FormBody.Builder()
            .add("text", text)
            .add("source_lang", src_lang)
            .add("target_lang", dst_lang)
            .build()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(MT_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val respBody = response.body?.string() ?: ""

        if (response.code == 200 &&
            respBody.startsWith("[\"")
        ) {
            val ret = respBody
                .replace("""^\["""".toRegex(), "")
                .replace(""""\]$""".toRegex(), "")
            return StringEscapeUtils.unescapeJava(ret)
        } else {
            return "*** Error: $respBody ***"
        }
    }
}