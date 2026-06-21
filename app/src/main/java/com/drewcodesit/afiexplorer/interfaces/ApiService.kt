/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer.interfaces

import com.drewcodesit.afiexplorer.models.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("{path}")
    suspend fun getPubs(
        @Path("path", encoded = true) path: String
    ): ApiResponse

}