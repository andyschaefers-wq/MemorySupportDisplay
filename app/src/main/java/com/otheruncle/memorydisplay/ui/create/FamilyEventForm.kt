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
fun FamilyEventFormScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: FamilyEventViewModel = hiltViewModel()
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
        stringResource(R.string.edit_family_event)
    } else {
        stringResource(R.string.create_family_event)
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
                // Icon Selector
                IconDropdownGeneric(
                    label = "Event Type",
                    icons = familyEventIcons,
                    selectedIcon = uiState.icon,
                    onIconSelected = viewModel::updateIcon
                )
                
                // Summary (Required)
                OutlinedTextField(
                    value = uiState.summary,
                    onValueChange = viewModel::updateSummary,
                    label = { Text(stringResource(R.string.event_summary) + " *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Dinner at Sarah's") }
                )
                
                // Date and Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DatePickerField(
                        value = uiState.eventDate,
                        onValueChange = viewModel::updateEventDate,
                        label = stringResource(R.string.form_date) + " *",
                        modifier = Modifier.weight(1f)
                    )
                    
                    TimePickerField(
                        value = uiState.eventTime,
                        onValueChange = viewModel::updateEventTime,
                        label = stringResource(R.string.form_time),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Location
                OutlinedTextField(
                    value = uiState.location,
                    onValueChange = viewModel::updateLocation,
                    label = { Text(stringResource(R.string.form_location)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., 456 Oak Street") }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Attendees
                OutlinedTextField(
                    value = uiState.attendeesText,
                    onValueChange = viewModel::updateAttendeesText,
                    label = { Text(stringResource(R.string.event_attendees)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Mom, Dad, Sarah") }
                )
                
                // What to Bring
                OutlinedTextField(
                    value = uiState.whatToBring,
                    onValueChange = viewModel::updateWhatToBring,
                    label = { Text(stringResource(R.string.event_bring)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Dessert, wine") }
                )
                
                // Transportation
                OutlinedTextField(
                    value = uiState.transportation,
                    onValueChange = viewModel::updateTransportation,
                    label = { Text(stringResource(R.string.event_transportation)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Mike is driving") }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconDropdownGeneric(
    label: String,
    icons: List<IconOption>,
    selectedIcon: String,
    onIconSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = icons.find { it.id == selectedIcon } ?: icons.first()
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            icons.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onIconSelected(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
