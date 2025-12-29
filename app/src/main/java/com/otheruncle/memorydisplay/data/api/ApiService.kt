package com.otheruncle.memorydisplay.data.api

import com.otheruncle.memorydisplay.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Memory Support Display API service interface
 * Base URL: https://otheruncle.com/memory_display/api/
 */
interface ApiService {

    // ==================== Authentication ====================

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<SuccessResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<SuccessResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<SuccessResponse>

    @POST("auth/setup")
    suspend fun setup(@Body request: SetupRequest): Response<LoginResponse>

    // ==================== Profile ====================

    @GET("profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<ProfileResponse>

    @Multipart
    @POST("profile/photo")
    suspend fun uploadProfilePhoto(@Part photo: MultipartBody.Part): Response<PhotoUploadResponse>

    @PUT("profile/password")
    suspend fun changePassword(@Body request: PasswordChangeRequest): Response<SuccessResponse>

    // ==================== Cards ====================

    /**
     * Get cards for display or user's cards
     * @param view 'display' for main display, 'mine' for user's cards
     * @param includeEditable '1' to include cards user can edit
     */
    @GET("cards")
    suspend fun getCards(
        @Query("view") view: String? = null,
        @Query("include_editable") includeEditable: String? = null
    ): Response<CardsResponse>

    @GET("cards/{id}")
    suspend fun getCard(@Path("id") id: Int): Response<Card>

    @POST("cards")
    suspend fun createCard(@Body request: CardCreateRequest): Response<CardResponse>

    @PUT("cards/{id}")
    suspend fun updateCard(
        @Path("id") id: Int,
        @Body request: CardUpdateRequest
    ): Response<CardResponse>

    @DELETE("cards/{id}")
    suspend fun deleteCard(@Path("id") id: Int): Response<SuccessResponse>

    @Multipart
    @POST("cards/{id}/image")
    suspend fun uploadCardImage(
        @Path("id") id: Int,
        @Part image: MultipartBody.Part
    ): Response<PhotoUploadResponse>

    // ==================== Calendar Review ====================

    @GET("calendar/pending")
    suspend fun getPendingEvents(): Response<PendingEventsResponse>

    @POST("calendar/review/{id}")
    suspend fun reviewEvent(
        @Path("id") id: Int,
        @Body request: ReviewRequest
    ): Response<CardResponse>

    // ==================== Family Status ====================

    @GET("family/status")
    suspend fun getFamilyStatus(): Response<FamilyStatusResponse>

    @PUT("family/status")
    suspend fun updateFamilyStatus(@Body request: FamilyStatusUpdateRequest): Response<FamilyStatusUpdateResponse>

    // ==================== Holidays ====================

    @GET("holidays")
    suspend fun getHolidays(): Response<HolidaysResponse>

    @POST("holidays")
    suspend fun createHoliday(@Body request: HolidayRequest): Response<HolidayResponse>

    @PUT("holidays/{id}")
    suspend fun updateHoliday(
        @Path("id") id: Int,
        @Body request: HolidayRequest
    ): Response<HolidayResponse>

    @DELETE("holidays/{id}")
    suspend fun deleteHoliday(@Path("id") id: Int): Response<SuccessResponse>

    // ==================== Family Members ====================

    @GET("users/family")
    suspend fun getFamilyMembers(): Response<FamilyMembersResponse>

    // ==================== Push Notifications ====================

    @POST("push/register")
    suspend fun registerPushToken(@Body request: PushRegisterRequest): Response<SuccessResponse>

    @HTTP(method = "DELETE", path = "push/unregister", hasBody = true)
    suspend fun unregisterPushToken(@Body request: PushUnregisterRequest): Response<SuccessResponse>
}
