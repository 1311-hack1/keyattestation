package com.android.keyattestation.api

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import com.android.keyattestation.verifier.*
import com.android.keyattestation.verifier.challengecheckers.ChallengeMatcher
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import java.time.Instant

@Serializable
data class VerifyRequest(
    val attestationChainPem: List<String>, // array of PEM certs
    val challenge: String
)

@Serializable
data class VerifyResponse(
    val ok: Boolean,
    val packageName: String? = null,
    val signingCertDigest: String? = null,
    val verifiedBootState: String? = null,
    val securityLevel: String? = null,
    val error: String? = null
)

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        
        routing {
            get("/health") {
                call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
            }
            
            post("/verify") {
                try {
                    val request = call.receive<VerifyRequest>()

                    // Parse cert chain
                    val certFactory = CertificateFactory.getInstance("X.509")
                    val certs = request.attestationChainPem.map {
                        certFactory.generateCertificate(it.byteInputStream()) as X509Certificate
                    }

                    // Create verifier with Google trust anchors
                    val verifier = Verifier(
                        trustAnchorsSource = GoogleTrustAnchors,
                        revokedSerialsSource = { emptySet() }, // No revocation checking for now
                        instantSource = object : InstantSource {
                            override fun instant(): Instant = Instant.now()
                        }
                    )

                    // Create challenge checker
                    val challengeBytes = Base64.getDecoder().decode(request.challenge)
                    val challengeChecker = ChallengeMatcher(challengeBytes)

                    // Verify the attestation
                    when (val result = verifier.verify(certs, challengeChecker)) {
                        is VerificationResult.Success -> {
                            // Get attestation application ID from software or hardware enforced
                            val keyDescription = certs.first().keyDescription()
                            val appId = keyDescription?.softwareEnforced?.attestationApplicationId 
                                ?: keyDescription?.hardwareEnforced?.attestationApplicationId
                            
                            call.respond(
                                HttpStatusCode.OK,
                                VerifyResponse(
                                    ok = true,
                                    packageName = appId?.packages?.firstOrNull()?.name,
                                    signingCertDigest = appId?.signatures?.joinToString { 
                                        it.toByteArray().joinToString("") { byte -> "%02x".format(byte) }
                                    },
                                    verifiedBootState = result.verifiedBootState.toString(),
                                    securityLevel = result.securityLevel.toString()
                                )
                            )
                        }
                        is VerificationResult.ChallengeMismatch -> {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                VerifyResponse(ok = false, error = "Challenge mismatch")
                            )
                        }
                        is VerificationResult.PathValidationFailure -> {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                VerifyResponse(ok = false, error = "Certificate path validation failed: ${result.cause.message}")
                            )
                        }
                        is VerificationResult.ChainParsingFailure -> {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                VerifyResponse(ok = false, error = "Certificate chain parsing failed")
                            )
                        }
                        is VerificationResult.ExtensionParsingFailure -> {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                VerifyResponse(ok = false, error = "Extension parsing failed: ${result.cause.message}")
                            )
                        }
                        is VerificationResult.ExtensionConstraintViolation -> {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                VerifyResponse(ok = false, error = "Extension constraint violation: ${result.cause}")
                            )
                        }
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        VerifyResponse(ok = false, error = "Internal server error: ${e.message}")
                    )
                }
            }
        }
    }.start(wait = true)
}
