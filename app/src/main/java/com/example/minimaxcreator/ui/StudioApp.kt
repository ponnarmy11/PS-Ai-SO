package com.example.minimaxcreator.ui

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.minimaxcreator.ui.history.HistoryScreen
import com.example.minimaxcreator.ui.history.HistoryViewModel
import com.example.minimaxcreator.ui.home.HomeScreen
import com.example.minimaxcreator.ui.home.HomeViewModel
import com.example.minimaxcreator.ui.settings.ApiKeyDialog
import com.example.minimaxcreator.ui.settings.SettingsScreen
import com.example.minimaxcreator.ui.settings.SettingsViewModel
import com.example.minimaxcreator.ui.components.DynamicBackdrop
import com.example.minimaxcreator.ui.theme.StudioTheme

@Composable
fun StudioApp() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    StudioTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            DynamicBackdrop()
            if (
                !settingsState.isLoading &&
                settingsState.settings.miniMaxApiKey.isBlank() &&
                settingsState.settings.qwenApiKey.isBlank()
            ) {
                ApiKeyDialog(onSave = settingsViewModel::saveKeys)
            }
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    AppBottomBar(navController)
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = {
                        fadeIn(animationSpec = tween(280, easing = EaseOutCubic)) +
                            slideInHorizontally(
                                animationSpec = tween(280, easing = EaseOutCubic),
                                initialOffsetX = { width -> width / 12 }
                            )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(180, easing = EaseOutCubic)) +
                            slideOutHorizontally(
                                animationSpec = tween(180, easing = EaseOutCubic),
                                targetOffsetX = { width -> -width / 18 }
                            )
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(280, easing = EaseOutCubic)) +
                            slideInHorizontally(
                                animationSpec = tween(280, easing = EaseOutCubic),
                                initialOffsetX = { width -> -width / 12 }
                            )
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(180, easing = EaseOutCubic)) +
                            slideOutHorizontally(
                                animationSpec = tween(180, easing = EaseOutCubic),
                                targetOffsetX = { width -> width / 18 }
                            )
                    },
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("home") {
                        HomeScreen(viewModel = homeViewModel)
                    }
                    composable("history") {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onRegenerate = { record ->
                                homeViewModel.prepareRegeneration(record)
                                navController.navigateToHome()
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(viewModel = settingsViewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination?.route
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color(0xCCF7FAFF),
        contentColor = Color(0xFF1D1D1F),
        tonalElevation = 0.dp,
        windowInsets = androidx.compose.material3.NavigationBarDefaults.windowInsets
    ) {
        NavigationBarItem(
            selected = currentDestination == "home",
            onClick = { navController.navigateToHome() },
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            label = { Text("创作") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0A84FF),
                selectedTextColor = Color(0xFF1D1D1F),
                unselectedIconColor = Color(0xFF6E6E73),
                unselectedTextColor = Color(0xFF6E6E73),
                indicatorColor = Color(0x33FFFFFF)
            )
        )
        NavigationBarItem(
            selected = currentDestination == "history",
            onClick = { navController.navigate("history") { launchSingleTop = true } },
            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
            label = { Text("历史") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0A84FF),
                selectedTextColor = Color(0xFF1D1D1F),
                unselectedIconColor = Color(0xFF6E6E73),
                unselectedTextColor = Color(0xFF6E6E73),
                indicatorColor = Color(0x33FFFFFF)
            )
        )
        NavigationBarItem(
            selected = currentDestination == "settings",
            onClick = { navController.navigate("settings") { launchSingleTop = true } },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text("设置") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0A84FF),
                selectedTextColor = Color(0xFF1D1D1F),
                unselectedIconColor = Color(0xFF6E6E73),
                unselectedTextColor = Color(0xFF6E6E73),
                indicatorColor = Color(0x33FFFFFF)
            )
        )
    }
}

private fun NavHostController.navigateToHome() {
    navigate("home") {
        launchSingleTop = true
        popUpTo("home") { inclusive = false }
    }
}
