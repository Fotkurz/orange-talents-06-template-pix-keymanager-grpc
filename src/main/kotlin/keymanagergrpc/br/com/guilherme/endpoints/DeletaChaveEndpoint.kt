package keymanagergrpc.br.com.guilherme.endpoints

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import io.micronaut.http.annotation.Controller
import keymanagergrpc.br.com.guilherme.ExcludeKeyRequest
import keymanagergrpc.br.com.guilherme.ExcludeKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.interceptor.ExistenciaDeChaveException
import keymanagergrpc.br.com.guilherme.interceptor.InterceptAndValidate
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.validacao.ClientBcbValidator
import keymanagergrpc.br.com.guilherme.validacao.ClientItauValidator
import org.slf4j.LoggerFactory
import javax.inject.Inject

@Controller
open class DeletaChaveEndpoint(
    @Inject val keyRepository: KeyRepository,
    @Inject val clientBcb: ClientBcb,
    @Inject val clientItau: ClientItau
): ExcludeKeyServiceGrpc.ExcludeKeyServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @InterceptAndValidate
    override fun exclui(request: ExcludeKeyRequest?, responseObserver: StreamObserver<Empty>?) {

        LOGGER.info("Buscando chave e cliente no database")

        val possibleChave = keyRepository.findByClientIdAndPixId(request!!.clientid, request.pixid)

        if(possibleChave.isEmpty) {
            LOGGER.error("Cliente não possuí chave pix do tipo")
            throw ExistenciaDeChaveException("PixId ou Cliente inválido")
        }

        val chave = possibleChave.get()

        val respostaItau = ClientItauValidator(clientItau).buscaPorContaETipoNoItau(chave.clientId, chave.tipoConta.toString())
        ClientBcbValidator(clientBcb).deletaChaveNoBcb(chave, "60701190")
        LOGGER.info("Deletando CHAVEPIX")
        keyRepository.deleteById(chave.pixId)
        LOGGER.info("Chave pix deletada com sucesso")

        responseObserver?.onNext(Empty.getDefaultInstance())
        responseObserver?.onCompleted()
    }

}