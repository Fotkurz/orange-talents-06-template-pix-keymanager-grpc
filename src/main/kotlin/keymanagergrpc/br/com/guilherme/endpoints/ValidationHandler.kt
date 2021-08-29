package keymanagergrpc.br.com.guilherme.endpoints

import io.micronaut.aop.Around

@Around
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidationHandler
