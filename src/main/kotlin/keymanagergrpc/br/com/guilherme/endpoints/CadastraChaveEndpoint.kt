package keymanagergrpc.br.com.guilherme.endpoints

import io.grpc.stub.StreamObserver
import io.micronaut.http.annotation.Controller
import keymanagergrpc.br.com.guilherme.CreateKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.CreateRequest
import keymanagergrpc.br.com.guilherme.CreateResponse
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.handler.InterceptAndValidate
import keymanagergrpc.br.com.guilherme.handler.ValidacaoErpItauException
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.modelo.TipoConta
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.validacao.ChavePixValidator
import keymanagergrpc.br.com.guilherme.validacao.ClientBcbValidator
import keymanagergrpc.br.com.guilherme.validacao.ClientItauValidator
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
    override fun registra(request: CreateRequest, responseObserver: StreamObserver<CreateResponse>?) {
        val validator = ChavePixValidator()

        LOGGER.info("Validando os dados do request")

        validator.validaCreateRequest(request, keyRepository)

        val respostaItau = ClientItauValidator(clientErp).buscaPorContaETipoNoItau(request.id, request.accountType.toString())

        val novaChave = request.toModel()
        ClientBcbValidator(clientBcb).cadastraChaveNoBcb(respostaItau, novaChave)

        if(respostaItau == null) throw ValidacaoErpItauException("Erro Itau")

        keyRepository.save(novaChave)

        LOGGER.info("Chave do tipo ${novaChave.tipoChave} cadastrada")
        responseObserver?.onNext(CreateResponse.newBuilder().setPixid(novaChave.pixId.toString()).build())
        responseObserver?.onCompleted()

    }

}

fun CreateRequest.toModel(): ChavePix {
    return ChavePix(
        tipoConta = TipoConta.valueOf(this.accountType.toString()),
        tipoChave = TipoChave.valueOf(this.keyType.toString()),
        clientId = this.id,
        chave = this.chave
    )
}