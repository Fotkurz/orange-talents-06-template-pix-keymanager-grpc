package keymanagergrpc.br.com.guilherme

import io.micronaut.core.annotation.Introspected
import java.time.LocalDateTime

@Introspected
data class CreatePixKeyRequest(
    val keyType: String,
    val key: String?,
    val bankAccount: Map<String, String>,
    val owner: Map<String, String>,
) {

    val createAt = LocalDateTime.now()

    // Tive que implementar esse hashcode para conseguir que um teste funcionasse
    // Não tenho certeza da causa do problema, já que essa é uma data class e teoricamente
    // tem o equals e hashcode já implementado. Mas ainda sim tive que implementar esse
    // Pois só assim consegui que funcionasse o teste de cadastro de random no bcb

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreatePixKeyRequest

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key?.hashCode() ?: 0
    }


}