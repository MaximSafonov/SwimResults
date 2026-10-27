package com.msafonov.swimresults.auth

import androidx.compose.runtime.Composable
import com.msafonov.swimresults.data.storage.AuthSessionPolicyStorage

/**
 * Compose-точка подготовки платформенного механизма авторизации.
 * Настроенный Android обеспечивает инициализацию VK ID; остальные текущие реализации
 * ничего не делают. Пользовательская авторизация этим вызовом не начинается.
 * Инициализация может выполняться и вне композиции, в том числе из Android Application.
 */
@Composable
expect fun InitializeAuthPlatform()

/**
 * Создаёт платформенный адаптер авторизации, связанный с общим состоянием и локальной политикой.
 * Создание само по себе не вызывает [AuthSession.restore] и не возвращает токены.
 *
 * @param session получатель публикуемых адаптером состояний входа.
 * @param policy локальное хранилище разрешения на восстановление сессии.
 * @return платформенная реализация [AuthSession].
 */
expect fun createAuthSession(session: AccountSession, policy: AuthSessionPolicyStorage): AuthSession

/**
 * Отображает предоставленный провайдером виджет входа для платформенного адаптера [auth].
 * Пользовательское взаимодействие и callbacks обрабатывает платформа; само отображение
 * не должно запускать авторизацию. Текущие реализации iOS и ненастроенного Android
 * не отображают виджет.
 *
 * @param auth адаптер соответствующей платформы, созданный через [createAuthSession].
 */
@Composable
expect fun AuthSignIn(auth: AuthSession)
