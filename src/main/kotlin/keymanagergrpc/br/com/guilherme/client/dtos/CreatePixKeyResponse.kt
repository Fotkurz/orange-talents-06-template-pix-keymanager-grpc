package keymanagergrpc.br.com.guilherme.client.dtos

import io.micronaut.core.annotation.Introspected

@Introspected
data class CreatePixKeyResponse(
    val keyType: String,
    var key: String,
    val bankAccount: Map<String, String>,
    val owner: Map<String, String>,
    val createdAt: String
    )

