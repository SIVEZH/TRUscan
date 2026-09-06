package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.complaint.ConfirmComplaintScreen
import com.example.ui.viewmodel.ComplaintViewModel

@Composable
fun ComplaintReviewScreen(
    scanId: String,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    viewModel: ComplaintViewModel = viewModel()
) {
    ConfirmComplaintScreen(
        viewModel = viewModel,
        onBackClick = onBackClick,
        onEditUserDetails = onBackClick,
        onEditProductDetails = onBackClick,
        onRetakeLivePhoto = onBackClick,
        onSubmitSuccess = { _ -> onContinueClick() }
    )
}
