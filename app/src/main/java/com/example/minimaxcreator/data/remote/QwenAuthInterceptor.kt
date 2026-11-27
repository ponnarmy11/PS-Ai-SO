package com.example.minimaxcreator.data.remote

import com.example.minimaxcreator.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class QwenAuthInterceptor(
    private val settingsRepository: SettingsRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val (apiKey, region) = runBlocking {
            settingsRepository.qwenApiKey() to settingsRepository.qwenRegion()
        }
        val host = if (region == "CHINA_BEIJING") "dashscope.aliyuncs.com" else "dashscope-intl.aliyuncs.com"
        val request = chain.request().newBuilder()
            .url(chain.request().url.newBuilder().host(host).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}
