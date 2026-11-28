package com.example.minimaxcreator.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.minimaxcreator.data.local.db.AppSettings
import com.example.minimaxcreator.data.local.db.ImageParams
import com.example.minimaxcreator.data.local.db.VideoParams
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val ApiKey = stringPreferencesKey("encrypted_api_key")
        val MiniMaxApiKey = stringPreferencesKey("mini_max_api_key")
        val QwenApiKey = stringPreferencesKey("qwen_api_key")
        val QwenRegion = stringPreferencesKey("qwen_region")
        val AgnesApiKey = stringPreferencesKey("agnes_api_key")
        val FalApiKey = stringPreferencesKey("fal_api_key")
        val DefaultImageModel = stringPreferencesKey("default_image_model")
        val DefaultVideoModel = stringPreferencesKey("default_video_model")
        val ModelParams = stringPreferencesKey("model_params")
        val AutoSave = booleanPreferencesKey("auto_save")
        val DynamicColors = booleanPreferencesKey("dynamic_colors")
        val ImageParams = stringPreferencesKey("image_params")
        val VideoParams = stringPreferencesKey("video_params")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        val legacyApiKey = decrypt(preferences[Keys.ApiKey] ?: "")
        val miniMaxApiKey = decrypt(preferences[Keys.MiniMaxApiKey] ?: "")
            .ifBlank { legacyApiKey }
        val modelParams = decodeModelParams(preferences[Keys.ModelParams] ?: "")
        AppSettings(
            apiKey = miniMaxApiKey,
            miniMaxApiKey = miniMaxApiKey,
            qwenApiKey = decrypt(preferences[Keys.QwenApiKey] ?: ""),
            qwenRegion = preferences[Keys.QwenRegion] ?: "SINGAPORE",
            agnesApiKey = decrypt(preferences[Keys.AgnesApiKey] ?: ""),
            falApiKey = decrypt(preferences[Keys.FalApiKey] ?: ""),
            defaultImageModel = preferences[Keys.DefaultImageModel] ?: "image-01",
            defaultVideoModel = preferences[Keys.DefaultVideoModel] ?: "MiniMax-H3",
            modelParams = modelParams,
            autoSave = preferences[Keys.AutoSave] ?: false,
            dynamicColors = preferences[Keys.DynamicColors] ?: false,
            imageParams = decodeImageParams(preferences[Keys.ImageParams]),
            videoParams = decodeVideoParams(preferences[Keys.VideoParams])
        )
    }

    suspend fun currentApiKey(): String = settings.first().miniMaxApiKey

    suspend fun qwenApiKey(): String = settings.first().qwenApiKey

    suspend fun qwenRegion(): String = settings.first().qwenRegion

    suspend fun setQwenRegion(region: String) {
        require(region in setOf("SINGAPORE", "CHINA_BEIJING"))
        context.settingsDataStore.edit { it[Keys.QwenRegion] = region }
    }

    suspend fun agnesApiKey(): String = settings.first().agnesApiKey

    suspend fun falApiKey(): String = settings.first().falApiKey

    suspend fun setApiKey(apiKey: String) {
        context.settingsDataStore.edit { preferences ->
            val encrypted = apiKey.trim().takeIf { it.isNotBlank() }?.let(::encrypt) ?: ""
            preferences[Keys.ApiKey] = encrypted
            preferences[Keys.MiniMaxApiKey] = encrypted
        }
    }

    suspend fun setMiniMaxApiKey(apiKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MiniMaxApiKey] = apiKey.trim().takeIf { it.isNotBlank() }?.let(::encrypt) ?: ""
            preferences[Keys.ApiKey] = preferences[Keys.MiniMaxApiKey] ?: ""
        }
    }

    suspend fun setQwenApiKey(apiKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.QwenApiKey] = apiKey.trim().takeIf { it.isNotBlank() }?.let(::encrypt) ?: ""
        }
    }

    suspend fun setFalApiKey(apiKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.FalApiKey] = apiKey.trim().takeIf { it.isNotBlank() }?.let(::encrypt) ?: ""
        }
    }

    suspend fun setAgnesApiKey(apiKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AgnesApiKey] = apiKey.trim().takeIf { it.isNotBlank() }?.let(::encrypt) ?: ""
        }
    }

    suspend fun setDefaultImageModel(model: String) {
        context.settingsDataStore.edit { it[Keys.DefaultImageModel] = model }
    }

    suspend fun setDefaultVideoModel(model: String) {
        context.settingsDataStore.edit { it[Keys.DefaultVideoModel] = model }
    }

    suspend fun setModelParams(model: String, paramsJson: String) {
        val current = decodeModelParams(
            context.settingsDataStore.data.first()[Keys.ModelParams] ?: ""
        ).toMutableMap()
        current[model] = paramsJson
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ModelParams] = json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                current
            )
        }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AutoSave] = enabled }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DynamicColors] = enabled }
    }

    suspend fun setImageParams(params: ImageParams) {
        context.settingsDataStore.edit { it[Keys.ImageParams] = json.encodeToString(ImageParams.serializer(), params) }
    }

    suspend fun setVideoParams(params: VideoParams) {
        context.settingsDataStore.edit { it[Keys.VideoParams] = json.encodeToString(VideoParams.serializer(), params) }
    }

    private fun decodeImageParams(value: String?): ImageParams {
        if (value.isNullOrBlank()) return ImageParams()
        return runCatching { json.decodeFromString(ImageParams.serializer(), value) }
            .getOrDefault(ImageParams())
    }

    private fun decodeVideoParams(value: String?): VideoParams {
        if (value.isNullOrBlank()) return VideoParams()
        return runCatching { json.decodeFromString(VideoParams.serializer(), value) }
            .getOrDefault(VideoParams())
    }

    private fun decodeModelParams(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                value
            )
        }.getOrDefault(emptyMap())
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, 12)
            val encrypted = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ALIAS = "minimax_creator_api_key"
    }
}
