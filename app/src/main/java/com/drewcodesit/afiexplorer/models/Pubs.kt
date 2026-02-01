package com.drewcodesit.afiexplorer.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApiResponse(
    @SerializedName("publications")
    val publications: List<Pubs> = emptyList()
)

data class Pubs(
    @SerializedName("PubID") var pubID: Int,
    @SerializedName("Number") var pubNumber: String? = "",
    @SerializedName("Title") var pubTitle: String? = "",
    @SerializedName("LastAction") var pubLastAction: String? = "",
    @SerializedName("CertDate") var pubCertDate: String? = "",
    @SerializedName("DocumentUrl") var pubDocumentUrl: String? = "",
    @SerializedName("RescindOrg") var pubRescindOrg: String? = "",
    @SerializedName("RescindLevel") var pubRescindLevel: String? = "",
    @SerializedName("GMDate") var pubGMDate: String? = "",
) {
    companion object{
        private val displayFormatter = SimpleDateFormat("yyyy MMM dd", Locale.US)
        private val gmFormatter = SimpleDateFormat("yyyy MMM dd", Locale.US)
    }

    // Extract the millis from /Date(____)/ safely
    private fun parseDotNetMillis(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0

        return runCatching {
            raw.substringAfter("/Date(")
                .substringBefore(")/")
                .toLong()
        }.getOrElse { 0 }
    }

    // Certified date in millis (used for sorting)
    fun certDateMillis(): Long { return parseDotNetMillis(pubCertDate) }

    //GM date in millis
    fun gmDateMillis(): Long { return parseDotNetMillis(pubGMDate) }

    //Formatted Certified Date for UI
    fun getCertDate(): String {
        val millis = certDateMillis()
        if (millis == 0L) return "N/A"
        return displayFormatter.format(Date(millis))
    }

    //Formatted Guidance Memorandum Date for UI
    fun getGMDate(): String {
        val millis = gmDateMillis()
        if (millis == 0L) return "N/A"
        return gmFormatter.format(Date(millis))
    }
}