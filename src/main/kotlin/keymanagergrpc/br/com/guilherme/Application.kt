package keymanagergrpc.br.com.guilherme

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("keymanagergrpc.br.com.guilherme")
		.start()
}

