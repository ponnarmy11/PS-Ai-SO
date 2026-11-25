package com.example.minimaxcreator.data.remote

import com.example.minimaxcreator.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AgnesAuthInterceptor(
    private val settingsRepository: SettingsRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = runBlocking { settingsRepository.agnesApiKey() }
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}
