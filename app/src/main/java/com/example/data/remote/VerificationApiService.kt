package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class SendEmailOtpRequest(val email: String)

@JsonClass(generateAdapter = true)
data class SendPhoneOtpRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class VerifyEmailOtpRequest(val email: String, val otp: String)

@JsonClass(generateAdapter = true)
data class VerifyPhoneOtpRequest(val phone: String, val otp: String)

@JsonClass(generateAdapter = true)
data class OtpResponse(val success: Boolean, val message: String?, val verified: Boolean?)

interface VerificationApiService {
    @POST("auth/email/send-otp")
    suspend fun sendEmailOtp(@Body request: SendEmailOtpRequest): OtpResponse

    @POST("auth/email/verify-otp")
    suspend fun verifyEmailOtp(@Body request: VerifyEmailOtpRequest): OtpResponse

    @POST("auth/phone/send-otp")
    suspend fun sendPhoneOtp(@Body request: SendPhoneOtpRequest): OtpResponse

    @POST("auth/phone/verify-otp")
    suspend fun verifyPhoneOtp(@Body request: VerifyPhoneOtpRequest): OtpResponse
}
