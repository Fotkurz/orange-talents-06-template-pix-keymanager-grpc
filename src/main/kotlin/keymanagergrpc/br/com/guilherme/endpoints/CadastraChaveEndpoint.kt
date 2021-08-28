package keymanagergrpc.br.com.guilherme.endpoints

import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.client.exceptions.HttpClientResponseException
import keymanagergrpc.br.com.guilherme.CreateKeyRequest
import keymanagergrpc.br.com.guilherme.CreateKeyResponse
import keymanagergrpc.br.com.guilherme.CreateKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.CreatePixKeyRequest
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.client.ItauResponseDto
import keymanagergrpc.br.com.guilherme.handler.InterceptAndValidate
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
    @Inject val clientErp: ClientItau,
    @Inject val clientBcb: ClientBcb
) : CreateKeyServiceGrpc.CreateKeyServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @InterceptAndValidate
    open override fun registra(request: CreateKeyRequest, responseObserver: StreamObserver<CreateKeyResponse>?) {
        val validator = ChavePixValidator()

        LOGGER.info("Validando os dados do request")

        if (validator.validaCreateRequest(request, responseObserver, keyRepository)) return

        var httpResponse: HttpResponse<ItauResponseDto?>? = null
        try {
            httpResponse = clientErp.buscaContaETipo(request.id, request.accountType.toString())
            LOGGER.info("Resposta: ${httpResponse.body.get()}")
        } catch (e: HttpClientResponseException) {
            responseObserver?.onError(
                Status.PERMISSION_DENIED
                    .withDescription("ID não encontrado")
                    .asRuntimeException()
            )
        }
        var novaChave = request.toModel()

        if(httpResponse?.body() != null) {
            val requestBcb = httpResponse.body()?.toBcb(novaChave.chave, novaChave.tipoChave.toString())
            LOGGER.info("Request BCB: ${requestBcb}")

            try {
                val retorno = clientBcb.cadastraChave(requestBcb!!)
                LOGGER.info("Response BCB: ${retorno}")
                if (novaChave.tipoChave.equals(TipoChave.RANDOM))
                    if (retorno?.body() != null) {
                        novaChave.chave = retorno.body.get().key
                    }

            }catch (e: HttpClientResponseException) {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("Erro na criação da chave")
                        .asRuntimeException()
                )
                return
            }
        }

        keyRepository.save(novaChave)

        LOGGER.info("Chave do tipo ${novaChave.tipoChave} cadastrada")
        responseObserver?.onNext(CreateKeyResponse.newBuilder().setPixid(novaChave.pixId.toString()).build())
        responseObserver?.onCompleted()

    }


}

fun CreateKeyRequest.toModel(): ChavePix {
    return ChavePix(
        tipoConta = TipoConta.valueOf(this.accountType.toString()),
        tipoChave = TipoChave.valueOf(this.keyType.toString()),
        clientId = this.id,
        chave = this.chave
    )
}