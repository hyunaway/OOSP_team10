// 경로: com/example/habittracker/MainActivity.kt
package com.example.habittracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.habittracker.ui.digital.DigitalInputScreen
import com.example.habittracker.ui.home.HomeScreen
import com.example.habittracker.ui.meal.MealInputScreen
import com.example.habittracker.ui.reports.ReportsScreen
import com.example.habittracker.ui.settings.SettingsScreen
import com.example.habittracker.ui.stretch.StretchInputScreen
import com.example.habittracker.ui.theme.HabitTrackerTheme
import com.example.habittracker.ui.water.WaterInputScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navHostController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitTrackerTheme {
                val navController = rememberNavController()
                    .also { navHostController = it }
                HabitTrackerApp(navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navHostController?.handleDeepLink(intent)
    }
}

object Routes {
    const val HOME = "home"
    const val WATER = "water"
    const val MEAL = "meal"
    const val DIGITAL = "digital"
    const val STRETCH = "stretch"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}

@Composable
private fun HabitTrackerApp(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?")

    val bottomNavRoutes = setOf(Routes.HOME, Routes.REPORTS, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                HabitBottomNav(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(
                route = Routes.HOME,
                deepLinks = listOf(navDeepLink { uriPattern = "app://habittracker/home" }),
            ) {
                HomeScreen(navController)
            }

            composable(
                route = "${Routes.WATER}?source={source}",
                arguments = listOf(navArgument("source") { defaultValue = "" }),
                deepLinks = listOf(navDeepLink { uriPattern = "app://habittracker/water?source={source}" }),
            ) {
                WaterInputScreen(navController)
            }

            composable(
                route = "${Routes.MEAL}?type={type}&source={source}",
                arguments = listOf(
                    navArgument("type") { defaultValue = "" },
                    navArgument("source") { defaultValue = "" },
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "app://habittracker/meal?type={type}&source={source}" }),
            ) {
                MealInputScreen(navController)
            }

            composable(
                route = "${Routes.DIGITAL}?app={app}&interventionId={interventionId}&source={source}",
                arguments = listOf(
                    navArgument("app") { defaultValue = "" },
                    navArgument("interventionId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("source") { defaultValue = "" },
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "app://habittracker/digital?app={app}&interventionId={interventionId}&source={source}"
                    }
                ),
            ) { backStackEntry ->
                val interventionId = backStackEntry.arguments?.getLong("interventionId") ?: -1L
                DigitalInputScreen(navController, interventionId)
            }

            composable(
                route = "${Routes.STRETCH}?trigger={trigger}",
                arguments = listOf(navArgument("trigger") { defaultValue = "normal" }),
                deepLinks = listOf(navDeepLink { uriPattern = "app://habittracker/stretch?trigger={trigger}" }),
            ) {
                StretchInputScreen(navController)
            }

            composable(Routes.REPORTS) {
                ReportsScreen()
            }

            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun HabitBottomNav(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = false }
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "홈") },
            label = { Text("홈") },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.REPORTS,
            onClick = {
                navController.navigate(Routes.REPORTS) {
                    popUpTo(Routes.HOME) { inclusive = false }
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "리포트") },
            label = { Text("리포트") },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.SETTINGS,
            onClick = {
                navController.navigate(Routes.SETTINGS) {
                    popUpTo(Routes.HOME) { inclusive = false }
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.Settings, contentDescription = "설정") },
            label = { Text("설정") },
        )
    }
}
