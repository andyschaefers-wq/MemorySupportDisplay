package com.otheruncle.memorydisplay.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.otheruncle.memorydisplay.R
import com.otheruncle.memorydisplay.data.model.Card
import com.otheruncle.memorydisplay.data.model.CardType
import com.otheruncle.memorydisplay.data.model.TemporalBadge
import com.otheruncle.memorydisplay.ui.theme.*

data class CardTypeOption(
    val type: String,
    val titleRes: Int,
    val icon: ImageVector
)

private val cardTypes = listOf(
    CardTypeOption("professional", R.string.create_professional, Icons.Outlined.MedicalServices),
    CardTypeOption("family-event", R.string.create_family_event, Icons.Outlined.Groups),
    CardTypeOption("other-event", R.string.create_other_event, Icons.Outlined.Event),
    CardTypeOption("trip", R.string.create_trip, Icons.Outlined.FlightTakeoff),
    CardTypeOption("reminder", R.string.create_reminder, Icons.Outlined.Notifications)
)

/**
 * Create screen - card type selector
 */
@Composable
fun CreateScreen(
    onSelectType: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.create_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        cardTypes.forEach { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onSelectType(option.type) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(option.titleRes),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

// ==================== Form Screens ====================
// Forms are now in their own files:
// - ProfessionalAppointmentForm.kt
// - FamilyEventForm.kt
// - OtherEventForm.kt
// - TripForm.kt
// - ReminderForm.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Int,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.form_delete)) },
            text = { Text(stringResource(R.string.form_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCard()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            uiState.card != null -> {
                CardDetailContent(
                    card = uiState.card!!,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun CardDetailContent(
    card: Card,
    modifier: Modifier = Modifier
) {
    val data = card.data

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card type header
        item {
            val typeText = when (card.cardType) {
                CardType.PROFESSIONAL_APPOINTMENT -> stringResource(R.string.card_type_professional)
                CardType.FAMILY_EVENT -> stringResource(R.string.card_type_family_event)
                CardType.OTHER_EVENT -> stringResource(R.string.card_type_other_event)
                CardType.FAMILY_TRIP -> stringResource(R.string.card_type_trip)
                CardType.REMINDER -> stringResource(R.string.card_type_reminder)
                CardType.FAMILY_MESSAGE -> stringResource(R.string.card_type_message)
                CardType.PENDING -> stringResource(R.string.card_type_pending)
            }
            Text(
                text = typeText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Temporal badge
        card.temporalBadge?.let { badge ->
            item {
                val (color, text) = when (badge) {
                    TemporalBadge.TODAY -> BadgeToday to stringResource(R.string.today)
                    TemporalBadge.TOMORROW -> BadgeTomorrow to stringResource(R.string.tomorrow)
                    TemporalBadge.UPCOMING -> BadgeUpcoming to "Upcoming"
                }
                Surface(
                    color = color,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        // If no data, show message
        if (data == null) {
            item {
                Text(
                    text = "No additional details available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@LazyColumn
        }

        // Professional Appointment fields
        if (card.cardType == CardType.PROFESSIONAL_APPOINTMENT) {
            data.professionalName?.let { name ->
                item { DetailRow(label = stringResource(R.string.prof_name), value = name) }
            }
            data.professionalType?.let { type ->
                item { DetailRow(label = stringResource(R.string.prof_type), value = type) }
            }
            data.purpose?.let { purpose ->
                item { DetailRow(label = stringResource(R.string.prof_purpose), value = purpose) }
            }
            data.driverName?.let { driver ->
                item { DetailRow(label = stringResource(R.string.prof_driver), value = driver) }
            }
            data.transportationNotes?.let { notes ->
                item { DetailRow(label = stringResource(R.string.prof_transport_notes), value = notes) }
            }
            data.preparationNotes?.let { notes ->
                item { DetailRow(label = stringResource(R.string.prof_prep_notes), value = notes) }
            }
        }

        // Family/Other Event fields
        if (card.cardType in listOf(CardType.FAMILY_EVENT, CardType.OTHER_EVENT)) {
            data.summary?.let { summary ->
                item { DetailRow(label = stringResource(R.string.event_summary), value = summary) }
            }
            data.whatToBring?.let { bring ->
                item { DetailRow(label = stringResource(R.string.event_bring), value = bring) }
            }
            data.transportation?.let { transport ->
                item { DetailRow(label = stringResource(R.string.event_transportation), value = transport) }
            }
        }

        // Trip fields
        if (card.cardType == CardType.FAMILY_TRIP) {
            data.destination?.let { dest ->
                item { DetailRow(label = stringResource(R.string.trip_destination), value = dest) }
            }
            data.departureDate?.let { date ->
                item { DetailRow(label = stringResource(R.string.trip_departure_date), value = date) }
            }
            data.departureAction?.let { action ->
                item { DetailRow(label = stringResource(R.string.trip_departure_action), value = action) }
            }
            data.returnDate?.let { date ->
                item { DetailRow(label = stringResource(R.string.trip_return_date), value = date) }
            }
            data.returnAction?.let { action ->
                item { DetailRow(label = stringResource(R.string.trip_return_action), value = action) }
            }
            data.whatDoing?.let { doing ->
                item { DetailRow(label = stringResource(R.string.trip_what_doing), value = doing) }
            }
        }

        // Reminder fields
        if (card.cardType == CardType.REMINDER) {
            data.text?.let { text ->
                item { DetailRow(label = stringResource(R.string.reminder_text), value = text) }
            }
            data.recurrenceType?.let { type ->
                item { 
                    DetailRow(
                        label = stringResource(R.string.reminder_recurrence), 
                        value = type.name.lowercase().replaceFirstChar { it.uppercase() }
                    ) 
                }
            }
        }

        // Family Message fields
        if (card.cardType == CardType.FAMILY_MESSAGE) {
            data.messagePreview?.let { preview ->
                item { DetailRow(label = stringResource(R.string.message_preview), value = preview) }
            }
            data.fullMessage?.let { message ->
                item { DetailRow(label = stringResource(R.string.message_full), value = message) }
            }
        }

        // Common fields
        data.eventDate?.let { date ->
            item { DetailRow(label = stringResource(R.string.form_date), value = date) }
        }
        data.eventTime?.let { time ->
            item { DetailRow(label = stringResource(R.string.form_time), value = time) }
        }
        data.location?.let { location ->
            item { DetailRow(label = stringResource(R.string.form_location), value = location) }
        }
        data.narrative?.let { narrative ->
            item { DetailRow(label = stringResource(R.string.form_narrative), value = narrative) }
        }

        // Image
        data.imagePath?.let { imagePath ->
            item {
                val imageUrl = when {
                    imagePath.startsWith("http://") || imagePath.startsWith("https://") -> imagePath
                    imagePath.startsWith("/") -> "https://otheruncle.com/memory_display/" + imagePath.removePrefix("/")
                    imagePath.startsWith("images/") -> "https://otheruncle.com/memory_display/" + imagePath
                    else -> "https://otheruncle.com/memory_display/images/" + imagePath
                }
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Card image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }

        // Allow others to edit
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (card.allowOthersEdit) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (card.allowOthersEdit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.form_allow_edit),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// EditCardScreen removed - now using EditCardRouter which redirects to type-specific forms
