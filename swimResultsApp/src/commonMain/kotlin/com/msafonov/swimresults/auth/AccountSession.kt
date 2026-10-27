package com.msafonov.swimresults.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

/**
 * Неизменяемый снимок состояния входа и поколения сессии [epoch].
 * При каждой публикации через [AccountSession.change] поколение увеличивается,
 * поэтому даже одинаковое состояние входа после перехода образует новый снимок.
 */
data class SessionSnapshot(val epoch: Long = 0, val auth: AuthState = AuthState.Restoring) {
    /** Возвращает подтверждённую идентичность только для [AuthState.Authenticated], иначе `null`. */
    val principal: AccountPrincipal? get() = (auth as? AuthState.Authenticated)?.principal
    /** Возвращает локальный ключ подтверждённой учётной записи через [principal] либо `null`. */
    val accountId: String? get() = principal?.accountId
}

/** Локальная операция отклонена: переданный снимок уже не совпадает с текущей сессией. */
class StaleSessionException : IllegalStateException("Account session changed")

/**
 * Сериализует короткие локальные операции и публикацию состояния сессии общим mutex.
 * Сетевые обращения к провайдеру должны выполняться за пределами этой блокировки.
 */
@Single
class AccountSession {
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(SessionSnapshot())
    /** Поток опубликованных снимков; подписчики могут читать состояние, но не изменять его напрямую. */
    val state = mutable.asStateFlow()
    /**
     * Возвращает текущее значение потока без захвата mutex.
     * Снимок может устареть сразу после чтения; для защищённой локальной операции нужен [guard].
     */
    val current: SessionSnapshot get() = mutable.value

    /**
     * Публикует [auth] в новом поколении сессии после выполнения [beforePublication].
     * Под общим mutex сравнивает весь [expected] с текущим снимком. Если они различаются,
     * возвращает `false`, не вызывая callback и не публикуя состояние. Значение `null`
     * отключает проверку актуальности ожидаемого снимка.
     *
     * При принятом переходе выполняет callback под той же блокировкой, затем публикует
     * снимок с `epoch + 1` и возвращает `true`. Callback предназначен для короткого
     * локального сохранения, а не сетевых операций. Mutex нерекурсивный: из callback
     * нельзя повторно входить в [change] или [guard].
     *
     * Исключения и отмена callback распространяются вызывающему коду: новый снимок
     * не публикуется, но уже выполненные внешние действия автоматически не откатываются.
     *
     * @param auth состояние входа для нового снимка.
     * @param expected ожидаемый полный снимок или `null` для перехода без проверки снимка.
     * @param beforePublication действие, которое должно завершиться до публикации.
     * @return `true`, если новый снимок опубликован; `false`, если [expected] устарел.
     */
    suspend fun change(auth: AuthState, expected: SessionSnapshot? = null, beforePublication: suspend () -> Unit = {}): Boolean = mutex.withLock {
        if (expected != null && current != expected) return@withLock false
        beforePublication()
        mutable.value = SessionSnapshot(current.epoch + 1, auth)
        true
    }

    /**
     * Выполняет короткую локальную операцию [block], пока [snapshot] остаётся текущим.
     * Под общим с [change] mutex сравнивает полный снимок; при несовпадении бросает
     * [StaleSessionException] до вызова [block]. Иначе удерживает mutex до завершения
     * блока, сериализуя его с публикациями, и возвращает результат блока.
     *
     * Исключения и отмена распространяются вызывающему коду; откат побочных эффектов
     * не предоставляется. В блоке нельзя выполнять сетевые операции провайдера или
     * повторно вызывать [guard] и [change]: mutex нерекурсивный.
     *
     * @param snapshot полный снимок, для которого допустима операция.
     * @param block локальная операция, выполняемая под блокировкой.
     * @return результат [block].
     * @throws StaleSessionException если снимок устарел до начала операции.
     */
    suspend fun <T> guard(snapshot: SessionSnapshot, block: suspend () -> T): T = mutex.withLock {
        if (current != snapshot) throw StaleSessionException()
        block()
    }

    /**
     * Однократно сравнивает полный [snapshot] с текущим снимком без захвата mutex.
     * Возвращает `true` при равенстве, но не резервирует актуальность для следующих
     * действий: состояние может сразу измениться. Для защищённой операции нужен [guard].
     */
    fun isCurrent(snapshot: SessionSnapshot): Boolean = current == snapshot
}
