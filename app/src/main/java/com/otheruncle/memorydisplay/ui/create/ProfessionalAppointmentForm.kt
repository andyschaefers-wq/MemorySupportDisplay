package com.otheruncle.memorydisplay.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalAppointmentFormScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ProfessionalAppointmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    // Navigate back on successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSave()
        }
    }
    
    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Determine title based on edit mode
    val title = if (uiState.isEditMode) {
        stringResource(R.string.edit_professional)
    } else {
        stringResource(R.string.create_professional)
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
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
            // Show loading indicator while fetching card data
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
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
                IconDropdown(
                    selectedIcon = uiState.icon,
                    onIconSelected = viewModel::updateIcon
                )
                
                // Professional Name (Required)
                OutlinedTextField(
                    value = uiState.professionalName,
                    onValueChange = viewModel::updateProfessionalName,
                    label = { Text(stringResource(R.string.prof_name) + " *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    isError = uiState.professionalName.isBlank() && uiState.error != null
                )
                
                // Professional Type (Optional)
                OutlinedTextField(
                    value = uiState.professionalType,
                    onValueChange = viewModel::updateProfessionalType,
                    label = { Text(stringResource(R.string.prof_type)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Cardiologist, Orthodontist") }
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
                        label = stringResource(R.string.form_time) + " *",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Purpose (Required)
                OutlinedTextField(
                    value = uiState.purpose,
                    onValueChange = viewModel::updatePurpose,
                    label = { Text(stringResource(R.string.prof_purpose) + " *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Annual checkup, Follow-up") }
                )
                
                // Location (Optional)
                OutlinedTextField(
                    value = uiState.location,
                    onValueChange = viewModel::updateLocation,
                    label = { Text(stringResource(R.string.form_location)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Salem Health, 890 Oak St") }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Transportation",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Driver Dropdown
                DriverDropdown(
                    selectedDriverId = uiState.driverId,
                    selectedDriverName = uiState.driverName,
                    familyMembers = uiState.familyMembers,
                    isLoading = uiState.isLoadingFamily,
                    onDriverSelected = viewModel::updateDriver
                )
                
                // Transportation Notes
                OutlinedTextField(
                    value = uiState.transportationNotes,
                    onValueChange = viewModel::updateTransportationNotes,
                    label = { Text(stringResource(R.string.prof_transport_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Pick up at 1:15 PM") }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Preparation Notes
                OutlinedTextField(
                    value = uiState.preparationNotes,
                    onValueChange = viewModel::updatePreparationNotes,
                    label = { Text(stringResource(R.string.prof_prep_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    placeholder = { Text("e.g., Bring insurance card, fast after midnight") }
                )
                
                // Narrative / Additional Notes
                OutlinedTextField(
                    value = uiState.narrative,
                    onValueChange = viewModel::updateNarrative,
                    label = { Text(stringResource(R.string.form_narrative)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    placeholder = { Text("Any additional context or information...") }
                )
                
                // Image Picker
                ImagePickerField(
                    selectedImageUri = uiState.selectedImageUri,
                    existingImagePath = uiState.existingImagePath,
                    onImageSelected = viewModel::updateSelectedImage,
                    modifier = Modifier.fillMaxWidth()
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
                    Column {
                        Text(
                            text = stringResource(R.string.form_allow_edit),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Other family members can modify this card",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Bottom spacing for keyboard
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconDropdown(
    selectedIcon: String,
    onIconSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = professionalIcons.find { it.id == selectedIcon } ?: professionalIcons.first()
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.prof_icon)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            professionalIcons.forEach { option ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverDropdown(
    selectedDriverId: Int?,
    selectedDriverName: String,
    familyMembers: List<DriverOption>,
    isLoading: Boolean,
    onDriverSelected: (Int?, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!isLoading) expanded = it }
    ) {
        OutlinedTextField(
            value = if (selectedDriverName.isNotBlank()) selectedDriverName else "No driver needed",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.prof_driver)) },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            familyMembers.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.name) },
                    onClick = {
                        onDriverSelected(member.id, if (member.id != null) member.name else "")
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Format date for display
    val displayValue = if (value.isNotBlank()) {
        try {
            LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (e: Exception) {
            value
        }
    } else {
        ""
    }
    
    OutlinedTextField(
        value = displayValue,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Select date")
            }
        },
        modifier = modifier
    )
    
    if (showDatePicker) {
        // Calculate initial millis from current value each time dialog opens
        // Use UTC since DatePicker works in UTC
        val initialMillis = if (value.isNotBlank()) {
            try {
                LocalDate.parse(value)
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } else {
            System.currentTimeMillis()
        }
        
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // DatePicker returns millis at midnight UTC
                            // Convert using UTC to get correct date
                            val date = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                            onValueChange(date.toString()) // YYYY-MM-DD format
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Format time for display
    val displayValue = if (value.isNotBlank()) {
        try {
            val time = LocalTime.parse(value)
            time.format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (e: Exception) {
            value
        }
    } else {
        ""
    }
    
    OutlinedTextField(
        value = displayValue,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.Schedule, contentDescription = "Select time")
            }
        },
        modifier = modifier
    )
    
    if (showTimePicker) {
        // Parse current value or default to 9:00 AM
        val (initialHour, initialMinute) = if (value.isNotBlank()) {
            try {
                val time = LocalTime.parse(value)
                Pair(time.hour, time.minute)
            } catch (e: Exception) {
                Pair(9, 0)
            }
        } else {
            Pair(9, 0)
        }
        
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        onValueChange(time.toString()) // HH:mm format
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
