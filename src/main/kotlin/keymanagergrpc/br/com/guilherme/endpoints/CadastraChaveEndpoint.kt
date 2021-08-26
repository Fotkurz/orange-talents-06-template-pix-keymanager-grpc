package keymanagergrpc.br.com.guilherme.endpoints

import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.annotation.Controller
import io.micronaut.http.client.exceptions.HttpClientResponseException
import keymanagergrpc.br.com.guilherme.CreateKeyRequest
import keymanagergrpc.br.com.guilherme.CreateKeyResponse
import keymanagergrpc.br.com.guilherme.CreateKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.modelo.TipoConta
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.validacao.ChavePixValidator
import org.slf4j.LoggerFactory
import javax.inject.Inject

@Controller
open class CadastraChaveEndpoint(
    @Inject val keyRepository: KeyRepository,
    @Inject val clientErp: ClientItau
) : CreateKeyServiceGrpc.CreateKeyServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun registra(request: CreateKeyRequest, responseObserver: StreamObserver<CreateKeyResponse>?) {
        val validator = ChavePixValidator()

        LOGGER.info("Validando os dados do request")
        if (validator.validaCreateRequest(request, responseObserver, keyRepository)) return
        try {
            clientErp.buscaContaETipo(request.id, request.accountType.toString())
        } catch (e: HttpClientResponseException) {
            responseObserver?.onError(
                Status.PERMISSION_DENIED
                    .withDescription("ID não encontrado")
                    .asRuntimeException()
            )
        }
        val novaChave = request.toModel()

        keyRepository.save(novaChave)

        LOGGER.info("Chave do tipo ${novaChave.tipoChave} cadastrada")
        responseObserver?.onNext(CreateKeyResponse.newBuilder().setPixid(novaChave.pixId.toString()).build())
        responseObserver?.onCompleted()

    }

    fun CreateKeyRequest.toModel(): ChavePix {
        return ChavePix(
            tipoConta = TipoConta.valueOf(this.accountType.toString()),
            tipoChave = TipoChave.valueOf(this.keyType.toString()),
            chave = this.chave,
            clientId = this.id
        )
    }
}

