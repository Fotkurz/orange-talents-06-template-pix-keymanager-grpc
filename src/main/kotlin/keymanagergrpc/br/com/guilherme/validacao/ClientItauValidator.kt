package keymanagergrpc.br.com.guilherme.validacao

import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientResponseException
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.client.dtos.ItauResponseDto
import keymanagergrpc.br.com.guilherme.handler.ValidacaoErpItauException

class ClientItauValidator(val clientErp: ClientItau) {

    fun buscaPorContaETipoNoItau(
        clientId: String, tipo: String,
    ): ItauResponseDto? {
        var respostaItau: HttpResponse<ItauResponseDto?>?
        try {
            respostaItau = clientErp.buscaContaETipo(clientId, tipo)
            if (respostaItau.body.isPresent) return respostaItau.body()
        } catch (e: HttpClientResponseException) {
            throw ValidacaoErpItauException("ID não encontrado")
        }
        return null
    }
}