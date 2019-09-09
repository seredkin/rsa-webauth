package io.cobalt.services;

import io.micronaut.security.authentication.providers.PasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import javax.inject.Singleton

@Singleton 
class BCryptPasswordEncoderService: PasswordEncoder {

    private val delegate =  BCryptPasswordEncoder()

    override fun encode(rawPassword:String):String = delegate.encode(rawPassword)

    override fun matches(rawPassword:String, encodedPassword:String):Boolean =
            delegate.matches(rawPassword, encodedPassword)
}