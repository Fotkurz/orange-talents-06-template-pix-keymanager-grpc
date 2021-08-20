package keymanagergrpc.br.com.guilherme.modelo


import org.hibernate.annotations.GenericGenerator
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "tbl_pix")
class ChavePix(
    @field:Column(nullable = false)
    @field:Enumerated(EnumType.STRING)
    val tipoChave: TipoChave,

    @field:Column(nullable = false, unique = true, length = 77)
    var chave: String,

    @field:Column(nullable = false)
    val clientId: String,

    @field:Column(nullable = false)
    @field:Enumerated(EnumType.STRING)
    val tipoConta: TipoConta
) {


    init {
        this.chave = setRandomKey()
    }

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    var pixId: UUID? = null

    fun setRandomKey(): String {
        if (this.tipoChave == TipoChave.RANDOMKEY && this.chave.isBlank()) {
            return UUID.randomUUID().toString()
        }
        return this.chave
    }


}

enum class TipoConta {
    CONTA_CORRENTE, CONTA_POUPANCA
}


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