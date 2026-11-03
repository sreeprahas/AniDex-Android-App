package com.anidex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AnimeEntry(
    val id: Int,
    val title: String,
    val image: String? = null,
    val watched: Int = 0,
    val total: Int? = null,
    val status: String = "watching",
    val score: Int = 0
)

class MalApi(private val client: OkHttpClient = OkHttpClient()) {
    private var accessToken: String? = null

    fun setToken(token: String) { accessToken = token }

    suspend fun exchangeCode(clientId: String, code: String, verifier: String): String =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("code_verifier", verifier)
                .add("redirect_uri", "anidex://oauth")
                .build()
            val req = Request.Builder().url("https://myanimelist.net/v1/oauth2/token").post(body).build()
            client.newCall(req).execute().use { response ->
                val text = response.body?.string().orEmpty()
                check(response.isSuccessful) { "MAL token exchange failed (${response.code}): $text" }
                JSONObject(text).getString("access_token").also { accessToken = it }
            }
        }

    suspend fun fetchMyAnimeList(): List<AnimeEntry> = withContext(Dispatchers.IO) {
        val token = requireNotNull(accessToken) { "Sign in to MAL first" }
        val result = mutableListOf<AnimeEntry>()
        var offset = 0
        do {
            val url = "https://api.myanimelist.net/v2/users/@me/animelist?fields=list_status,num_episodes,main_picture&limit=100&offset=$offset"
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
            val page = client.newCall(req).execute().use { response ->
                val text = response.body?.string().orEmpty()
                check(response.isSuccessful) { "MAL list request failed (${response.code}): $text" }
                JSONObject(text)
            }
            val arr = page.getJSONArray("data")
            for (i in 0 until arr.length()) {
                val node = arr.getJSONObject(i)
                val anime = node.getJSONObject("node")
                val status = node.optJSONObject("list_status") ?: JSONObject()
                val picture = anime.optJSONObject("main_picture")?.optString("large")
                result += AnimeEntry(
                    id = anime.getInt("id"),
                    title = anime.getString("title"),
                    image = picture,
                    watched = status.optInt("num_episodes_watched", 0),
                    total = anime.optInt("num_episodes").takeIf { it > 0 },
                    status = status.optString("status", "watching"),
                    score = status.optInt("score", 0)
                )
            }
            offset += arr.length()
        } while (arr.length() == 100)
        result
    }

    suspend fun updateStatus(id: Int, watched: Int, status: String, score: Int = 0) =
        withContext(Dispatchers.IO) {
            val token = requireNotNull(accessToken) { "Sign in to MAL first" }
            val body = FormBody.Builder()
                .add("num_watched_episodes", watched.coerceAtLeast(0).toString())
                .add("status", status)
                .add("score", score.coerceIn(0, 10).toString())
                .build()
            val req = Request.Builder().url("https://api.myanimelist.net/v2/anime/$id/my_list_status")
                .header("Authorization", "Bearer $token").put(body).build()
            client.newCall(req).execute().use { response ->
                val text = response.body?.string().orEmpty()
                check(response.isSuccessful) { "MAL update failed (${response.code}): $text" }
            }
        }
}
