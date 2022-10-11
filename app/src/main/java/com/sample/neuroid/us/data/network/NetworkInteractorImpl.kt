package com.sample.neuroid.us.data.network

import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.TimeoutException
import javax.inject.Inject

class NetworkInteractorImpl @Inject constructor() : NetworkInteractor {

    override suspend fun <T> safeApiCall(
        dispatcher: CoroutineDispatcher,
        apiCall: suspend () -> T,
    ): NetworkResult<T> = withContext(dispatcher) {
        try {
            val response = apiCall.invoke()
            NetworkResult(result = response)
        } catch (throwable: Throwable) {
            NetworkResult(networkError = createError(throwable))
        }
    }

    private fun createError(throwable: Throwable): NetworkError {
        return when (throwable) {
            is IOException -> {
                NetworkError(
                    NetworkErrorType.CONNECTION_ERROR,
                    throwable.message,
                    NetworkErrorType.CONNECTION_ERROR.name,
                )
            }
            is ConnectException -> {
                NetworkError(
                    NetworkErrorType.CONNECTION_ERROR,
                    throwable.message,
                    NetworkErrorType.CONNECTION_ERROR.name,
                )
            }
            is TimeoutException -> {
                NetworkError(
                    NetworkErrorType.CONNECTION_ERROR,
                    throwable.message,
                    NetworkErrorType.CONNECTION_ERROR.name,
                )
            }
            is HttpException -> {
                val bodyResponse: String? = throwable.response()?.errorBody()?.string()
                NetworkError(
                    NetworkErrorType.API_ERROR,
                    bodyResponse,
                    throwable.code().toString(),
                )
            }
            is JsonDataException -> {
                NetworkError(
                    NetworkErrorType.API_ERROR,
                    throwable.message,
                )
            }
            is NullPointerException -> {
                NetworkError(
                    NetworkErrorType.NO_CONTENT_ERROR,
                    throwable.message,
                )
            }
            else -> {
                NetworkError(
                    NetworkErrorType.UNKNOWN_ERROR,
                    throwable.message,
                )
            }
        }
    }

}