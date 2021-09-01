package keymanagergrpc.br.com.guilherme.validacao

import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientResponseException
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.dtos.ItauResponseDto
import keymanagergrpc.br.com.guilherme.client.dtos.PixKeyDetailsResponse
import keymanagergrpc.br.com.guilherme.interceptor.ExistenciaDeChaveException
import keymanagergrpc.br.com.guilherme.interceptor.ValidacaoBcbException
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.client.dtos.DeletePixKeyRequest
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import org.slf4j.LoggerFactory

class ClientBcbValidator(val clientBcb: ClientBcb) {

    val LOGGER = LoggerFactory.getLogger(this.javaClass)

    fun cadastraChaveNoBcb(
        respostaItau: ItauResponseDto?,
        novaChave: ChavePix,
    ): ChavePix? {
        if (respostaItau != null) {
            val requestBcb = respostaItau.toBcb(novaChave.chave, novaChave.tipoChave.toString())
            try {
                val retorno = clientBcb.cadastraChave(requestBcb!!)
                LOGGER.info(retorno.body.toString())
                if (novaChave.tipoChave.equals(TipoChave.RANDOM))
                    if (retorno.body() != null) {
                        novaChave.chave = retorno.body().key
                    }
            } catch (e: HttpClientResponseException) {
                throw ValidacaoBcbException("Falha no cadastro da chave no Banco Central")
            }
        }
        return null
    }

    fun deletaChaveNoBcb(chave: ChavePix, ispb: String) {
        val deletePixKeyRequest = DeletePixKeyRequest(chave.chave.toString(), ispb)
        try {
            val respostaBcb = clientBcb.deletaChave(chave.chave.toString(), deletePixKeyRequest)
        } catch (e: HttpClientResponseException) {
            throw ValidacaoBcbException("Chave Pix não encontrada no sistema BCB")
        }
    }

    fun consultaChaveNoBcb(
        chave: String
    ): PixKeyDetailsResponse {
        try {
            val httpResponse: HttpResponse<PixKeyDetailsResponse>
            httpResponse = clientBcb.consultaChave(chave)
            LOGGER.info("Response BCB: ${httpResponse.getBody()}")
            return httpResponse.getBody().get()
        } catch (e: HttpClientResponseException) {
            throw ExistenciaDeChaveException("Chave não cadastrada no BCB")
        }
    }

}