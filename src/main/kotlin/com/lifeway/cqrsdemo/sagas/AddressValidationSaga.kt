package com.lifeway.cqrsdemo.sagas

import com.lifeway.cqrsdemo.domain.*
import com.lifeway.cqrsdemo.services.AddressValidationService
import com.lifeway.cqrsdemo.services.ValidateAddressRequest
import com.lifeway.cqrsdemo.views.PersonViewHandler
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.eventhandling.GenericEventMessage
import org.axonframework.modelling.saga.SagaEventHandler
import org.axonframework.modelling.saga.SagaLifecycle
import org.axonframework.modelling.saga.StartSaga
import org.axonframework.spring.stereotype.Saga
import reactor.core.publisher.Mono

import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.axonframework.messaging.Message
import org.axonframework.messaging.interceptors.MessageHandlerInterceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired


@Saga
class AddressValidationSaga {

    @Autowired @Transient private val addressValidationService: AddressValidationService? = null
    @Autowired @Transient private val commandGateway: ReactorCommandGateway? = null

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    private var status: ValidationStatus = ValidationStatus.PENDING

    @StartSaga
    @SagaEventHandler(associationProperty = "addressId")
    fun handle(event: AddressAdded) {
        addressValidationService!!
            .validate(ValidateAddressRequest(event.address.addressId, event.address.line1, event.address.line2))
            .flatMap {
                status = it.validationStatus
                commandGateway!!.send<Unit>(
                    when(status) {
                        ValidationStatus.VALID -> ConfirmAddress(event.personId, it)
                        else -> InvalidateAddress(event.personId, it)
                    }
                )
            }
            .subscribe()
    }

    @SagaEventHandler(associationProperty = "addressId")
    fun handle(event: AddressValidated) {
        status = event.address.validationStatus
//        SagaLifecycle.end()
    }

    @SagaEventHandler(associationProperty = "addressId")
    fun handle(event: AddressInvalidated) {
        status = event.address.validationStatus
//        SagaLifecycle.end()
    }
}