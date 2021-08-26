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
import keymanagergrpc.br.com.guilherme.ExcludeKeyRequest
import keymanagergrpc.br.com.guilherme.ExcludeKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.client.ClientResponseDto
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.modelo.TipoConta
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class DeletaChaveEndpointTest {

    @Inject
    lateinit var grpcClient: ExcludeKeyServiceGrpc.ExcludeKeyServiceBlockingStub

    @Inject
    lateinit var keyRepository: KeyRepository

    @Inject
    lateinit var clientItau: ClientItau

    @BeforeEach
    internal fun setUp() {
        keyRepository.deleteAll()
    }

    /*
    CENARIO: Ok - Deve deletar corretamente mediante clientId e pixId válidos
    CENARIO: OK - Deve falhar ao tentar deletar chavePix com clientId Inválido (Inexistente no itau)
    CENARIO: OK - Deve falhar ao tentar deletar chavePix com pixId inválido (inexistente)
    CENARIO: OK - Deve ter sucesso ao remover uma chave mockando o client itau com cliente valido
    CENARIO: OK - Deve falhar ao remover uma chave mockando o client itau com cliente invalido
     */

    @Test
    internal fun deveDeletarCorretamentePixidDoBanco() {
        val chave1 = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678910",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(chave1)

        val keyBuscada = keyRepository.findByClientId("1")

        grpcClient.exclui(
            ExcludeKeyRequest
            .newBuilder()
            .setPixid(keyBuscada[0].pixId.toString())
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
    internal fun deveTerSucessoAoRemoverChaveComMockDoClienteItauValido() {
        val chave = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678910",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(chave)

        Mockito.`when`(clientItau.buscaContaETipo("1", "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(
                    ClientResponseDto(
                        tipo = "CONTA_CORRENTE",
                        titular = mapOf(
                            Pair("id", "1"),
                            Pair("nome", "Tolkien"),
                            Pair("cpf", "12345678910")
                        )
                    )
                )
            )

        grpcClient.exclui(ExcludeKeyRequest.newBuilder()
            .setPixid(chave.pixId.toString())
            .setClientid(chave.clientId)
            .build()
        )

        val lista = keyRepository.findAll()

        assertTrue(lista.count() == 0)

    }

    @Test
    internal fun deveFalharAoTentarExcluirUmaChaveCujoClientIdNaoExistaNoItau() {
        val chave = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678910",
            clientId = "idInexistente",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(chave)

        Mockito.`when`(clientItau.buscaContaETipo("idInexistente", "CONTA_CORRENTE"))
            .thenThrow(HttpClientResponseException::class.java)

        val error = assertThrows<StatusRuntimeException> {
        grpcClient.exclui(ExcludeKeyRequest.newBuilder()
            .setPixid(chave.pixId.toString())
            .setClientid(chave.clientId)
            .build()
        )}

        val lista = keyRepository.findAll()

        assertTrue(lista.count() == 1)
    }

    @MockBean(ClientItau::class)
    fun clientItau(): ClientItau? {
        return Mockito.mock(ClientItau::class.java)
    }

    @Factory
    class ExcludeClient {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): ExcludeKeyServiceGrpc.ExcludeKeyServiceBlockingStub? {
            return ExcludeKeyServiceGrpc.newBlockingStub(channel)
        }
    }
}