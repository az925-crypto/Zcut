package com.zaaam.zcut.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zaaam.zcut.core.domain.GenerationDocument
import com.zaaam.zcut.core.domain.GenerationEntry
import com.zaaam.zcut.core.domain.GenerationHistory
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private val Context.generationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "zcut_generations"
)

/**
 * Local generation history (F24). Nothing is generated on device — this only
 * remembers results the user imported, so the history grid shows real files.
 */
interface GenerationHistoryRepository {
    val entries: Flow<List<GenerationEntry>>

    suspend fun add(entry: GenerationEntry)

    suspend fun delete(id: String)

    suspend fun get(id: String): GenerationEntry?
}

class JsonGenerationHistoryRepository(
    private val context: Context
) : GenerationHistoryRepository {

    private val documentKey = stringPreferencesKey("generation_document")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val entries: Flow<List<GenerationEntry>> = context.generationDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> decode(preferences[documentKey]) }
        .flowOn(Dispatchers.IO)

    override suspend fun add(entry: GenerationEntry) {
        withContext(Dispatchers.IO) {
            context.generationDataStore.edit { preferences ->
                val current = decode(preferences[documentKey])
                val next = GenerationHistory.add(current, entry)
                preferences[documentKey] = encode(next)
            }
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            context.generationDataStore.edit { preferences ->
                val current = decode(preferences[documentKey])
                preferences[documentKey] = encode(current.filterNot { it.id == id })
            }
        }
    }

    override suspend fun get(id: String): GenerationEntry? =
        entries.first().firstOrNull { it.id == id }

    /**
     * A corrupt payload must not take the app down; an unreadable history is
     * reported as empty rather than crashing on every read.
     */
    private fun decode(raw: String?): List<GenerationEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        val document = runCatching {
            json.decodeFromString(GenerationDocument.serializer(), raw)
        }.getOrNull() ?: return emptyList()
        return GenerationHistory.sanitize(document.entries)
    }

    private fun encode(entries: List<GenerationEntry>): String =
        json.encodeToString(
            GenerationDocument.serializer(),
            GenerationDocument(GenerationHistory.sanitize(entries))
        )
}
