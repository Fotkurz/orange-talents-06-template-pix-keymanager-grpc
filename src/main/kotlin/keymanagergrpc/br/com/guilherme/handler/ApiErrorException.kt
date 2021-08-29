package keymanagergrpc.br.com.guilherme.handler

open class ApiErrorException(s: String): RuntimeException(s)

class TipoContaInvalidoException(s: String): ApiErrorException(s)

class TipoChaveInvalidoException(s: String): ApiErrorException(s)

class ChaveDuplicadaException(s: String): ApiErrorException(s)

class ValidacaoErpItauException(s: String): ApiErrorException(s)

class ValidacaoBcbException(s: String): ApiErrorException(s)

class ExistenciaDeChaveException(s: String): ApiErrorException(s)