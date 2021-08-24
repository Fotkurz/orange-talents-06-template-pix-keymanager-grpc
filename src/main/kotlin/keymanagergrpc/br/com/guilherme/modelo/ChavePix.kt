package keymanagergrpc.br.com.guilherme.modelo


import org.hibernate.annotations.GenericGenerator
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(name = "tbl_pix")
class ChavePix(
    @field:Column(nullable = false)
    @field:Enumerated(EnumType.STRING)
    @field:NotNull
    val tipoChave: TipoChave,

    @field:Column(nullable = false, unique = true, length = 77)
    @field:NotBlank
    var chave: String?,

    @field:Column(nullable = false)
    @field:NotBlank
    val clientId: String,

    @field:Column(nullable = false)
    @field:Enumerated(EnumType.STRING)
    @field:NotNull
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
        if (this.tipoChave == TipoChave.RANDOMKEY && this.chave!!.isNullOrBlank()) {
            return UUID.randomUUID().toString()
        }
        return this.chave!!
    }

}
