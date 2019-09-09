package io.cobalt

import io.micronaut.runtime.Micronaut

object WebAuthService {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("io.cobalt")
                .mainClass(WebAuthService.javaClass)
                .start()
    }
}