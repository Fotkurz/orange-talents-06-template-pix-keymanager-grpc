package keymanagergrpc.br.com.guilherme.endpoints

import io.grpc.stub.StreamObserver
import io.micronaut.http.annotation.Controller
import keymanagergrpc.br.com.guilherme.ListKeyRequest
import keymanagergrpc.br.com.guilherme.ListKeyResponse
import keymanagergrpc.br.com.guilherme.ListKeyServiceGrpc
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.interceptor.ExistenciaDeChaveException
import keymanagergrpc.br.com.guilherme.interceptor.InterceptAndValidate
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.validacao.ClientItauValidator
import javax.inject.Inject

@Controller
open class ListaChavesEndpoint(
    @Inject val keyRepository: KeyRepository,
    @Inject val clientItau: ClientItau
): ListKeyServiceGrpc.ListKeyServiceImplBase() {

    @InterceptAndValidate
    override fun listar(request: ListKeyRequest?, responseObserver: StreamObserver<ListKeyResponse>?) {

        val validaItau = ClientItauValidator(clientItau)

        if(validaItau.checaExistenciaDeClienteNoItau(request!!.clientId)) {
            val listaDeChaves = keyRepository.findByClientId(request.clientId)


        }

    }
}