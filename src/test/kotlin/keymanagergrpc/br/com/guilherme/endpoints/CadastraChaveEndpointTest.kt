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
import keymanagergrpc.br.com.guilherme.AccountType
import keymanagergrpc.br.com.guilherme.KeyType
import keymanagergrpc.br.com.guilherme.KeymanagerRequest
import keymanagergrpc.br.com.guilherme.KeymanagerServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.client.ClientResponseDto
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.modelo.TipoConta
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CadastraChaveEndpointTest(
    private val grpcClient: KeymanagerServiceGrpc.KeymanagerServiceBlockingStub,
    private var keyRepository: KeyRepository,
) {

    @field:Inject
    lateinit var clientItau: ClientItau

    @BeforeEach
    internal fun setUp() {
        keyRepository.deleteAll()
    }

    @Test
    internal fun `deve cadastrar corretamente no sistema e retornar o id interno`() {

        Mockito.`when`(clientItau.buscaContaETipo("c56dfef4-7901-44fb-84e2-a2cefb157890", "CONTA_CORRENTE"))
            .thenReturn(
                HttpResponse.ok(
                    ClientResponseDto(
                        tipo = "CONTA_CORRENTE",
                        titular = mapOf(
                            Pair("id", "c56dfef4-7901-44fb-84e2-a2cefb157890"),
                            Pair("nome", "Rafael M C Ponte"),
                            Pair("cpf", "02467781054")
                        )
                    )
                )
            )

        val response = grpcClient.registra(
            KeymanagerRequest.newBuilder()
            .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setAccountType(AccountType.CONTA_CORRENTE)
            .setKeyType(KeyType.CPF)
            .setChave("02467781054")
            .build())

        val novaChave = keyRepository.findById(UUID.fromString(response.pixid)).get()

        with(response) {
            assertNotNull(this)
            assertEquals(novaChave.pixId.toString(), this.pixid.toString())
        }
    }

    @Test
    internal fun `deve falhar se cliente ja tiver a chave cadastrada`() {

        keyRepository.save(
            ChavePix(
                tipoConta = TipoConta.CONTA_CORRENTE,
                tipoChave = TipoChave.CPF,
                clientId = "c56dfef4-7901-44fb-84e2-a2cefb157890",
                chave = "02467781054"
            )
        )

        val retornaErro = assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                KeymanagerRequest.newBuilder()
                .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.CPF)
                .setChave("02467781054")
                .build())
        }

        with(retornaErro) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code)
            assertEquals("usuário já tem chave do tipo ${KeyType.CPF} cadastrada", this.status.description)
        }
    }

    @Test
    internal fun `deve retornar erro caso a chave esteja vazia para um dos valores obrigatorios`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                KeymanagerRequest.newBuilder()
                .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.CPF)
                .setChave("")
                .build())
        }

        with(retornaErro) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertEquals("CPF Inválido", this.status.description)
        }
    }

    @Test
    internal fun `deve retornar erro se chave cpf tem formato invalido`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                KeymanagerRequest.newBuilder()
                .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.CPF)
                .setChave("0246aegaeg7781054")
                .build())
        }
        assertNotNull(retornaErro)
        assertEquals(Status.INVALID_ARGUMENT.code, retornaErro.status.code)
        assertEquals("CPF Inválido", retornaErro.status.description)
    }

    @Test
    internal fun `deve retornar erro se chave celular tem formato invalido`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                KeymanagerRequest.newBuilder()
                .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.CELULAR)
                .setChave("esseehumformatoinvalido")
                .build())
        }
        assertNotNull(retornaErro)
        assertEquals(Status.INVALID_ARGUMENT.code, retornaErro.status.code)
        assertEquals("CELULAR Inválido", retornaErro.status.description)
    }

    @Test
    internal fun `deve retornar erro se chave email tem formato invalido`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                KeymanagerRequest.newBuilder()
                .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.EMAIL)
                .setChave("esseehumformatoinvalido")
                .build())
        }
        assertNotNull(retornaErro)
        assertEquals(Status.INVALID_ARGUMENT.code, retornaErro.status.code)
        assertEquals("EMAIL Inválido", retornaErro.status.description)
    }

    @Test
    internal fun `deve retornar erro se a chave aleatoria ter valor associado`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                KeymanagerRequest.newBuilder()
                .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.RANDOMKEY)
                .setChave("valorassociado")
                .build())
        }

        assertNotNull(retornaErro)
        assertEquals(retornaErro.status.code, Status.INVALID_ARGUMENT.code)
        assertEquals("Chave Aleatória não pode ter valor associado", retornaErro.status.description)
    }

    @Test
    internal fun `deve retornar erro caso tipo de conta nao tenha valor associado`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                KeymanagerRequest.newBuilder()
                .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setKeyType(KeyType.RANDOMKEY)
                .setChave("")
                .build())
        }

        assertNotNull(retornaErro)
        assertEquals(retornaErro.status.code, Status.INVALID_ARGUMENT.code)
        assertEquals("Tipo de conta é obrigatório", retornaErro.status.description)
    }

    @Test
    internal fun `deve falhar quando o id do cliente nao existir no itau`() {
        Mockito.`when`(clientItau.buscaContaETipo("1", "CONTA_CORRENTE"))
            .thenThrow(HttpClientResponseException("Erro",HttpResponse.badRequest("bad request")))

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                KeymanagerRequest.newBuilder()
                .setId("1")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.CPF)
                .setChave("02467781054")
                .build())
        }

        assertNotNull(retornaErro)
        assertEquals(retornaErro.status.code, Status.NOT_FOUND.code)
        assertEquals(retornaErro.status.description, "ID não encontrado")
    }

    @Test
    internal fun `deve falhar se tipo de chave nao for passado`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                KeymanagerRequest.newBuilder()
                .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setChave("02467781054")
                .build())
        }

        assertNotNull(retornaErro)
        assertEquals(Status.INVALID_ARGUMENT.code, retornaErro.status.code)
        assertEquals("Tipo chave é obrigatório", retornaErro.status.description)
    }

    @MockBean(ClientItau::class)
    fun clientItau(): ClientItau? {
        return Mockito.mock(ClientItau::class.java)
    }

    @Factory
    class Clients {
        // Utilizamos nosso grpcserverchannel.name pq se usarmos o localhost:50051 vamos ter o problema de
        // O grpc sempre sobe em uma porta aleatória em ambientes de teste.
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeymanagerServiceGrpc.KeymanagerServiceBlockingStub? {
            return KeymanagerServiceGrpc.newBlockingStub(channel)
        }
    }
}