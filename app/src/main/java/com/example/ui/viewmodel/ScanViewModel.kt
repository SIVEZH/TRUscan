package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.example.data.repository.ValidationRepository
import com.example.domain.model.ImageSource
import com.example.domain.model.ProductImage
import com.example.domain.model.ValidationState
import com.example.domain.model.ValidationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    // Retained for any legacy references / quick re-enable if needed
    @Suppress("unused")
    private val repository = ValidationRepository(application)
    
    // Unified Product Image Collection
    private val _productImages = MutableStateFlow<List<ProductImage>>(emptyList())
    val productImages: StateFlow<List<ProductImage>> = _productImages.asStateFlow()

    fun addImage(uri: Uri, source: ImageSource = ImageSource.CAMERA) {
        val newImage = ProductImage(
            id = UUID.randomUUID().toString(),
            uri = uri,
            source = source,
            timestamp = System.currentTimeMillis()
        )
        _productImages.value = _productImages.value + newImage
    }

    fun addImages(uris: List<Uri>, source: ImageSource = ImageSource.GALLERY) {
        val newItems = uris.map { uri ->
            ProductImage(
                id = UUID.randomUUID().toString(),
                uri = uri,
                source = source,
                timestamp = System.currentTimeMillis()
            )
        }
        _productImages.value = _productImages.value + newItems
    }

    fun removeImage(id: String) {
        _productImages.value = _productImages.value.filter { it.id != id }
    }

    fun removeImageAt(index: Int) {
        if (index in _productImages.value.indices) {
            val list = _productImages.value.toMutableList()
            list.removeAt(index)
            _productImages.value = list
        }
    }

    fun clearImages() {
        _productImages.value = emptyList()
    }

    // Compatibility accessors
    val frontImageUri: StateFlow<Uri?> get() {
        val flow = MutableStateFlow(_productImages.value.firstOrNull()?.uri)
        return flow
    }

    val backImageUri: StateFlow<Uri?> get() {
        val flow = MutableStateFlow(_productImages.value.getOrNull(1)?.uri)
        return flow
    }
}


