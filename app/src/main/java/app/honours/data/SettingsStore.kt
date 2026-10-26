package app.honours.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.honours.domain.Gender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** How Russian words address the person: «помыл» or «помыла». */
    val gender: Gender? = null,
    /** The hour the commission sits and sends its one invitation; -1 for never. */
    val hour: Int = 21,
    val sound: Boolean = true,
    /** Who signs the award sheets sent to others. */
    val signer: String = "",
    /** Shuffles the agenda so it differs from phone to phone. */
    val salt: Long = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        gender = Gender.entries.firstOrNull { it.name == this[GENDER] },
        hour = this[HOUR] ?: 21,
        sound = this[SOUND] ?: true,
        signer = this[SIGNER] ?: "",
        salt = this[SALT] ?: 0,
    )

    suspend fun now(): Settings = settings.first().let { s ->
        if (s.salt != 0L) s else { update { it.copy(salt = Random.nextLong(1, Long.MAX_VALUE)) }; settings.first() }
    }

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[GENDER] = s.gender?.name ?: ""
            it[HOUR] = s.hour
            it[SOUND] = s.sound
            it[SIGNER] = s.signer
            it[SALT] = s.salt
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val GENDER = stringPreferencesKey("gender")
        val HOUR = intPreferencesKey("hour")
        val SOUND = booleanPreferencesKey("sound")
        val SIGNER = stringPreferencesKey("signer")
        val SALT = longPreferencesKey("salt")
    }
}
