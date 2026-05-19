package com.devsrimanth.mlkit.testCases.utils

import com.devsrimanth.mlkit.testCases.model.User
import retrofit2.http.GET

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>
}