# bootiful-ddd-demo
A Demo Application applying the principals of Domain Driven Design using the Springboot ecosystem, Reactor Core, and Axon Framework (CQRS/ES).

### Running locally... 🏃

You will need:

```
Docker 18+
Java 11
Gradle
```

#### **Step 1** - Stand up your deps. 

`docker-compose up --build`

This command stands up mongo, redis, and kafka. If you want to run them headlessly simply use the `-d` flag.

#### **Step 2** - Build jar from source.

`gradlew bootJar`

This gradle command builds an executable jar and places it in the `./build/libs/` directory for you. 


### **Step 3** - Run jar with server your desired server options.

`java -jar ./build/libs/bootiful-ddd-0.0.1-SNAPSHOT.jar --server.port=8080 --spring.profiles.active=local`


*Alternatively, you can replace steps 2 and 3 with the springboot runner in IntelliJ if you have it.*

---
### Docs 🗞

![High Level Architecture](https://github.com/LifewayIT/bootiful-ddd-demo/blob/master/architecture/component_high_level.png?raw=true)

An explanation of each of the components pictured is as follows:

`Aggregate Root` - the encapsulation of business processes and events that, in sequence, “aggregate” into a state of the domain model. “Commands” can be sent to the aggregate that are validated and translated into a sequence of “Events” according to the business logic of the domain. All state consistency, ordering of events, and commands are handled by the AR.

`Event Store` - used by the Aggregate Root to persist new events in order and fetch events that have previously been dispatched. This is the central and critical “data layer” for a CQRS/ES implementation.

`Saga` - a downstream consumer of domain events that is responsible for some asynchronous business process that maintains its own state. These processes should consume an event, change its own state, potentially perform a side effect based on this state, and emit commands back to the Aggregate Root depending on the results of all of these actions. It can be viewed as a Finite State Machine (FSM) encapsulating a business process.

`View Handlers` - a downstream consumer of domain events that updates various repositories used for read operations by clients. These processes should be repeatable, pure functions that will always result in the same state for a given VIew Projection (VP).

`View Projections` - a data layer built to optimize the “read” side of the domain. Should be easily rebuildable by events from the Aggregate Root via the View Handlers.

`Query Handler` - a series of HTTP controllers (REST, gQL, etc) that can query a model held within View Projections of the domain.

`External Event Adapter` - consumer of events from external systems/domains that either adapts them into commands for an Aggregate Root or state updates for a view projection dependent on external data. (Note: any external data needs to be consistently established as a part of the view handler. If that is not maintained by the view handlers, there is significant risk in not being able to rebuild views on demand.) Additionally, this component is responsible for adapting domain events for external consumption in the enterprise.


---
### TODO
- [x] - Event sourced aggregate
- [x] - View Handlers and View Projections
- [x] - Query Handler
- [x] - Saga
- [x] - Event Adapter Example
- [x] - Command Response using Redis
