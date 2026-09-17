package com.example.data.remote

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response as OkHttpResponse
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GoogleSheetsApiService {

    @GET("macros/s/AKfycbzyFpZ4tqUZ34-wNvDT-SlFOn3Gx4n-Jm2I_kQaKGWM7HEfKGejchmDPu2d_Ph1TufmFA/exec")
    suspend fun getSheetData(): Response<GoogleSheetsResponse>

    @POST("macros/s/AKfycbzyFpZ4tqUZ34-wNvDT-SlFOn3Gx4n-Jm2I_kQaKGWM7HEfKGejchmDPu2d_Ph1TufmFA/exec")
    suspend fun addGanado(@Body request: AddGanadoRequest): Response<ApiResponse>

    @POST("macros/s/AKfycbzyFpZ4tqUZ34-wNvDT-SlFOn3Gx4n-Jm2I_kQaKGWM7HEfKGejchmDPu2d_Ph1TufmFA/exec")
    suspend fun addPesaje(@Body request: AddPesajeRequest): Response<ApiResponse>
}

/**
 * Interceptor especial para Google Apps Script:
 * 1. Google Apps Script requiere que las peticiones POST se envíen con Content-Type: text/plain
 *    para evitar pre-flight CORS y errores de gateway en el redirect.
 * 2. Maneja redirecciones 302 hacia script.googleusercontent.com siguiendo el método GET
 *    con el resultado de la ejecución.
 */
class GoogleAppsScriptInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val originalRequest = chain.request()

        val modifiedRequest = if (originalRequest.method.equals("POST", ignoreCase = true)) {
            val body = originalRequest.body
            val buffer = okio.Buffer()
            body?.writeTo(buffer)
            val jsonContent = buffer.readUtf8()

            originalRequest.newBuilder()
                .header("Content-Type", "text/plain; charset=utf-8")
                .post(jsonContent.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .build()
        } else {
            originalRequest
        }

        var response = chain.proceed(modifiedRequest)

        // Manejo manual de redirecciones si OkHttp no siguió el redirect 302 con GET
        var redirectCount = 0
        while ((response.code == 302 || response.code == 301 || response.code == 307 || response.code == 308) && redirectCount < 5) {
            val location = response.header("Location") ?: break
            Log.d("AppsScriptInterceptor", "Siguiendo redirección a: $location")
            response.close()
            val redirectRequest = Request.Builder()
                .url(location)
                .get()
                .build()
            response = chain.proceed(redirectRequest)
            redirectCount++
        }

        return response
    }
}

object RetrofitClient {
    private const val BASE_URL = "https://script.google.com/"

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(GoogleAppsScriptInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(35, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val apiService: GoogleSheetsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleSheetsApiService::class.java)
    }
}
