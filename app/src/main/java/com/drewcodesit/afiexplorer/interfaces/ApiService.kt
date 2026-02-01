package com.drewcodesit.afiexplorer.interfaces

import com.drewcodesit.afiexplorer.models.ApiResponse
import retrofit2.http.GET

interface ApiService {

    @GET("publications")
    suspend fun getPubs(): ApiResponse
}