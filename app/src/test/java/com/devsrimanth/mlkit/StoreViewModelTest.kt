package com.devsrimanth.mlkit

import com.devsrimanth.mlkit.testCases.model.User
import com.devsrimanth.mlkit.testCases.repository.StoreRepository
import com.devsrimanth.mlkit.testCases.ui.StoreViewModel
import com.devsrimanth.mlkit.testCases.ui.UiState
import com.devsrimanth.mlkit.testCases.utils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockRepository = mockk<StoreRepository>()

    // The REAL ViewModel we are testing
    private lateinit var viewModel: StoreViewModel

    // @Before runs before every single @Test
    // IMPORTANT — init block inside ViewModel calls getUsers() immediately
    // So we must tell the mock what to return BEFORE creating ViewModel
    // Otherwise mockk throws "no answer found" error on init
    @Before
    fun setup() {
        // Tell mock to return empty success by default for the init block call
        coEvery { mockRepository.getUsers() } returns Result.success(emptyList())

        // Now safe to create ViewModel — init block runs, mock handles it
        viewModel = StoreViewModel(mockRepository)
    }


    /**
     * Test Case 1: Initial State
     * Scenario  : ViewModel is created
     * Expected  : StateFlow's initial value is Loading — user sees spinner immediately
     */
    @Test
    fun `initial state is Loading when ViewModel is created`() = runTest {

        coEvery { mockRepository.getUsers() } returns Result.success(emptyList())
        val freshViewModel = StoreViewModel(mockRepository)

        // ASSERT — first value in StateFlow must be Loading
        // We check .value directly — no Turbine needed for initial state
        assertThat(freshViewModel.uiState.value is UiState.Loading).isTrue()
    }


    /**
     * Test Case 2: Loading → Success
     * Scenario  : Repository returns success with user list
     * Expected  : StateFlow emits Loading first then Success with correct data
     */
    @Test
    fun `getUsers emits Loading then Success when repository returns data`() = runTest {

        // ARRANGE
        val fakeUsers = listOf(
            User(id = 1, username = "Ravi", email = "ravi@test.com", password = "pass123"),
            User(id = 2, username = "Ananya", email = "ananya@test.com", password = "pass456")
        )
        coEvery { mockRepository.getUsers() } returns Result.success(fakeUsers)

        viewModel.getUsers()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state is UiState.Success).isTrue()

        val data = (state as UiState.Success<List<User>>).data
        assertThat(data).isEqualTo(fakeUsers)
        assertThat(data).hasSize(2)
        assertThat(data.first().username).isEqualTo("Ravi")
    }


    /**
     * Test Case 3: Loading → Error
     * Scenario  : Repository returns failure with exception message
     * Expected  : StateFlow emits Loading first then Error with correct message
     */
    @Test
    fun `getUsers emits Loading then Error when repository fails`() = runTest {

        coEvery { mockRepository.getUsers() } returns
                Result.failure(Exception("No internet connection"))

        viewModel.getUsers()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state is UiState.Error).isTrue()
        assertThat((state as UiState.Error).message).isEqualTo("No internet connection")
    }


    /**
     * Test Case 4: Error with null message
     * Scenario  : Repository returns failure with exception that has null message
     * Expected  : StateFlow emits Error with "Unknown error" message — never null
     */
    @Test
    fun `getUsers shows Unknown error when exception message is null`() = runTest {

        // ARRANGE — exception with no message
        coEvery { mockRepository.getUsers() } returns
                Result.failure(Exception())  // null message

        viewModel.getUsers()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state is UiState.Error).isTrue()
        assertThat((state as UiState.Error).message).isEqualTo("Unknown error")
    }


    /**
     * Test Case 5: Success with empty list
     * Scenario  : Repository returns success but with empty user list
     * Expected  : StateFlow emits Success with empty list — no crash, just no data
     */
    @Test
    fun `getUsers emits Success with empty list when repository returns no data`() = runTest {

        // ARRANGE
        coEvery { mockRepository.getUsers() } returns Result.success(emptyList())

        viewModel.getUsers()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state is UiState.Success).isTrue()
        assertThat((state as UiState.Success<List<User>>).data).isEmpty()
    }


    /**
     * Test Case 6: Repository called on init
     * Scenario  : ViewModel is created
     */
    @Test
    fun `repository getUsers is called automatically on ViewModel init`() = runTest {

        coVerify(atLeast = 1) { mockRepository.getUsers() }
    }


    /**
     * Test Case 7: Repository called on manual getUsers
     * Scenario  : User taps "Retry" button that calls getUsers() again
     * Expected  : Repository's getUsers() is called again — allows retrying after error
     */
    @Test
    fun `repository getUsers is called again when getUsers is invoked manually`() = runTest {

        coEvery { mockRepository.getUsers() } returns Result.success(emptyList())

        viewModel.getUsers()
        advanceUntilIdle()

        coVerify(atLeast = 2) { mockRepository.getUsers() }
    }


    /**
     * Test Case 8: Loading state resets on retry
     * Scenario  : User calls getUsers again after an error
     * Expected  : StateFlow emits Loading again before fetching new data — shows spinner on retry
     */
    @Test
    fun `getUsers resets state to Loading before fetching new data`() = runTest {

        val fakeUsers = listOf(
            User(id = 1, username = "Ravi", email = "ravi@test.com", password = "pass123")
        )
        coEvery { mockRepository.getUsers() } coAnswers {
            delay(100)
            Result.success(fakeUsers)
        }

        viewModel.getUsers()
        runCurrent()
        assertThat(viewModel.uiState.value is UiState.Loading).isTrue()

        advanceUntilIdle()
        assertThat(viewModel.uiState.value is UiState.Success).isTrue()
    }


    /**
     * Test Case 9: Success state contains correct number of users
     * Scenario  : Repository returns success with a list of 5 users
     * Expected  : StateFlow emits Success with list of 5 users — data integrity check
     */
    @Test
    fun `getUsers Success state contains correct number of users`() = runTest {

        // ARRANGE — 5 users
        val fakeUsers = (1..5).map {
            User(id = it, username = "User$it", email = "user$it@test.com", password = "pass$it")
        }
        coEvery { mockRepository.getUsers() } returns Result.success(fakeUsers)

        viewModel.getUsers()
        advanceUntilIdle()

        val success = viewModel.uiState.value as UiState.Success<List<User>>
        assertThat(success.data).hasSize(5)
        assertThat(success.data.map { it.username })
            .containsExactly("User1", "User2", "User3", "User4", "User5")
            .inOrder()
    }
}