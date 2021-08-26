package keymanagergrpc.br.com.guilherme.endpoints

import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.annotation.Controller
import io.micronaut.http.client.exceptions.HttpClientResponseException
import keymanagergrpc.br.com.guilherme.ExcludeKeyRequest
import keymanagergrpc.br.com.guilherme.ExcludeKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.validacao.ChavePixValidator
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

@Controller
open class DeletaChaveEndpoint(
    @Inject val keyRepository: KeyRepository,
    @Inject val clientErp: ClientItau
): ExcludeKeyServiceGrpc.ExcludeKeyServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun exclui(request: ExcludeKeyRequest?, responseObserver: StreamObserver<Empty>?) {

        val validator = ChavePixValidator()

        LOGGER.info("Buscando chave e cliente no database")
        val chaves = keyRepository.findByClientIdAndPixId(request!!.clientid, UUID.fromString(request.pixid))
        if(chaves.isEmpty()) {
            LOGGER.error("Cliente não possuí chave pix do tipo")
            responseObserver?.onError(
                Status.NOT_FOUND
                    .withDescription("Cliente não possuí essa chave pix")
                    .asRuntimeException()
            )
            return
        }

        try {
            LOGGER.info("Buscando cliente no ERP")
            clientErp.buscaContaETipo(chaves.get(0).clientId, chaves.get(0).tipoConta.toString())
        } catch (e: HttpClientResponseException) {
            LOGGER.error("Não é um cliente válido")
            responseObserver?.onError(
                Status.PERMISSION_DENIED
                    .withDescription("ID não encontrado")
                    .asRuntimeException()
            )
            return
        }

        LOGGER.info("Deletando CHAVEPIX")
        keyRepository.deleteById(chaves[0].pixId!!)

        responseObserver?.onNext(Empty.getDefaultInstance())
        responseObserver?.onCompleted()
    }

}