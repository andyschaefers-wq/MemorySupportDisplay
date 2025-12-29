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
fun ReminderFormScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ReminderViewModel = hiltViewModel()
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
        stringResource(R.string.edit_reminder)
    } else {
        stringResource(R.string.create_reminder)
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
                    label = "Reminder Type",
                    icons = reminderIcons,
                    selectedIcon = uiState.icon,
                    onIconSelected = viewModel::updateIcon
                )
                
                // Reminder Text (Required)
                OutlinedTextField(
                    value = uiState.text,
                    onValueChange = viewModel::updateText,
                    label = { Text(stringResource(R.string.reminder_text) + " *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    placeholder = { Text("e.g., Take medication with breakfast") }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Recurrence Type
                RecurrenceDropdown(
                    selectedType = uiState.recurrenceType,
                    onTypeSelected = viewModel::updateRecurrenceType
                )
                
                // Show different fields based on recurrence type
                when (uiState.recurrenceType) {
                    "one-time" -> {
                        Text(
                            text = stringResource(R.string.reminder_appear),
                            style = MaterialTheme.typography.titleSmall
                        )
                        DatePickerField(
                            value = uiState.appearSpecific,
                            onValueChange = viewModel::updateAppearSpecific,
                            label = "Show on date *",
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = stringResource(R.string.reminder_disappear),
                            style = MaterialTheme.typography.titleSmall
                        )
                        DatePickerField(
                            value = uiState.disappearSpecific,
                            onValueChange = viewModel::updateDisappearSpecific,
                            label = "Hide after date",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    "weekly" -> {
                        Text(
                            text = stringResource(R.string.reminder_appear),
                            style = MaterialTheme.typography.titleSmall
                        )
                        DayOfWeekDropdown(
                            label = "Show starting *",
                            selectedDay = uiState.appearDay,
                            onDaySelected = viewModel::updateAppearDay
                        )
                        
                        Text(
                            text = stringResource(R.string.reminder_disappear),
                            style = MaterialTheme.typography.titleSmall
                        )
                        DayOfWeekDropdown(
                            label = "Hide after",
                            selectedDay = uiState.disappearDay,
                            onDaySelected = viewModel::updateDisappearDay
                        )
                    }
                    
                    "monthly" -> {
                        Text(
                            text = stringResource(R.string.reminder_appear),
                            style = MaterialTheme.typography.titleSmall
                        )
                        DayOfMonthDropdown(
                            label = "Show on day *",
                            selectedDay = uiState.appearDate,
                            onDaySelected = viewModel::updateAppearDate
                        )
                        
                        Text(
                            text = stringResource(R.string.reminder_disappear),
                            style = MaterialTheme.typography.titleSmall
                        )
                        DayOfMonthDropdown(
                            label = "Hide after day",
                            selectedDay = uiState.disappearDate,
                            onDaySelected = viewModel::updateDisappearDate
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Narrative
                OutlinedTextField(
                    value = uiState.narrative,
                    onValueChange = viewModel::updateNarrative,
                    label = { Text(stringResource(R.string.form_narrative)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
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
private fun RecurrenceDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = recurrenceOptions.find { it.type == selectedType } ?: recurrenceOptions.first()
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.reminder_recurrence)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            recurrenceOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onTypeSelected(option.type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfWeekDropdown(
    label: String,
    selectedDay: Int?,
    onDaySelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = daysOfWeek.find { it.second == selectedDay }?.first ?: "Select day"
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayValue,
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
            daysOfWeek.forEach { (name, value) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onDaySelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfMonthDropdown(
    label: String,
    selectedDay: Int?,
    onDaySelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = selectedDay?.toString() ?: "Select day"
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayValue,
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
            (1..31).forEach { day ->
                DropdownMenuItem(
                    text = { Text(day.toString()) },
                    onClick = {
                        onDaySelected(day)
                        expanded = false
                    }
                )
            }
        }
    }
}
