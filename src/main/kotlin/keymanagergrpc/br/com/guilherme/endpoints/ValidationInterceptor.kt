package keymanagergrpc.br.com.guilherme.endpoints

import io.grpc.BindableService
import io.grpc.Status
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
@InterceptorBean(ValidationHandler::class)
class ValidationInterceptor: MethodInterceptor<BindableService, Any?> {

    val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun intercept(context: MethodInvocationContext<BindableService, Any?>): Any? {
        try{
            return context.proceed()
        } catch (e: Exception) {
            e.printStackTrace()

            val statusError = when(e) {
                is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
                is IllegalStateException -> Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
                else -> Status.UNKNOWN.withDescription("erro inesperado").asRuntimeException()
            }
            return null
        }
    }

}
