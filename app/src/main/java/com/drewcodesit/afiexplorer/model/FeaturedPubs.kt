package com.drewcodesit.afiexplorer.model

import com.google.gson.annotations.SerializedName

data class FeaturedPubs (
    @SerializedName("PubID")
    var PubID: Int,

    @SerializedName("Number")
    var Number: String? = "",

    @SerializedName("Title")
    var Title: String? = "",

    @SerializedName("DocumentUrl")
    var DocumentUrl: String? = "",

    @SerializedName("RescindOrg")
    var RescindOrg: String? = "",
)