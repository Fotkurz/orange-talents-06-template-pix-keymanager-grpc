package keymanagergrpc.br.com.guilherme.client

import io.micronaut.core.annotation.Introspected

@Introspected
data class ClientResponseDto(val tipo: String,
                             val titular: Map<String, String>) {


}
