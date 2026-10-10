package com.phenom.gym.ui.profile

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phenom.gym.ui.dashboard.DashboardScreen
import com.phenom.gym.ui.dashboard.DashboardViewModel

/**
 * Profile page — wraps the existing Dashboard (user info, balance, API keys).
 * Accessible from bottom nav "Profile" tab.
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onApiKeys: () -> Unit,
    vm: DashboardViewModel = viewModel()
) {
    DashboardScreen(vm = vm, onLogout = onLogout, onApiKeys = onApiKeys)
}
