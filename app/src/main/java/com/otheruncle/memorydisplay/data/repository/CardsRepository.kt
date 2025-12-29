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
class CardsRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {

    /**
     * Get user's cards and cards they can edit
     */
    suspend fun getMyCards(): NetworkResult<List<Card>> {
        val result = safeApiCall {
            apiService.getCards(view = "mine", includeEditable = "1")
        }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.cards)
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Get cards currently displayed on the panel (excludes expired cards)
     */
    suspend fun getDisplayCards(): NetworkResult<List<Card>> {
        val result = safeApiCall {
            apiService.getCards(view = "display")
        }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.cards)
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Get single card by ID
     */
    suspend fun getCard(id: Int): NetworkResult<Card> {
        return safeApiCall { apiService.getCard(id) }
    }

    /**
     * Create a new card
     */
    suspend fun createCard(
        cardType: CardType,
        data: CardDataRequest,
        allowOthersEdit: Boolean = false
    ): NetworkResult<Card> {
        val typeString = when (cardType) {
            CardType.PROFESSIONAL_APPOINTMENT -> "professional-appointment"
            CardType.FAMILY_EVENT -> "family-event"
            CardType.OTHER_EVENT -> "other-event"
            CardType.FAMILY_TRIP -> "family-trip"
            CardType.REMINDER -> "reminder"
            CardType.FAMILY_MESSAGE -> "family-message"
            CardType.PENDING -> "pending"
        }

        val result = safeApiCall {
            apiService.createCard(
                CardCreateRequest(
                    cardType = typeString,
                    allowOthersEdit = allowOthersEdit,
                    data = data
                )
            )
        }

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.card != null) {
                    NetworkResult.Success(response.card)
                } else {
                    NetworkResult.Error(response.error ?: "Failed to create card")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Update an existing card
     */
    suspend fun updateCard(
        id: Int,
        data: CardDataRequest,
        allowOthersEdit: Boolean? = null
    ): NetworkResult<Card> {
        val result = safeApiCall {
            apiService.updateCard(
                id,
                CardUpdateRequest(
                    allowOthersEdit = allowOthersEdit,
                    data = data
                )
            )
        }

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.card != null) {
                    NetworkResult.Success(response.card)
                } else {
                    NetworkResult.Error(response.error ?: "Failed to update card")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Delete a card
     */
    suspend fun deleteCard(id: Int): NetworkResult<Unit> {
        val result = safeApiCall { apiService.deleteCard(id) }

        return when (result) {
            is NetworkResult.Success -> {
                if (result.data.success) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(result.data.error ?: "Failed to delete card")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Upload image for a card
     */
    suspend fun uploadCardImage(cardId: Int, imageUri: Uri): NetworkResult<String> {
        val file = uriToFile(imageUri) ?: return NetworkResult.Error("Failed to process image")

        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", file.name, requestBody)

        val result = safeApiCall { apiService.uploadCardImage(cardId, part) }

        // Clean up temp file
        file.delete()

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.imagePath != null) {
                    NetworkResult.Success(response.imagePath)
                } else {
                    NetworkResult.Error(response.error ?: "Failed to upload image")
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
