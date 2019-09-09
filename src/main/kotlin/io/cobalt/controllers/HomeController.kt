package io.cobalt.controllers

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import java.security.Principal

@Secured("isAuthenticated()")
@Controller
class HomeController {

    @Produces(MediaType.TEXT_PLAIN)
    @Get("/home")
    fun index(principal: Principal) : String {
        return "UserName: ${principal.name}"
    }

}