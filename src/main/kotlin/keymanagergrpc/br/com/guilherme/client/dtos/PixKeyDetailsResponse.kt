package keymanagergrpc.br.com.guilherme.client.dtos

import io.micronaut.core.annotation.Introspected
import keymanagergrpc.br.com.guilherme.AccountType
import keymanagergrpc.br.com.guilherme.CheckKeyResponse
import keymanagergrpc.br.com.guilherme.KeyType
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.time.LocalDate

@Introspected
data class PixKeyDetailsResponse(
    val keyType: String,
    val key: String,
    val bankAccount: Map<String, String>,
    val owner: Map<String, String>,
    val createdAt: String
){

    fun toResponse(): CheckKeyResponse? {

        val nomeInstituicao = bankAccount["participant"]?.let { nomeInstituicao(it) }

        if(nomeInstituicao != null) {
            return CheckKeyResponse.newBuilder()
                .setKey(key)
                .setKeytype(KeyType.valueOf(keyType))
                .setNometitular(owner["name"])
                .setCpftitular(owner["taxIdNumber"])
                .setInstituicao(
                    CheckKeyResponse.Instituicao.newBuilder()
                        .setNome(bankAccount["participant"])
                        .setNumero(bankAccount["accountNumber"])
                        .setAccounttype(AccountType.CONTA_CORRENTE)
                        .build()
                )
                .setDatahora(LocalDate.now().toString())
                .build()

        }
        return null

    }

    fun nomeInstituicao(ispb: String): String? {
            var linha: String
            val ispbListFile = "ParticipantesSTRport.csv"

            try {
                val br = BufferedReader(FileReader(ispbListFile))
                while (br.readLine().also {
                        linha = it
                    } != null) {
                    val colunas = linha.split(",")
                    if(colunas.contains(ispb)) {
                        return colunas[1]
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        return null
    }


}
