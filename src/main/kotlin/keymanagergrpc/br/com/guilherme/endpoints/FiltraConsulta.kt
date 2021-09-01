package keymanagergrpc.br.com.guilherme.endpoints

import com.google.api.Http
import io.micronaut.http.client.exceptions.HttpClientResponseException
import keymanagergrpc.br.com.guilherme.CheckKeyRequest
import keymanagergrpc.br.com.guilherme.client.dtos.PixKeyDetailsResponse
import keymanagergrpc.br.com.guilherme.interceptor.ExistenciaDeChaveException
import keymanagergrpc.br.com.guilherme.interceptor.ValidacaoBcbException
import keymanagergrpc.br.com.guilherme.interceptor.ValidacaoErpItauException
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.validacao.ClientBcbValidator
import keymanagergrpc.br.com.guilherme.validacao.ClientItauValidator
import org.slf4j.LoggerFactory

class FiltraConsulta {

    val LOGGER = LoggerFactory.getLogger(this.javaClass)

    fun filtraTipoConsulta(
        request: CheckKeyRequest,
        keyRepository: KeyRepository,
        clientItauValidator: ClientItauValidator,
        clientBcbValidator: ClientBcbValidator
    ): PixKeyDetailsResponse? {
        when {
            (request.hasPorChave() && !request.hasPorPixId()) -> {
                LOGGER.info("Consulta externa")
                return clientBcbValidator.consultaChaveNoBcb(request.porChave.toString())
            }
            (request.hasPorPixId() && !request.hasPorChave()) -> {
                LOGGER.info("Consulta de cliente itau")

                val possibleChave =
                    keyRepository.findByClientIdAndPixId(request.porPixId.clientId, request.porPixId.pixId)

                if (possibleChave.isPresent) {
                    val pix = possibleChave.get()

                    clientItauValidator.buscaPorContaETipoNoItau(pix.clientId, pix.tipoConta.toString())

                    return clientBcbValidator.consultaChaveNoBcb(pix.chave.toString())
                }

            }
        }
        throw ExistenciaDeChaveException("Necessário enviar um id de cliente e pixid para itau e chave para externos")
    }
}