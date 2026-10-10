package com.phenom.gym.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phenom.gym.data.local.SessionManager
import com.phenom.gym.ui.apikeys.ApiKeysScreen
import com.phenom.gym.ui.apikeys.ApiKeysViewModel
import com.phenom.gym.ui.dashboard.DashboardViewModel
import com.phenom.gym.ui.gym.GymScreen
import com.phenom.gym.ui.home.HomeScreen
import com.phenom.gym.ui.login.LoginScreen
import com.phenom.gym.ui.login.LoginViewModel
import com.phenom.gym.ui.profile.ProfileScreen
import com.phenom.gym.ui.chat.ChatScreen
import com.phenom.gym.ui.theme.CyberBg

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val API_KEYS = "api_keys"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val session = SessionManager(context)

    val startDestination = if (session.isLoggedIn()) Routes.MAIN else Routes.LOGIN

    NavHost(navController = nav, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel()
            LoginScreen(
                vm = vm,
                onLoggedIn = {
                    nav.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onLogout = {
                    session.clear()
                    nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onApiKeys = { nav.navigate(Routes.API_KEYS) }
            )
        }

        composable(Routes.API_KEYS) {
            val vm: ApiKeysViewModel = viewModel()
            ApiKeysScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onApiKeys: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
    val dashboardVm: DashboardViewModel = viewModel()

    Box(modifier = Modifier.fillMaxSize().background(CyberBg)) {
        when (selectedTab) {
            BottomTab.HOME -> HomeScreen(
                onNavigateToGym = { selectedTab = BottomTab.GYM },
                onNavigateToProfile = { selectedTab = BottomTab.PROFILE },
                onNavigateToScan = { selectedTab = BottomTab.CHAT }
            )
            BottomTab.GYM -> GymScreen()
            BottomTab.CHAT -> ChatScreen()
            BottomTab.PROFILE -> ProfileScreen(
                onLogout = onLogout,
                onApiKeys = onApiKeys,
                vm = dashboardVm
            )
        }

        CyberBottomBar(
            selected = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
