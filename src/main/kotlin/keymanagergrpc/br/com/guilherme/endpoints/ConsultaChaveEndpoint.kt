package keymanagergrpc.br.com.guilherme.endpoints

import io.grpc.stub.StreamObserver
import io.micronaut.http.annotation.Controller
import keymanagergrpc.br.com.guilherme.CheckKeyRequest
import keymanagergrpc.br.com.guilherme.CheckKeyResponse
import keymanagergrpc.br.com.guilherme.CheckKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientBcb
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.interceptor.InterceptAndValidate
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.validacao.ClientBcbValidator
import keymanagergrpc.br.com.guilherme.validacao.ClientItauValidator
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import javax.inject.Inject

// TODO: Talvez precisemos de dois endpoints, um para consultar interno e outro externo

@Controller
open class ConsultaChaveEndpoint(
    @Inject val keyRepository: KeyRepository,
    @Inject val clientItau: ClientItau,
    @Inject val clientBcb: ClientBcb
) : CheckKeyServiceGrpc.CheckKeyServiceImplBase() {


    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    // TODO: Fluxo -> no internal podemos receber direto o pixid e o clientid, vamos usar eles para achar nossa chave
    // TODO: No BCB precisamos passar nossa CHAVE

    @InterceptAndValidate
    override fun consulta(request: CheckKeyRequest?, responseObserver: StreamObserver<CheckKeyResponse>?) {


        val filtro = FiltraConsulta()

        val clientBcbValidator = ClientBcbValidator(clientBcb)
        val clientItauValidator = ClientItauValidator(clientItau)

        val resposta = request?.let {
            filtro.filtraTipoConsulta(
                request = it,
                keyRepository = keyRepository,
                clientItauValidator = clientItauValidator,
                clientBcbValidator = clientBcbValidator
            )
        }

        responseObserver?.onNext(resposta?.toResponse())
        responseObserver?.onCompleted()

    }

}