package com.lifeway.bootiful.ddd.somethingfun

import com.lifeway.bootiful.ddd.utils.Json
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaMethod

data class EventMeta(val eventId: String = UUID.randomUUID().toString(), val aggregateId: String, val eventType: String, val sequence: Int? = null)

abstract class Event {
    abstract val meta: EventMeta
    abstract fun copy(
        aggregateId: String = this.meta.aggregateId,
        eventType: String = this.meta.eventType,
        sequence: Int? = this.meta.sequence,
        eventId: String = this.meta.eventId
    ): Event
}

data class CommandMeta(val commandId: String = UUID.randomUUID().toString(), val aggregateId: String, val commandType: String)
open class Command(aggregateId: String, commandType: String, commandId: String = UUID.randomUUID().toString()){
    val meta: CommandMeta = CommandMeta(commandId, aggregateId, commandType)
}

data class CommandResponse(val newState: AggregateState, val appliedEvents: List<Event>)


interface EventStore {
    fun getEvents(aggregateId: String): Flux<Event>
    fun saveEvent(event: Event): Mono<Unit>
}

interface EventBus

fun interface EventHandlerFn {
    fun handle(event: Event, state: AggregateState?): AggregateState
}

fun interface CommandHandlerFn {
    fun handle(command: Command, state: AggregateState?): List<Event>
}

abstract class AggregateState {
    abstract val id: String
    abstract val lastSequence: Int
    abstract val handledCommands: List<String>
    abstract val deleted: Boolean
    abstract fun copy(id: String = this.id, lastSequence: Int = this.lastSequence, handledCommands: List<String> = this.handledCommands, deleted: Boolean = this.deleted): AggregateState
}

abstract class Entity<T>(val id: String)


@kotlin.annotation.Target(AnnotationTarget.FUNCTION) annotation class CommandHandler
@kotlin.annotation.Target(AnnotationTarget.FUNCTION) annotation class EventSourcedHandler

private inline fun <reified T : List<Event>> mapReflectedMethodToCommandHandler(instance: Any, fn: KFunction<*>): CommandHandlerFn {
    if (fn.returnType.isSubtypeOf(T::class.starProjectedType)) {
        return CommandHandlerFn { e: Command, state ->
            fn.call(instance, e, state) as T
        }
    } else {
        throw Exception("Error mapping command handler function. Check return types.")
    }
}

private inline fun <reified T : AggregateState> mapReflectedFunctionEventHandler(instance: Any, fn: KFunction<*>): EventHandlerFn {
    if (fn.returnType.isSubtypeOf(T::class.starProjectedType)) {
        return EventHandlerFn { e: Event, state ->
            fn.call(instance, e, state) as T
        }
    } else {
        throw Exception("Error mapping event handler function. Check return types, must extend aggregate state.")
    }
}

abstract class Aggregate(id: String, private val eventStore: EventStore) : Entity<AggregateState>(id) {
    private val eventHandlers: MutableMap<String, EventHandlerFn> = mutableMapOf()
    private val commandHandlers: MutableMap<String, CommandHandlerFn> = mutableMapOf()

    init {
        scanHandlers()
    }

    private fun scanHandlers() {
        eventHandlers.putAll(
            this::class.functions
                .filter {
                    it.annotations.any { a -> a is EventSourcedHandler }
                }
                .filter {
                    it.parameters.size == 3 && it.parameters[1].type.isSubtypeOf(Event::class.starProjectedType) && it.parameters[2].type.withNullability(false).isSubtypeOf(AggregateState::class.starProjectedType)
                }
                .map {
                    val type = it.javaMethod!!.parameters[0].type.simpleName
                    val handler = mapReflectedFunctionEventHandler<AggregateState>(this, it)
                    type to handler
                }
                .toMap()
        )
        commandHandlers.putAll(
            this::class.functions
                .filter { it.annotations.any { a -> a is CommandHandler } }
                .filter {
                    it.parameters.size == 3 && it.parameters[1].type.isSubtypeOf(Command::class.starProjectedType) && it.parameters[2].type.withNullability(false).isSubtypeOf(AggregateState::class.starProjectedType)
                }
                .map {
                    val type = it.javaMethod!!.parameters[0].type.simpleName
                    val handler = mapReflectedMethodToCommandHandler<List<Event>>(this, it)
                    type to handler
                }
                .toMap()
        )
    }

    fun sendCommand(command: Command): Mono<CommandResponse> {
        return getState()
            .flatMap {
                val events = handleCommand(command, it.orElse(null))
                applyEvents(events, it.orElse(null))
            }
    }

    private fun getState(): Mono<Optional<AggregateState>> {
        return eventStore
            .getEvents(id)
            .sort { e1, e2 -> e1.meta.sequence!! - e2.meta.sequence!! }
            .collectList()
            .map {
                println(Json.serialize(it))
                if (it.size == 0) Optional.empty() else Optional.ofNullable(reduceToState(it))
            }
    }

    private fun reduceToState(events: List<Event>, state: AggregateState? = null): AggregateState? {

        if (events.isEmpty()) {
            return null
        }

        return events.fold(state) { accumulatedState, event -> handleEvent(event, accumulatedState).copy(lastSequence = event.meta.sequence!!) }
    }

    private fun handleEvent(event: Event, state: AggregateState?): AggregateState {
        val handler = eventHandlers[event.meta.eventType] ?: throw RuntimeException("No handler for type ${event.meta.eventType}.")
        return handler.handle(event, state)
    }

    private fun handleCommand(command: Command, state: AggregateState?): List<Event> {
        val handler = commandHandlers[command.meta.commandType] ?: throw RuntimeException("No handler for type ${command.meta.commandType}.")
        return handler.handle(command, state)
    }

    private fun applyEvents(events: List<Event>, state: AggregateState?): Mono<CommandResponse> {
        var seq = state?.lastSequence?.plus(1) ?: 1
        val sequencedEvents = events.map { it.copy(sequence = seq++) }

        return  Flux
            .fromIterable(sequencedEvents)
            .flatMap {
                eventStore.saveEvent(it).map{ _ -> it}
            }
            .collectList()
            .map {
                val newState = reduceToState(it, state)
                CommandResponse(newState!!, it)
            }
    }
}
