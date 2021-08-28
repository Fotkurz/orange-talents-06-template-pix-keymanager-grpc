package keymanagergrpc.br.com.guilherme.handler

import javax.inject.Singleton

@Singleton
open class ApiErrorException(s: String): RuntimeException(s)

@Singleton
class TipoContaInvalidoException(s: String): ApiErrorException(s)

@Singleton
class TipoChaveInvalidoException(s: String): ApiErrorException(s)