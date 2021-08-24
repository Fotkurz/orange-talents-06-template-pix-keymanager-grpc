package keymanagergrpc.br.com.guilherme.modelo

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import net.bytebuddy.implementation.bytecode.Throw
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.persistence.PersistenceException
import javax.validation.ConstraintViolationException

@MicronautTest(transactional = false)
internal class ChavePixTest(val keyRepository: KeyRepository) {

    /*
    TODO: OK -Deve instanciar corretamente uma chave pix
    TODO: Deve falhar ao tentar instanciar chave com tipoChave nulo
    TODO: Deve falhar ao tentar instanciar chave com chave nulo
    TODO: Deve falhar ao tentar instanciar chave com clientId nulo
    TODO: Deve falhar ao tentar instanciar chave com tipoConta nulo
    TODO: OK - Deve falhar ao tentar instanciar chave com chave duplicada
     */

    @BeforeEach
    internal fun setUp() {
        keyRepository.deleteAll()
    }

    @Test
    internal fun deveInstanciarCorretamenteMinhaChavePix() {

        val novaChave = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678910",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(novaChave)
        val lista = keyRepository.findAll()

        assertNotNull(lista)
        assertEquals(1, lista.count())
        assertEquals("12345678910", lista.elementAt(0).chave)

    }

    @Test
    internal fun naoDeveSalvarUmaChavePixRepetida() {

        val chave1 = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678910",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        val chave2 = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "98765432110",
            clientId = "2",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        val repetida = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "12345678910",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(chave1)
        keyRepository.save(chave2)
        val excecao = assertThrows<PersistenceException> {
            keyRepository.save(repetida)
        }

        val lista = keyRepository.findAll()
        assertNotNull(lista)
        assertEquals(2, lista.count())
        assertEquals("12345678910", lista.elementAt(0).chave)

    }

    @Test
    internal fun deveFalharAoInstanciarChavePixComChaveNula() {
        val novaChave = ChavePix(
            tipoChave = TipoChave.CPF,
            chave = "",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        assertThrows<ConstraintViolationException> {
            keyRepository.save(novaChave)
        }

        val lista = keyRepository.findAll()

        assertEquals(0, lista.count())
    }

    @Test
    internal fun deveGerarUmaChaveAleatoriaAoCadastrarTipoRandom() {
        val novaChave = ChavePix(
            tipoChave = TipoChave.RANDOMKEY,
            chave = "",
            clientId = "1",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        keyRepository.save(novaChave)

        val lista = keyRepository.findAll()
        assertEquals(1, lista.count())
        assertNotNull(lista.elementAt(0).chave)
    }
}