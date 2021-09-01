package keymanagergrpc.br.com.guilherme.validacao

import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientResponseException
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.client.dtos.ItauResponseDto
import keymanagergrpc.br.com.guilherme.interceptor.ExistenciaDeChaveException
import keymanagergrpc.br.com.guilherme.interceptor.ValidacaoErpItauException

class ClientItauValidator(val clientItau: ClientItau) {

    fun buscaPorContaETipoNoItau(
        clientId: String, tipoConta: String,
    ): ItauResponseDto? {
        var respostaItau: HttpResponse<ItauResponseDto?>?
        try {
            respostaItau = clientItau.buscaContaETipo(clientId, tipoConta)
            if (respostaItau.body.isPresent) return respostaItau.body()
        } catch (e: HttpClientResponseException) {
            throw ValidacaoErpItauException("Client inexistente no sistema do itau")
        }
        return null
    }

    fun checaExistenciaDeClienteNoItau(
        clientId: String
    ): Boolean {

        try {
            val resposta = clientItau.buscaPorClienteId(clientId)
            if(resposta != null) {
                return true
            }
            return false
        } catch(e: HttpClientResponseException) {
            throw ValidacaoErpItauException("Client inexistente no sistema do itau")
        }
    }

}