package com.walcker.games.features.domain.shared.error

internal sealed class GamesError(
    override val message: String = "Erro",
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    class Network(
        message: String = "Erro de conexão. Verifique sua internet.",
        cause: Throwable? = null,
    ) : GamesError(message, cause)

    class NotFound(
        val resource: String = "Recurso",
        cause: Throwable? = null,
    ) : GamesError("$resource não encontrado.", cause)

    class PermissionDenied(
        message: String = "Você não tem permissão para isso.",
        cause: Throwable? = null,
    ) : GamesError(message, cause)

    class ValidationError(
        message: String = "Dados inválidos.",
        cause: Throwable? = null,
    ) : GamesError(message, cause)

    class Unknown(
        message: String = "Erro desconhecido. Tente novamente.",
        cause: Throwable? = null,
    ) : GamesError(message, cause)
}

internal fun Throwable.toGamesError(): GamesError {
    val message = message ?: "Erro desconhecido"
    return when {
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
