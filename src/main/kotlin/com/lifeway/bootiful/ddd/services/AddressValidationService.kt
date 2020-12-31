package com.lifeway.bootiful.ddd.services

import com.lifeway.bootiful.ddd.aggregate.Address
import com.lifeway.bootiful.ddd.aggregate.ValidationStatus.VALID
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

data class ValidateAddressRequest(val addressId: String, val line1: String, val line2: String?)

interface AddressValidationService {
    fun validate(req: ValidateAddressRequest): Mono<Address>
}

@Component
class DefaultAddressValidationService() : AddressValidationService {
    override fun validate(req: ValidateAddressRequest): Mono<Address> {
        val address = Address(req.addressId, req.line1, req.line2, VALID)
        return Mono.just(address)
    }
}
