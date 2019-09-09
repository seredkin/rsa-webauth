package io.cobalt.services;

import arrow.core.Option
import arrow.core.getOrElse
import io.cobalt.webauth.data.AccountRepo
import io.micronaut.security.authentication.providers.UserFetcher
import io.micronaut.security.authentication.providers.UserState
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import javax.inject.Singleton

@Singleton
class UserFetcherService(private val repo: AccountRepo) : UserFetcher {
    override fun findByUsername(username: String): Publisher<UserState> {
        val user: Option<UserState> = repo.findByEmail(username)
        return user.map { Flowable.just(it) }.getOrElse { Flowable.empty() }
    }
}