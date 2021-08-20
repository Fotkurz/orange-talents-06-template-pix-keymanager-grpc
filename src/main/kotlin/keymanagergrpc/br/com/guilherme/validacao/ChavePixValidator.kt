package keymanagergrpc.br.com.guilherme.validacao

import io.grpc.Status
import io.grpc.stub.StreamObserver
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.KeyType
import keymanagergrpc.br.com.guilherme.KeymanagerRequest
import keymanagergrpc.br.com.guilherme.KeymanagerResponse
import keymanagergrpc.br.com.guilherme.modelo.TipoChave

class ChavePixValidator(val keyRepository: KeyRepository, val request: KeymanagerRequest, val responseObserver: StreamObserver<KeymanagerResponse>?) {

    fun validaRequest(): Boolean {
        // Verifica o tamanho da chave
        if(!tamanhoMaximoValido(request.chave)) return false
        // Verifica se já existe a chave com aquela
        if(validaSeJaExisteChaveETipo(request, responseObserver)) return false
        // Valida se a chave tem formato válido para aquele tipo específico
        if(validaSeOTipoTemChaveValida(request, responseObserver)) return false
        // Valida se a chave tipo aleatório veio sem dados
        if(validaTipoAleatorio(request, responseObserver)) return false

        return true
    }

    // Validação se o cliente em questão já tem esse tipo de chave cadastrado
    fun validaSeJaExisteChaveETipo(request: KeymanagerRequest, responseObserver: StreamObserver<KeymanagerResponse>?): Boolean {
        if (keyRepository.existsByClientIdAndTipoChave(request.id, TipoChave.valueOf(request.keyType.toString()))) {
                    responseObserver?.onError(
                        Status.ALREADY_EXISTS
                            .withDescription("usuário já tem chave do tipo ${request.keyType} cadastrada")
                            .asRuntimeException()
                    )
                    return true
        }
        return false
    }

    // Validação de tipo de chave
    fun validaSeOTipoTemChaveValida(request: KeymanagerRequest, responseObserver: StreamObserver<KeymanagerResponse>?): Boolean{
        if (request.keyType == KeyType.CPF || request.keyType == KeyType.CELULAR || request.keyType == KeyType.EMAIL) {
            if (request.chave.isNullOrBlank()) {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("Campo chave obrigatório")
                        .asRuntimeException()
                )
                return true
            }
            when {
                (request.keyType.equals(KeyType.CPF) && !cpfValido(request.chave)) -> {
                    responseObserver?.onError(
                        Status.INVALID_ARGUMENT
                            .withDescription("CPF Inválido")
                            .asRuntimeException()
                    )
                    return true
                }
                (request.keyType.equals(KeyType.CELULAR) && !celularValido(request.chave)) -> {
                    responseObserver?.onError(
                        Status.INVALID_ARGUMENT
                            .withDescription("Celular inválido")
                            .asRuntimeException()
                    )
                    return true
                }
                (request.keyType.equals(KeyType.EMAIL) && !emailValido(request.chave)) -> {
                    responseObserver?.onError(
                        Status.INVALID_ARGUMENT
                            .withDescription("Email inválido")
                            .asRuntimeException()
                    )
                    return true
                }
            }

        }
    return false
    }

    // Valida se o tipo for aleatório e não tem valor
    fun validaTipoAleatorio(request: KeymanagerRequest, responseObserver: StreamObserver<KeymanagerResponse>?): Boolean {
        // Validação se a chave aleatória tem valor
        if (request.keyType == KeyType.RANDOMKEY) {
            if (!request.chave.isNullOrBlank()) {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("Chave Aleatória não pode ter valor associado")
                        .asRuntimeException()
                )
                return true
            }
        }
        return false
    }

    // Valida se não é null
    fun notNull(valor: Any): Boolean {
        if(valor != null) return true
        return false
    }

    // Valida seo CPF tem formato valido
    fun cpfValido(valor: String): Boolean {
        if(notNull(valor)) {
            val regex = Regex("^[0-9]{11}\$")
            if(valor.matches(regex)) return true
        }
        return false
    }
    // Valida seo celular tem formato valido
    fun celularValido(valor: String): Boolean {
        if(notNull(valor)) {
            val regex = Regex("^\\+[1-9][0-9]\\d{1,14}\$")
            if(valor.matches(regex)) return true
        }
        return false
    }
    // Valida seo email tem formato valido
    fun emailValido(valor: String): Boolean {
        if(notNull(valor)) {
            // Regex disponível em https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
            val regex = Regex("^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\$")
            if(valor.matches(regex)) return true
        }
        return false
    }

    fun tamanhoMaximoValido(valor: String): Boolean {
        if (valor.length <= 77) return true
        return false
    }
}