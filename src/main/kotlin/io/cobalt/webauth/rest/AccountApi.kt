package io.cobalt.webauth.rest

import java.time.Instant

data class AccountExists(val email: String) {
    var suspended: Boolean? = false
}

data class AccountFree(val email: String) {
    var rsaPub: ByteArray? = null
}

data class Welcome(val message: String, val createdAt: Instant = Instant.now())
data class GoodBye(val message: String, val createdAt: Instant = Instant.now())

data class CertResponse (
    val token: String,
    val cert: String
)

data class SignUpRequest(
        val token: String,
        val email: String,
        val name: String,
        val password: String,
        val contacts: String
)

data class SignInRequest(
        val email: String,
        val password: String
)

data class SignInResponse(
        val email: String,
        val token: String,
        val createdAt: Instant
)

data class SignUpResponse(
        val email: String,
        val message: String
)