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
    var RescindOrg: String? = ""

) {
    @JvmName("getCertDate1")
    fun getCertDate(): String? {
        val rawDate = CertDate
        val temp = rawDate?.substring(6, rawDate.length - 2)
        val timeInMillis = temp?.toLong()
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeInMillis!!))

    }
}
