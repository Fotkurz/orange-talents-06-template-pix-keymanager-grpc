package keymanagergrpc.br.com.guilherme.modelo

import java.util.*

enum class TipoChave {
    CPF,
    CELULAR,
    EMAIL,
    RANDOMKEY;

    fun randomKeyGenerator(): String? {
        if(this.equals(RANDOMKEY))
            return UUID.randomUUID().toString()
        return null
    }

    fun checkKey(): Boolean {
        if(this.equals(RANDOMKEY)) return true
        return false
    }
}