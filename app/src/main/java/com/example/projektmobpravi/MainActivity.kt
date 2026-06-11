package com.example.projektmobpravi

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.projektmobpravi.ui.navigation.NavGraph
import com.example.projektmobpravi.ui.navigation.Screen
import com.example.projektmobpravi.ui.theme.AppLanguageState
import com.example.projektmobpravi.ui.theme.AppThemeState
import com.example.projektmobpravi.ui.theme.CroatianStrings
import com.example.projektmobpravi.ui.theme.EnglishStrings
import com.example.projektmobpravi.ui.theme.LocalLanguage
import com.example.projektmobpravi.ui.theme.LocalStrings
import com.example.projektmobpravi.ui.theme.LocalTheme
import com.example.projektmobpravi.ui.theme.PROJEKTMOBPRAVITheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

private const val PREFS_NAME     = "app_prefs"
private const val KEY_DARK       = "dark_theme"
private const val KEY_REMEMBER   = "remember_me"
private const val KEY_ENGLISH    = "language_english"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var isDark    by rememberSaveable { mutableStateOf(prefs.getBoolean(KEY_DARK,    false)) }
            var isEnglish by rememberSaveable { mutableStateOf(prefs.getBoolean(KEY_ENGLISH, false)) }

            val startDestination = remember {
                val rememberMe = prefs.getBoolean(KEY_REMEMBER, false)
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (rememberMe && currentUser != null) {
                    Screen.Home.route
                } else {
                    if (!rememberMe && currentUser != null) FirebaseAuth.getInstance().signOut()
                    Screen.Login.route
                }
            }

            CompositionLocalProvider(
                LocalTheme provides AppThemeState(
                    isDark = isDark,
                    toggle = {
                        isDark = !isDark
                        prefs.edit().putBoolean(KEY_DARK, isDark).apply()
                    }
                ),
                LocalLanguage provides AppLanguageState(
                    isEnglish = isEnglish,
                    toggle    = {
                        isEnglish = !isEnglish
                        prefs.edit().putBoolean(KEY_ENGLISH, isEnglish).apply()
                    }
                ),
                LocalStrings provides if (isEnglish) EnglishStrings else CroatianStrings
            ) {
                PROJEKTMOBPRAVITheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color    = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavGraph(navController = navController, startDestination = startDestination)
                    }
                }
            }
        }
    }
}
