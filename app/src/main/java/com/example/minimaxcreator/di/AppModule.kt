package com.example.minimaxcreator.di

import android.content.Context
import com.example.minimaxcreator.data.local.db.AppDatabase
import com.example.minimaxcreator.data.local.db.GenerationRecordDao
import com.example.minimaxcreator.data.remote.ApiAuthInterceptor
import com.example.minimaxcreator.data.remote.api.MiniMaxApiService
import com.example.minimaxcreator.data.remote.api.AgnesApiService
import com.example.minimaxcreator.data.remote.api.SeedanceApiService
import com.example.minimaxcreator.data.remote.api.FalVideoApiService
import com.example.minimaxcreator.data.repository.SettingsRepository
import com.example.minimaxcreator.data.remote.QwenAuthInterceptor
import com.example.minimaxcreator.data.remote.api.QwenApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    @Named("miniMax")
    fun provideMiniMaxOkHttpClient(settingsRepository: SettingsRepository): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(ApiAuthInterceptor(settingsRepository))
            .build()
    }

    @Provides
    @Singleton
    @Named("agnes")
    fun provideAgnesOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("agnesRetrofit")
    fun provideAgnesRetrofit(json: Json, @Named("agnes") client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://apihub.agnes-ai.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAgnesApi(@Named("agnesRetrofit") retrofit: Retrofit): AgnesApiService =
        retrofit.create(AgnesApiService::class.java)

    @Provides
    @Singleton
    @Named("seedance")
    fun provideSeedanceOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("seedanceRetrofit")
    fun provideSeedanceRetrofit(json: Json, @Named("seedance") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://ps-ai-studio-seedance.ps-ai-studio.workers.dev/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideSeedanceApi(@Named("seedanceRetrofit") retrofit: Retrofit): SeedanceApiService =
        retrofit.create(SeedanceApiService::class.java)

    @Provides
    @Singleton
    fun provideFalVideoApi(@Named("seedanceRetrofit") retrofit: Retrofit): FalVideoApiService =
        retrofit.create(FalVideoApiService::class.java)

    @Provides
    @Singleton
    @Named("qwen")
    fun provideQwenOkHttpClient(settingsRepository: SettingsRepository): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(QwenAuthInterceptor(settingsRepository))
            .build()
    }

    @Provides
    @Singleton
    @Named("miniMaxRetrofit")
    fun provideMiniMaxRetrofit(json: Json, @Named("miniMax") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.minimax.io/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideMiniMaxApi(@Named("miniMaxRetrofit") retrofit: Retrofit): MiniMaxApiService {
        return retrofit.create(MiniMaxApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("qwenRetrofit")
    fun provideQwenRetrofit(json: Json, @Named("qwen") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://dashscope.aliyuncs.com/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideQwenApi(@Named("qwenRetrofit") retrofit: Retrofit): QwenApiService {
        return retrofit.create(QwenApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.build(context)
    }

    @Provides
    @Singleton
    fun provideGenerationRecordDao(database: AppDatabase): GenerationRecordDao {
        return database.generationRecordDao()
    }
}
