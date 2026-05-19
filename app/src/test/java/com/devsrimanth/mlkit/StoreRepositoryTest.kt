package com.devsrimanth.mlkit

import com.devsrimanth.mlkit.testCases.model.User
import com.devsrimanth.mlkit.testCases.repository.StoreRepository
import com.devsrimanth.mlkit.testCases.utils.ApiService
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class StoreRepositoryTest {

    private val mockApi = mockk<ApiService>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: StoreRepository

    @Before
    fun setup() {
        repository = StoreRepository(
            api = mockApi,
            dispatcher = testDispatcher
        )
    }


    /**
     * This test verifies that when the API successfully returns a list of users, the repository's getUsers() method
     * returns a Result.success containing the same list of users, and that the data is correctly mapped and not null.
     */
    @Test
    fun `getUsers returns success with correct user list when api succeeds`() = runTest {
        val fakeData = listOf(
            User(id = 1, username = "John Doe", email = "john.doe@test.com", password = "pass123"),
            User(id = 2, username = "Ananya Sharma", email = "ananya@test.com", password = "pass456"),
            User(id = 3, username = "Kiran Kumar", email = "kiran@test.com", password = "pass789")
        )

        coEvery { mockApi.getUsers() } returns fakeData

        val result = repository.getUsers()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(fakeData)
        assertThat(result.getOrNull()).hasSize(3)
        assertThat(result.getOrNull()?.first()?.username).isEqualTo("John Doe")
    }


    /**
     * This test verifies that when the API throws a generic exception (like a RuntimeException or any unchecked exception),
     * the repository's getUsers() method catches it and returns a Result.failure, rather than
        * letting the exception propagate and crash the app.
     */
    @Test
    fun `getUsers returns failure when api throws generic exception`() = runTest {

        coEvery { mockApi.getUsers() } throws Exception("No internet connection")

        val result = repository.getUsers()

        assertThat(result.isFailure).isTrue()   // must be failure, not success
        assertThat(result.isSuccess).isFalse()  // double confirm it is NOT success
    }

    /**
     * This test verifies that when the API throws a generic exception with a specific error message,
     * the repository's getUsers() method returns a Result.failure with the same error message in
     *
     */
    @Test
    fun `getUsers returns failure with correct error message`() = runTest {

        val errorMessage = "Service temporarily unavailable"
        coEvery { mockApi.getUsers() } throws Exception(errorMessage)

        val result = repository.getUsers()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo(errorMessage)
    }


    /**
     * This test verifies that when the API returns an empty list of users,
     * the repository's getUsers() method
     */
    @Test
    fun `getUsers returns success with empty list when api returns no users`() = runTest {

        coEvery { mockApi.getUsers() } returns emptyList()

        val result = repository.getUsers()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNotNull()
        assertThat(result.getOrNull()).isEmpty()
    }

    /**
     * This test verifies that when we call repository.getUsers(),
     * the underlying API method getUsers() is called exactly once.
     *
     */
    @Test
    fun `getUsers calls api exactly once per invocation`() = runTest {

        // ARRANGE
        coEvery { mockApi.getUsers() } returns emptyList()

        // ACT — call repository once
        repository.getUsers()

        // ASSERT — verify the api was hit exactly 1 time
        coVerify(exactly = 1) { mockApi.getUsers() }
    }

    /**
     * This test verifies that if we call repository.getUsers() multiple times,
     * the underlying API method getUsers() is also called the same number of times.
     */
    @Test
    fun `getUsers calls api twice when invoked twice`() = runTest {

        // ARRANGE
        coEvery { mockApi.getUsers() } returns emptyList()

        // ACT — call repository twice
        repository.getUsers()
        repository.getUsers()

        // ASSERT — api must have been called exactly 2 times
        coVerify(exactly = 2) { mockApi.getUsers() }
    }


    /**
     * This test simulates a scenario where the device has no network connectivity or the server is unreachable,
     * causing an IOException (like SocketTimeoutException or UnknownHostException) to be thrown by Retrofit.
     * The repository should catch this exception and return a Result.failure, rather than letting the exception propagate and crash the app.
     */
    @Test
    fun `getUsers returns failure when IOException is thrown`() = runTest {

        // ARRANGE — simulate no network / socket timeout
        coEvery { mockApi.getUsers() } throws IOException("Connection refused")

        // ACT
        val result = repository.getUsers()

        // ASSERT
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Connection refused")
    }


    /**
     * This test simulates a scenario where the server responds with an HTTP error (like 404 Not Found or 500 Internal Server Error).
     * The repository should catch the HttpException thrown by Retrofit and return a Result.failure, rather than letting the exception propagate.
     */
    @Test
    fun `getUsers returns failure when HttpException is thrown`() = runTest {

        // ARRANGE — simulate a 404 Not Found from server
        val httpException = HttpException(Response.error<List<User>>(404, ResponseBody.create(null, "Not Found")))
        coEvery { mockApi.getUsers() } throws httpException

        // ACT
        val result = repository.getUsers()

        // ASSERT
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(HttpException::class.java)
    }


    /**
     * This test ensures that no matter what exception is thrown by the API, the repository's getUsers() method
     * catches it and returns a Result.failure, rather than letting the exception propagate and crash the app.
     */
    @Test
    fun `getUsers never throws exception — always returns Result`() = runTest {

        coEvery { mockApi.getUsers() } throws RuntimeException("Unexpected crash")

        val result = repository.getUsers()
        assertThat(result.isFailure).isTrue()
    }
}