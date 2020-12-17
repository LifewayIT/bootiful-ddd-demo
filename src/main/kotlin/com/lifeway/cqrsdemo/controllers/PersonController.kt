package com.lifeway.cqrsdemo.controllers

import com.lifeway.cqrsdemo.aggregate.AddAddress
import com.lifeway.cqrsdemo.aggregate.CreatePerson
import com.lifeway.cqrsdemo.services.CommandService
import com.lifeway.cqrsdemo.services.CommandWrapper
import com.lifeway.cqrsdemo.views.PersonViewRepo
import com.lifeway.cqrsdemo.views.ProfileViewRepo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono

data class Result(val message: String?)
data class ErrorResponse(val message: String?)
data class CreatePersonRequest(val id: String, val firstName: String, val lastName: String)
data class CreateAddressRequest(val line1: String, val line2: String?)

@Configuration
@EnableWebFlux
class PersonRouter(private val personHandler: PersonHandler) {
    @Bean
    fun router(): RouterFunction<*> {
        return router {
            POST("/person", personHandler::createPerson)
            GET("/person/{id}", personHandler::getPerson)
            POST("/person/{id}/address", personHandler::createAddress)
            GET("/person/{id}/profile", personHandler::getProfile)
            GET("/") { ServerResponse.notFound().build() }
        }
    }
}

@Component
class PersonHandler(
    private val commandService: CommandService,
    private val personViewRepo: PersonViewRepo,
    private val profileViewRepo: ProfileViewRepo,
) {
    fun createPerson(request: ServerRequest): Mono<ServerResponse> {
        return request
            .bodyToMono(CreatePersonRequest::class.java)
            .flatMap {
                commandService.sendForResponse(CommandWrapper.from(
                        it.id,
                        CreatePerson(it.id, it.firstName, it.lastName)
                ))
            }
            .flatMap { ServerResponse.ok().bodyValue(it) }
            .onErrorResume {
                ServerResponse.status(500).bodyValue(ErrorResponse(it.message))
            }
    }

    fun getPerson(request: ServerRequest): Mono<ServerResponse> {
        val personId = request.pathVariable("id")
        return personViewRepo.findById(personId)
            .map {  ServerResponse.ok().bodyValue(it) }
            .orElseGet { ServerResponse.notFound().build() }
    }

    fun getProfile(request: ServerRequest): Mono<ServerResponse> {
        val personId = request.pathVariable("id")
        return profileViewRepo.findById(personId)
                .map {  ServerResponse.ok().bodyValue(it) }
                .orElseGet { ServerResponse.notFound().build() }
    }

    fun createAddress(request: ServerRequest): Mono<ServerResponse> {
        val personId = request.pathVariable("id")
        return request
            .bodyToMono(CreateAddressRequest::class.java)
            .flatMap {
                commandService.sendForResponse(CommandWrapper.from(
                    personId,
                    AddAddress(personId, it.line1, it.line2)
                ))
            }
            .flatMap { ServerResponse.ok().bodyValue(it) }
            .onErrorResume {
                ServerResponse.status(500).bodyValue(ErrorResponse(it.message))
            }
    }
}
