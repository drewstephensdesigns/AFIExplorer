package com.drewcodesit.afiexplorer.interfaces

import com.drewcodesit.afiexplorer.models.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("{path}")
    suspend fun getPubs(@Path("path", encoded = true) path: String): ApiResponse
}