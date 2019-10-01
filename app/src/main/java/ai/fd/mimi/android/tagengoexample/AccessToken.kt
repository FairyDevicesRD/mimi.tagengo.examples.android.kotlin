package ai.fd.mimi.android.tagengoexample

import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

data class AccessTokenSchema(
    var selfLink: String,
    var expires_in: Int,
    var kind: String,
    var progress: Int,
    var status: String,
    var accessToken: String,
    var error: String,
    var operationId: String,
    var startTimestamp: Int,
    var endTimestamp: Int,
    var targetLink: String
)

const val AUTH_URL = "https://auth.mimi.fd.ai/v2/token"
const val AUTH_GRANT_TYPE = "https://auth.mimi.fd.ai/grant_type/application_credentials"
const val AUTH_SCOPE =
    "https://apis.mimi.fd.ai/auth/nict-asr/websocket-api-service;https://apis.mimi.fd.ai/auth/nict-asr/http-api-service;https://apis.mimi.fd.ai/auth/nict-tra/http-api-service;https://apis.mimi.fd.ai/auth/nict-tts/http-api-service"

class AccessToken(val appId: String, val secret: String) {

    fun get(): String? {
        val body = FormBody.Builder()
            .add("grant_type", AUTH_GRANT_TYPE)
            .add("client_id", appId)
            .add("client_secret", secret)
            .add("scope", AUTH_SCOPE)
            .build()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(AUTH_URL)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.code == 200 && responseBody.length > 0) {
            val gson = Gson()
            val token = gson.fromJson(responseBody, AccessTokenSchema::class.java)
            if (token != null) {
                return token.accessToken
            }
        }
        return null
    }
}
