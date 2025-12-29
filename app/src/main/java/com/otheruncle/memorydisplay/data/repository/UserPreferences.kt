package com.otheruncle.memorydisplay.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.otheruncle.memorydisplay.data.model.User
import com.otheruncle.memorydisplay.data.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val USER_ID = intPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_ROLE = stringPreferencesKey("user_role")
        val PROFILE_PHOTO = stringPreferencesKey("profile_photo")
        val BIRTHDAY = stringPreferencesKey("birthday")
        val CITY = stringPreferencesKey("city")
        val STATE = stringPreferencesKey("state")
        val TIMEZONE = stringPreferencesKey("timezone")
        val CALENDAR_URL = stringPreferencesKey("calendar_url")
        val CALENDAR_REVIEWER = booleanPreferencesKey("calendar_reviewer")
        val EVENT_REMINDERS = booleanPreferencesKey("event_reminders")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        // Stored credentials for biometric auto-login
        val STORED_EMAIL = stringPreferencesKey("stored_email")
        val STORED_PASSWORD = stringPreferencesKey("stored_password")
    }

    /**
     * Save user data
     */
    suspend fun saveUser(user: User) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.USER_NAME] = user.name
            prefs[Keys.USER_EMAIL] = user.email
            prefs[Keys.USER_ROLE] = user.role.name
            user.profilePhoto?.let { prefs[Keys.PROFILE_PHOTO] = it } ?: prefs.remove(Keys.PROFILE_PHOTO)
            user.birthday?.let { prefs[Keys.BIRTHDAY] = it } ?: prefs.remove(Keys.BIRTHDAY)
            user.city?.let { prefs[Keys.CITY] = it } ?: prefs.remove(Keys.CITY)
            user.state?.let { prefs[Keys.STATE] = it } ?: prefs.remove(Keys.STATE)
            user.timezone?.let { prefs[Keys.TIMEZONE] = it } ?: prefs.remove(Keys.TIMEZONE)
            user.calendarUrl?.let { prefs[Keys.CALENDAR_URL] = it } ?: prefs.remove(Keys.CALENDAR_URL)
            prefs[Keys.CALENDAR_REVIEWER] = user.calendarReviewer
            prefs[Keys.EVENT_REMINDERS] = user.eventReminders
        }
    }

    /**
     * Get user data
     */
    suspend fun getUser(): User? {
        val prefs = context.userDataStore.data.first()
        val userId = prefs[Keys.USER_ID] ?: return null

        return User(
            id = userId,
            name = prefs[Keys.USER_NAME] ?: "",
            email = prefs[Keys.USER_EMAIL] ?: "",
            role = try {
                UserRole.valueOf(prefs[Keys.USER_ROLE] ?: "FAMILY_MEMBER")
            } catch (e: Exception) {
                UserRole.FAMILY_MEMBER
            },
            profilePhoto = prefs[Keys.PROFILE_PHOTO],
            birthday = prefs[Keys.BIRTHDAY],
            city = prefs[Keys.CITY],
            state = prefs[Keys.STATE],
            timezone = prefs[Keys.TIMEZONE],
            calendarUrl = prefs[Keys.CALENDAR_URL],
            calendarReviewer = prefs[Keys.CALENDAR_REVIEWER] ?: false,
            eventReminders = prefs[Keys.EVENT_REMINDERS] ?: false
        )
    }

    /**
     * Observe user data changes
     */
    fun observeUser(): Flow<User?> = context.userDataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID] ?: return@map null

        User(
            id = userId,
            name = prefs[Keys.USER_NAME] ?: "",
            email = prefs[Keys.USER_EMAIL] ?: "",
            role = try {
                UserRole.valueOf(prefs[Keys.USER_ROLE] ?: "FAMILY_MEMBER")
            } catch (e: Exception) {
                UserRole.FAMILY_MEMBER
            },
            profilePhoto = prefs[Keys.PROFILE_PHOTO],
            birthday = prefs[Keys.BIRTHDAY],
            city = prefs[Keys.CITY],
            state = prefs[Keys.STATE],
            timezone = prefs[Keys.TIMEZONE],
            calendarUrl = prefs[Keys.CALENDAR_URL],
            calendarReviewer = prefs[Keys.CALENDAR_REVIEWER] ?: false,
            eventReminders = prefs[Keys.EVENT_REMINDERS] ?: false
        )
    }

    /**
     * Check if user is a calendar reviewer
     */
    suspend fun isCalendarReviewer(): Boolean {
        val prefs = context.userDataStore.data.first()
        return prefs[Keys.CALENDAR_REVIEWER] ?: false
    }

    /**
     * Get/set biometric preference
     */
    suspend fun isBiometricEnabled(): Boolean {
        val prefs = context.userDataStore.data.first()
        return prefs[Keys.BIOMETRIC_ENABLED] ?: false
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    /**
     * Store credentials for biometric auto-login
     */
    suspend fun saveCredentials(email: String, password: String) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.STORED_EMAIL] = email
            prefs[Keys.STORED_PASSWORD] = password
        }
    }

    /**
     * Get stored credentials for biometric auto-login
     */
    suspend fun getStoredCredentials(): Pair<String, String>? {
        val prefs = context.userDataStore.data.first()
        val email = prefs[Keys.STORED_EMAIL] ?: return null
        val password = prefs[Keys.STORED_PASSWORD] ?: return null
        return Pair(email, password)
    }

    /**
     * Check if we have stored credentials
     */
    suspend fun hasStoredCredentials(): Boolean {
        val prefs = context.userDataStore.data.first()
        return prefs[Keys.STORED_EMAIL] != null && prefs[Keys.STORED_PASSWORD] != null
    }

    /**
     * Clear stored credentials (when user explicitly disables biometric)
     */
    suspend fun clearCredentials() {
        context.userDataStore.edit { prefs ->
            prefs.remove(Keys.STORED_EMAIL)
            prefs.remove(Keys.STORED_PASSWORD)
        }
    }

    /**
     * Clear all user data (logout)
     * Note: Preserves biometric preference and stored credentials for auto-login
     */
    suspend fun clearUser() {
        context.userDataStore.edit { prefs ->
            // Preserve biometric setting and credentials for auto-login
            val biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false
            val storedEmail = prefs[Keys.STORED_EMAIL]
            val storedPassword = prefs[Keys.STORED_PASSWORD]
            
            prefs.clear()
            
            prefs[Keys.BIOMETRIC_ENABLED] = biometricEnabled
            storedEmail?.let { prefs[Keys.STORED_EMAIL] = it }
            storedPassword?.let { prefs[Keys.STORED_PASSWORD] = it }
        }
    }
}
