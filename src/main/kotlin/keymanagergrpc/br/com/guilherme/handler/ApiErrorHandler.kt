package keymanagergrpc.br.com.guilherme.handler

import io.grpc.Status
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.core.exceptions.ExceptionHandler
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
@InterceptorBean(InterceptAndValidate::class)
class ApiErrorHandler: MethodInterceptor<Any?, Any?>{

    val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun intercept(context: MethodInvocationContext<Any?, Any?>?): Any? {

        LOGGER.info(context?.targetMethod.toString())

        try {
            context?.proceed()
        } catch(e: RuntimeException) {
            when(e){
                is TipoContaInvalidoException -> LOGGER.info("Deu ruim ${e.message}")
            }
        }
        return null
    }
}