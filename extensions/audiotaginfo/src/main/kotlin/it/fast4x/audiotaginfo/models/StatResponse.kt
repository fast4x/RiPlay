package it.fast4x.audiotaginfo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatResponse(
    val success: Boolean? = false,
    @SerialName("expiration_date") // Expiration date and time of the API key
    val expirationDate: String,
    @SerialName("queries_count") // Total count of API queries made using this key
    val queriesCount: Int,
    @SerialName("uploaded_duration_sec") // Total audio duration of audio files passed to identification using this key
    val uploadedDurationSec: Int,
    @SerialName("uploaded_size_bytes") // Total size (in bytes) of files passed to identification using this key
    val uploadedSizeBytes: Int,
    @SerialName("credits_spent") // Total amount of credit points spent using this key
    val creditsSpent: Int,
    @SerialName("current_credit_balance") // Current credit balance on account owning this key
    val currentCreditBalance: Int,
    @SerialName("identification_free_sec_remainder") // Remaining budget of free seconds to analyze
    val identificationFreeSecRemainder: Int
)
