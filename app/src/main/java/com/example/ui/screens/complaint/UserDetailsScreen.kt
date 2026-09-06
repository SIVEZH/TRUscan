package com.example.ui.screens.complaint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedBackground
import com.example.ui.viewmodel.ComplaintViewModel

@Composable
fun UserDetailsScreen(
    viewModel: ComplaintViewModel,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val fullName by viewModel.fullName.collectAsState()
    val email by viewModel.email.collectAsState()
    val phone by viewModel.phoneNumber.collectAsState()
    val address by viewModel.address.collectAsState()

    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var touched by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        touched = true
        val (valid, errorMap) = viewModel.validateUserDetails()
        errors = errorMap
        return valid
    }

    FrostedBackground {
        Scaffold(
            topBar = {
                ComplaintFlowHeader(
                    step = 1,
                    totalSteps = 4,
                    stepTitle = "Your Details",
                    stepDescription = "Please enter your contact information. Authorities require these details to officially log and respond to your complaint.",
                    onBackClick = onBackClick
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // 1. Full Name
                        Column {
                            Text(
                                text = "Full Name *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = {
                                    viewModel.fullName.value = it
                                    if (touched) validate()
                                },
                                placeholder = { Text("e.g. Ramesh Kumar", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Full Name",
                                        tint = if (errors.containsKey("fullName")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                },
                                isError = errors.containsKey("fullName"),
                                supportingText = {
                                    if (errors.containsKey("fullName")) {
                                        Text(errors["fullName"]!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    } else {
                                        Text("As per your official identification", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("complaint_full_name_input")
                            )
                        }

                        // 2. Email Address
                        Column {
                            Text(
                                text = "Email Address *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    viewModel.email.value = it
                                    if (touched) validate()
                                },
                                placeholder = { Text("e.g. ramesh.kumar@example.com", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email",
                                        tint = if (errors.containsKey("email")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                },
                                isError = errors.containsKey("email"),
                                supportingText = {
                                    if (errors.containsKey("email")) {
                                        Text(errors["email"]!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    } else {
                                        Text("Complaint updates will be sent to this email", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("complaint_email_input")
                            )
                        }

                        // 3. Phone Number
                        Column {
                            Text(
                                text = "Phone Number *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    // Keep digits and optional leading plus
                                    val filtered = it.filter { ch -> ch.isDigit() || ch == '+' }
                                    viewModel.phoneNumber.value = filtered
                                    if (touched) validate()
                                },
                                placeholder = { Text("e.g. 9876543210", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone Number",
                                        tint = if (errors.containsKey("phone")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                },
                                isError = errors.containsKey("phone"),
                                supportingText = {
                                    if (errors.containsKey("phone")) {
                                        Text(errors["phone"]!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    } else {
                                        Text("10-digit mobile number for SMS status updates", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("complaint_phone_input")
                            )
                        }

                        // 4. Address
                        Column {
                            Text(
                                text = "Your Address *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = {
                                    viewModel.address.value = it
                                    if (touched) validate()
                                },
                                placeholder = { Text("Door / House No., Street, Village or City, Pincode", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Address",
                                        tint = if (errors.containsKey("address")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                },
                                isError = errors.containsKey("address"),
                                supportingText = {
                                    if (errors.containsKey("address")) {
                                        Text(errors["address"]!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    } else {
                                        Text("Official postal address for jurisdictional authority review", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                minLines = 3,
                                maxLines = 5,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("complaint_address_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (validate()) {
                            onContinueClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("continue_to_product_details_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Continue to Product Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
