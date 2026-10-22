package com.thelast.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thelast.app.ui.event.EventDetailRoute
import com.thelast.app.ui.event.EventDetailViewModel
import com.thelast.app.ui.event.EventFormRoute
import com.thelast.app.ui.event.EventFormViewModel
import com.thelast.app.ui.home.HomeRoute
import com.thelast.app.ui.home.HomeViewModel

@Composable
fun TheLastNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Home,
    ) {
        composable(Routes.Home) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            HomeRoute(
                viewModel = homeViewModel,
                onAddEvent = {
                    navController.navigate(Routes.NewEvent)
                },
                onEventSelected = { eventTypeId ->
                    navController.navigate(Routes.eventDetail(eventTypeId))
                },
            )
        }
        composable(Routes.NewEvent) {
            val viewModel: EventFormViewModel = viewModel(factory = EventFormViewModel.createFactory())
            EventFormRoute(
                viewModel = viewModel,
                onClose = { navController.popBackStack() },
                onArchived = {
                    navController.popBackStack(Routes.Home, inclusive = false)
                },
            )
        }
        composable(
            route = Routes.EventDetail,
            arguments = listOf(navArgument("eventTypeId") { type = NavType.LongType }),
        ) { entry ->
            val eventTypeId = entry.arguments?.getLong("eventTypeId") ?: return@composable
            val viewModel: EventDetailViewModel = viewModel(
                key = "event-detail-$eventTypeId",
                factory = EventDetailViewModel.createFactory(eventTypeId),
            )
            EventDetailRoute(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                },
                onEditEvent = {
                    navController.navigate(Routes.editEvent(eventTypeId))
                },
                onEventDeleted = {
                    navController.popBackStack(Routes.Home, inclusive = false)
                },
            )
        }
        composable(
            route = Routes.EditEvent,
            arguments = listOf(navArgument("eventTypeId") { type = NavType.LongType }),
        ) { entry ->
            val eventTypeId = entry.arguments?.getLong("eventTypeId") ?: return@composable
            val viewModel: EventFormViewModel = viewModel(
                key = "event-form-$eventTypeId",
                factory = EventFormViewModel.createFactory(eventTypeId),
            )
            EventFormRoute(
                viewModel = viewModel,
                onClose = { navController.popBackStack() },
                onArchived = {
                    navController.popBackStack(Routes.Home, inclusive = false)
                },
            )
        }
    }
}

object Routes {
    const val Home = "home"
    const val EventDetail = "event/{eventTypeId}"
    const val NewEvent = "event/new"
    const val EditEvent = "event/{eventTypeId}/edit"

    fun eventDetail(eventTypeId: Long): String {
        return "event/$eventTypeId"
    }

    fun editEvent(eventTypeId: Long): String {
        return "event/$eventTypeId/edit"
    }
}
