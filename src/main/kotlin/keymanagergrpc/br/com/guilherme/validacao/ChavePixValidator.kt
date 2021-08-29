package keymanagergrpc.br.com.guilherme.validacao

import io.grpc.Status
import io.grpc.StatusRuntimeException
import keymanagergrpc.br.com.guilherme.AccountType
import keymanagergrpc.br.com.guilherme.CreateRequest
import keymanagergrpc.br.com.guilherme.KeyType
import keymanagergrpc.br.com.guilherme.handler.ChaveDuplicadaException
import keymanagergrpc.br.com.guilherme.handler.TipoChaveInvalidoException
import keymanagergrpc.br.com.guilherme.handler.TipoContaInvalidoException
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import org.slf4j.LoggerFactory

class ChavePixValidator {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)
    // TODO: Melhorar o funcionamento dessa classe, ela ainda é altamente acoplada

    // Validação se o cliente em questão já tem esse tipo de chave cadastrado
    fun validaCreateRequest(
        request: CreateRequest,
        keyRepository: KeyRepository
    ): Boolean {

        if(
            validaTipoConta(request.accountType) &&
            validaTipoChave(request.keyType, request.chave) &&
            validaChaveJaExistente(request.id, request.keyType, keyRepository))
        {
            return true
        }

        LOGGER.info("Validação concluída com sucesso")
        return false
    }

    fun validaTipoConta(
       accountType: AccountType
    ): Boolean {
        return when (accountType) {
            AccountType.UNKNOWN_ACCOUNT -> {
                throw TipoContaInvalidoException("Tipo de conta é obrigatório")
            }
            else -> true
        }
    }

    // Valida se cliente já tem chave do tipo
    fun validaChaveJaExistente(
        clientId: String, keyType: KeyType, keyRepository: KeyRepository
    ): Boolean = when (keyRepository.existsByClientIdAndTipoChave(clientId, TipoChave.valueOf(keyType.toString()))) {
        true -> {
            throw ChaveDuplicadaException("usuário já tem chave do tipo ${keyType} cadastrada")
        }
        else -> true
    }

    fun validaTipoChave(
        keyType: KeyType, chave: String
    ): Boolean {
        when(keyType) {
            KeyType.UNRECOGNIZED, KeyType.UNKNOWN_TYPE -> {
                LOGGER.error("Chave nao reconhecido")
                throw TipoChaveInvalidoException("Tipo chave é obrigatório")
            }
            KeyType.RANDOM -> {
                if(!chave.isNullOrBlank()) {
                    throw TipoChaveInvalidoException("Chave Aleatória não pode ter valor associado")
                }
            }
            KeyType.CPF -> {
                if(!cpfValido(chave)) {
                    throw TipoChaveInvalidoException("CPF Inválido")
                }
            }
            KeyType.CELULAR -> {
                if(!celularValido(chave)) {
                    throw TipoChaveInvalidoException("CELULAR Inválido")
                }
            }
            KeyType.EMAIL -> {
                if(!emailValido(chave)) {
                    throw TipoChaveInvalidoException("EMAIL Inválido")
                }
            }

        }
        return true
    }

    // Valida seo CPF tem formato valido
    private fun cpfValido(valor: String): Boolean {
        if (valor != null) {
            val regex = Regex("^[0-9]{11}\$")
            if (valor.matches(regex)) return true
        }
        return false
    }

    // Valida seo celular tem formato valido
    private fun celularValido(valor: String): Boolean {
        if (!valor.isNullOrBlank()) {
            val regex = Regex("^\\+[1-9][0-9]\\d{1,14}\$")
            if (valor.matches(regex)) return true
        }
        return false
    }

    // Valida seo email tem formato valido
    private fun emailValido(valor: String): Boolean {
        if (!valor.isNullOrBlank()) {
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