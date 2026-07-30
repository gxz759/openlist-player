package com.example.contentplayer.data

import com.example.contentplayer.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class ContentItem(
    val path: String,
    val name: String,
    @SerializedName("server_id") val serverId: String,
    @SerializedName("is_dir") val isDirectory: Boolean = false,
    val size: Long? = null,
    val modified: String? = null,
)

data class ResolveRequest(
    @SerializedName("server_id") val serverId: String,
    val path: String,
)
data class ResolvedMedia(
    val url: String? = null,
    @SerializedName("proxy_url") val proxyUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
)
data class ProgressRequest(
    @SerializedName("server_id") val serverId: String,
    val path: String,
    @SerializedName("position_seconds") val positionSeconds: Double,
    @SerializedName("duration_seconds") val durationSeconds: Double,
)
data class FavoriteRequest(
    @SerializedName("server_id") val serverId: String,
    val path: String,
    val name: String,
)
data class LoginRequest(val username: String, val password: String)
data class SavedItem(
    val path: String,
    @SerializedName("server_id") val serverId: String,
    val name: String? = null,
)

interface ContentApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest)

    @GET("api/v1/content")
    suspend fun content(
        @Query("path") path: String,
        @Query("q") query: String? = null,
    ): List<ContentItem>

    @POST("api/v1/playback/resolve")
    suspend fun resolve(@Body request: ResolveRequest): ResolvedMedia

    @PUT("api/v1/progress")
    suspend fun progress(@Body request: ProgressRequest)

    @GET("api/v1/favorites")
    suspend fun favorites(): List<SavedItem>

    @POST("api/v1/favorites")
    suspend fun favorite(@Body request: FavoriteRequest)

    @GET("api/v1/history")
    suspend fun history(): List<SavedItem>
}

object ApiProvider {
    private val cookies = object : CookieJar {
        private val values = mutableMapOf<String, List<Cookie>>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            values[url.host] = cookies
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            values[url.host].orEmpty().filter { it.matches(url) }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookies)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ContentApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ContentApi::class.java)
}
