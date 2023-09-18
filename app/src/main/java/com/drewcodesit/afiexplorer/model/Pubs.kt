package com.drewcodesit.afiexplorer.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by drewstephens
 */

class Pubs(

    @SerializedName("PubID")
    var PubID: Int,

    @SerializedName("Number")
    var Number: String? = "",

    @SerializedName("Title")
    var Title: String? = "",

    @SerializedName("LastAction")
    var LastAction: String? = "",

    @SerializedName("CertDate")
    var CertDate: String? = "",

    @SerializedName("DocumentUrl")
    var DocumentUrl: String? = "",

    @SerializedName("RescindOrg")
    var RescindOrg: String? = "",

    @SerializedName("GMDate")
    var GMDate: String? = ""

) {
    // Gets Certified Current Date
    @JvmName("getCertDate1")
    fun getCertDate(): String? {
        val rawDate = CertDate
        val temp = rawDate?.substring(6, rawDate.length - 2)
        val timeInMillis = temp?.toLong()
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeInMillis!!))
    }

    // Gets Guidance Memorandum date
    @JvmName("getGMDate1")
    fun getGMDate(): String? {
        val rawDate2 = GMDate
        val temp2 = rawDate2?.substring(6, rawDate2.length - 2)
        val timeInMillis2 = temp2?.toLong() ?: 0L // Provide a default value (0L) if temp2 is null
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeInMillis2))
    }
}
