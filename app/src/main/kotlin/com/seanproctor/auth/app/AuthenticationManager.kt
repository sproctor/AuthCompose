package com.seanproctor.auth.app

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import org.apache.commons.codec.binary.Base64
import java.awt.Desktop
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.resume

class AuthenticationManager {
    var verifier: String? = null
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun authenticateUser(
        domain: String,
        clientId: String,
        redirectUri: String,
        scope: String,
        audience: String,
    ) {
        coroutineScope.launch {
            verifier = createVerifier()
            val challenge = createChallenge(verifier!!)
            val redirectTo = URLEncoder.encode(redirectUri, Charsets.UTF_8)
            val encodedScope = URLEncoder.encode(scope, Charsets.UTF_8)
            val url =
                "https://$domain/authorize?response_type=code&code_challenge=$challenge&code_challenge_method=S256" +
                        "&client_id=$clientId&redirect_uri=$redirectTo&scope=$encodedScope&audience=$audience"

            withContext(Dispatchers.IO) {
                Desktop.getDesktop().browse(URI(url))
            }

            val code = suspendCancellableCoroutine<String> { continuation ->
                val server = HttpServer.create(InetSocketAddress(5789), 0)

                server.createContext("/callback") { http ->
                    val parameters = http.requestURI.query?.let { decodeQueryString(it) }
                    val code = parameters?.get("code") ?: throw RuntimeException("Received a response with no code")
                    println("got a request: ${http.requestURI}")
                    println("code: $code")

                    sendResponse(http)

                    continuation.resume(code)
                }

                server.start()
            }

            val client = HttpClient()

            val response = client.post<HttpResponse> {
                url("https://$domain/oauth/token")
                header("content-type", "application/x-www-form-urlencoded")
                body = "grant_type=authorization_code&client_id=$clientId&code_verifier=$verifier" +
                        "&code=$code&redirect_uri=$redirectTo"
            }

            println("response: ${response.readText()}")
        }
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

    private fun decodeQueryString(queryString: String): Map<String, String> {
        return queryString
            .split("&")
            .mapNotNull { portion ->
                val parts = portion.split("=")
                if (parts.size > 1) {
                    parts[0] to parts[1]
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun sendResponse(http: HttpExchange) {
        http.responseHeaders.add("Content-type", "text/plain")
        http.sendResponseHeaders(200, 0)
        PrintWriter(http.responseBody).use { out ->
            out.println("OK")
        }
    }
}

@JvmInline
value class AccessToken(val token: String)
