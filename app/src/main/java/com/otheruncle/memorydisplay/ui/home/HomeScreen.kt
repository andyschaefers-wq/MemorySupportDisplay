package com.otheruncle.memorydisplay.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.otheruncle.memorydisplay.R
import com.otheruncle.memorydisplay.data.model.Card
import com.otheruncle.memorydisplay.data.model.CardType
import com.otheruncle.memorydisplay.data.model.TemporalBadge
import com.otheruncle.memorydisplay.ui.theme.*

/**
 * Base URL for the server
 */
private const val BASE_URL = "https://otheruncle.com/memory_display/"

/**
 * Build the full image URL from an image path.
 * Images are stored in the images/ directory on the server.
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
        imagePath.startsWith("images/") -> {
            // Already has images/ prefix
            BASE_URL + imagePath
        }
        else -> {
            // Just filename - add images/ prefix
            BASE_URL + "images/" + imagePath
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCardClick: (Int) -> Unit,
    onEditCard: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Refresh data when screen becomes visible (e.g., returning from edit)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // State for delete confirmation dialog
    var cardToDelete by remember { mutableStateOf<Card?>(null) }

    // Delete confirmation dialog
    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete Card?") },
            text = { 
                Text("Are you sure you want to delete \"${getCardTitle(card)}\"? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCard(card.id)
                        cardToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Update Widget
            item {
                StatusUpdateWidget(
                    location = uiState.statusLocation,
                    note = uiState.statusNote,
                    isUpdating = uiState.isUpdatingStatus,
                    success = uiState.statusUpdateSuccess,
                    error = uiState.statusUpdateError,
                    onLocationChange = viewModel::updateStatusLocation,
                    onNoteChange = viewModel::updateStatusNote,
                    onSubmit = viewModel::submitStatus
                )
            }

            // Loading state
            if (uiState.isLoading && !uiState.isRefreshing) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Error state
            uiState.error?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = viewModel::loadData) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }

            // My Cards section
            if (uiState.myCards.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.home_my_cards),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(uiState.myCards, key = { it.id }) { card ->
                    SwipeableCardItem(
                        card = card,
                        canEdit = true,
                        canDelete = true,
                        isMuted = false,
                        onClick = { onCardClick(card.id) },
                        onEdit = { onEditCard(card.id) },
                        onDelete = { cardToDelete = card }
                    )
                }
            }

            // Editable Cards section
            if (uiState.editableCards.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_editable_cards),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(uiState.editableCards, key = { it.id }) { card ->
                    SwipeableCardItem(
                        card = card,
                        canEdit = true,
                        canDelete = false, // Can't delete others' cards
                        isMuted = false,
                        onClick = { onCardClick(card.id) },
                        onEdit = { onEditCard(card.id) },
                        onDelete = { }
                    )
                }
            }

            // Other Displayed Cards section (read-only, muted styling)
            if (uiState.otherDisplayedCards.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_other_cards),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(uiState.otherDisplayedCards, key = { it.id }) { card ->
                    // Read-only card - no edit, no delete, muted styling
                    CardItem(
                        card = card,
                        canEdit = false,
                        isMuted = true,
                        onClick = { onCardClick(card.id) },
                        onEdit = { }
                    )
                }
            }

            // Empty state
            if (!uiState.isLoading && 
                uiState.myCards.isEmpty() && 
                uiState.editableCards.isEmpty() && 
                uiState.otherDisplayedCards.isEmpty() && 
                uiState.error == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.EventNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.home_no_cards),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusUpdateWidget(
    location: String,
    note: String,
    isUpdating: Boolean,
    success: Boolean,
    error: String?,
    onLocationChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Update Your Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                label = { Text("Location (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isUpdating
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text("Status (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isUpdating
            )

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSubmit,
                enabled = (location.isNotBlank() || note.isNotBlank()) && !isUpdating,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else if (success) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Update")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCardItem(
    card: Card,
    canEdit: Boolean,
    canDelete: Boolean,
    isMuted: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (canDelete) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    false // Don't actually dismiss, let the dialog handle it
                } else {
                    false
                }
            },
            positionalThreshold = { it * 0.4f }
        )
        
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    },
                    label = "swipe_color"
                )
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(color, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true
        ) {
            CardItem(
                card = card,
                canEdit = canEdit,
                isMuted = isMuted,
                onClick = onClick,
                onEdit = onEdit
            )
        }
    } else {
        CardItem(
            card = card,
            canEdit = canEdit,
            isMuted = isMuted,
            onClick = onClick,
            onEdit = onEdit
        )
    }
}

@Composable
private fun CardItem(
    card: Card,
    canEdit: Boolean,
    isMuted: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    // Get base background color for card type
    val baseBackgroundColor = when (card.cardType) {
        CardType.PROFESSIONAL_APPOINTMENT -> CardProfessional
        CardType.FAMILY_EVENT -> CardFamilyEvent
        CardType.OTHER_EVENT -> CardOtherEvent
        CardType.FAMILY_TRIP -> CardTrip
        CardType.REMINDER -> CardReminder
        CardType.FAMILY_MESSAGE -> CardMessage
        CardType.PENDING -> CardPending
    }
    
    // Apply muting if needed (same background color, but muted text)
    val backgroundColor = baseBackgroundColor
    val textAlpha = if (isMuted) 0.6f else 1f
    val secondaryTextAlpha = if (isMuted) 0.5f else 0.7f

    val icon = getCardIcon(card)
    val typeLabel = getCardTypeLabel(card.cardType)
    val title = getCardTitle(card)
    val subtitle = getCardSubtitle(card)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    // Card type label
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = secondaryTextAlpha * 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Temporal badge
                    card.temporalBadge?.let { badge ->
                        TemporalBadgeChip(badge, isMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = textAlpha),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = secondaryTextAlpha),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Date/Time
                    val dateTime = buildDateTimeString(card)
                    if (dateTime.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Black.copy(alpha = secondaryTextAlpha)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dateTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = secondaryTextAlpha)
                            )
                        }
                    }

                    // Location
                    card.data?.location?.let { location ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Black.copy(alpha = secondaryTextAlpha)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = secondaryTextAlpha),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Creator info for editable cards
                    card.creatorName?.let { creatorName ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "By $creatorName",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black.copy(alpha = secondaryTextAlpha + 0.1f)
                        )
                    }
                }

                // Edit button (only if editable)
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.Black.copy(alpha = secondaryTextAlpha)
                        )
                    }
                }
            }

            // Image preview if available
            card.data?.imagePath?.let { imagePath ->
                val imageUrl = buildImageUrl(imagePath)
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop,
                    alpha = if (isMuted) 0.7f else 1f
                )
            }
        }
    }
}

@Composable
private fun TemporalBadgeChip(badge: TemporalBadge, isMuted: Boolean) {
    val (color, text) = when (badge) {
        TemporalBadge.TODAY -> BadgeToday to stringResource(R.string.today)
        TemporalBadge.TOMORROW -> BadgeTomorrow to stringResource(R.string.tomorrow)
        TemporalBadge.UPCOMING -> BadgeUpcoming to "Upcoming"
    }

    Surface(
        color = if (isMuted) color.copy(alpha = 0.7f) else color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = if (isMuted) 0.8f else 1f),
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getCardIcon(card: Card): ImageVector {
    return when (card.cardType) {
        CardType.PROFESSIONAL_APPOINTMENT -> {
            when (card.data?.icon) {
                "stethoscope" -> Icons.Default.MedicalServices
                "tooth" -> Icons.Default.Medication
                "eye" -> Icons.Default.Visibility
                "brain" -> Icons.Default.Psychology
                "heart" -> Icons.Default.Favorite
                "bone" -> Icons.Default.Accessibility
                else -> Icons.Default.MedicalServices
            }
        }
        CardType.FAMILY_EVENT -> Icons.Default.Groups
        CardType.OTHER_EVENT -> Icons.Default.Event
        CardType.FAMILY_TRIP -> Icons.Default.FlightTakeoff
        CardType.REMINDER -> Icons.Default.Notifications
        CardType.FAMILY_MESSAGE -> Icons.Default.Email
        CardType.PENDING -> Icons.Default.HourglassEmpty
    }
}

private fun getCardTypeLabel(cardType: CardType): String {
    return when (cardType) {
        CardType.PROFESSIONAL_APPOINTMENT -> "APPOINTMENT"
        CardType.FAMILY_EVENT -> "FAMILY EVENT"
        CardType.OTHER_EVENT -> "EVENT"
        CardType.FAMILY_TRIP -> "TRIP"
        CardType.REMINDER -> "REMINDER"
        CardType.FAMILY_MESSAGE -> "MESSAGE"
        CardType.PENDING -> "PENDING"
    }
}

private fun getCardTitle(card: Card): String {
    val data = card.data
    return when (card.cardType) {
        CardType.PROFESSIONAL_APPOINTMENT -> {
            // Show professional name, or purpose if no name
            data?.professionalName?.ifBlank { null }
                ?: data?.purpose?.ifBlank { null }
                ?: "(No details)"
        }
        CardType.FAMILY_EVENT -> {
            data?.summary?.ifBlank { null } ?: "(No details)"
        }
        CardType.OTHER_EVENT -> {
            data?.summary?.ifBlank { null } ?: "(No details)"
        }
        CardType.FAMILY_TRIP -> {
            data?.destination?.ifBlank { null } ?: "(No details)"
        }
        CardType.REMINDER -> {
            data?.text?.ifBlank { null } ?: "(No details)"
        }
        CardType.FAMILY_MESSAGE -> {
            data?.messagePreview?.ifBlank { null } ?: "(No details)"
        }
        CardType.PENDING -> {
            data?.title?.ifBlank { null } ?: "(No details)"
        }
    }
}

private fun getCardSubtitle(card: Card): String {
    val data = card.data ?: return ""
    return when (card.cardType) {
        CardType.PROFESSIONAL_APPOINTMENT -> {
            // If we have professional name as title, show type + purpose as subtitle
            // If we used purpose as title (no name), show type only
            val hasName = !data.professionalName.isNullOrBlank()
            val type = data.professionalType?.ifBlank { null }
            val purpose = data.purpose?.ifBlank { null }
            
            if (hasName) {
                // Title is name, show type and purpose
                listOfNotNull(
                    type?.let { "($it)" },
                    purpose
                ).joinToString(" • ")
            } else {
                // Title is purpose (or no details), just show type
                type?.let { "($it)" } ?: ""
            }
        }
        CardType.FAMILY_EVENT -> {
            // Show what to bring
            data.whatToBring?.ifBlank { null } ?: ""
        }
        CardType.OTHER_EVENT -> {
            data.whatToBring?.ifBlank { null } ?: ""
        }
        CardType.FAMILY_TRIP -> {
            // Show what doing
            data.whatDoing?.ifBlank { null } ?: ""
        }
        CardType.REMINDER -> ""
        CardType.FAMILY_MESSAGE -> {
            // Show beginning of full message
            data.fullMessage?.take(100)?.ifBlank { null } ?: ""
        }
        CardType.PENDING -> data.description?.ifBlank { null } ?: ""
    }
}

private fun buildDateTimeString(card: Card): String {
    val data = card.data ?: return ""
    val parts = mutableListOf<String>()

    when (card.cardType) {
        CardType.FAMILY_TRIP -> {
            data.departureDate?.let { parts.add(it) }
            data.returnDate?.let { parts.add("- $it") }
        }
        else -> {
            data.eventDate?.let { parts.add(it) }
            data.eventTime?.let { parts.add(it) }
        }
    }

    return parts.joinToString(" ")
}
