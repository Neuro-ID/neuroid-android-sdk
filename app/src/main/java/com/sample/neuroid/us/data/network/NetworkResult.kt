package com.sample.neuroid.us.data.network

class NetworkResult<T>(val result: T? = null, val networkError: NetworkError? = null) {
    val isError: Boolean
        get() = networkError != null

    val requiredResult: T
        get() = result!!

    val requiredError: NetworkError
        get() = networkError!!
}

enum class NetworkErrorType {
    CONNECTION_ERROR,
    API_ERROR,
    UNKNOWN_ERROR,
    NO_CONTENT_ERROR,
}


data class Error(
    val code: String,
    val message: String,
)

data class Validation(
    val type: List<Error>?,
    val provider: List<Error>?,
)

data class NetworkError(
    var type: NetworkErrorType,
    var rawError: String? = null,
    var errorCode: String? = null,
) {
    val ensureErrorMessage: String
        get() {
            rawError?.let {
                return it
            }
            return type.name
        }
}