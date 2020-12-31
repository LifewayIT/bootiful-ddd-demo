package com.lifeway.bootiful.ddd.somethingfun

import com.lifeway.bootiful.ddd.utils.Json
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.test.Test

class FakeEventStore: EventStore {

    private val events: MutableList<Event> = mutableListOf()

    override fun getEvents(aggregateId: String): Flux<Event> {
        return Flux.fromIterable(events)
    }

    override fun saveEvent(event: Event): Mono<Unit> {
        events.add(event)
        return Mono.just(Unit)
    }
}

abstract class Yep(val foo: String)

data class CreateExample(val exampleId: String, val foo: String) : Command(exampleId, "CreateExample")

data class ExampleCreated(
    val exampleId: String, val foo: String, override val meta: EventMeta = EventMeta(aggregateId = exampleId, eventType = "ExampleCreated")) : Event() {
    override fun copy(aggregateId: String, eventType: String, sequence: Int?, eventId: String): ExampleCreated =
        copy(meta = meta.copy(eventId, aggregateId, eventType, sequence))
}

data class ExampleState(
    override val id: String, override val lastSequence: Int = 0, override val handledCommands: List<String> = listOf(), override val deleted: Boolean = false,
    val foo: String,
): AggregateState() {
    override fun copy(id: String, lastSequence: Int, handledCommands: List<String>, deleted: Boolean): AggregateState =
            copy(id = id, lastSequence = lastSequence, handledCommands = handledCommands, deleted=deleted, foo=foo)
}

class ExampleAggregate(id: String) : Aggregate(id, eventStore = FakeEventStore()) {
    @CommandHandler
    fun handle(command: CreateExample, state: ExampleState?): List<Event> {
        val event = ExampleCreated(command.exampleId, command.foo)
        return listOf(event)
    }

    @EventSourcedHandler
    fun handle(event: ExampleCreated, state: ExampleState?): ExampleState {
        return state?.copy(foo = event.foo) ?: ExampleState(id, foo = event.foo)
    }
}

class FunTest {
    @Test
    fun testExample()  {
        val aggregate = ExampleAggregate("id")
        val res = aggregate
            .sendCommand(CreateExample("id", "foo"))
            .block()
        println(Json.serialize(res!!))
    }
}
