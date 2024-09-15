package com.drewcodesit.afiexplorer.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Pubs(
    @SerializedName("PubID")
    var pubID: Int,

    @SerializedName("Number")
    var pubNumber: String? = "",

    @SerializedName("Title")
    var pubTitle: String? = "",

    @SerializedName("LastAction")
    var pubLastAction: String? = "",

    @SerializedName("CertDate")
    var pubCertDate: String? = "",

    @SerializedName("DocumentUrl")
    var pubDocumentUrl: String? = "",

    @SerializedName("RescindOrg")
    var pubRescindOrg: String? = "",

    @SerializedName("GMDate")
    var pubGMDate: String? = "",
) {
    // Gets Certified Current Date
    @JvmName("getCertDate1")
    fun getCertDate(): String? {
        val rawDate = pubCertDate
        val temp = rawDate?.substring(6, rawDate.length - 2)
        val timeInMillis = temp?.toLong()
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeInMillis!!))
    }

    // Gets Guidance Memorandum date
    @JvmName("getGMDate1")
    fun getGMDate(): String? {
        val rawDate2 = pubGMDate
        val temp2 = rawDate2?.substring(6, rawDate2.length - 2)
        val timeInMillis2 = temp2?.toLong() ?: 0L // Provide a default value (0L) if temp2 is null
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeInMillis2))
    }
}