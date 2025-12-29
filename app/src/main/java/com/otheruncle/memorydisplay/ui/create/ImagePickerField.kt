package com.otheruncle.memorydisplay.ui.create

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.io.File

/**
 * Base URL for the server
 */
private const val BASE_URL = "https://otheruncle.com/memory_display/"

/**
 * Build the full image URL from an image path.
 * Handles different formats:
 * - Full URL (starts with http): use as-is
 * - Absolute path (starts with /): append to base
 * - Relative path with subdirectory (subdir/file.jpg): append to images/
 * - Just filename (file.jpg): append to images/
 */
private fun buildImageUrl(imagePath: String): String {
    return when {
        imagePath.startsWith("http://") || imagePath.startsWith("https://") -> {
            // Already a full URL
            imagePath
        }
        imagePath.startsWith("/") -> {
            // Absolute path from server root
            BASE_URL + imagePath.removePrefix("/")
        }
        else -> {
            // Filename or relative path - all images are in images/ directory
            BASE_URL + "images/" + imagePath
        }
    }
}

/**
 * Reusable image picker composable that supports:
 * - Gallery selection (Photo Picker on Android 13+, fallback to GetContent)
 * - Camera capture with FileProvider for secure temp file storage
 * - Preview of selected/existing image
 * - Remove image option
 */
@Composable
fun ImagePickerField(
    selectedImageUri: Uri?,
    existingImagePath: String?,
    onImageSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOptionsDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Check if we have an image to display (either newly selected or existing)
    val hasImage = selectedImageUri != null || !existingImagePath.isNullOrBlank()
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, create temp file and launch camera
            val tempFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
        }
    }
    
    // Photo Picker launcher (Android 13+)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onImageSelected(it) }
    }
    
    // Fallback content picker (Android 12 and below)
    val contentPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImageSelected(it) }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { onImageSelected(it) }
        }
        tempCameraUri = null
    }
    
    // Launch camera when we have a URI ready
    LaunchedEffect(tempCameraUri) {
        tempCameraUri?.let { uri ->
            cameraLauncher.launch(uri)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "Image (Optional)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (hasImage) {
            // Show image preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                // Determine the image source
                val imageUrl: Any? = when {
                    selectedImageUri != null -> selectedImageUri
                    !existingImagePath.isNullOrBlank() -> {
                        // Construct full URL for existing image
                        val fullUrl = buildImageUrl(existingImagePath)
                        Log.d("ImagePickerField", "Loading existing image path: $existingImagePath")
                        Log.d("ImagePickerField", "Constructed URL: $fullUrl")
                        fullUrl
                    }
                    else -> null
                }
                
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        // Ensure EXIF orientation is respected (default but explicit)
                        .allowHardware(false) // Disable hardware bitmaps for better compatibility
                        .build(),
                    contentDescription = "Selected image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    },
                    error = {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "Failed to load image",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Failed to load image",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            // Show the path for debugging
                            if (!existingImagePath.isNullOrBlank()) {
                                Text(
                                    text = existingImagePath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
                
                // Remove button
                IconButton(
                    onClick = { onImageSelected(null) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove image",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Change image button
                TextButton(
                    onClick = { showOptionsDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Text("Change")
                }
            }
        } else {
            // Show add image card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { showOptionsDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to add image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // Options dialog
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Add Image") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = {
                            Icon(Icons.Default.Photo, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            showOptionsDialog = false
                            // Use Photo Picker on Android 13+, fallback otherwise
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                contentPickerLauncher.launch("image/*")
                            }
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Take Photo") },
                        leadingContent = {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            showOptionsDialog = false
                            // Check camera permission
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                // Create temp file and launch camera
                                val tempFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    tempFile
                                )
                                tempCameraUri = uri
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
