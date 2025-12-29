package com.otheruncle.memorydisplay.data.api

/**
 * Wrapper class for API responses that handles success/error states
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw Exception(message)
        is Loading -> throw IllegalStateException("Result is still loading")
    }

    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String, Int?) -> Unit): NetworkResult<T> {
        if (this is Error) action(message, code)
        return this
    }

    inline fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    companion object {
        fun <T> success(data: T): NetworkResult<T> = Success(data)
        fun error(message: String, code: Int? = null): NetworkResult<Nothing> = Error(message, code)
        fun loading(): NetworkResult<Nothing> = Loading
    }
}

/**
 * Error codes for programmatic handling
 */
object ErrorCodes {
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val VALIDATION_ERROR = 422
    const val SERVER_ERROR = 500
    const val NETWORK_ERROR = -1
    const val TIMEOUT_ERROR = -2
    const val UNKNOWN_ERROR = -3
}

/**
 * Context for API calls to provide appropriate error messages
 */
enum class ApiContext {
    LOGIN,           // Login attempts - 401 means bad credentials
    AUTH,            // Other auth operations (forgot password, setup, etc.)
    GENERAL          // Regular API calls - 401 means session expired
}

/**
 * Extension to convert Retrofit Response to NetworkResult with context-aware error messages
 */
suspend fun <T> safeApiCall(
    context: ApiContext = ApiContext.GENERAL,
    apiCall: suspend () -> retrofit2.Response<T>
): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Error("Empty response body", response.code())
            }
        } else {
            val errorMessage = getErrorMessage(response.code(), context)
            NetworkResult.Error(errorMessage, response.code())
        }
    } catch (e: java.net.UnknownHostException) {
        NetworkResult.Error("No network connection. Check your internet and try again.", ErrorCodes.NETWORK_ERROR)
    } catch (e: java.net.SocketTimeoutException) {
        NetworkResult.Error("Connection timed out. Please try again.", ErrorCodes.TIMEOUT_ERROR)
    } catch (e: java.net.ConnectException) {
        NetworkResult.Error("Unable to connect to server. Please try again.", ErrorCodes.NETWORK_ERROR)
    } catch (e: java.io.IOException) {
        NetworkResult.Error("Network error. Check your connection and try again.", ErrorCodes.NETWORK_ERROR)
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "An unexpected error occurred.", ErrorCodes.UNKNOWN_ERROR)
    }
}

/**
 * Get appropriate error message based on HTTP code and context
 */
private fun getErrorMessage(code: Int, context: ApiContext): String {
    return when (code) {
        401 -> when (context) {
            ApiContext.LOGIN -> "Invalid email or password. Please try again."
            ApiContext.AUTH -> "Authentication failed. Please try again."
            ApiContext.GENERAL -> "Your session has expired. Please log in again."
        }
        403 -> "You don't have permission to perform this action."
        404 -> "The requested item could not be found."
        422 -> "Please check your input and try again."
        500 -> "Server error. Please try again later."
        502, 503, 504 -> "Server is temporarily unavailable. Please try again later."
        else -> "Something went wrong. Please try again."
    }
}
