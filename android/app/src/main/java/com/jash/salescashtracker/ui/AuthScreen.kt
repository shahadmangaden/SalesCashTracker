package com.jash.salescashtracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jash.salescashtracker.R
import com.jash.salescashtracker.data.Repository
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
    var showPassword by remember { mutableStateOf(false) }
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

    GlassBackground(forceLight = true) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 42.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            // Brand
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Glass.Blue), contentAlignment = Alignment.Center) {
                    Lc(R.drawable.lc_plane, Color.White, 24.dp)
                }
                Column {
                    T("Sales Cash", 16.sp, Glass.Ink, FontWeight.Bold)
                    T("Tracker", 13.sp, Glass.Muted, FontWeight.Medium)
                }
            }

            if (mode == AuthMode.SignIn) Illustration()

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                T(when (mode) { AuthMode.SignIn -> "Welcome back."; AuthMode.SignUp -> "Create account."; AuthMode.Reset -> "Reset password." },
                    36.sp, Glass.Ink, FontWeight.Bold)
                T(when (mode) {
                    AuthMode.SignIn -> "Every journey. Every dirham.\nYour travel business, in balance."
                    AuthMode.SignUp -> "One account for your phone, the website\nand the Windows app."
                    AuthMode.Reset -> "We'll email you a link\nto choose a new password."
                }, 14.sp, Glass.Muted, maxLines = 3)
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassInput("Email address", email, { email = it.trim() }, placeholder = "you@example.com",
                    icon = R.drawable.lc_mail, keyboard = KeyboardType.Email)
                if (mode != AuthMode.Reset) {
                    GlassInput("Password", password, { password = it }, placeholder = "At least 6 characters",
                        icon = if (showPassword) R.drawable.lc_eye else R.drawable.lc_eye_off, onIcon = { showPassword = !showPassword },
                        keyboard = KeyboardType.Password, password = !showPassword)
                }
                if (mode == AuthMode.SignUp) {
                    GlassInput("Confirm password", password2, { password2 = it }, placeholder = "Type it again",
                        keyboard = KeyboardType.Password, password = !showPassword)
                }
                if (mode == AuthMode.SignIn) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        T("Forgot password?", 13.sp, Glass.Blue, FontWeight.SemiBold,
                            Modifier.clip(RoundedCornerShape(6.dp)).clickable { mode = AuthMode.Reset; message = "" }, align = TextAlign.End)
                    }
                }
                if (message.isNotEmpty()) T(message, 13.sp, if (ok) Glass.Green else Glass.Red, FontWeight.SemiBold, maxLines = 3)
                PrimaryButton(
                    when (mode) { AuthMode.SignIn -> "Sign in"; AuthMode.SignUp -> "Create account"; AuthMode.Reset -> "Send reset link" },
                    R.drawable.lc_arrow_right, { submit() }, busy = busy,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)) {
                if (mode == AuthMode.SignIn) {
                    T("New here?", 13.sp, Glass.Muted)
                    T("Create account", 13.sp, Glass.Blue, FontWeight.SemiBold, Modifier.clickable { mode = AuthMode.SignUp; message = "" })
                } else {
                    T("Already have an account?", 13.sp, Glass.Muted)
                    T("Sign in", 13.sp, Glass.Blue, FontWeight.SemiBold, Modifier.clickable { mode = AuthMode.SignIn; message = "" })
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Lc(R.drawable.lc_shield_check, Glass.Muted, 13.dp)
                T("Your business data stays yours.", 11.sp, Glass.Muted)
            }
        }
    }
}

/** Tilted glass "travel card" with orbit rings and a "Looking good" badge. */
@Composable
private fun Illustration() {
    Box(Modifier.fillMaxWidth().height(164.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(174.dp).align(Alignment.TopCenter)) {
            drawCircle(Color.White.copy(alpha = 0.7f), radius = size.minDimension / 2 - 1f, style = Stroke(1.5f))
            drawCircle(Color.White.copy(alpha = 0.55f), radius = size.minDimension * 118f / 174f / 2, style = Stroke(1.5f))
        }
        Row(
            Modifier.rotate(-7f).width(222.dp).height(123.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color(0xFF5C7AA5).copy(alpha = 0.16f), spotColor = Color(0xFF5C7AA5).copy(alpha = 0.2f))
                .glass(24.dp, 0.63f, shadow = false).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconTile(R.drawable.lc_tickets_plane, Glass.BlueBg, Glass.Blue, 52.dp, 18.dp, 28.dp)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                T("DXB → Everywhere", 12.sp, Glass.Muted)
                T("All in balance.", 19.sp, Glass.Ink, FontWeight.Bold)
            }
        }
        Row(
            Modifier.offset(x = 100.dp, y = 55.dp).clip(RoundedCornerShape(100.dp)).background(Color.White).padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Lc(R.drawable.lc_circle_check, Glass.Green, 16.dp)
            T("Looking good", 12.sp, Glass.Green, FontWeight.SemiBold)
        }
    }
}
