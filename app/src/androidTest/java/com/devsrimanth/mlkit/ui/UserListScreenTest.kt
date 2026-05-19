package com.devsrimanth.mlkit.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.devsrimanth.mlkit.testCases.model.User
import com.devsrimanth.mlkit.testCases.ui.StoreViewModel
import com.devsrimanth.mlkit.testCases.ui.UiState
import com.devsrimanth.mlkit.testCases.ui.UserListScreen
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: StoreViewModel

    // fake users — reuse across all Success state tests
    val fakeUsers = listOf(
        User(id = 1, username = "John Doe", email = "john.doe@test.com", password = "pass123"),
        User(id = 2, username = "Ananya Sharma", email = "ananya@test.com", password = "pass456"),
        User(id = 3, username = "Kiran Kumar", email = "kiran@test.com", password = "pass789")
    )

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
    }



    @Test
    fun loading_indicator_is_displayed_when_state_is_loading() {
        every { viewModel.uiState } returns MutableStateFlow(UiState.Loading)

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertIsDisplayed()
    }

    // Scenario  : Loading state is showing
    // Expected  : user_list must NOT exist on screen at all
    @Test
    fun user_list_does_not_exist_when_state_is_loading() {
        every { viewModel.uiState } returns MutableStateFlow(UiState.Loading)

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        // assertDoesNotExist checks the composable was never even rendered
        composeTestRule
            .onNodeWithTag("user_list")
            .assertDoesNotExist()
    }

    // Scenario  : Loading state is showing
    // Expected  : error_message must NOT exist on screen
    @Test
    fun error_message_does_not_exist_when_state_is_loading() {
        every { viewModel.uiState } returns MutableStateFlow(UiState.Loading)

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithTag("error_message")
            .assertDoesNotExist()
    }



    // Scenario  : Repository returned users, ViewModel emits Success
    // Expected  : user_list LazyColumn is visible on screen
    @Test
    fun user_list_is_displayed_when_state_is_success() {
        every { viewModel.uiState } returns MutableStateFlow(UiState.Success(fakeUsers))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithTag("user_list")
            .assertIsDisplayed()
    }

    // Scenario  : Success state with users
    // Expected  : loading_indicator must NOT exist — spinner gone after data loads
    @Test
    fun loading_indicator_does_not_exist_when_state_is_success() {
        every { viewModel.uiState } returns MutableStateFlow(UiState.Success(fakeUsers))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertDoesNotExist()
    }

    // Scenario  : Success state with 3 users
    // Expected  : each user name is visible on screen by testTag
    @Test
    fun user_names_are_displayed_when_state_is_success() {
        every { viewModel.uiState } returns MutableStateFlow(UiState.Success(fakeUsers))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("user_name_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_name_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_name_3").assertIsDisplayed()
    }

    // Test 7
    // Scenario  : Success state with users
    // Expected  : actual user name text is correct on screen
    // onNodeWithText finds node by its text content — not testTag
    @Test
    fun user_name_text_is_correct_when_state_is_success() {
        every { viewModel.uiState } returns MutableStateFlow(UiState.Success(fakeUsers))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        // onNodeWithText — finds composable by its actual text content
        composeTestRule.onNodeWithText("Ravi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ananya").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kiran").assertIsDisplayed()
    }

    // Scenario  : Success state with empty list
    // Expected  : user_list still exists — empty list is still Success not Error
    @Test
    fun user_list_exists_when_success_state_has_empty_list() {
        every { viewModel.uiState } returns MutableStateFlow(UiState.Success(emptyList()))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        // LazyColumn renders even with empty list — it just shows nothing inside
        composeTestRule
            .onNodeWithTag("user_list")
            .assertIsDisplayed()
    }


    // Scenario  : Repository failed, ViewModel emits Error
    // Expected  : error_message Text is visible on screen
    @Test
    fun error_message_is_displayed_when_state_is_error() {
        every { viewModel.uiState } returns
                MutableStateFlow(UiState.Error("Network error"))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithTag("error_message")
            .assertIsDisplayed()
    }

    // Scenario  : Error state is showing
    // Expected  : the exact error message text is correct on screen
    @Test
    fun error_message_text_is_correct_when_state_is_error() {
        every { viewModel.uiState } returns
                MutableStateFlow(UiState.Error("Network error"))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        // onNodeWithText — verify the actual message text shown to user
        composeTestRule
            .onNodeWithText("Network error")
            .assertIsDisplayed()
    }

    // Scenario  : Error state is showing
    // Expected  : retry_button is visible so user can try again
    @Test
    fun retry_button_is_displayed_when_state_is_error() {
        every { viewModel.uiState } returns
                MutableStateFlow(UiState.Error("Network error"))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithTag("retry_button")
            .assertIsDisplayed()
    }

    // Scenario  : Error state is showing
    // Expected  : loading_indicator must NOT exist — no spinner during error
    @Test
    fun loading_indicator_does_not_exist_when_state_is_error() {
        every { viewModel.uiState } returns
                MutableStateFlow(UiState.Error("Network error"))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertDoesNotExist()
    }

    // Scenario  : Error state is showing
    // Expected  : user_list must NOT exist — no list during error
    @Test
    fun user_list_does_not_exist_when_state_is_error() {
        every { viewModel.uiState } returns
                MutableStateFlow(UiState.Error("Network error"))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithTag("user_list")
            .assertDoesNotExist()
    }

    // Scenario  : User sees error screen and taps Retry button
    // Expected  : viewModel.getUsers() is called exactly once
    @Test
    fun clicking_retry_button_calls_getUsers_on_viewmodel() {
        every { viewModel.uiState } returns
                MutableStateFlow(UiState.Error("Network error"))

        composeTestRule.setContent {
            UserListScreen(viewModel = viewModel)
        }

        // performClick — simulates real user tapping the button
        composeTestRule
            .onNodeWithTag("retry_button")
            .performClick()

        verify(exactly = 1) { viewModel.getUsers() }
    }
}