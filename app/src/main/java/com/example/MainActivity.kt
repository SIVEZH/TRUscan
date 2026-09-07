package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AuthViewModelFactory
import com.example.ui.viewmodel.ScanViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val database = AppDatabase.getDatabase(this)
    val sessionManager = com.example.data.local.SessionManager(this)
    val repository = AuthRepository(database.userDao(), sessionManager)
    val factory = AuthViewModelFactory(repository)

    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        val authViewModel: AuthViewModel = viewModel(factory = factory)
        val scanViewModel: ScanViewModel = viewModel()
        val complaintViewModel: com.example.ui.viewmodel.ComplaintViewModel = viewModel()
        val authState by authViewModel.authState.collectAsState()

        LaunchedEffect(authState) {
            val currentRoute = navController.currentDestination?.route
            if (authState is AuthState.Success) {
                val role = (authState as AuthState.Success).user.role
                val target = if (role == "AUTHORITY") "authority_dashboard" else "user_dashboard"
                if (currentRoute == "selection" || currentRoute == "user_login" || currentRoute == "authority_login" || currentRoute == "user_signup") {
                    navController.navigate(target) {
                        popUpTo("selection") { inclusive = true }
                    }
                }
            } else if (authState is AuthState.Idle) {
                if (currentRoute != null && currentRoute != "selection" && currentRoute != "user_login" && currentRoute != "authority_login" && currentRoute != "user_signup") {
                    navController.navigate("selection") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    navController.navigate("camera")
                } else {
                    Toast.makeText(this@MainActivity, "Camera permission is required to capture product photos.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = "selection",
            modifier = Modifier.padding(innerPadding)
          ) {
            composable("selection") {
              LoginSelectionScreen(
                onUserLoginClick = { navController.navigate("user_login") },
                onAuthorityLoginClick = { navController.navigate("authority_login") }
              )
            }
            composable("user_login") {
              UserLoginScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                  navController.navigate("user_dashboard") {
                    popUpTo("selection") { inclusive = false }
                  }
                },
                onSignUpClick = {
                  navController.navigate("user_signup")
                }
              )
            }
            
            composable("user_signup") {
              UserSignUpScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onSignUpSuccess = {
                  navController.navigate("user_dashboard") {
                    popUpTo("selection") { inclusive = false }
                  }
                },
                onLoginClick = {
                  navController.popBackStack()
                }
              )
            }
            composable("authority_login") {
              AuthorityLoginScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                  navController.navigate("authority_dashboard") {
                    popUpTo("selection") { inclusive = false }
                  }
                }
              )
            }
            composable("user_dashboard") {
              UserDashboardScreen(
                onScanProductClick = { navController.navigate("scan_product") },
                onPreviousScansClick = { navController.navigate("previous_scans") },
                onLogoutClick = {
                  authViewModel.logout()
                }
              )
            }
            composable("scan_product") {
              ScanProductScreen(
                  viewModel = scanViewModel,
                  onBackClick = { navController.popBackStack() },
                  onUseCameraClick = { 
                      if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                          navController.navigate("camera")
                      } else {
                          cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                      }
                  },
                  onContinueClick = {
                      navController.navigate("select_category")
                  }
              )
            }
            composable("select_category") {
                com.example.ui.screens.SelectCategoryScreen(
                    onCategorySelected = { category ->
                        scanViewModel.setCategory(category)
                        navController.navigate("validation_report")
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("validation_report") {
                val productImages = scanViewModel.productImages.collectAsState().value
                val selectedCategory = scanViewModel.selectedCategory.collectAsState().value
                val authState = authViewModel.authState.collectAsState().value
                val userId = (authState as? AuthState.Success)?.user?.userId ?: "unknown"
                if (productImages.isNotEmpty()) {
                    com.example.ui.analysis.ValidationScreen(
                        images = productImages,
                        category = selectedCategory,
                        userId = userId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateHome = { 
                            navController.popBackStack("user_dashboard", inclusive = false)
                        },
                        onRaiseComplaint = { scanId -> navController.navigate("complaint_user_details/$scanId") }
                    )
                }
            }
            composable("complaint_user_details/{scanId}") { backStackEntry ->
                val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                val authState = authViewModel.authState.collectAsState().value
                val user = (authState as? AuthState.Success)?.user
                val productImages = scanViewModel.productImages.collectAsState().value
                val isComplaintAllowed by complaintViewModel.isComplaintAllowed.collectAsState()

                LaunchedEffect(scanId) {
                    complaintViewModel.initialize(
                        context = this@MainActivity,
                        scanId = scanId,
                        currentUserId = user?.userId ?: "unknown",
                        userName = user?.name,
                        userEmail = user?.email,
                        userPhone = user?.phone,
                        initialImages = productImages.ifEmpty { null }
                    )
                }

                LaunchedEffect(isComplaintAllowed) {
                    if (isComplaintAllowed == false) {
                        Toast.makeText(
                            this@MainActivity,
                            "Complaints can only be raised for confirmed mandatory violations.",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.popBackStack()
                    }
                }

                com.example.ui.screens.complaint.UserDetailsScreen(
                    viewModel = complaintViewModel,
                    onBackClick = { navController.popBackStack() },
                    onContinueClick = { navController.navigate("complaint_product_details/$scanId") }
                )
            }
            composable("complaint_product_details/{scanId}") { backStackEntry ->
                val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                com.example.ui.screens.complaint.ProductPurchaseDetailsScreen(
                    viewModel = complaintViewModel,
                    onBackClick = { navController.popBackStack() },
                    onContinueClick = { navController.navigate("complaint_live_photo/$scanId") }
                )
            }
            composable("complaint_live_photo/{scanId}") { backStackEntry ->
                val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                com.example.ui.screens.complaint.LivePhotoScreen(
                    viewModel = complaintViewModel,
                    onBackClick = { navController.popBackStack() },
                    onContinueClick = { navController.navigate("complaint_confirm/$scanId") }
                )
            }
            composable("complaint_confirm/{scanId}") { backStackEntry ->
                val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                com.example.ui.screens.complaint.ConfirmComplaintScreen(
                    viewModel = complaintViewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditUserDetails = { navController.navigate("complaint_user_details/$scanId") },
                    onEditProductDetails = { navController.navigate("complaint_product_details/$scanId") },
                    onRetakeLivePhoto = { navController.navigate("complaint_live_photo/$scanId") },
                    onSubmitSuccess = { complaintId ->
                        navController.navigate("complaint_submitted/$complaintId") {
                            popUpTo("complaint_user_details/$scanId") { inclusive = true }
                        }
                    }
                )
            }
            composable("complaint_submitted/{complaintId}") { backStackEntry ->
                val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                com.example.ui.screens.complaint.ComplaintSuccessScreen(
                    complaintId = complaintId,
                    viewModel = complaintViewModel,
                    onViewComplaintClick = {
                        navController.popBackStack("user_dashboard", inclusive = false)
                    },
                    onBackToHomeClick = {
                        navController.popBackStack("user_dashboard", inclusive = false)
                    }
                )
            }
            composable("complaint_screen/{scanId}") { backStackEntry ->
                val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                LaunchedEffect(scanId) {
                    navController.navigate("complaint_user_details/$scanId") {
                        popUpTo("complaint_screen/$scanId") { inclusive = true }
                    }
                }
            }
            composable("complaint_review/{scanId}") { backStackEntry ->
                val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                LaunchedEffect(scanId) {
                    navController.navigate("complaint_confirm/$scanId") {
                        popUpTo("complaint_review/$scanId") { inclusive = true }
                    }
                }
            }
            composable("previous_scans") {
                val authState = authViewModel.authState.collectAsState().value
                val userId = (authState as? AuthState.Success)?.user?.userId ?: "unknown"
                PreviousScansScreen(
                    userId = userId,
                    onBackClick = { navController.popBackStack() },
                    onScanProductClick = { 
                        navController.popBackStack()
                        navController.navigate("scan_product") 
                    },
                    onRaiseComplaint = { scanId ->
                        navController.navigate("complaint_user_details/$scanId")
                    }
                )
            }
            composable("camera") {
                val capturedImages by scanViewModel.productImages.collectAsState()
                CameraScreen(
                    capturedImages = capturedImages,
                    onImageCaptured = { uri ->
                        scanViewModel.addImage(uri, com.example.domain.model.ImageSource.CAMERA)
                    },
                    onRemoveImage = { id ->
                        scanViewModel.removeImage(id)
                    },
                    onDoneClick = {
                        navController.popBackStack()
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable("authority_dashboard") {
              val user = (authState as? AuthState.Success)?.user
              AuthorityDashboardScreen(
                category = user?.authorityCategory,
                onLogoutClick = {
                  authViewModel.logout()
                },
                onComplaintClick = { complaintId ->
                  navController.navigate("authority_complaint_detail/$complaintId")
                }
              )
            }
            composable("authority_complaint_detail/{complaintId}") { backStackEntry ->
                val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                val user = (authState as? AuthState.Success)?.user
                AuthorityComplaintDetailScreen(
                    complaintId = complaintId,
                    authorityId = user?.userId ?: "unknown",
                    authorityCategory = user?.authorityCategory ?: "UNKNOWN",
                    onBackClick = { navController.popBackStack() },
                    onNavigateToComplainant = { id -> navController.navigate("complainant_details/$id") }
                )
            }
            composable("complainant_details/{complaintId}") { backStackEntry ->
                val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                val user = (authState as? AuthState.Success)?.user
                ComplainantDetailsScreen(
                    complaintId = complaintId,
                    authorityId = user?.userId ?: "unknown",
                    authorityCategory = user?.authorityCategory ?: "UNKNOWN",
                    onBackClick = { navController.popBackStack() }
                )
            }
          }
        }
      }
    }
  }
}
