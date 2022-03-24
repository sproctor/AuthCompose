package com.seanproctor.auth.app

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.commons.codec.binary.Base64
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.resume

class AuthenticationManager {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun authenticateUser(
        domain: String,
        clientId: String,
        redirectUri: String,
        scope: String,
        audience: String,
    ) {
        coroutineScope.launch {
            try {
                val verifier = createVerifier()
                val challenge = createChallenge(verifier)
                val url = createLoginUrl(
                    domain = domain,
                    clientId = clientId,
                    redirectUri = redirectUri,
                    scope = scope,
                    challenge = challenge,
                    audience = audience,
                )

                println("Launching URL: $url")

                withContext(Dispatchers.IO) {
                    Desktop.getDesktop().browse(URI(url))
                }

                val code = waitForCallback()

                println("getting token")

                getToken(
                    domain = domain,
                    clientId = clientId,
                    verifier = verifier,
                    code = code,
                    redirectUri = redirectUri,
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun getToken(
        domain: String,
        clientId: String,
        verifier: String,
        code: String,
        redirectUri: String,
    ) {
        val encodedRedirectUri = URLEncoder.encode(redirectUri, Charsets.UTF_8)

        val client = HttpClient {
            install(JsonFeature)
        }

        val response = client.post<TokenResponse> {
            url("https://$domain/oauth/token")
            header("content-type", "application/x-www-form-urlencoded")
            body = "grant_type=authorization_code&client_id=$clientId&code_verifier=$verifier" +
                    "&code=$code&redirect_uri=$encodedRedirectUri"
        }

        println("response: $response")
    }

    private suspend fun waitForCallback(): String {
        return suspendCancellableCoroutine { continuation ->
            var server: NettyApplicationEngine? = null
            server = embeddedServer(Netty, port = 5789) {
                routing {
                    get ("/callback") {
                        val code = call.parameters["code"] ?: throw RuntimeException("Received a response with no code")
                        println("got code: $code")
                        call.respondText("OK")

                        continuation.resume(code)

                        server!!.stop(1000L, 1000L)
                    }
                }
            }.start(wait = false)
        }
    }

    private fun createLoginUrl(
        domain: String,
        clientId: String,
        redirectUri: String,
        scope: String,
        challenge: String,
        audience: String,
    ): String {
        val encodedRedirectUri = URLEncoder.encode(redirectUri, Charsets.UTF_8)
        val encodedScope = URLEncoder.encode(scope, Charsets.UTF_8)
        val encodedAudience = URLEncoder.encode(audience, Charsets.UTF_8)
        return "https://$domain/authorize?response_type=code&code_challenge=$challenge" +
                "&code_challenge_method=S256&client_id=$clientId&redirect_uri=$encodedRedirectUri" +
                "&scope=$encodedScope&audience=$encodedAudience"
    }

    private fun createVerifier(): String {
        val sr = SecureRandom()
        val code = ByteArray(32)
        sr.nextBytes(code)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(code)
    }

    private fun createChallenge(verifier: String): String {
        val bytes: ByteArray = verifier.toByteArray(Charsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes, 0, bytes.size)
        val digest = md.digest()
        return Base64.encodeBase64URLSafeString(digest)
    }
}

@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("id_token")
    val idToken: String,
    val scope: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("token_type")
    val tokenType: String,
)
