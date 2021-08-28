package keymanagergrpc.br.com.guilherme.endpoints

import io.micronaut.aop.Around
import io.micronaut.context.annotation.Type

@Around
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidationHandler
