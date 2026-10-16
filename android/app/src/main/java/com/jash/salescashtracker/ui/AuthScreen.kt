package com.jash.salescashtracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jash.salescashtracker.R
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.ui.theme.AppColors
import kotlinx.coroutines.launch

private enum class AuthMode { SignIn, SignUp, Reset }

fun friendlyAuthError(e: Throwable): String {
    val m = (e.message ?: e.toString())
    return when {
        "INVALID_LOGIN_CREDENTIALS" in m || "password is invalid" in m || "credential is incorrect" in m ||
            "supplied auth credential" in m -> "Wrong email or password."
        "no user record" in m -> "No account with that email. Create an account first."
        "badly formatted" in m -> "That email address doesn't look right."
        "already in use" in m -> "An account with this email already exists. Sign in instead."
        "at least 6" in m || "WEAK_PASSWORD" in m -> "Password must be at least 6 characters."
        "network" in m.lowercase() -> "No internet connection. Try again when online."
        "blocked all requests" in m || "TOO_MANY" in m -> "Too many attempts. Wait a few minutes and try again."
        else -> m
    }
}

@Composable
fun AuthScreen() {
    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        message = ""; ok = false
        if (email.isBlank()) { message = "Enter your email."; return }
        if (mode != AuthMode.Reset && password.length < 6) { message = "Password must be at least 6 characters."; return }
        if (mode == AuthMode.SignUp && password != password2) { message = "The two passwords don't match."; return }
        busy = true
        scope.launch {
            try {
                when (mode) {
                    AuthMode.SignIn -> Repository.signIn(email, password)
                    AuthMode.SignUp -> Repository.signUp(email, password)
                    AuthMode.Reset -> { Repository.resetPassword(email); ok = true; message = "Reset link sent. Check your email (and spam folder)." }
                }
            } catch (e: Exception) {
                message = friendlyAuthError(e)
            }
            busy = false
        }
    }

    GlassBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(92.dp).glass(RoundedCornerShape(24.dp), strong = true, elevation = 14.dp).background(Color(0xFF1A4E9E)),
            )
            Spacer(Modifier.height(12.dp))
            Text("Sales Cash Tracker", color = Glass.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Sales & cash paid, on every device", color = Glass.Muted, fontSize = 15.sp)
            Spacer(Modifier.height(24.dp))

            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), strong = true, elevation = 16.dp)) {
                Column(Modifier.padding(22.dp)) {
                    Text(
                        when (mode) { AuthMode.SignIn -> "Sign in"; AuthMode.SignUp -> "Create account"; AuthMode.Reset -> "Reset password" },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (mode) {
                            AuthMode.SignIn -> "Use the same email and password as on the website and Windows app."
                            AuthMode.SignUp -> "One account for all your devices."
                            AuthMode.Reset -> "We'll email you a link to choose a new password."
                        },
                        style = MaterialTheme.typography.bodyMedium, color = AppColors.Secondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email, onValueChange = { email = it.trim() }, label = { Text("Email") },
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (mode != AuthMode.Reset) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password, onValueChange = { password = it }, label = { Text("Password") },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (mode == AuthMode.SignUp) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password2, onValueChange = { password2 = it }, label = { Text("Confirm password") },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (message.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(message, color = if (ok) AppColors.GreenText else AppColors.RedText, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { submit() },
                        enabled = !busy,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Glass.Tint),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else Text(when (mode) { AuthMode.SignIn -> "Sign in"; AuthMode.SignUp -> "Create account"; AuthMode.Reset -> "Send reset link" })
                    }
                    Spacer(Modifier.height(6.dp))
                    if (mode == AuthMode.SignIn) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { mode = AuthMode.Reset; message = "" }) { Text("Forgot password?") }
                            TextButton(onClick = { mode = AuthMode.SignUp; message = "" }) { Text("Create account") }
                        }
                    } else {
                        TextButton(onClick = { mode = AuthMode.SignIn; message = "" }) { Text("← Back to sign in") }
                    }
                }
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}
