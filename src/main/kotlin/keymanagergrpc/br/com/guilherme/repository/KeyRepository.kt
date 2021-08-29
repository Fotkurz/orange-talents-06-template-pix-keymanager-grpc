package keymanagergrpc.br.com.guilherme.repository

import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import keymanagergrpc.br.com.guilherme.modelo.ChavePix
import keymanagergrpc.br.com.guilherme.modelo.TipoChave

@Repository
interface KeyRepository: CrudRepository<ChavePix, String>{

    fun existsByClientIdAndTipoChave(clientId: String, tipoChave: TipoChave): Boolean
    fun findByClientId(clientId: String): List<ChavePix>
    fun findByClientIdAndPixId(clientId: String, pixid: String): ChavePix?

}
