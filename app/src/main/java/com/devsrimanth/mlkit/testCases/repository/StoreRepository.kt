package com.devsrimanth.mlkit.testCases.repository

import com.devsrimanth.mlkit.testCases.utils.ApiService
import com.devsrimanth.mlkit.testCases.model.User
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StoreRepository @Inject constructor(
    private val api: ApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getUsers(): Result<List<User>> =
        withContext(dispatcher) {
            try {
                Result.success(api.getUsers())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}