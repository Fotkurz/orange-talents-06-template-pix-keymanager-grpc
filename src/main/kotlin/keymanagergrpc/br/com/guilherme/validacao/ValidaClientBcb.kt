package keymanagergrpc.br.com.guilherme.validacao

import io.micronaut.http.client.exceptions.HttpClientResponseException
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.ItauResponseDto
import keymanagergrpc.br.com.guilherme.handler.ValidacaoBcbException
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave

class ValidaClientBcb(val clientBcb: ClientBcb) {

    fun cadastraChaveNoBcb(
        respostaItau: ItauResponseDto?,
        novaChave: ChavePix,
    ): ChavePix? {
        if (respostaItau != null) {
            val requestBcb = respostaItau.toBcb(novaChave.chave, novaChave.tipoChave.toString())
            try {
                val retorno = clientBcb.cadastraChave(requestBcb!!)
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

    fun deletaChaveNoBcb(chave: ChavePix) {
        try {
            val respostaBcb = clientBcb.deletaChave(chave.pixId)
        } catch (e: HttpClientResponseException) {
            throw ValidacaoBcbException("Chave Pix não encontrada no sistema BCB")
        }
    }

}