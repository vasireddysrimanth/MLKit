package com.devsrimanth.mlkit.testCases.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsrimanth.mlkit.testCases.model.User

@Composable
fun UserListScreen(
    viewModel: StoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is UiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
        }

        is UiState.Success -> LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("user_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.data, key = { it.id }) { user ->
                UserCard(user = user)
            }
        }

        is UiState.Error -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("error_message")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.getUsers() },
                modifier = Modifier.testTag("retry_button")
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun UserCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("user_name_${user.id}")
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}