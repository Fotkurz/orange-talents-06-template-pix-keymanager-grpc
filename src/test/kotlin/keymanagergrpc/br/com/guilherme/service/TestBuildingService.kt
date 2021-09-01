package keymanagergrpc.br.com.guilherme.service

import io.micronaut.http.HttpResponse
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.client.dtos.*
import keymanagergrpc.br.com.guilherme.client.dtos.DeletePixKeyRequest
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestBuildingService {

        @Inject
        lateinit var clientBcb: ClientBcb

        @Inject
        lateinit var clientItau: ClientItau

        fun createDeleteKeyPixRequest(key: String): DeletePixKeyRequest {
                return DeletePixKeyRequest(key, "60701190")
            }

        fun createPixResponseKeyParaBcb(key: String, keyType: String): CreatePixKeyResponse {
            return CreatePixKeyResponse(
                keyType = keyType,
                key = key,
                bankAccount = mapOf(
                    Pair("participant", "60701190"),
                    Pair("branch", "0001"),
                    Pair("accountNumber", "123456"),
                    Pair("accountType", "CACC")
                ),
                owner = mapOf(
                    Pair("type", "NATURAL_PERSON"),
                    Pair("name", "Tolkien"),
                    Pair("taxIdNumber", "12345678912")
                ),
                createdAt = LocalDateTime.now().toString()
            )
        }

        fun createResponseParaItau(tipoConta: String): ItauResponseDto {
            return ItauResponseDto(
                tipo = tipoConta,
                instituicao = Instituicao(nome = "ITAU", ispb = "1111"),
                titular = Titular(id = "cb3a88ca-eb05-41f3-9871-b18ace28ee40", nome = "Tolkien", cpf = "12345678912"),
                agencia = "0001",
                numero = "121212"
            )
        }

        fun createPixKeyDetailsResponse(key: String, keyType: String): PixKeyDetailsResponse {
            return PixKeyDetailsResponse(
                keyType = keyType,
                key = key,
                bankAccount = mapOf(
                    Pair("participant", "60701190"),
                    Pair("branch", "0001"),
                    Pair("accountNumber", "123456"),
                    Pair("accountType", "CACC")
                ),
                owner = mapOf(
                    Pair("type", "NATURAL_PERSON"),
                    Pair("name", "Tolkien"),
                    Pair("taxIdNumber", "12345678912")
                ),
                createdAt = LocalDate.now().toString()
            )
        }

        fun createPixRequestKeyParaBcb(key: String, keyType: String): CreatePixKeyRequest {
            return CreatePixKeyRequest(
                keyType = keyType,
                key = key,
                bankAccount = mapOf(
                    Pair("participant", "60701190"),
                    Pair("branch", "0001"),
                    Pair("accountNumber", "123456"),
                    Pair("accountType", "CACC")
                ),
                owner = mapOf(
                    Pair("type", "NATURAL_PERSON"),
                    Pair("name", "Tolkien"),
                    Pair("taxIdNumber", "12345678912")
                )
            )
        }

        fun mockaConsultaChaveBcb(key: String): OngoingStubbing<HttpResponse<PixKeyDetailsResponse>>? {
            return Mockito.`when`(clientBcb.consultaChave(key))
        }

        fun mockaRequisicaoItau(clientId: String, tipoConta: String): OngoingStubbing<HttpResponse<ItauResponseDto?>>? {
            return Mockito.`when`(clientItau.buscaContaETipo(clientId, tipoConta))
        }

        fun mockaCreateRequisicaoBcb(requestBcb: CreatePixKeyRequest): OngoingStubbing<HttpResponse<CreatePixKeyResponse>>? {
            return Mockito.`when`(clientBcb.cadastraChave(requestBcb))
        }

        fun mockaDeleteChaveBcb(key: String, deletePixKeyRequest: DeletePixKeyRequest): OngoingStubbing<HttpResponse<Map<String, String>>>? {
        return Mockito.`when`(clientBcb.deletaChave(key, deletePixKeyRequest))
    }


}