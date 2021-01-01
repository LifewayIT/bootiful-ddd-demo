//package com.lifeway.bootiful.ddd.utils
//
//import org.axonframework.eventsourcing.eventstore.EventStore
//import org.springframework.stereotype.Component
//import reactor.core.publisher.Flux
//
//@Component
//class EventReplayer(private val eventStore: EventStore) {
//
//    fun replay() {
//        val stream = eventStore.openStream(eventStore.createTailToken()).asStream()
//        Flux.fromStream(stream).subscribe {
//            println(it.identifier)
//        }
//    }
//
//}
