package com.otheruncle.memorydisplay.ui.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.otheruncle.memorydisplay.data.model.CardType
import com.otheruncle.memorydisplay.ui.navigation.Screen

/**
 * Router that loads a card and redirects to the appropriate edit form based on card type
 */
@Composable
fun EditCardRouter(
    cardId: Int,
    navController: NavController,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.card) {
        uiState.card?.let { card ->
            // Navigate to the appropriate edit screen based on card type
            val route = when (card.cardType) {
                CardType.PROFESSIONAL_APPOINTMENT -> Screen.EditProfessional.createRoute(cardId)
                CardType.FAMILY_EVENT -> Screen.EditFamilyEvent.createRoute(cardId)
                CardType.OTHER_EVENT -> Screen.EditOtherEvent.createRoute(cardId)
                CardType.FAMILY_TRIP -> Screen.EditTrip.createRoute(cardId)
                CardType.REMINDER -> Screen.EditReminder.createRoute(cardId)
                CardType.FAMILY_MESSAGE -> Screen.EditMessage.createRoute(cardId)
                CardType.PENDING -> {
                    // Pending cards use the review flow
                    navController.popBackStack()
                    return@LaunchedEffect
                }
            }
            
            // Replace this screen with the edit form
            navController.navigate(route) {
                popUpTo(Screen.EditCard.route) { inclusive = true }
            }
        }
    }
    
    // Show loading while determining card type
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
