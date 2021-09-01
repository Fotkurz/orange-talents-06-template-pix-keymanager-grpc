package keymanagergrpc.br.com.guilherme.endpoints

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import keymanagergrpc.br.com.guilherme.*
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.modelo.TipoConta
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.service.TestBuildingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class DeletaChaveEndpointTest {

    @Inject
    lateinit var t: TestBuildingService

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

    private val uuid = "cb3a88ca-eb05-41f3-9871-b18ace28ee40"
    private val chaveCpf = "12345678912"
    private val tipoConta = TipoConta.CONTA_CORRENTE.toString()
    private val tipoChave = TipoChave.CPF.toString()

    @BeforeEach
    internal fun setUp() {
        keyRepository.deleteAll()

        t.mockaRequisicaoItau(clientId = uuid, tipoConta = tipoConta)!!
            .thenReturn(HttpResponse.ok(t.createResponseParaItau(tipoConta = tipoConta)))

        t.mockaCreateRequisicaoBcb(t.createPixRequestKeyParaBcb(key = chaveCpf, keyType = tipoChave))
            ?.thenReturn(HttpResponse.created((t.createPixResponseKeyParaBcb(key = chaveCpf, keyType = tipoChave))))

        grpcCadastra.registra(
            CreateRequest.newBuilder()
                .setId(uuid)
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.CPF)
                .setChave(chaveCpf)
                .build()
        )
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

        t.mockaDeleteChaveBcb(
            "12345678912",
            t.createDeleteKeyPixRequest("12345678912"))?.thenReturn(HttpResponse.ok())

        val chave = keyRepository.findByChave("12345678912")

        grpcClient.exclui(
            ExcludeKeyRequest
            .newBuilder()
            .setPixid(chave?.pixId)
            .setClientid(uuid)
            .build()
        )

        val lista = keyRepository.findAll()

        assertTrue(lista.toList().isEmpty())
    }

    @Test
    internal fun deveFalharAoTentarDeletarChavePixComClientIdInvalidoEChaveValida() {

        val chave = keyRepository.findByChave("12345678912")

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.exclui(
                ExcludeKeyRequest
                    .newBuilder()
                    .setPixid(chave?.pixId)
                    .setClientid("clientidinvalido")
                    .build()
            )
        }

        val lista = keyRepository.findAll()

        assertEquals(Status.NOT_FOUND.code, error.status.code)
        assertEquals("PixId ou Cliente inválido", error.status.description)

    }

    @Test
    internal fun deveFalharAoTentarExcluirUmaChaveComPixIdInvalidoEClientIdValido() {

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.exclui(
                ExcludeKeyRequest
                    .newBuilder()
                    .setPixid("pixidinvalido")
                    .setClientid(uuid)
                    .build()
            )
        }

        val lista = keyRepository.findAll()

        assertEquals(Status.NOT_FOUND.code, error.status.code)
        assertEquals("PixId ou Cliente inválido", error.status.description)
    }

    @Test
    internal fun deveFalharAoTentarExcluirUmaChaveCujoClientIdNaoExistaNoItau() {

        t.mockaRequisicaoItau(uuid, "CONTA_CORRENTE")?.thenThrow(HttpClientResponseException::class.java)

        val chave = keyRepository.findByChave(chaveCpf)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.exclui(
                ExcludeKeyRequest
                    .newBuilder()
                    .setPixid(chave?.pixId)
                    .setClientid(uuid)
                    .build()
            )
        }

        val lista = keyRepository.findAll()

        assertEquals(Status.PERMISSION_DENIED.code, error.status.code)
        assertEquals("Client inexistente no sistema do itau", error.status.description)

    }

    @Test
    internal fun deveFalharAoTentarDeletarChaveNaoCadastradaNoBcb() {

        t.mockaDeleteChaveBcb(key = chaveCpf, t.createDeleteKeyPixRequest(chaveCpf))
            ?.thenThrow(HttpClientResponseException::class.java)

        val chave = keyRepository.findByChave(chaveCpf)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.exclui(
                ExcludeKeyRequest
                    .newBuilder()
                    .setPixid(chave?.pixId)
                    .setClientid(uuid)
                    .build()
            )
        }

        val lista = keyRepository.findAll()

        assertEquals(Status.INVALID_ARGUMENT.code, error.status.code)
        assertEquals("Chave Pix não encontrada no sistema BCB", error.status.description)

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