package org.immuni.android.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.lang.Exception
import retrofit2.Response

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    @field:Json(name = "error") val error: Boolean = false,
    @field:Json(name = "message") val message: String?,
    @field:Json(name = "error_code") val errorCode: Int?
) {
    var httpCode: Int? = null
}

/**
 * Convert a Retrofit [Response] in a [ErrorResponse] the app can easily handle.
 */
fun Response<*>.toErrorResponse(): ErrorResponse? {
    val error = try {
        val str = this.errorBody()?.string()
        if (str != null) {
            val moshi = Moshi.Builder().build()
            val jsonAdapter = moshi.adapter(ErrorResponse::class.java)
            jsonAdapter.fromJson(str)
        } else ErrorResponse(
            true,
            "the body is null",
            null
        )
    } catch (e: Exception) {
        ErrorResponse(
            true,
            e.localizedMessage,
            null
        )
    }

    error?.apply {
        httpCode = this@toErrorResponse.code()
    }
    return error
}

/**
 * Convert an [ErrorResponse] in JSON.
 */
fun ErrorResponse.toJson(): String {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ErrorResponse::class.java)
    return jsonAdapter.toJson(this)
}