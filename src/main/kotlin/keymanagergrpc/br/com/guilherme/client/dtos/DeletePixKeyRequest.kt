package keymanagergrpc.br.com.guilherme.client.dtos

import io.micronaut.core.annotation.Introspected

@Introspected
data class DeletePixKeyRequest(
    val key: String,
    val participant: String
)