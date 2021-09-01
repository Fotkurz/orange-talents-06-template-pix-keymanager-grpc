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
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.modelo.TipoConta
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.service.TestBuildingService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class ConsultaChaveEndpointTest {

    @Inject
    lateinit var grpcClient: CheckKeyServiceGrpc.CheckKeyServiceBlockingStub

    @Inject
    lateinit var grpcCadastra: CreateKeyServiceGrpc.CreateKeyServiceBlockingStub

    @Inject
    lateinit var clientBcb: ClientBcb

    @Inject
    lateinit var clientItau: ClientItau

    @Inject
    lateinit var keyRepository: KeyRepository

    @Inject
    lateinit var t: TestBuildingService

    private val uuid = "cb3a88ca-eb05-41f3-9871-b18ace28ee40"
    private val chaveCpf = "12345678912"
    private val tipoConta = TipoConta.CONTA_CORRENTE.toString()
    private val tipoChave = TipoChave.CPF.toString()

    @BeforeEach
    fun setUp() {
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

    @Test
    internal fun deveConsultarCorretamenteMedianteClientIdEPixIdPassado() {

        val chaveDoBanco = keyRepository.findByClientId(uuid)[0]

        t.mockaConsultaChaveBcb(chaveDoBanco.chave.toString())?.thenReturn(
            HttpResponse.ok(
                t.createPixKeyDetailsResponse(
                    key = chaveDoBanco.chave.toString(),
                    keyType = chaveDoBanco.tipoChave.toString()
                )
            )
        )

        val chaveSalva = keyRepository.findByChave("12345678912")

        val resposta = grpcClient.consulta(CheckKeyRequest.newBuilder()
            .setPorPixId(CheckKeyRequest.PorPixId.newBuilder()
                .setClientId(uuid)
                .setPixId(chaveSalva?.pixId)
                .build())
            .build())

        assertEquals(chaveDoBanco.chave, resposta.key)
        assertEquals(chaveDoBanco.tipoChave.toString(), resposta.keytype.toString())
    }

    @Test
    internal fun deveConsultarCorretamenteMedianteChavePassado() {

        t.mockaConsultaChaveBcb(chaveCpf)?.thenReturn(
            HttpResponse.ok(t.createPixKeyDetailsResponse(
                    key = chaveCpf,
                    keyType = tipoChave
                )))

        val resposta = grpcClient.consulta(CheckKeyRequest.newBuilder()
            .setPorChave(chaveCpf)
            .build())

        assertEquals(chaveCpf, resposta.key)
        assertEquals(tipoChave, resposta.keytype.toString())

    }

    @Test
    internal fun deveFalharAoConsultarChaveInexistenteNoBcb() {

        t.mockaConsultaChaveBcb("chaveinexistente")?.thenThrow(
            HttpClientResponseException::class.java
        )

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(
                CheckKeyRequest.newBuilder()
                    .setPorChave("chaveinexistente")
                    .build()
            )
        }

        assertEquals(Status.NOT_FOUND.code, error.status.code)
        assertEquals("Chave não cadastrada no BCB", error.status.description)
    }

    @Test
    internal fun deveFalharAoConsultarPorPixIdInexistenteEClientIdExistente() {

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(
                CheckKeyRequest.newBuilder()
                    .setPorPixId(
                        CheckKeyRequest.PorPixId.newBuilder()
                            .setPixId("Pixidinexistente")
                            .setClientId(uuid)
                            .build()
                    )
                    .build()
            )
        }

        assertEquals(Status.NOT_FOUND.code, error.status.code)
        assertEquals("Necessário enviar um id de cliente e pixid para itau e chave para externos", error.status.description)

    }

    @Test
    internal fun deveFalharAoConsultarPorPixIdEClientIdCasoClientIdNaoExistaNoItauTest() {

        val novaChave = ChavePix(
            tipoChave = TipoChave.CPF,
            tipoConta = TipoConta.CONTA_CORRENTE,
            chave = "98765432112",
            clientId = "clientidinexistente"
        )

        keyRepository.save(novaChave)

        t.mockaRequisicaoItau(clientId = "clientidinexistente", tipoConta = tipoConta)
            ?.thenThrow(HttpClientResponseException::class.java)

        val chaveSalva = keyRepository.findByChave(novaChave.chave.toString())

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(
                CheckKeyRequest.newBuilder()
                    .setPorPixId(
                        CheckKeyRequest.PorPixId.newBuilder()
                            .setPixId(chaveSalva?.pixId)
                            .setClientId("clientidinexistente")
                            .build()
                    )
                    .build()
            )
        }

        assertEquals(Status.PERMISSION_DENIED.code, error.status.code)
        assertEquals("Client inexistente no sistema do itau", error.status.description)

    }

    @MockBean(ClientItau::class)
    fun clientItau(): ClientItau {
        return Mockito.mock(ClientItau::class.java)
    }

    @MockBean(ClientBcb::class)
    fun clientBcb(): ClientBcb {
        return Mockito.mock(ClientBcb::class.java)
    }

    @Factory
    class CheckClient {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CheckKeyServiceGrpc.CheckKeyServiceBlockingStub? {
            return CheckKeyServiceGrpc.newBlockingStub(channel)
        }
    }

}