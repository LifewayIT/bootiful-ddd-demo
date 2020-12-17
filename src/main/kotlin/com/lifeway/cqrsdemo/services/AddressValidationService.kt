package com.lifeway.cqrsdemo.services

import com.lifeway.cqrsdemo.aggregate.Address
import com.lifeway.cqrsdemo.aggregate.ValidationStatus.VALID
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

data class ValidateAddressRequest(val addressId: String, val line1: String, val line2: String?)

interface AddressValidationService {
    fun validate(req: ValidateAddressRequest): Mono<Address>
}

@Component
class AddressValidationServiceImpl() : AddressValidationService {
    override fun validate(req: ValidateAddressRequest): Mono<Address> {
        val address = Address(req.addressId, req.line1, req.line2, VALID)
        return Mono.just(address)
    }
}
