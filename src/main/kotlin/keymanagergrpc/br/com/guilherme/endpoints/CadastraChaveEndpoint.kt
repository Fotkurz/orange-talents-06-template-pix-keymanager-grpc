package keymanagergrpc.br.com.guilherme.endpoints

import io.grpc.stub.StreamObserver
import io.micronaut.http.annotation.Controller
import keymanagergrpc.br.com.guilherme.KeymanagerRequest
import keymanagergrpc.br.com.guilherme.KeymanagerResponse
import keymanagergrpc.br.com.guilherme.KeymanagerServiceGrpc
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
) : KeymanagerServiceGrpc.KeymanagerServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    //    @ErrorHandler
    override fun registra(request: KeymanagerRequest, responseObserver: StreamObserver<KeymanagerResponse>?) {
        val validator =
            ChavePixValidator(keyRepository = keyRepository, request = request, responseObserver = responseObserver)

        LOGGER.info("Tentando validar a chave")
        if(!validator.validaRequest()) {
            LOGGER.info("Validação falhou")
            return
        }

        LOGGER.info("Buscando no sistema legado do Itaú")
        val retornoClient = clientErp.buscaContaETipo(request.id, request.accountType.toString())

        val novaChave = request.toModel()

        keyRepository.save(novaChave)

        LOGGER.info("Chave do tipo ${novaChave.tipoChave} cadastrada")
        responseObserver?.onNext(KeymanagerResponse.newBuilder().setPixid(novaChave.pixId.toString()).build())
        responseObserver?.onCompleted()

    }
}

fun KeymanagerRequest.toModel(): ChavePix {
    return ChavePix(
        tipoConta = TipoConta.valueOf(this.accountType.toString()),
        tipoChave = TipoChave.valueOf(this.keyType.toString()),
        chave = this.chave,
        clientId = this.id
    )
}
