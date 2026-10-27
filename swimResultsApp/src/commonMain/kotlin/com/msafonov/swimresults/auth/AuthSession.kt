package com.msafonov.swimresults.auth

/**
 * Подтверждённая провайдером идентичность, передаваемая через платформенную границу.
 * Содержит идентификатор пользователя и отображаемое имя, но не хранит токены.
 */
data class AccountPrincipal(val userId: String, val displayName: String) {
    /**
     * Формирует стабильный локальный ключ `vk:<userId>` для пространства данных учётной записи.
     * Ключ зависит от [userId], а не от отображаемого имени, и не является токеном авторизации.
     */
    val accountId: String get() = "vk:$userId"
    /**
     * Отклоняет пустой или состоящий только из пробельных символов [userId]
     * с [IllegalArgumentException]. Не проверяет токен или числовой формат идентификатора.
     */
    init { require(userId.isNotBlank()) }
}

/** Категории ошибок входа и выхода без токенов и платформенных объектов исключений. */
enum class AuthError { Storage, Network, Provider, Expired, LogoutUnconfirmed }

/**
 * Общее состояние входа: конфигурация платформы, восстановление, гость,
 * подтверждённая учётная запись или необходимость повторной авторизации.
 * Подтверждённую идентичность содержит только [Authenticated].
 */
sealed interface AuthState {
    data object NotConfigured : AuthState
    data object Unsupported : AuthState
    data object Restoring : AuthState
    data class Guest(val error: AuthError? = null) : AuthState
    data class Authenticated(val principal: AccountPrincipal) : AuthState
    data class ReauthenticationRequired(val error: AuthError) : AuthState
}

interface AuthSession {
    /**
     * Запрашивает платформенное восстановление доступной сессии и публикацию результата
     * через [AccountSession], переданный при создании адаптера. Успешный вход не гарантирован.
     *
     * Настроенный Android-адаптер учитывает сохранённое разрешение восстановления,
     * доступный кэш SDK и при необходимости обновляет токен. Неподдерживаемая или
     * ненастроенная реализация публикует соответствующее состояние [AuthState].
     * Ограничение числа попыток восстановления зависит от реализации, а не от интерфейса.
     * Объявление не маскирует исключения и отмену: их обработка определяется платформой.
     */
    suspend fun restore()
    /**
     * Запрашивает платформенный выход из учётной записи.
     * В настроенном Android-адаптере сначала сохраняется запрет автоматического
     * восстановления и публикуется [AuthState.Guest], затем вызывается выход в SDK.
     * Если SDK не подтвердил выход, состояние гостя содержит [AuthError.LogoutUnconfirmed].
     *
     * Текущие реализации iOS и ненастроенного Android не выполняют действий.
     * Общая декларация не гарантирует подтверждение выхода провайдером и не маскирует
     * исключения или отмену; детали обработки зависят от платформенной реализации.
     */
    suspend fun logout()
}
