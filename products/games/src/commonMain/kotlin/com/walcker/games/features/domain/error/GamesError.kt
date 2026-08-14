package com.walcker.games.features.domain.error

/**
 * Erros do produto de partidas: leitura, busca, join, validações de negócio.
 * Categoriza exceções do Firestore/Network em tipos de domínio.
 */
internal sealed class GamesError(
    override val message: String = "Erro",
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Falha de rede ou conexão ao Firestore.
     */
    class Network(
        message: String = "Erro de conexão. Verifique sua internet.",
        cause: Throwable? = null,
    ) : GamesError(message, cause)

    /**
     * Partida ou jogador não encontrado.
     */
    class NotFound(
        val resource: String = "Recurso",
        cause: Throwable? = null,
    ) : GamesError("$resource não encontrado.", cause)

    /**
     * Falta de permissão para realizar ação (não autenticado, etc).
     */
    class PermissionDenied(
        message: String = "Você não tem permissão para isso.",
        cause: Throwable? = null,
    ) : GamesError(message, cause)

    /**
     * Erro de validação de negócio (lotação cheia, não autenticado para join, etc).
     */
    class ValidationError(
        message: String = "Dados inválidos.",
        cause: Throwable? = null,
    ) : GamesError(message, cause)

    /**
     * Erro desconhecido ou não mapeado.
     */
    class Unknown(
        message: String = "Erro desconhecido. Tente novamente.",
        cause: Throwable? = null,
    ) : GamesError(message, cause)
}

/**
 * Map exceptions to GamesError for domain boundary crossing.
 */
internal fun Throwable.toGamesError(): GamesError {
    val message = message ?: "Erro desconhecido"
    return when {
        // Firestore errors (por padrão todas são Network no MVP)
        message.contains("Network", ignoreCase = true) ||
            message.contains("Connection", ignoreCase = true) ||
            message.contains("UNAVAILABLE", ignoreCase = true) -> {
            GamesError.Network(cause = this)
        }

        message.contains("NOT_FOUND", ignoreCase = true) ||
            message.contains("not found", ignoreCase = true) -> {
            GamesError.NotFound(cause = this)
        }

        message.contains("PERMISSION_DENIED", ignoreCase = true) ||
            message.contains("permission", ignoreCase = true) -> {
            GamesError.PermissionDenied(cause = this)
        }

        message.contains("INVALID_ARGUMENT", ignoreCase = true) ||
            message.contains("validation", ignoreCase = true) -> {
            GamesError.ValidationError(message = message)
        }

        else -> GamesError.Unknown(cause = this)
    }
}
