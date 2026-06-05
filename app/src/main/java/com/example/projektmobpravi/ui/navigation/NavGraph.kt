package com.example.projektmobpravi.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.projektmobpravi.ui.add.AddTransactionScreen
import com.example.projektmobpravi.ui.add.CreateCategoryScreen
import com.example.projektmobpravi.ui.budget.BudgetScreen
import com.example.projektmobpravi.ui.home.HomeScreen
import com.example.projektmobpravi.ui.login.LoginScreen
import com.example.projektmobpravi.ui.register.RegisterScreen
import com.example.projektmobpravi.ui.scan.ScanScreen
import com.example.projektmobpravi.ui.stats.StatsScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object AddTransaction : Screen(
        "add_transaction?transactionId={transactionId}&scannedAmount={scannedAmount}&scannedNote={scannedNote}"
    ) {
        const val addRoute = "add_transaction?transactionId=-1"
        fun editRoute(transactionId: Int) = "add_transaction?transactionId=$transactionId"
        fun fromScanRoute(amount: String, note: String) =
            "add_transaction?transactionId=-1&scannedAmount=${Uri.encode(amount)}&scannedNote=${Uri.encode(note)}"
    }
    object Stats : Screen("stats")
    object Scan : Screen("scan")
    object Budget : Screen("budget")
    object CreateCategory : Screen("create_category")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.IntType; defaultValue = -1 },
                navArgument("scannedAmount") { type = NavType.StringType; defaultValue = "" },
                navArgument("scannedNote") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt("transactionId") ?: -1
            val scannedAmount = backStackEntry.arguments?.getString("scannedAmount") ?: ""
            val scannedNote = backStackEntry.arguments?.getString("scannedNote") ?: ""
            AddTransactionScreen(
                navController = navController,
                transactionId = if (transactionId == -1) null else transactionId,
                scannedAmountFromRoute = scannedAmount,
                scannedNoteFromRoute = scannedNote
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(navController = navController)
        }
        composable(Screen.Scan.route) {
            ScanScreen(navController = navController)
        }
        composable(Screen.Budget.route) {
            BudgetScreen(navController = navController)
        }
        composable(Screen.CreateCategory.route) {
            CreateCategoryScreen(navController = navController)
        }
    }
}