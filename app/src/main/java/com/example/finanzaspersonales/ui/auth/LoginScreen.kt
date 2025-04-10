package com.example.finanzaspersonales.ui.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    // Callback to navigate when login is successful
    onLoginSuccess: () -> Unit,
    // (Add callback for navigating to registration later)
    viewModel: AuthViewModel = viewModel() // Get ViewModel instance
) {
    val uiState = viewModel.loginUiState
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- Google Sign-In Logic ---
    val oneTapClient = remember { Identity.getSignInClient(context) }
    val webClientId = "82342736652-ta7btlericu37mj2mo32rkdvu64dk1m1.apps.googleusercontent.com" // Paste your Web Client ID here

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val idToken = credential.googleIdToken
                    Log.d("LoginScreen", "Google Sign-In Got ID token: $idToken")
                    viewModel.signInWithGoogleToken(idToken)
                } catch (e: ApiException) {
                    Log.e("LoginScreen", "Google Sign-In failed: ${e.localizedMessage}", e)
                    viewModel.signInWithGoogleToken(null) // Notify ViewModel of failure
                }
            } else {
                 Log.w("LoginScreen", "Google Sign-In cancelled or failed. Result code: ${result.resultCode}")
                 // Optionally notify viewmodel about cancellation/external failure
            }
        }
    )

    fun beginGoogleSignIn() {
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(webClientId) // Crucial: Use the Web Client ID
                    .setFilterByAuthorizedAccounts(false) // Allow selection even if already signed in
                    .build()
            )
            .setAutoSelectEnabled(false) // Set to true if you want auto sign-in for returning users
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    googleSignInLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Couldn't start Google Sign-In UI: ${e.localizedMessage}", e)
                     viewModel.signInWithGoogleToken(null) // Notify failure
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginScreen", "Google Sign-In begin failed: ${e.localizedMessage}", e)
                 viewModel.signInWithGoogleToken(null) // Notify failure
            }
    }
    // --- End Google Sign-In Logic ---

    // Effect to navigate when loginSuccess becomes true
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess()
            viewModel.resetLoginSuccess() // Reset flag after triggering navigation
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.emailInput,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.errorMessage != null // Show error state
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.passwordInput,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            isError = uiState.errorMessage != null // Show error state
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Display error message if present
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::signInWithEmailPassword,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading // Disable button while loading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign In")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Add Google Sign In Button ---
        Button(
            onClick = { coroutineScope.launch { beginGoogleSignIn() } }, // Launch in coroutine scope
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
             // You might want to add a Google logo here later
            Text("Sign In with Google")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // (Add "Don't have an account? Register" text/button later)
    }
} 