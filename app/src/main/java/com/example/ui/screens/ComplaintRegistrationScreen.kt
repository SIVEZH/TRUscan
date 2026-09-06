package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.UserEntity
import com.example.ui.screens.complaint.UserDetailsScreen
import com.example.ui.viewmodel.ComplaintViewModel

@Composable
fun ComplaintRegistrationScreen(
    scanId: String,
    user: UserEntity?,
    onBackClick: () -> Unit,
    onReviewComplaint: () -> Unit,
    viewModel: ComplaintViewModel = viewModel()
) {
    UserDetailsScreen(
        viewModel = viewModel,
        onBackClick = onBackClick,
        onContinueClick = onReviewComplaint
    )
}
