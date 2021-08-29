package keymanagergrpc.br.com.guilherme.endpoints

import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import keymanagergrpc.br.com.guilherme.CreateKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.ExcludeKeyRequest
import keymanagergrpc.br.com.guilherme.ExcludeKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.client.dtos.*
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.modelo.TipoConta
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class DeletaChaveEndpointTest {

    @Inject
    lateinit var grpcClient: ExcludeKeyServiceGrpc.ExcludeKeyServiceBlockingStub

    @Inject
    lateinit var grpcCadastra: CreateKeyServiceGrpc.CreateKeyServiceBlockingStub

    @Inject
    lateinit var keyRepository: KeyRepository

    @Inject
    lateinit var clientItau: ClientItau

    @Inject
    lateinit var clientBcb: ClientBcb

    @BeforeEach
    internal fun setUp() {
        keyRepository.deleteAll()
    }

    /*
    CENARIO: Ok - Deve deletar corretamente mediante clientId e pixId válidos e cadastradas no ITAU e BCB
    CENARIO: OK - Deve falhar ao tentar deletar chavePix com clientId Inválido (Inexistente no itau)
    CENARIO: OK - Deve falhar ao tentar deletar chavePix com pixId inválido (inexistente)
    CENARIO: OK - Deve ter sucesso ao remover uma chave mockando o client itau com cliente valido
    CENARIO: OK - Deve falhar ao remover uma chave mockando o client itau com cliente invalido
    CENARIO: OK - Deve falhar ao tentar remover uma chave nao cadastrada no sistema do BCB
     */

    @Test
    internal fun deveDeletarCorretamentePixidDoBanco() {

        Mockito.`when`(clientItau.buscaContaETipo("1", "CONTA_CORRENTE"))
            .thenReturn(
                HttpResponse.ok(
                    ItauResponseDto(
                        tipo = "CONTA_CORRENTE",
                        instituicao = Instituicao(nome = "ITAU", ispb = "1111"),
                        titular = Titular(id = "1", nome = "Tolkien", cpf = "12345678912"),
                        agencia = "0001",
                        numero = "1212"
                    )
                )
            )

        val chave = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678912",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        Mockito.`when`(clientBcb.deletaChave(key = chave.pixId))
            .thenReturn(
                HttpResponse.ok(mapOf(
                    Pair("key", chave.pixId),
                    Pair("participant", "1111")
                ))
            )

        keyRepository.save(chave)

        grpcClient.exclui(
            ExcludeKeyRequest
            .newBuilder()
            .setPixid(chave.pixId)
            .setClientid("1")
            .build()
        )

        val lista = keyRepository.findAll()

        assertTrue(lista.toList().isEmpty())
    }

    @Test
    internal fun deveFalharAoTentarDeletarChavePixComClientIdInvalidoEChaveValida() {
        val chave1 = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678910",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(chave1)

        val keyBuscada = keyRepository.findByClientId("1")

        val erro = assertThrows<StatusRuntimeException> {
        grpcClient.exclui(ExcludeKeyRequest
            .newBuilder()
            .setPixid(keyBuscada[0].pixId.toString())
            .setClientid("invalido")
            .build())
        }

        val lista = keyRepository.findAll()

        assertEquals(1, lista.count())
        assertEquals("12345678910", lista.first().chave)
    }

    @Test
    internal fun deveFalharAoTentarExcluirUmaChaveComPixIdInvalidoEClientIdValido() {
        val chave1 = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678910",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(chave1)

        val keyBuscada = keyRepository.findByClientId("1")

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.exclui(ExcludeKeyRequest
                .newBuilder()
                .setPixid(UUID.randomUUID().toString())
                .setClientid("1")
                .build())
        }

        val lista = keyRepository.findAll()

        assertEquals(1, lista.count())
        assertEquals("12345678910", lista.first().chave)
    }

    @Test
    internal fun deveFalharAoTentarExcluirUmaChaveCujoClientIdNaoExistaNoItau() {

        val chave = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678912",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(chave)

        Mockito.`when`(clientItau.buscaContaETipo("1", "CONTA_CORRENTE"))
            .thenThrow(HttpClientResponseException::class.java)

        val error = assertThrows<StatusRuntimeException> {
        grpcClient.exclui(ExcludeKeyRequest.newBuilder()
            .setPixid(chave.pixId)
            .setClientid("CONTA_CORRENTE")
            .build()
        )}

        val lista = keyRepository.findAll()

        assertTrue(lista.count() == 1)
    }

    @Test
    internal fun deveFalharAoTentarDeletarChaveNaoCadastradaNoBcb() {
        val chave = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678912",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(chave)

        Mockito.`when`(clientBcb.deletaChave(key = chave.pixId))
            .thenThrow(HttpClientResponseException::class.java)

        Mockito.`when`(clientItau.buscaContaETipo("1", "CONTA_CORRENTE"))
            .thenReturn(
                HttpResponse.ok(
                    ItauResponseDto(
                        tipo = "CONTA_CORRENTE",
                        instituicao = Instituicao(nome = "ITAU", ispb = "1111"),
                        titular = Titular(id = "1", nome = "Tolkien", cpf = "12345678912"),
                        agencia = "0001",
                        numero = "1212"
                    )
                )
            )

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.exclui(
                ExcludeKeyRequest
                    .newBuilder()
                    .setPixid(chave.pixId)
                    .setClientid("1")
                    .build()
            )
        }

    }

    fun criaPixResponseKeyParaBcb(chave: String, tipo: String): CreatePixKeyResponse {
        return CreatePixKeyResponse(
            keyType = tipo,
            key = chave,
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

    fun criaResponseParaItau(tipo: String): ItauResponseDto {
        return ItauResponseDto(
            tipo = tipo,
            instituicao = Instituicao(nome = "ITAU", ispb = "60701190"),
            titular = Titular(id = "1", nome = "Tolkien", cpf = "12345678912"),
            agencia = "0001",
            numero = "123456"
        )
    }

    fun criaPixRequestKeyParaBcb(key: String, keyType: String): CreatePixKeyRequest {
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

    fun mockaRequisicaoItauComSucesso(
        clientId: String,
        tipo: String
    ): OngoingStubbing<HttpResponse<ItauResponseDto?>>? {
        return Mockito.`when`(clientItau.buscaContaETipo(clientId, tipo))
    }

    fun mockaCreateRequisicaoBcb(requestBcb: CreatePixKeyRequest): OngoingStubbing<HttpResponse<CreatePixKeyResponse>>? {
        return Mockito.`when`(clientBcb.cadastraChave(requestBcb))
    }
    fun mockaDeleteChaveBcb(key: String): OngoingStubbing<HttpResponse<Map<String, String>>>? {
        return Mockito.`when`(clientBcb.deletaChave(key))
    }

    @MockBean(ClientItau::class)
    fun clientItau(): ClientItau? {
        return Mockito.mock(ClientItau::class.java)
    }

    @MockBean(ClientBcb::class)
    fun clientBcb(): ClientBcb?{
        return Mockito.mock((ClientBcb::class.java))
    }

    @Factory
    class ExcludeClient {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): ExcludeKeyServiceGrpc.ExcludeKeyServiceBlockingStub? {
            return ExcludeKeyServiceGrpc.newBlockingStub(channel)
        }
    }
}