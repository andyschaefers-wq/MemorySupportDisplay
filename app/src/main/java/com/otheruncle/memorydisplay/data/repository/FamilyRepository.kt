package com.otheruncle.memorydisplay.data.repository

import com.otheruncle.memorydisplay.data.api.ApiService
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.api.safeApiCall
import com.otheruncle.memorydisplay.data.model.FamilyMember
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * Get list of family members for driver/attendee selection
     */
    suspend fun getFamilyMembers(): NetworkResult<List<FamilyMember>> {
        val result = safeApiCall { apiService.getFamilyMembers() }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.members)
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}
