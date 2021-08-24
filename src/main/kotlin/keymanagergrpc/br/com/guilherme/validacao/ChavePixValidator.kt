package keymanagergrpc.br.com.guilherme.validacao

import io.grpc.Status
import io.grpc.stub.StreamObserver
import keymanagergrpc.br.com.guilherme.AccountType
import keymanagergrpc.br.com.guilherme.KeyType
import keymanagergrpc.br.com.guilherme.KeymanagerRequest
import keymanagergrpc.br.com.guilherme.KeymanagerResponse
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import org.slf4j.LoggerFactory

class ChavePixValidator(
    val keyRepository: KeyRepository,
    val request: KeymanagerRequest,
    val responseObserver: StreamObserver<KeymanagerResponse>?
) {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    // Validação se o cliente em questão já tem esse tipo de chave cadastrado
    fun validaRequest(
        request: KeymanagerRequest,
        responseObserver: StreamObserver<KeymanagerResponse>?,
    ): Boolean {

        when {
            validaTipoConta(request, responseObserver) -> {
                LOGGER.info("Validando tipo de conta")
                return true
            }

            validaTipoChaveValido(request, responseObserver) -> {
                LOGGER.info("Formato de chave inválida")
                return true
            }

            validaChaveJaExistente(request, responseObserver) -> {
                LOGGER.info("Validação de chave existente falhou")
                return true
            }

        }

        LOGGER.info("Validação concluída com sucesso")
        return false
    }

    private fun validaTipoConta(
        request: KeymanagerRequest,
        responseObserver: StreamObserver<KeymanagerResponse>?
    ) = when(request.accountType) {
        AccountType.UNKNOWN_ACCOUNT -> {
            responseObserver?.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("Tipo de conta é obrigatório")
                    .asRuntimeException()
            )
            true
        }
        else -> {
            false
        }
    }

    // Valida se cliente já tem chave do tipo
    private fun validaChaveJaExistente(
        request: KeymanagerRequest,
        responseObserver: StreamObserver<KeymanagerResponse>?
    ) = when (keyRepository.existsByClientIdAndTipoChave(request.id, TipoChave.valueOf(request.keyType.toString()))) {
        true -> {
            responseObserver?.onError(
                Status.ALREADY_EXISTS
                    .withDescription("usuário já tem chave do tipo ${request.keyType} cadastrada")
                    .asRuntimeException()
            )
            true
        }
        else -> false
    }

    // Valida se o tipo de chave tem formato valido
    private fun validaTipoChaveValido(
        request: KeymanagerRequest,
        responseObserver: StreamObserver<KeymanagerResponse>?
    ) = when (request.keyType) {
        KeyType.UNRECOGNIZED, KeyType.UNKNOWN_TYPE -> {
            LOGGER.info("Chave nao reconhecida")
            responseObserver?.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("Tipo chave é obrigatório")
                    .asRuntimeException()
            )
            true
        }
        KeyType.RANDOMKEY -> {
            when (!request.chave.isNullOrBlank()) {
                true -> {
                    responseObserver?.onError(
                        Status.INVALID_ARGUMENT
                            .withDescription("Chave Aleatória não pode ter valor associado")
                            .asRuntimeException()
                    )
                    true
                }
                else -> false
            }
        }
        KeyType.CPF -> {
            when (!cpfValido(request.chave)) {
                true -> {
                    responseObserver?.onError(
                        Status.INVALID_ARGUMENT
                            .withDescription("CPF Inválido")
                            .asRuntimeException()
                    )
                    true
                }
                else -> false
            }
        }
        KeyType.CELULAR -> {
            when (!celularValido(request.chave)) {
                true -> {
                    responseObserver?.onError(
                        Status.INVALID_ARGUMENT
                            .withDescription("CELULAR Inválido")
                            .asRuntimeException()
                    )
                    true
                }
                else -> false
            }
        }
        KeyType.EMAIL -> {
            when (!emailValido(request.chave)) {
                true -> {
                    responseObserver?.onError(
                        Status.INVALID_ARGUMENT
                            .withDescription("EMAIL Inválido")
                            .asRuntimeException()
                    )
                    true
                }
                else -> false
            }
        }
        else -> false
    }

    // Valida se o campo chave é nulo
    private fun campoChaveENulo(request: KeymanagerRequest): Boolean {
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

    private fun tamanhoMaximoExcedido(valor: String): Boolean {
        if (valor.length > 77) {
            responseObserver?.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("Chave deve ter menos que 77 caracteres")
                    .asRuntimeException()
            )
            return true
        }
        return false
    }

}