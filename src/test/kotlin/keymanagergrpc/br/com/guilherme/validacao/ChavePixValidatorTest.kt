package keymanagergrpc.br.com.guilherme.validacao

import io.grpc.Status
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import keymanagergrpc.br.com.guilherme.AccountType
import keymanagergrpc.br.com.guilherme.KeyType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@MicronautTest(transactional = false)
internal class ChavePixValidatorTest {

    internal val validator = ChavePixValidator()

    @Test
    internal fun deveValidarCorretamenteTipoContaContaCorrente() {
        val resultado = validator.validaTipoConta(AccountType.CONTA_CORRENTE)
        assertEquals(null, resultado)
    }

    @Test
    internal fun deveValidarCorretamenteTipoContaPoupanca() {
        val resultado = validator.validaTipoConta(AccountType.CONTA_POUPANCA)
        assertEquals(null, resultado)
    }

//    @Test
//    internal fun deveFalharAoValidarTipoContaInvalidoOuUnknown() {
//        val resultado = validator.validaTipoConta(AccountType.UNKNOWN_ACCOUNT)
//        assertEquals(Status.INVALID_ARGUMENT.code, resultado?.status?.code)
//        assertEquals("Tipo de conta é obrigatório", resultado?.status?.description)
//    }

    @Test
    internal fun deveValidarCorretamenteTipoChave() {
        val resultado = validator.validaTipoChave(KeyType.CPF, "12345678912")
        assertNull(resultado)
    }

    @Test
    internal fun deveFalharAoTentarValidarTipoChaveDesconhecido() {
        val resultado = validator.validaTipoChave(KeyType.UNKNOWN_TYPE, "chaveforadepadrao")
        assertEquals(Status.INVALID_ARGUMENT.code, resultado?.status?.code)
        assertEquals("Tipo chave é obrigatório", resultado?.status?.description)
    }

    @Test
    internal fun deveFalharAoCadastrarTipoRandomComValor() {
        val resultado = validator.validaTipoChave(KeyType.RANDOM, "chaveforadepadrao")
        assertEquals(Status.INVALID_ARGUMENT.code, resultado?.status?.code)
        assertEquals("Chave Aleatória não pode ter valor associado", resultado?.status?.description)
    }

    @Test
    internal fun deveFalharAoValidarTipoEmailInvalido() {
        val resultado = validator.validaTipoChave(KeyType.EMAIL, "chaveforadepadrao")
        assertEquals(Status.INVALID_ARGUMENT.code, resultado?.status?.code)
        assertEquals("EMAIL Inválido", resultado?.status?.description)
    }

    @Test
    internal fun deveFalharAoValidarTipoCelularInvalido() {
        val resultado = validator.validaTipoChave(KeyType.CELULAR, "chaveforadepadrao")
        assertEquals(Status.INVALID_ARGUMENT.code, resultado?.status?.code)
        assertEquals("CELULAR Inválido", resultado?.status?.description)
    }

    @Test
    internal fun deveFalharAoValidarTipoCpfInvalido() {
        val resultado = validator.validaTipoChave(KeyType.CPF, "chaveforadepadrao")
        assertEquals(Status.INVALID_ARGUMENT.code, resultado?.status?.code)
        assertEquals("CPF Inválido", resultado?.status?.description)
    }
}