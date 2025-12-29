package com.otheruncle.memorydisplay.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.otheruncle.memorydisplay.data.api.ApiService
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.api.safeApiCall
import com.otheruncle.memorydisplay.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) {

    /**
     * Get current user's profile from server
     */
    suspend fun getProfile(): NetworkResult<User> {
        val result = safeApiCall { apiService.getProfile() }

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.user != null) {
                    // Update local cache
                    userPreferences.saveUser(response.user)
                    NetworkResult.Success(response.user)
                } else {
                    NetworkResult.Error(response.error ?: "Failed to load profile")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(
        birthday: String? = null,
        city: String? = null,
        state: String? = null,
        timezone: String? = null,
        calendarUrl: String? = null,
        eventReminders: Boolean? = null
    ): NetworkResult<User> {
        val result = safeApiCall {
            apiService.updateProfile(
                ProfileUpdateRequest(
                    birthday = birthday,
                    city = city,
                    state = state,
                    timezone = timezone,
                    calendarUrl = calendarUrl,
                    eventReminders = eventReminders
                )
            )
        }

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.user != null) {
                    userPreferences.saveUser(response.user)
                    NetworkResult.Success(response.user)
                } else {
                    NetworkResult.Error(response.error ?: "Failed to update profile")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Upload profile photo
     */
    suspend fun uploadProfilePhoto(imageUri: Uri): NetworkResult<String> {
        val file = uriToFile(imageUri) ?: return NetworkResult.Error("Failed to process image")

        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("photo", file.name, requestBody)

        val result = safeApiCall { apiService.uploadProfilePhoto(part) }

        // Clean up temp file
        file.delete()

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.profilePhoto != null) {
                    NetworkResult.Success(response.profilePhoto)
                } else {
                    NetworkResult.Error(response.error ?: "Failed to upload photo")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Change password
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): NetworkResult<Unit> {
        val result = safeApiCall {
            apiService.changePassword(
                PasswordChangeRequest(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            )
        }

        return when (result) {
            is NetworkResult.Success -> {
                if (result.data.success) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(result.data.error ?: "Failed to change password")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Update family status (location/note)
     */
    suspend fun updateStatus(location: String, note: String? = null): NetworkResult<String> {
        val result = safeApiCall {
            apiService.updateFamilyStatus(
                FamilyStatusUpdateRequest(
                    location = location,
                    note = note
                )
            )
        }

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success) {
                    NetworkResult.Success(response.expiresAt ?: "")
                } else {
                    NetworkResult.Error(response.error ?: "Failed to update status")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Helper to convert Uri to File for upload.
     * Handles EXIF orientation to ensure image is properly rotated.
     */
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            // First, copy to a temp file to read EXIF
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            // Read EXIF orientation
            val exifOrientation = try {
                val exif = ExifInterface(tempFile.absolutePath)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            
            // If no rotation needed, return the temp file as-is
            if (exifOrientation == ExifInterface.ORIENTATION_NORMAL || 
                exifOrientation == ExifInterface.ORIENTATION_UNDEFINED) {
                return tempFile
            }
            
            // Decode, rotate, and re-save the image
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            if (bitmap == null) {
                return tempFile // Fall back to original if decode fails
            }
            
            val matrix = Matrix()
            when (exifOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1f, 1f)
                }
            }
            
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            
            // Save rotated bitmap to a new file
            val rotatedFile = File.createTempFile("upload_rotated_", ".jpg", context.cacheDir)
            FileOutputStream(rotatedFile).use { output ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            
            // Clean up
            bitmap.recycle()
            if (rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            tempFile.delete()
            
            rotatedFile
        } catch (e: Exception) {
            null
        }
    }
}
