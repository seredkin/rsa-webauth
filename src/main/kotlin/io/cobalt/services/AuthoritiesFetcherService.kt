package io.cobalt.services;

import io.cobalt.webauth.data.AccountRepo
import io.micronaut.security.authentication.providers.AuthoritiesFetcher
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import javax.inject.Singleton

@Singleton
class AuthoritiesFetcherService(private val repo: AccountRepo) : AuthoritiesFetcher {
    override fun findAuthoritiesByUsername(username: String): Publisher<List<String>> {
        return Flowable.just(listOf("ROLE_USER"))//TODO Anton fetch roles
    }
}