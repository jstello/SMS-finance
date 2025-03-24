package com.example.finanzaspersonales.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.finanzaspersonales.data.model.Category
import java.util.UUID

/**
 * Screen for adding or editing a category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    viewModel: CategoriesViewModel,
    category: Category? = null,
    onBack: () -> Unit,
    onSave: (Category) -> Unit
) {
    val isEditing = category != null
    val title = if (isEditing) "Edit Category" else "Add Category"
    
    // State for category name
    var categoryName by remember { mutableStateOf(category?.name ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    // State for category color
    var selectedColor by remember { mutableStateOf(category?.color ?: CategoryColors.BLUE) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Category name input
            OutlinedTextField(
                value = categoryName,
                onValueChange = { 
                    categoryName = it
                    nameError = if (it.isBlank()) "Name cannot be empty" else null
                },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Color selection
            Text(
                text = "Select Color",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Color picker
            ColorPicker(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Preview
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preview:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Color indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor))
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Category name
                    Text(
                        text = categoryName.ifBlank { "Category Name" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save button
            Button(
                onClick = {
                    if (categoryName.isBlank()) {
                        nameError = "Name cannot be empty"
                    } else {
                        val newCategory = Category(
                            id = category?.id ?: UUID.randomUUID().toString(),
                            name = categoryName,
                            color = selectedColor
                        )
                        onSave(newCategory)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Update Category" else "Add Category")
            }
        }
    }
}

/**
 * Color picker component
 */
@Composable
fun ColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(CategoryColors.ALL) { color ->
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = 3.dp,
                        color = if (color == selectedColor) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

/**
 * Predefined category colors
 */
object CategoryColors {
    const val RED = 0xFFE57373.toInt()
    const val PINK = 0xFFF06292.toInt()
    const val PURPLE = 0xFFBA68C8.toInt()
    const val DEEP_PURPLE = 0xFF9575CD.toInt()
    const val INDIGO = 0xFF7986CB.toInt()
    const val BLUE = 0xFF64B5F6.toInt()
    const val LIGHT_BLUE = 0xFF4FC3F7.toInt()
    const val CYAN = 0xFF4DD0E1.toInt()
    const val TEAL = 0xFF4DB6AC.toInt()
    const val GREEN = 0xFF81C784.toInt()
    const val LIGHT_GREEN = 0xFFAED581.toInt()
    const val LIME = 0xFFDCE775.toInt()
    const val YELLOW = 0xFFFFD54F.toInt()
    const val AMBER = 0xFFFFB74D.toInt()
    const val ORANGE = 0xFFFFB74D.toInt()
    const val DEEP_ORANGE = 0xFFFF8A65.toInt()
    const val BROWN = 0xFFA1887F.toInt()
    const val GREY = 0xFF90A4AE.toInt()
    
    val ALL = listOf(
        RED, PINK, PURPLE, DEEP_PURPLE, INDIGO, BLUE, LIGHT_BLUE,
        CYAN, TEAL, GREEN, LIGHT_GREEN, LIME, YELLOW, AMBER,
        ORANGE, DEEP_ORANGE, BROWN, GREY
    )
} 