package com.example.projektmobpravi.ui.register

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun register(username: String, email: String, password: String, confirmPassword: String) {
        // Validacija
        when {
            username.isBlank() || email.isBlank() || password.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Sva polja moraju biti ispunjena"
                )
                return
            }
            password != confirmPassword -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Lozinke se ne podudaraju"
                )
                return
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Lozinka mora imati najmanje 6 znakova"
                )
                return
            }
        }

        if (!isOnline()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Nema internetske veze. Provjeri konekciju i pokušaj ponovo."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // Kreiraj korisnika u Firebase Auth
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: throw Exception("Greška pri kreiranju računa")
                val userId = firebaseUser.uid

                // Postavi displayName u Firebase Auth
                firebaseUser.updateProfile(
                    userProfileChangeRequest { displayName = username }
                ).await()

                // Spremi korisničke podatke u Firestore
                val user = hashMapOf(
                    "username" to username,
                    "email" to email,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(userId).set(user).await()

                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Registracija neuspješna: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}