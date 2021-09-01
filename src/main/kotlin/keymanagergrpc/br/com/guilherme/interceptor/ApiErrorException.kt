package keymanagergrpc.br.com.guilherme.interceptor

open class ApiErrorException(s: String): RuntimeException(s)

// Exceção para tipo de conta inválido para o sistema
class TipoContaInvalidoException(s: String): ApiErrorException(s)

// Exceção para tipo chave inválido ou fora do padrão
class TipoChaveInvalidoException(s: String): ApiErrorException(s)

// Exceção para duplicidade de chave para o mesmo cliente
class ChaveDuplicadaException(s: String): ApiErrorException(s)

// Exceção para retorno do Itau
class ValidacaoErpItauException(s: String): ApiErrorException(s)

// Exceção para o retorno do BCB
class ValidacaoBcbException(s: String): ApiErrorException(s)

// Exceção para falta de existência de chave
class ExistenciaDeChaveException(s: String): ApiErrorException(s)