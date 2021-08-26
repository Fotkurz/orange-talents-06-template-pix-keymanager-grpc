package keymanagergrpc.br.com.guilherme.validacao

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpResponse
import keymanagergrpc.br.com.guilherme.AccountType
import keymanagergrpc.br.com.guilherme.CreateKeyRequest
import keymanagergrpc.br.com.guilherme.CreateKeyResponse
import keymanagergrpc.br.com.guilherme.KeyType
import keymanagergrpc.br.com.guilherme.client.ClientItau
import keymanagergrpc.br.com.guilherme.client.ClientResponseDto
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import org.slf4j.LoggerFactory

class ChavePixValidator {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    // Validação se o cliente em questão já tem esse tipo de chave cadastrado
    fun validaCreateRequest(
        request: CreateKeyRequest,
        responseObserver: StreamObserver<CreateKeyResponse>?,
        keyRepository: KeyRepository
    ): Boolean {
        // TODO: Melhorar o funcionamento dessa classe, ela ainda é altamente acoplada

        validaTipoConta(request.accountType).apply {
            when (this) {
                is StatusRuntimeException -> {
                    responseObserver?.onError(this)
                    return true
                }
            }
        }

        validaTipoChave(request.keyType, request.chave).apply {
            when(this) {
                is StatusRuntimeException -> {
                    responseObserver?.onError(this)
                    return true
                }
            }
        }

        validaChaveJaExistente(request.id, request.keyType, keyRepository).apply {
            when(this) {
                is StatusRuntimeException -> {
                    responseObserver?.onError(this)
                    return true
                }
            }
        }

        LOGGER.info("Validação concluída com sucesso")
        return false
    }

    public fun validaTipoConta(
       accountType: AccountType
    ): StatusRuntimeException? {
        return when (accountType) {
            AccountType.UNKNOWN_ACCOUNT -> {
                Status.INVALID_ARGUMENT
                    .withDescription("Tipo de conta é obrigatório")
                    .asRuntimeException()
            }
            else -> null
        }
    }

    // Valida se cliente já tem chave do tipo
    private fun validaChaveJaExistente(
        clientId: String, keyType: KeyType, keyRepository: KeyRepository
    ): StatusRuntimeException? = when (keyRepository.existsByClientIdAndTipoChave(clientId, TipoChave.valueOf(keyType.toString()))) {
        true -> {
            Status.ALREADY_EXISTS
                    .withDescription("usuário já tem chave do tipo ${keyType} cadastrada")
                    .asRuntimeException()

        }
        else -> null
    }

    private fun validaTipoChave(
        keyType: KeyType, chave: String
    ): StatusRuntimeException? {
        when(keyType) {
            KeyType.UNRECOGNIZED, KeyType.UNKNOWN_TYPE -> {
                LOGGER.error("Chave nao reconhecido")
                return Status.INVALID_ARGUMENT
                    .withDescription("Tipo chave é obrigatório")
                    .asRuntimeException()
            }
            KeyType.RANDOMKEY -> {
                if(notNull(chave)) {
                    return Status.INVALID_ARGUMENT
                        .withDescription("Chave Aleatória não pode ter valor associado")
                        .asRuntimeException()
                }
                return null
            }
            KeyType.CPF -> {
                if(!cpfValido(chave)) {
                    return Status.INVALID_ARGUMENT
                            .withDescription("CPF Inválido")
                            .asRuntimeException()
                }
                return null
            }
            KeyType.CELULAR -> {
                if(!celularValido(chave)) {
                    return Status.INVALID_ARGUMENT
                        .withDescription("CELULAR Inválido")
                        .asRuntimeException()
                }
                return null
            }
            KeyType.EMAIL -> {
                if(!emailValido(chave)) {
                    return Status.INVALID_ARGUMENT
                        .withDescription("EMAIL Inválido")
                        .asRuntimeException()
                }
                return null
            }

        }
        return null
    }

    // Valida se o campo chave é nulo
    private fun campoChaveENulo(request: CreateKeyRequest): Boolean {
        if (request.chave.isNullOrBlank()) {
            return true
        }
        return false
    }

    // Valida se não é null
    private fun notNull(valor: Any): Boolean {
        if (valor != null) return true
        return false
    }

    // Valida seo CPF tem formato valido
    private fun cpfValido(valor: String): Boolean {
        if (notNull(valor)) {
            val regex = Regex("^[0-9]{11}\$")
            if (valor.matches(regex)) return true
        }
        return false
    }

    // Valida seo celular tem formato valido
    private fun celularValido(valor: String): Boolean {
        if (notNull(valor)) {
            val regex = Regex("^\\+[1-9][0-9]\\d{1,14}\$")
            if (valor.matches(regex)) return true
        }
        return false
    }

    // Valida seo email tem formato valido
    private fun emailValido(valor: String): Boolean {
        if (notNull(valor)) {
            // Regex disponível em https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
            val regex =
                Regex("^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\$")
            if (valor.matches(regex)) return true
        }
        return false
    }

    private fun tamanhoMaximoExcedido(valor: String): StatusRuntimeException? {
        if (valor.length > 77) {
            return Status.INVALID_ARGUMENT
                    .withDescription("Chave deve ter menos que 77 caracteres")
                    .asRuntimeException()
        }
        return null
    }

}