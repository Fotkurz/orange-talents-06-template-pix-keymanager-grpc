package keymanagergrpc.br.com.guilherme.client.dtos

import io.micronaut.core.annotation.Introspected
import keymanagergrpc.br.com.guilherme.modelo.TipoConta

@Introspected
data class ItauResponseDto(
    val tipo: String,
    val instituicao: Instituicao,
    val titular: Titular,
    val agencia: String,
    val numero: String
) {

    private val ispbItau: String = "60701190"

    fun toBcb(chave: String?, tipoChave: String): CreatePixKeyRequest {

        return CreatePixKeyRequest(
            keyType = tipoChave,
            key = chave,
            bankAccount = mapOf(
                Pair("participant", ispbItau),
                Pair("branch", agencia),
                Pair("accountNumber", numero),
                Pair("accountType", TipoConta.valueOf(tipo).sigla.toString()),
            ),
            owner = mapOf(
                Pair("type", "NATURAL_PERSON"),
                Pair("name", this.titular.nome),
                Pair("taxIdNumber", this.titular.cpf)
            )
        )
    }

}
@Introspected
data class Instituicao(val nome: String, val ispb: String) {

}
@Introspected
data class Titular(val id: String, val nome: String, val cpf: String) {
}