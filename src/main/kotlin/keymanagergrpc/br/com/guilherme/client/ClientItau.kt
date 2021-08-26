package keymanagergrpc.br.com.guilherme.client

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

@Client("\${clients.erpitau.url}")
interface ClientItau {

    @Get("/api/v1/clientes/{clienteId}/contas{?tipo}")
    fun buscaContaETipo(@PathVariable clienteId: String, @QueryValue tipo: String): HttpResponse<ClientResponseDto>

    fun buscaConta(@PathVariable clienteId: String): HttpResponse<ClientResponseDto>
}
