package keymanagergrpc.br.com.guilherme.client

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import keymanagergrpc.br.com.guilherme.CreatePixKeyRequest

@Client("\${clients.bcb.url}")
interface ClientBcb {

    @Post("/api/v1/pix/keys", produces = [MediaType.APPLICATION_XML], processes = [MediaType.APPLICATION_XML])
    fun cadastraChave(@Body cadastraPixDto: CreatePixKeyRequest): HttpResponse<CreatePixKeyResponse>

    @Get("/api/v1/pix/keys/{key}", produces = [MediaType.APPLICATION_XML], processes = [MediaType.APPLICATION_XML])
    fun deletaChave(@PathVariable key: String): HttpResponse<Map<String, String>>
}