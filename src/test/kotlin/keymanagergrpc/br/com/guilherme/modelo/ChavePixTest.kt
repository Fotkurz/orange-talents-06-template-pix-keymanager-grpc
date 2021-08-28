package keymanagergrpc.br.com.guilherme.modelo

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.persistence.PersistenceException
import javax.validation.ConstraintViolationException

@MicronautTest(transactional = false)
internal class ChavePixTest(val keyRepository: KeyRepository) {

    @BeforeEach
    internal fun setUp() {
        keyRepository.deleteAll()
    }

    @Test
    internal fun deveInstanciarCorretamenteAChavePix() {

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
    internal fun deveGerarUmaChaveAleatoriaAoCadastrarTipoRandom() {
        val novaChave = ChavePix(
            tipoChave = TipoChave.RANDOM,
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