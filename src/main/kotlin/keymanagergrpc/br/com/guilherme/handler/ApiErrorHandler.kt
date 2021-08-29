package keymanagergrpc.br.com.guilherme.handler

import io.grpc.BindableService
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import keymanagergrpc.br.com.guilherme.repository.KeyRepository
import keymanagergrpc.br.com.guilherme.validacao.ChavePixValidator
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@InterceptorBean(InterceptAndValidate::class)
class ApiErrorHandler(@Inject val repository: KeyRepository) : MethodInterceptor<BindableService, Any> {

    val LOGGER = LoggerFactory.getLogger(this.javaClass)

    /*
    TODO: Arrumar essa classe para chamar as validações antes mesmo de chegar no método
     */

    override fun intercept(context: MethodInvocationContext<BindableService, Any?>?): Any? {

        LOGGER.info("Interceptando método: ${context!!.methodName}")
        val validator = ChavePixValidator()

        try {
            if (context != null) {
                    context?.proceed()
//                    val request = context.parameterValues[0] as CreateRequest
//                    val respostaValidator = validator.validaCreateRequest(request, repository)
//                    LOGGER.info(respostaValidator.toString())
            }
        } catch (e: ApiErrorException) {

            val error = when(e) {
                is TipoContaInvalidoException, is TipoChaveInvalidoException ->
                    Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
                is ValidacaoBcbException ->
                    Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
                is ValidacaoErpItauException ->
                    Status.PERMISSION_DENIED.withDescription(e.message).asRuntimeException()
                is ChaveDuplicadaException ->
                    Status.ALREADY_EXISTS.withDescription(e.message).asRuntimeException()
                is ExistenciaDeChaveException ->
                    Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
                else -> Status.UNKNOWN.withDescription("Excecao desconhecida").asRuntimeException()
            }

            val responseObserver = context.parameterValues[1] as StreamObserver<*>?
            responseObserver!!.onError(error)
        }
        return null
    }

}