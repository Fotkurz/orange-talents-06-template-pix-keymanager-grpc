package keymanagergrpc.br.com.guilherme.validacao

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import keymanagergrpc.br.com.guilherme.AccountType
import keymanagergrpc.br.com.guilherme.KeyType
import keymanagergrpc.br.com.guilherme.interceptor.ChaveDuplicadaException
import keymanagergrpc.br.com.guilherme.interceptor.TipoChaveInvalidoException
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import keymanagergrpc.br.com.guilherme.modelo.TipoConta
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class ChavePixValidatorTest {

    internal val validator = ChavePixValidator()

    @Inject
    lateinit var keyRepository: KeyRepository

    @Test
    internal fun deveValidarCorretamenteTipoContaContaCorrente() {
        val resultado = validator.validaTipoConta(AccountType.CONTA_CORRENTE)
        assertTrue(resultado)
    }

    @Test
    internal fun deveValidarCorretamenteTipoContaPoupanca() {
        val resultado = validator.validaTipoConta(AccountType.CONTA_POUPANCA)
        assertTrue(resultado)
    }

    @Test
    internal fun deveValidarCorretamenteTipoChave() {
        val resultado = validator.validaTipoChave(KeyType.CPF, "12345678912")
        assertTrue(resultado)
    }

    @Test
    internal fun deveFalharAoTentarValidarTipoChaveDesconhecido() {
        val resultado = assertThrows<TipoChaveInvalidoException> {
            validator.validaTipoChave(KeyType.UNKNOWN_TYPE, "chaveforadepadrao")
        }

        assertEquals("Tipo chave é obrigatório", resultado.message)
    }

    @Test
    internal fun deveFalharAoCadastrarTipoRandomComValor() {
        val resultado = assertThrows<TipoChaveInvalidoException> {
            validator.validaTipoChave(KeyType.RANDOM, "chaveforadepadrao")
        }

        assertEquals("Chave Aleatória não pode ter valor associado", resultado.message)
    }

    @Test
    internal fun deveFalharAoValidarTipoEmailInvalido() {
        val resultado = assertThrows<TipoChaveInvalidoException> {
            validator.validaTipoChave(KeyType.EMAIL, "chaveforadepadrao")
        }

        assertEquals("EMAIL Inválido", resultado.message)
    }

    @Test
    internal fun deveFalharAoValidarTipoCelularInvalido() {
        val resultado = assertThrows<TipoChaveInvalidoException> {
            validator.validaTipoChave(KeyType.PHONE, "chaveforadepadrao")
        }

        assertEquals("CELULAR Inválido", resultado.message)
    }

    @Test
    internal fun deveFalharAoValidarTipoCpfInvalido() {
        val resultado = assertThrows<TipoChaveInvalidoException> {
            validator.validaTipoChave(KeyType.CPF, "chaveforadepadrao")
        }

        assertEquals("CPF Inválido", resultado.message)
    }

    @Test
    internal fun deveFalharAoValidarChavePixJaCadastrada() {
        val chave = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678912",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(chave)

        val validator = ChavePixValidator()
        val error = assertThrows<ChaveDuplicadaException> {
            validator.validaChaveJaExistente(chave.clientId, keyType = KeyType.CPF, keyRepository)
        }

        assertEquals("usuário já tem chave do tipo CPF cadastrada", error.message)
    }
}