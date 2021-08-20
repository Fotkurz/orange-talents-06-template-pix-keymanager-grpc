package keymanagergrpc.br.com.guilherme.repository

import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave
import java.util.*

@Repository
interface KeyRepository: CrudRepository<ChavePix, UUID>{

    fun existsByClientIdAndTipoChave(clientId: String, tipoChave: TipoChave): Boolean

    fun findByClientId(clientId: String): ChavePix

}
