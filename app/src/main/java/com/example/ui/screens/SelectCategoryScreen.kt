package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ProductCategory
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCategoryScreen(
    onCategorySelected: (ProductCategory) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Product Category", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate100,
                    titleContentColor = Slate900,
                    navigationIconContentColor = Slate900
                )
            )
        },
        containerColor = Slate100
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Choose the category that best matches the product you are checking.",
                fontSize = 16.sp,
                color = Slate500,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            CategoryCard(
                emoji = "🍎",
                title = "Food",
                description = "Food, beverages and edible packaged products",
                onClick = { onCategorySelected(ProductCategory.FOOD) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            CategoryCard(
                emoji = "🧴",
                title = "Cosmetics",
                description = "Cosmetic and personal-care products",
                onClick = { onCategorySelected(ProductCategory.COSMETICS) }
            )
        }
    }
}

@Composable
fun CategoryCard(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 40.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Slate500
                )
            }
        }
    }
}
