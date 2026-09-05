package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import android.widget.Toast

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val database = AppDatabase.getDatabase(this)
    val repository = AuthRepository(database.userDao())
    val factory = AuthViewModelFactory(repository)

    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        val authViewModel: AuthViewModel = viewModel(factory = factory)
        val scanViewModel: ScanViewModel = viewModel()
        val complaintViewModel: com.example.ui.viewmodel.ComplaintViewModel = viewModel()
        val authState by authViewModel.authState.collectAsState()

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    navController.navigate("camera/true") // Defaulting to front, will be overridden by args
                } else {
                    Toast.makeText(this, "Camera permission required.", Toast.LENGTH_SHORT).show()
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
                  navController.popBackStack("selection", inclusive = false)
                }
              )
            }
            composable("scan_product") {
              ScanProductScreen(
                  viewModel = scanViewModel,
                  onBackClick = { navController.popBackStack() },
                  onUseCameraClick = { isFront -> 
                      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                      // We save the destination in state or pass it via navigation arg
                      navController.navigate("camera/$isFront")
                  },
                  onContinueClick = {
                      navController.navigate("validation_report")
                  }
              )
            }
            composable("validation_report") {
                val frontUri = scanViewModel.frontImageUri.collectAsState().value
                val backUri = scanViewModel.backImageUri.collectAsState().value
                val authState = authViewModel.authState.collectAsState().value
                val userId = (authState as? AuthState.Success)?.user?.id?.toString() ?: "unknown"
                if (frontUri != null && backUri != null) {
                    com.example.ui.analysis.ValidationScreen(
                        frontUri = frontUri,
                        backUri = backUri,
                        userId = userId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateHome = { 
                            navController.popBackStack("user_dashboard", inclusive = false)
                        },
                        onRaiseComplaint = { scanId -> navController.navigate("complaint_screen/$scanId") }
                    )
                }
            }
            composable("complaint_screen/{scanId}") { backStackEntry ->
                val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                val authState = authViewModel.authState.collectAsState().value
                val user = (authState as? AuthState.Success)?.user
                
                com.example.ui.screens.ComplaintRegistrationScreen(
                    scanId = scanId,
                    user = user,
                    viewModel = complaintViewModel,
                    onBackClick = { navController.popBackStack() },
                    onReviewComplaint = { navController.navigate("complaint_review/$scanId") }
                )
            }
            composable("complaint_review/{scanId}") { backStackEntry ->
                val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                com.example.ui.screens.ComplaintReviewScreen(
                    scanId = scanId,
                    viewModel = complaintViewModel,
                    onBackClick = { navController.popBackStack() },
                    onContinueClick = { 
                        navController.popBackStack("user_dashboard", inclusive = false) 
                    }
                )
            }
            composable("previous_scans") {
                val authState = authViewModel.authState.collectAsState().value
                val userId = (authState as? AuthState.Success)?.user?.id?.toString() ?: "unknown"
                PreviousScansScreen(
                    userId = userId,
                    onBackClick = { navController.popBackStack() },
                    onScanProductClick = { 
                        navController.popBackStack()
                        navController.navigate("scan_product") 
                    }
                )
            }
            composable("camera/{isFront}") { backStackEntry ->
                val isFront = backStackEntry.arguments?.getString("isFront")?.toBoolean() ?: true
                CameraScreen(
                    isFrontProduct = isFront,
                    onBackClick = { navController.popBackStack() },
                    onImageCaptured = { uri -> 
                        if (isFront) {
                            scanViewModel.setFrontImage(uri)
                            // Immediately navigate to back camera
                            navController.popBackStack()
                            navController.navigate("camera/false")
                        } else {
                            scanViewModel.setBackImage(uri)
                            navController.popBackStack()
                        }
                    }
                )
            }
            composable("authority_dashboard") {
              val user = (authState as? AuthState.Success)?.user
              AuthorityDashboardScreen(
                category = user?.authorityCategory,
                onLogoutClick = {
                  authViewModel.logout()
                  navController.popBackStack("selection", inclusive = false)
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
                    authorityId = user?.id?.toString() ?: "unknown",
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
                    authorityId = user?.id?.toString() ?: "unknown",
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
