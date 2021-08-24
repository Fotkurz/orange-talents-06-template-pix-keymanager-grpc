package keymanagergrpc.br.com.guilherme.endpoints

import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import keymanagergrpc.br.com.guilherme.KeymanagerRequest
import keymanagergrpc.br.com.guilherme.KeymanagerResponse
import keymanagergrpc.br.com.guilherme.KeymanagerServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.client.ClientResponseDto
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
) : KeymanagerServiceGrpc.KeymanagerServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    /*
    TODO: Ainda preciso arrumar um jeito de lançar a exceção na validação, como é requerido,
        atualmente ele só da o erro.
     */

    override fun registra(request: KeymanagerRequest, responseObserver: StreamObserver<KeymanagerResponse>?) {
        val validator = ChavePixValidator(keyRepository, request, responseObserver)


        LOGGER.info("Validando os dados do request")
        LOGGER.info("Request: $request")
        if (validator.validaRequest(request, responseObserver)) return
        if (validaId(request, responseObserver)) return

        val novaChave = request.toModel()

        keyRepository.save(novaChave)

        LOGGER.info("Chave do tipo ${novaChave.tipoChave} cadastrada")
        responseObserver?.onNext(KeymanagerResponse.newBuilder().setPixid(novaChave.pixId.toString()).build())
        responseObserver?.onCompleted()

    }

    private fun validaId(
        request: KeymanagerRequest,
        responseObserver: StreamObserver<KeymanagerResponse>?
    ): Boolean {

        try {
            val retornoClient: HttpResponse<ClientResponseDto>? = clientErp
                .buscaContaETipo(
                    request.id,
                    request.accountType.toString()
                )
        } catch (e: Exception) {
            responseObserver?.onError(
                Status.NOT_FOUND
                    .withDescription("ID não encontrado")
                    .asRuntimeException()
            )
            return true
        }
        return false
    }

    fun KeymanagerRequest.toModel(): ChavePix {
        return ChavePix(
            tipoConta = TipoConta.valueOf(this.accountType.toString()),
            tipoChave = TipoChave.valueOf(this.keyType.toString()),
            chave = this.chave,
            clientId = this.id
        )
    }
}

