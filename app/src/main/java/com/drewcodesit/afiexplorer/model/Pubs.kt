package com.drewcodesit.afiexplorer.model

import com.google.gson.annotations.SerializedName

/**
 * Created by drewstephens
 */

class Pubs(
    private val id: Int,

    @SerializedName("Number")
    var Number: String? = "",

    @SerializedName("Title")
    var Title: String? = "",

    @SerializedName("LastAction")
    var LastAction: String? = "",

    @SerializedName("CertDate")
    var CertDate: String,

    @SerializedName("DocumentUrl")
    var DocumentUrl: String? = "",

    @SerializedName("RescindOrg")
    var RescindOrg: String? = ""

) {
    fun getId(): Int{
        return id
    }
}
