package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ComplaintEntity
import com.example.domain.model.ComplaintData
import com.example.domain.model.ImageSource
import com.example.domain.model.ProductImage
import com.example.domain.model.ValidationReport
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class ComplaintViewModel(application: Application) : AndroidViewModel(application) {

    // Identifiers & Context
    private val _currentScanId = MutableStateFlow("")
    val currentScanId: StateFlow<String> = _currentScanId.asStateFlow()

    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _productName = MutableStateFlow<String?>(null)
    val productName: StateFlow<String?> = _productName.asStateFlow()

    private val _category = MutableStateFlow("FOOD")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _reportJson = MutableStateFlow("")
    val reportJson: StateFlow<String> = _reportJson.asStateFlow()

    private val _isComplaintAllowed = MutableStateFlow<Boolean?>(null)
    val isComplaintAllowed: StateFlow<Boolean?> = _isComplaintAllowed.asStateFlow()

    // 1. User Details
    val fullName = MutableStateFlow("")
    val email = MutableStateFlow("")
    val phoneNumber = MutableStateFlow("")
    val address = MutableStateFlow("")

    // Backwards compatibility aliases
    val name = fullName
    val phone = phoneNumber

    // 2. Product & Purchase Details
    val productImages = MutableStateFlow<List<ProductImage>>(emptyList())
    val purchasePlace = MutableStateFlow("") // Shop / Store / Market name
    val purchaseLocation = MutableStateFlow("") // Address / Area / City
    val purchaseDate = MutableStateFlow<String?>("") // Optional purchase date
    val additionalDescription = MutableStateFlow("") // Optional description
    val receiptUri = MutableStateFlow<String?>(null)

    // 3. Live Photo
    val livePhotoUri = MutableStateFlow("")
    val livePhotoBitmap = MutableStateFlow<Bitmap?>(null)

    // 4. Confirmation Checkbox & Submission State
    val isConfirmed = MutableStateFlow(false)
    val isSubmitting = MutableStateFlow(false)
    val submissionError = MutableStateFlow<String?>(null)

    // 5. Result
    val submittedComplaint = MutableStateFlow<ComplaintData?>(null)

    /**
     * Initializes the complaint flow for a given scan.
     * Preserves user-entered values if navigating within the same scan session.
     */
    fun initialize(
        context: Context,
        scanId: String,
        currentUserId: String = "unknown",
        userName: String? = null,
        userEmail: String? = null,
        userPhone: String? = null,
        initialImages: List<ProductImage>? = null
    ) {
        if (_currentScanId.value == scanId && scanId.isNotBlank()) {
            // Already initialized for this scan; keep entered fields intact
            if (productImages.value.isEmpty() && !initialImages.isNullOrEmpty()) {
                productImages.value = initialImages
            }
            return
        }

        _currentScanId.value = scanId
        _userId.value = currentUserId
        _isComplaintAllowed.value = null

        // Prefill user details if fields are empty
        if (fullName.value.isBlank() && !userName.isNullOrBlank()) {
            fullName.value = userName
        }
        if (email.value.isBlank() && !userEmail.isNullOrBlank()) {
            email.value = userEmail
        }
        if (phoneNumber.value.isBlank() && !userPhone.isNullOrBlank()) {
            phoneNumber.value = userPhone
        }

        if (!initialImages.isNullOrEmpty()) {
            productImages.value = initialImages
        }

        // Load ScanEntity from Room database to link product details and images if needed
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val scan = db.scanDao().getScanById(scanId)
                if (scan != null) {
                    _productName.value = scan.productName
                    _category.value = scan.category
                    _reportJson.value = scan.reportJson

                    val allowed = try {
                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                        val report = moshi.adapter(ValidationReport::class.java).fromJson(scan.reportJson)
                        report?.hasConfirmedMandatoryViolation == true
                    } catch (e: Exception) {
                        false
                    }
                    _isComplaintAllowed.value = allowed

                    // If product images list is still empty, populate from scan
                    if (productImages.value.isEmpty()) {
                        val list = mutableListOf<ProductImage>()
                        if (scan.frontImageUri.isNotBlank()) {
                            list.add(
                                ProductImage(
                                    id = "front",
                                    uri = Uri.parse(scan.frontImageUri),
                                    source = ImageSource.CAMERA
                                )
                            )
                        }
                        if (scan.backImageUri.isNotBlank() && scan.backImageUri != scan.frontImageUri) {
                            list.add(
                                ProductImage(
                                    id = "back",
                                    uri = Uri.parse(scan.backImageUri),
                                    source = ImageSource.CAMERA
                                )
                            )
                        }
                        productImages.value = list
                    }
                } else {
                    _isComplaintAllowed.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isComplaintAllowed.value = false
            }
        }
    }

    fun setLivePhoto(uri: String, bitmap: Bitmap? = null) {
        livePhotoUri.value = uri
        livePhotoBitmap.value = bitmap
    }

    fun clearLivePhoto() {
        livePhotoUri.value = ""
        livePhotoBitmap.value = null
    }

    fun setPurchaseDate(date: String?) {
        purchaseDate.value = date
    }

    // Validation for User Details
    fun validateUserDetails(): Pair<Boolean, Map<String, String>> {
        val errors = mutableMapOf<String, String>()

        if (fullName.value.trim().isBlank()) {
            errors["fullName"] = "Please enter your full name"
        }

        val emailVal = email.value.trim()
        if (emailVal.isBlank()) {
            errors["email"] = "Please enter your email address"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
            errors["email"] = "Please enter a valid email address (e.g., name@example.com)"
        }

        val phoneVal = phoneNumber.value.trim().replace(Regex("[^0-9]"), "")
        if (phoneNumber.value.trim().isBlank()) {
            errors["phone"] = "Please enter your phone number"
        } else if (phoneVal.length < 10) {
            errors["phone"] = "Please enter a valid 10-digit phone number"
        }

        if (address.value.trim().isBlank()) {
            errors["address"] = "Please enter your address"
        }

        return Pair(errors.isEmpty(), errors)
    }

    // Validation for Product / Purchase Details
    fun validateProductDetails(): Pair<Boolean, Map<String, String>> {
        val errors = mutableMapOf<String, String>()

        if (purchasePlace.value.trim().isBlank()) {
            errors["purchasePlace"] = "Please enter the shop or market name"
        }

        if (purchaseLocation.value.trim().isBlank()) {
            errors["purchaseLocation"] = "Please enter the purchase location or address"
        }

        if (productImages.value.isEmpty()) {
            errors["productImages"] = "At least one product image is required"
        }

        return Pair(errors.isEmpty(), errors)
    }

    // Validation for Live Photo
    fun validateLivePhoto(): Pair<Boolean, String?> {
        return if (livePhotoUri.value.isBlank()) {
            Pair(false, "Live photo is required before continuing")
        } else {
            Pair(true, null)
        }
    }

    // Full Validation before submission
    fun validateAll(): Pair<Boolean, String?> {
        val (userOk, userErrors) = validateUserDetails()
        if (!userOk) {
            return Pair(false, userErrors.values.firstOrNull() ?: "Please complete all required personal details")
        }

        val (prodOk, prodErrors) = validateProductDetails()
        if (!prodOk) {
            return Pair(false, prodErrors.values.firstOrNull() ?: "Please complete all required purchase details")
        }

        val (photoOk, photoError) = validateLivePhoto()
        if (!photoOk) {
            return Pair(false, photoError ?: "Please take a live photo to attach")
        }

        if (!isConfirmed.value) {
            return Pair(false, "Please confirm the declaration checkbox before submitting")
        }

        return Pair(true, null)
    }

    /**
     * Submits the complaint to local Room database.
     * Generates a unique complaint reference formatted as: TRU-YYYYMMDD-XXXXXX
     * ZERO AI calls are made on the live photo or complaint data.
     */
    fun submitComplaint(
        context: Context,
        onSuccess: (complaintId: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (isSubmitting.value) return

        if (_isComplaintAllowed.value == false) {
            val err = "Complaints can only be raised for confirmed mandatory violations."
            submissionError.value = err
            onError(err)
            return
        }

        val (valid, errorMessage) = validateAll()
        if (!valid) {
            submissionError.value = errorMessage
            onError(errorMessage ?: "Please verify all required fields")
            return
        }

        isSubmitting.value = true
        submissionError.value = null

        viewModelScope.launch {
            try {
                val dateStr = DateTimeFormatter.ofPattern("yyyyMMdd")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now())
                val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
                val complaintId = "TRU-$dateStr-$randomSuffix"
                val createdAt = Instant.now().toString()

                val imagesJson = productImages.value.joinToString(";") { it.uri.toString() }

                val entity = ComplaintEntity(
                    complaintId = complaintId,
                    userId = _userId.value.ifBlank { "user_local" },
                    sourceScanId = _currentScanId.value,
                    category = _category.value,
                    productName = _productName.value,
                    reportJson = _reportJson.value,
                    complainantName = fullName.value.trim(),
                    complainantEmail = email.value.trim(),
                    complainantPhone = phoneNumber.value.trim(),
                    complainantAddress = address.value.trim(),
                    purchaseLocation = purchaseLocation.value.trim(),
                    purchaseDate = purchaseDate.value?.trim()?.takeIf { it.isNotBlank() },
                    receiptUri = receiptUri.value,
                    purchasePlace = purchasePlace.value.trim(),
                    additionalDescription = additionalDescription.value.trim().takeIf { it.isNotBlank() },
                    livePhotoUri = livePhotoUri.value,
                    productImagesJson = imagesJson,
                    status = "NEW",
                    authorityId = null,
                    authorityAction = null,
                    rejectionReason = null,
                    actionTimestamp = null,
                    createdAt = createdAt
                )

                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    db.complaintDao().insertComplaint(entity)
                }

                val complaintData = ComplaintData(
                    complaintId = complaintId,
                    sourceScanId = _currentScanId.value,
                    fullName = fullName.value.trim(),
                    email = email.value.trim(),
                    phoneNumber = phoneNumber.value.trim(),
                    address = address.value.trim(),
                    productImages = productImages.value,
                    purchasePlace = purchasePlace.value.trim(),
                    purchaseLocation = purchaseLocation.value.trim(),
                    purchaseDate = purchaseDate.value?.trim()?.takeIf { it.isNotBlank() },
                    additionalDescription = additionalDescription.value.trim().takeIf { it.isNotBlank() },
                    livePhotoUri = livePhotoUri.value,
                    complaintCreatedAt = createdAt
                )

                submittedComplaint.value = complaintData
                isSubmitting.value = false

                onSuccess(complaintId)
            } catch (e: Exception) {
                e.printStackTrace()
                isSubmitting.value = false
                val err = "Unable to record complaint. Please try again."
                submissionError.value = err
                onError(err)
            }
        }
    }

    /**
     * Resets complaint form state for fresh submissions
     */
    fun resetForm() {
        _currentScanId.value = ""
        _isComplaintAllowed.value = null
        fullName.value = ""
        email.value = ""
        phoneNumber.value = ""
        address.value = ""
        productImages.value = emptyList()
        purchasePlace.value = ""
        purchaseLocation.value = ""
        purchaseDate.value = ""
        additionalDescription.value = ""
        receiptUri.value = null
        livePhotoUri.value = ""
        livePhotoBitmap.value = null
        isConfirmed.value = false
        isSubmitting.value = false
        submissionError.value = null
        submittedComplaint.value = null
    }
}
