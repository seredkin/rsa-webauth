package io.cobalt.services

import io.micronaut.context.annotation.Value
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.Mailer
import org.simplejavamail.mailer.config.ServerConfig
import org.simplejavamail.mailer.config.TransportStrategy
import javax.inject.Singleton

@Singleton
class EmailService {
    @Value("\${mail.smtp.host}")
    lateinit var smtpHost: String
    @Value("\${mail.smtp.port}")
    lateinit var smtpPort: String
    @Value("\${webauth.host}")
    lateinit var webauthHost:String

    fun sendMail(nameTo:String, emailTo:String, token:String) {
        val email = EmailBuilder()
                .from("Cobalt Platform Account Registration", "no-reply@cobalt.io")
                .subject("Welcome to Cobalt.IO Platform!")
                .to(nameTo, emailTo)
                .text("Proceed to this link $webauthHost/confirm/${token}").build()

        val session = Mailer.createMailSession(ServerConfig("localhost", 1025), TransportStrategy.SMTP_PLAIN)

        Mailer(session).sendMail(email)
    }


}