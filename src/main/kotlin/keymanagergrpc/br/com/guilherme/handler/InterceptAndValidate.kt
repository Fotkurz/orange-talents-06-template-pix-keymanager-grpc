package keymanagergrpc.br.com.guilherme.handler

import io.micronaut.aop.Around

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Around
annotation class InterceptAndValidate()
