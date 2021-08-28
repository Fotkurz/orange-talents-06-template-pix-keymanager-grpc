package keymanagergrpc.br.com.guilherme.modelo


import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(name = "tbl_pix")
data class ChavePix(
    @field:Column(nullable = false)
    @field:Enumerated(EnumType.STRING)
    @field:NotNull
    val tipoChave: TipoChave,

    @field:Column(unique = true)
    var chave: String?,

    @field:Column(nullable = false)
    @field:NotBlank
    val clientId: String,

    @field:Column(nullable = false)
    @field:Enumerated(EnumType.STRING)
    @field:NotNull
    val tipoConta: TipoConta
) {

    @Id
    var pixId: String = UUID.randomUUID().toString()
}
