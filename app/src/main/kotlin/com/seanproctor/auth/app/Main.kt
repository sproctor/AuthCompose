package com.seanproctor.auth.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
@Preview
fun App() {
    val authenticationManager = remember { AuthenticationManager() }

    MaterialTheme {
        Button(onClick = {
            authenticationManager.authenticateUser(
                domain = Config.domain,
                clientId = Config.clientId,
                redirectUri = "http://localhost:5789/callback",
                scope = "openid offline_access",
                audience = Config.audience,
            )
        }) {
            Text("Login")
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
