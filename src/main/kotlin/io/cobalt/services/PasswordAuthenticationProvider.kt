package io.cobalt.services

import io.cobalt.webauth.data.AccountRepo
import io.micronaut.security.authentication.AuthenticationFailed
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import javax.inject.Singleton

@Singleton
class PasswordAuthenticationProvider(private val repo: AccountRepo) : AuthenticationProvider {
    override fun authenticate(ar: AuthenticationRequest<*, *>?): Publisher<AuthenticationResponse> {
        return if (ar != null && ar.identity != null && ar.secret != null) {
            Flowable.just(repo.authenticate(ar))
        } else Flowable.just(AuthenticationFailed())
    }
}