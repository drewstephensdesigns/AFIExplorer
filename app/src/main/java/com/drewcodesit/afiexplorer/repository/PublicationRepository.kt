/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer.repository

import android.util.Log
import com.drewcodesit.afiexplorer.interfaces.ApiService
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.objects.PublicationEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PublicationRepository {

    // Singleton-like initialization of Retrofit so it's built only once
    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Fetches publications from all network paths concurrently.
     * Returns a flattened, unified list of Pubs.
     */
    suspend fun fetchAllPublications(): List<Pubs> = withContext(Dispatchers.IO) {
        try {
            PublicationEndpoints.ALL_PATHS.map { path ->
                async {
                    try {
                        apiService.getPubs(path).publications
                    } catch (e: Exception) {
                        Log.e("PublicationRepository", "Failed to fetch path: $path", e)
                        emptyList() // Fallback for a single failing endpoint
                    }
                }
            }.awaitAll().flatten()
        } catch (e: Exception) {
            Log.e("PublicationRepository", "Critical error fetching publications", e)
            emptyList()
        }
    }
}