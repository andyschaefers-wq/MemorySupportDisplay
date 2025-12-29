package com.otheruncle.memorydisplay.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.otheruncle.memorydisplay.R

/**
 * Review screen - list pending calendar events for review
 * Only visible to users with calendar_reviewer = true
 * TODO: Implement full functionality with ViewModel
 */
@Composable
fun ReviewScreen(
    onEventClick: (Int) -> Unit
) {
    // TODO: Replace with actual data from ViewModel
    val pendingEvents = remember { emptyList<PendingEventItem>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.review_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (pendingEvents.isEmpty()) {
            Text(
                text = stringResource(R.string.review_no_pending),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingEvents) { event ->
                    PendingEventCard(
                        event = event,
                        onClick = { onEventClick(event.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingEventCard(
    event: PendingEventItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${event.date} ${event.time ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (event.location != null) {
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.review_from_calendar, event.sourceName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Temporary data class for UI
private data class PendingEventItem(
    val id: Int,
    val title: String,
    val date: String,
    val time: String?,
    val location: String?,
    val sourceName: String
)

/**
 * Review detail screen - convert pending event to proper card type
 * TODO: Implement full functionality
 */
@Composable
fun ReviewDetailScreen(
    eventId: Int,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedCardType by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.review_select_type),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card type selection
        listOf(
            "professional-appointment" to R.string.create_professional,
            "family-event" to R.string.create_family_event,
            "other-event" to R.string.create_other_event
        ).forEach { (type, labelRes) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedCardType = type }
                    .padding(vertical = 12.dp)
            ) {
                RadioButton(
                    selected = selectedCardType == type,
                    onClick = { selectedCardType = type }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // TODO: Show pre-filled form based on selected type

        Text(
            text = "Event ID: $eventId",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = onSave,
                enabled = selectedCardType != null
            ) {
                Text(stringResource(R.string.form_save))
            }
        }
    }
}
