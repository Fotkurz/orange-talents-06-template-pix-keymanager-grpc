package keymanagergrpc.br.com.guilherme.endpoints

import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.annotation.Controller
import io.micronaut.http.client.exceptions.HttpClientResponseException
import keymanagergrpc.br.com.guilherme.ExcludeKeyRequest
import keymanagergrpc.br.com.guilherme.ExcludeKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.validacao.ChavePixValidator
import org.slf4j.LoggerFactory
import javax.inject.Inject

@Controller
open class DeletaChaveEndpoint(
    @Inject val keyRepository: KeyRepository,
    @Inject val clientErp: ClientItau,
    @Inject val clientBcb: ClientBcb
): ExcludeKeyServiceGrpc.ExcludeKeyServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun exclui(request: ExcludeKeyRequest?, responseObserver: StreamObserver<Empty>?) {

        val validator = ChavePixValidator()

        LOGGER.info("Buscando chave e cliente no database")

        val chave = keyRepository.findByClientIdAndPixId(request!!.clientid, request.pixid)
        if(chave == null) {
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
            clientErp.buscaContaETipo(chave.clientId, chave.tipoConta.toString())
        } catch (e: HttpClientResponseException) {
            LOGGER.error("Não é um cliente válido")
            responseObserver?.onError(
                Status.PERMISSION_DENIED
                    .withDescription("ID não encontrado")
                    .asRuntimeException()
            )
            return
        }

        try {
            LOGGER.info("Informando ao BCB")
            val respostaBcb = clientBcb.deletaChave(chave.pixId)
            LOGGER.info("Chave deletada: ${respostaBcb.body().get("key")}")
        } catch (e: HttpClientResponseException) {
            LOGGER.error("Chave pix não existente no bcb")
            responseObserver?.onError(
                Status.PERMISSION_DENIED
                    .withDescription("Chave Pix não encontrada no sistema BCB")
                    .asRuntimeException()
            )
            return
        }

        LOGGER.info("Deletando CHAVEPIX")
        keyRepository.deleteById(chave.pixId)
        LOGGER.info("Chave pix deletada cpom sucesso")

        responseObserver?.onNext(Empty.getDefaultInstance())
        responseObserver?.onCompleted()
    }

}