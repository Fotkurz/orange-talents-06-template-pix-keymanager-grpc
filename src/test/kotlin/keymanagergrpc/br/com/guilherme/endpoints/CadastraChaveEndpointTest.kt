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
import keymanagergrpc.br.com.guilherme.client.*
import keymanagergrpc.br.com.guilherme.client.dtos.CreatePixKeyRequest
import keymanagergrpc.br.com.guilherme.client.dtos.CreatePixKeyResponse
import keymanagergrpc.br.com.guilherme.client.dtos.Instituicao
import keymanagergrpc.br.com.guilherme.client.dtos.ItauResponseDto
import keymanagergrpc.br.com.guilherme.client.dtos.Titular
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.modelo.TipoConta
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.stubbing.OngoingStubbing
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CadastraChaveEndpointTest() {

    @Inject
    lateinit var grpcClient: CreateKeyServiceGrpc.CreateKeyServiceBlockingStub

    @Inject
    lateinit var keyRepository: KeyRepository

    @Inject
    lateinit var clientBcb: ClientBcb

    @Inject
    lateinit var clientItau: ClientItau

    @BeforeEach
    internal fun setUp() {
        keyRepository.deleteAll()
    }

    // Happy path completo
    @Test
    internal fun `deve cadastrar corretamente no sistema e retornar o id interno`() {
        mockaCreateRequisicaoBcb(criaPixRequestKeyParaBcb("12345678912", "CPF"))
            ?.thenReturn(HttpResponse.ok())

        mockaRequisicaoItauComSucesso("1", "CONTA_CORRENTE")
            ?.thenReturn(HttpResponse.ok(criaResponseParaItau(tipo = "CONTA_CORRENTE")))

        val response = grpcClient.registra(
            CreateRequest.newBuilder()
                .setId("1")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.CPF)
                .setChave("12345678912")
                .build()
        )

        val novaChave = keyRepository.findById(response.pixid).get()

        with(response) {
            assertNotNull(this)
            assertEquals(novaChave.pixId, this.pixid.toString())
        }
    }

    @Test
    internal fun `deve falhar se cliente ja tiver a chave cadastrada`() {

        keyRepository.save(
            ChavePix(
                tipoConta = TipoConta.CONTA_CORRENTE,
                tipoChave = TipoChave.CPF,
                clientId = "1",
                chave = "12345678912"
            )
        )

        val retornaErro = assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                CreateRequest.newBuilder()
                    .setId("1")
                    .setAccountType(AccountType.CONTA_CORRENTE)
                    .setKeyType(KeyType.CPF)
                    .setChave("12345678912")
                    .build()
            )
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
                CreateRequest.newBuilder()
                    .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                    .setAccountType(AccountType.CONTA_CORRENTE)
                    .setKeyType(KeyType.CPF)
                    .setChave("")
                    .build()
            )
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
                CreateRequest.newBuilder()
                    .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                    .setAccountType(AccountType.CONTA_CORRENTE)
                    .setKeyType(KeyType.CPF)
                    .setChave("0246aegaeg7781054")
                    .build()
            )
        }
        assertNotNull(retornaErro)
        assertEquals(Status.INVALID_ARGUMENT.code, retornaErro.status.code)
        assertEquals("CPF Inválido", retornaErro.status.description)
    }

    @Test
    internal fun `deve retornar erro se chave celular tem formato invalido`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                CreateRequest.newBuilder()
                    .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                    .setAccountType(AccountType.CONTA_CORRENTE)
                    .setKeyType(KeyType.CELULAR)
                    .setChave("esseehumformatoinvalido")
                    .build()
            )
        }
        assertNotNull(retornaErro)
        assertEquals(Status.INVALID_ARGUMENT.code, retornaErro.status.code)
        assertEquals("CELULAR Inválido", retornaErro.status.description)
    }

    @Test
    internal fun `deve retornar erro se chave email tem formato invalido`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                CreateRequest.newBuilder()
                    .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                    .setAccountType(AccountType.CONTA_CORRENTE)
                    .setKeyType(KeyType.EMAIL)
                    .setChave("esseehumformatoinvalido")
                    .build()
            )
        }
        assertNotNull(retornaErro)
        assertEquals(Status.INVALID_ARGUMENT.code, retornaErro.status.code)
        assertEquals("EMAIL Inválido", retornaErro.status.description)
    }

    @Test
    internal fun `deve retornar erro se a chave aleatoria ter valor associado`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                CreateRequest.newBuilder()
                    .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                    .setAccountType(AccountType.CONTA_CORRENTE)
                    .setKeyType(KeyType.RANDOM)
                    .setChave("valorassociado")
                    .build()
            )
        }

        assertNotNull(retornaErro)
        assertEquals(retornaErro.status.code, Status.INVALID_ARGUMENT.code)
        assertEquals("Chave Aleatória não pode ter valor associado", retornaErro.status.description)
    }

    @Test
    internal fun `deve retornar erro caso tipo de conta nao tenha valor associado`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                CreateRequest.newBuilder()
                    .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                    .setKeyType(KeyType.RANDOM)
                    .setChave("")
                    .build()
            )
        }

        assertNotNull(retornaErro)
        assertEquals(retornaErro.status.code, Status.INVALID_ARGUMENT.code)
        assertEquals("Tipo de conta é obrigatório", retornaErro.status.description)
    }

    @Test
    internal fun `deve falhar quando o id do cliente nao existir no itau`() {
        mockaRequisicaoItauComSucesso("1", "CONTA_CORRENTE")
            ?.thenThrow(HttpClientResponseException::class.java)

        mockaCreateRequisicaoBcb(criaPixRequestKeyParaBcb("12345678912", "CPF"))

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                CreateRequest.newBuilder()
                    .setId("1")
                    .setAccountType(AccountType.CONTA_CORRENTE)
                    .setKeyType(KeyType.CPF)
                    .setChave("12345678912")
                    .build()
            )
        }
        assertNotNull(retornaErro)
        assertEquals(Status.PERMISSION_DENIED.code, retornaErro.status.code)
        assertEquals("ID não encontrado", retornaErro.status.description)
    }

    @Test
    internal fun `deve falhar se tipo de chave nao for passado`() {

        val retornaErro = assertThrows<StatusRuntimeException> {
            val primeiro = grpcClient.registra(
                CreateRequest.newBuilder()
                    .setId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                    .setAccountType(AccountType.CONTA_CORRENTE)
                    .setChave("02467781054")
                    .build()
            )
        }

        assertNotNull(retornaErro)
        assertEquals(Status.INVALID_ARGUMENT.code, retornaErro.status.code)
        assertEquals("Tipo chave é obrigatório", retornaErro.status.description)
    }

    @Test
    internal fun `deve cadastrar corretamente chave random gerada pelo bcb`() {

        mockaRequisicaoItauComSucesso("1", "CONTA_CORRENTE")
            ?.thenReturn(HttpResponse.ok(criaResponseParaItau(tipo = "CONTA_CORRENTE")))

        val requestPix = criaPixRequestKeyParaBcb(
            chave = "",
            tipo = TipoChave.RANDOM.toString()
        )

        val bcbPixResponse = criaPixResponseKeyParaBcb(
            chave = "chavegerada",
            tipo = TipoChave.RANDOM.toString()
        )

        mockaCreateRequisicaoBcb(requestPix)?.thenReturn(HttpResponse.created(bcbPixResponse))

        bcbPixResponse.key = "novachavegerada"
        `when`(clientBcb.cadastraChave(requestPix))
            .thenReturn(HttpResponse.created(bcbPixResponse))

        val response = grpcClient.registra(
            CreateRequest.newBuilder()
                .setId("1")
                .setAccountType(AccountType.CONTA_CORRENTE)
                .setKeyType(KeyType.RANDOM)
                .build()
        )

        val chaveNova = keyRepository.findByClientId("1")
        assertEquals("novachavegerada", chaveNova[0].chave)
    }

    @Test
    internal fun `deve retornar erro caso falhe na criacao da chave no bcb`() {
        mockaRequisicaoItauComSucesso("1", "CONTA_CORRENTE")
            ?.thenReturn(HttpResponse.ok(criaResponseParaItau("CONTA_CORRENTE")))

        `when`(clientBcb.cadastraChave(criaPixRequestKeyParaBcb("12345678912", "CONTA_CORRENTE")))
            ?.thenThrow(HttpClientResponseException::class.java)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                CreateRequest.newBuilder()
                    .setId("1")
                    .setKeyType(KeyType.CPF)
                    .setChave("12345678912")
                    .setAccountType(AccountType.CONTA_CORRENTE)
                    .build()
            )
        }

        assertEquals(Status.INVALID_ARGUMENT.code, error.status.code)
        assertEquals("Falha no cadastro da chave no Banco Central", error.status.description)

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
            instituicao = Instituicao(nome = "ITAU", ispb = "1111"),
            titular = Titular(id = "1", nome = "Tolkien", cpf = "12345678912"),
            agencia = "0001",
            numero = "121212"
        )
    }

    fun criaPixRequestKeyParaBcb(chave: String, tipo: String): CreatePixKeyRequest {
        return CreatePixKeyRequest(
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
            )
        )
    }

    fun mockaRequisicaoItauComSucesso(
        clientId: String,
        tipo: String
    ): OngoingStubbing<HttpResponse<ItauResponseDto?>>? {
        return `when`(clientItau.buscaContaETipo(clientId, tipo))
    }

    fun mockaCreateRequisicaoBcb(requestBcb: CreatePixKeyRequest): OngoingStubbing<HttpResponse<CreatePixKeyResponse>>? {
        return `when`(clientBcb.cadastraChave(requestBcb))
    }

    @MockBean(ClientBcb::class)
    fun clientBcb(): ClientBcb {
        return mock(ClientBcb::class.java)
    }

    @MockBean(ClientItau::class)
    fun clientItau(): ClientItau {
        return mock(ClientItau::class.java)
    }

    @Factory
    class RegisterClient {
        // Utilizamos nosso grpcserverchannel.name pq se usarmos o localhost:50051 vamos ter o problema de
        // O grpc sempre sobe em uma porta aleatória em ambientes de teste.
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CreateKeyServiceGrpc.CreateKeyServiceBlockingStub? {
            return CreateKeyServiceGrpc.newBlockingStub(channel)
        }
    }

}
