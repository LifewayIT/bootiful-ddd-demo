package com.lifeway.cqrsdemo.services

import com.lifeway.cqrsdemo.domain.AddAddress
import com.lifeway.cqrsdemo.utils.Json
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.CompletableFuture

data class CommandWrapper (
    val id: String = UUID.randomUUID().toString().toLowerCase(),
    val payload: String,
    val commandType: String,
    val shardKey: String,
    val awaitingReply: Boolean = false,
) {
    companion object {
        fun <T: Any> from(key: String, t:T): CommandWrapper = CommandWrapper(
            payload = Json.serialize(t),
            commandType = t::class.java.simpleName,
            shardKey = key
        )
    }
}

data class MessageResponse (
    val commandId: String? = null,
    val result: Any? = null,
    val message: String? = null,
    val successful: Boolean? = true
)

interface CommandService {
    fun send(command: CommandWrapper): Mono<Unit>
    fun sendForResponse(command: CommandWrapper): Mono<MessageResponse>
    fun commitResponse(response: MessageResponse)
}

@Component
class DefaultCommandService(private val kafkaTemplate: KafkaTemplate<String, String>): CommandService {

    private val responseRegistry: MutableMap<String, CompletableFuture<MessageResponse>> = mutableMapOf()

    override fun send(command: CommandWrapper): Mono<Unit> {
        return Mono
            .fromFuture(
                kafkaTemplate.send("commands", command.shardKey, Json.serialize(command)).completable()
            )
            .map { Unit }
    }

    override fun sendForResponse(command: CommandWrapper): Mono<MessageResponse> {
        val promise = CompletableFuture<MessageResponse>()
        responseRegistry[command.id] = promise
        return Mono
            .fromFuture(
                kafkaTemplate.send("commands", command.shardKey, Json.serialize(command.copy(awaitingReply = true))).completable()
            )
            .flatMap { Mono.fromFuture(promise) }
    }

    override fun commitResponse(response: MessageResponse) {
        val promise = responseRegistry[response.commandId]
        promise?.complete(response)
        responseRegistry.remove(response.commandId)
    }
}
