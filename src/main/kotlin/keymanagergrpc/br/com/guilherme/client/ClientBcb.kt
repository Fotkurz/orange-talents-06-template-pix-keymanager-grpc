package keymanagergrpc.br.com.guilherme.client

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
import keymanagergrpc.br.com.guilherme.client.dtos.CreatePixKeyRequest
import keymanagergrpc.br.com.guilherme.client.dtos.CreatePixKeyResponse
import keymanagergrpc.br.com.guilherme.client.dtos.PixKeyDetailsResponse
import keymanagergrpc.br.com.guilherme.client.dtos.DeletePixKeyRequest

@Client("\${clients.bcb.url}")
interface ClientBcb {

    @Post("/api/v1/pix/keys", produces = [MediaType.APPLICATION_XML], processes = [MediaType.APPLICATION_XML])
    fun cadastraChave(@Body cadastraPixDto: CreatePixKeyRequest): HttpResponse<CreatePixKeyResponse>

    @Get("/api/v1/pix/keys/{key}", produces = [MediaType.APPLICATION_XML], processes = [MediaType.APPLICATION_XML])
    fun consultaChave(@PathVariable key: String): HttpResponse<PixKeyDetailsResponse>

    @Delete("/api/v1/pix/keys/{key}", produces = [MediaType.APPLICATION_XML], processes = [MediaType.APPLICATION_XML])
    fun deletaChave(@PathVariable key: String, @Body deletePixKeyRequest: DeletePixKeyRequest): HttpResponse<Map<String, String>>

    // TODO: Criar a função de consultar chave

}