package com.otheruncle.memorydisplay.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.otheruncle.memorydisplay.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripFormScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: TripViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSave()
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    val title = if (uiState.isEditMode) {
        stringResource(R.string.edit_trip)
    } else {
        stringResource(R.string.create_trip)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = uiState.isValid && !uiState.isSaving && !uiState.isLoading && !uiState.isUploadingImage
                    ) {
                        if (uiState.isSaving || uiState.isUploadingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.form_save))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val focusManager = LocalFocusManager.current
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Destination (Required)
                OutlinedTextField(
                    value = uiState.destination,
                    onValueChange = viewModel::updateDestination,
                    label = { Text(stringResource(R.string.trip_destination) + " *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Denver, Seattle") }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Departure",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Departure Date (Required)
                DatePickerField(
                    value = uiState.departureDate,
                    onValueChange = viewModel::updateDepartureDate,
                    label = stringResource(R.string.trip_departure_date) + " *",
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Departure Action
                OutlinedTextField(
                    value = uiState.departureAction,
                    onValueChange = viewModel::updateDepartureAction,
                    label = { Text(stringResource(R.string.trip_departure_action)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., flying, driving") }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Return",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Return Date
                DatePickerField(
                    value = uiState.returnDate,
                    onValueChange = viewModel::updateReturnDate,
                    label = stringResource(R.string.trip_return_date),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Return Action
                OutlinedTextField(
                    value = uiState.returnAction,
                    onValueChange = viewModel::updateReturnAction,
                    label = { Text(stringResource(R.string.trip_return_action)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., flying home, driving back") }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // What Doing
                OutlinedTextField(
                    value = uiState.whatDoing,
                    onValueChange = viewModel::updateWhatDoing,
                    label = { Text(stringResource(R.string.trip_what_doing)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Business conference, Visiting family") }
                )
                
                // Narrative
                OutlinedTextField(
                    value = uiState.narrative,
                    onValueChange = viewModel::updateNarrative,
                    label = { Text(stringResource(R.string.form_narrative)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    placeholder = { Text("Any additional context...") }
                )
                
                // Image Picker
                ImagePickerField(
                    selectedImageUri = uiState.selectedImageUri,
                    existingImagePath = uiState.existingImagePath,
                    onImageSelected = viewModel::updateSelectedImage,
                    modifier = Modifier.fillMaxWidth()
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Allow Others to Edit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateAllowOthersEdit(!uiState.allowOthersEdit) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.allowOthersEdit,
                        onCheckedChange = viewModel::updateAllowOthersEdit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.form_allow_edit))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
