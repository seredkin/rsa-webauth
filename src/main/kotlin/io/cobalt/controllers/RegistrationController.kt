package io.cobalt.controllers

import arrow.core.Option
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.cobalt.services.EmailService
import io.cobalt.webauth.data.AccountRepo
import io.cobalt.webauth.rest.CertResponse
import io.cobalt.webauth.rest.SignInRequest
import io.cobalt.webauth.rest.SignUpRequest
import io.cobalt.webauth.rest.Welcome
import io.cobalt.webauth.rsa.decrypt
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.badRequest
import io.micronaut.http.HttpResponse.ok
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.session.Session
import io.micronaut.websocket.RxWebSocketSession
import java.security.KeyPairGenerator
import java.security.Principal
import java.security.PrivateKey
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.Nullable
import javax.annotation.PostConstruct


@Secured("isAnonymous()")
@Controller
class RegistrationController(private val accountRepo: AccountRepo, private val mapper: ObjectMapper, private val emailService: EmailService) {


    private val keyCache = buildKeyCache()
    private val rsaKeygen = KeyPairGenerator.getInstance("RSA")

    @Post("/signup/{token}", consumes = [MediaType.TEXT_PLAIN], produces = [MediaType.APPLICATION_JSON])
    fun signUp(@PathVariable token: String, @Body body: ByteArray, @Nullable principal: Principal?): Welcome {

        val json = decryptJson(body, token)

        keyCache.invalidateAll(mutableListOf(token))

        val sr: SignUpRequest = mapper.readValue(json, SignUpRequest::class.java)
        val emailToken = UUID.randomUUID().toString()

        emailService.sendMail(sr.name, sr.email, emailToken)

        return accountRepo.signup(sr, emailToken)
    }

    @Post("/signIn/{token}", consumes = [MediaType.TEXT_PLAIN], produces = [MediaType.APPLICATION_JSON])
    fun signIn(@PathVariable token: String, @Body body: ByteArray, @Nullable principal: Principal?): HttpResponse<*> {

        val json = decryptJson(body, token)
        keyCache.invalidateAll(mutableListOf(token))
        val sr: SignInRequest = mapper.readValue(json, SignInRequest::class.java)
        return ok(accountRepo.signIn(sr))
    }

    private fun decryptJson(body: ByteArray, token: String): String {
        val payload = Base64.getDecoder().decode(body)
        val privateKey = Option.fromNullable(keyCache.getIfPresent(token)).getOrElse { error("Session security was not established.") }
        return decrypt(payload, privateKey)
    }

    @Get("/confirm/{emailToken}")
    fun confirmEmail(@PathVariable emailToken: String): HttpResponse<*> = accountRepo
            .confirmEmail(emailToken).map { ok(it) }.getOrElse { badRequest("Email wasn't verified") }


    @Get("/reset/{token}", produces = [MediaType.APPLICATION_JSON])
    fun reset(@PathVariable token: String, session: RxWebSocketSession) = accountRepo.passwordReset(token)

    @Get("/cert", produces = [MediaType.APPLICATION_JSON])
    fun cert(session: Session): CertResponse {

        val keys = rsaKeygen.generateKeyPair()

        session.put("pk", keys.private)
        session.put("pub", keys.public)

        val publicKeyB64 = Base64.getEncoder().encodeToString(keys.public.encoded)
        val token = UUID.randomUUID().toString()
        keyCache.put(token, keys.private)

        return CertResponse(token, publicKeyB64)
    }

    @PostConstruct
    fun init() {
        rsaKeygen.initialize(4096)
    }

    private fun buildKeyCache(): Cache<String, PrivateKey> {
        return CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES).build()
    }
}



