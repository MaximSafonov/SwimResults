# VK ID в Android-сборке SwimResults

Приложение VK ID пока не зарегистрировано. Обычная сборка использует `NotConfigured`: гостевой профиль, тема, калькулятор, таймер и локальная история доступны; SDK, его manifest, init и запросы отсутствуют. На iOS вход имеет состояние `Unsupported`.

## Включение после регистрации

Зарегистрируйте Android-приложение `com.msafonov.swimresults` с нужными сертификатами в [кабинете VK ID](https://id.vk.ru/business/go). Получите настоящие параметры поддерживаемой mobile SDK схемы. Не помещайте server-only secret в APK. Mobile-параметр `VKIDClientSecret`, который требует официальный SDK manifest, виден в распространяемом APK и не является серверным защищённым хранилищем.

В игнорируемом `local.properties` или Gradle properties задаются только четыре ключа: `VKIDClientID`, `VKIDClientSecret`, `VKIDRedirectHost`, `VKIDRedirectScheme`. Значения в этом документе намеренно отсутствуют. ID — положительное Int без ведущих нулей, host — `vk.ru`, scheme — `vk` плюс этот реальный ID. Все поля обязательны. Неполная/некорректная конфигурация выбирает unconfigured путь без фиктивных значений. Не передавайте credentials через командную строку, исходники, логи или отчёты.

Gradle подключает ровно один дополнительный каталог существующего `androidMain`: `androidVkIdConfigured` либо `androidVkIdUnconfigured`. Только configured использует `com.vk.id:vkid:2.7.3`, `com.vk.id:onetap-compose:2.7.3` и официальные placeholders; отдельный Gradle plugin не требуется. VK Maven content ограничен `com.vk` с подгруппами и `ru.ok.tracer`. Фактическое разрешение транзитивных зависимостей ещё не проверено.

## Поведение

Вход запускает публичный OneTap SignIn по действию пользователя: fast auth выключен, выбор другого аккаунта включён, дополнительных scopes и подписок нет. Открытие вкладки не запускает authorize. OAuth/PKCE, redirect receiver и зашифрованное хранение credentials принадлежат SDK; приложение не разбирает произвольные intents и не хранит вторую копию токенов. Общий код получает только ID/имя principal. SDK init однократный, при восстановлении разрешённого сеанса или показе виджета; возможна стандартная SDK analytics. `logsEnabled=false`.

Приложение хранит один несекретный DataStore-флаг разрешения восстановления, default false. Успешный реальный login разрешает restore. Выход сначала надёжно запрещает restore и публикует гостевой контекст, затем выполняет ограниченный по времени публичный SDK logout. Ошибка записи запрета не объявляется успешным выходом. При сбое VK ID локальная сессия уже закрыта, очистка SDK credentials/серверный logout не обещаются; доступна повторная попытка. После рестарта suppression препятствует самопроизвольному возврату аккаунта.

Валидный SDK cache восстанавливает текущий аккаунт без сети; expiry измеряется Unix milliseconds (`-1` — без срока). При истечении account-контекст скрывается и выполняется одна ограниченная попытка публичного refresh. Сетевой сбой не считается отзывом credentials; UI предлагает повторный вход. Все callbacks и локальные commits проверяют epoch. SDK networking не держит DB/session lock.

Аккаунт VK и спортсмены — разные сущности; данные владельца не создают спортсмена. Это локальный namespace, а не backend ACL или синхронизация. Тема независима от аккаунта. Свежая Room v3 хранит полную дату рождения и разряд спортсмена. Установленных пользователей нет, поэтому миграции и backfill не предусмотрены. Заплывы с `athleteId=NULL` доступны в разделе «На этом устройстве · без спортсмена». Login и создание спортсмена их не назначают. Для назначения требуется явный выбор до 200 конкретных результатов, свой спортсмен и отдельное подтверждение точного списка; одна Room transaction меняет только `athleteId`. Отмена не пишет данные.

## Проверки

Текущий статус: **configured adapter NOT_COMPILED, SDK auth NOT_VALIDATED, schema3 NOT_GENERATED**. Исходник Android adapter подготовлен по публичным API, но регистрацию и настоящую совместимость SDK он не доказывает. Новую schema3 создаёт только KSP при отдельно согласованной Verification. Экспорты v1 и v2 неизменяемы; они не являются upgrade path установленной базы.

Отдельно согласовать offline common tests/unconfigured Android+iOS compilation, KSP schema3, fresh-v3 instrumentation на разрешённом устройстве; тесты используют только уникальные disposable test-cache DB и не открывают базу приложения. Для configured compilation нужны реальная конфигурация и разрешение точных SDK/transitive downloads. Не использовать выдуманный clientID, demo account или production bypass ради проверки.

После этого согласовать SDK login с тестовым VK аккаунтом: explicit tap/cancel/error/retry, возврат из VK/browser, cached restart, expiry/refresh, online/offline logout и restart, account A/B; merged manifest receiver/host/scheme/exported components и source selection. UI QA: guest/settings/back, локальная история/CRUD/share, несколько спортсменов, длинные имена/крупный шрифт/узкое окно, поворот и восстановление формы, dirty exit, смена active athlete при черновике, logout при форме/confirmation, назначение/отмена точного списка. До разрешённого выполнения всё **NOT_RUN**.

Публичные источники: [SDK README/manifest placeholders](https://github.com/VKCOM/vkid-android-sdk#установка), [VKID public API](https://vkcom.github.io/vkid-android-sdk/vkid/com.vk.id/-v-k-i-d/index.html), [OneTap](https://vkcom.github.io/vkid-android-sdk/onetap-compose/com.vk.id.onetap.compose.onetap/-one-tap.html), [AccessToken](https://github.com/VKCOM/vkid-android-sdk/blob/master/sdk/core/vkid/src/main/java/com/vk/id/AccessToken.kt), [refresh failures](https://vkcom.github.io/vkid-android-sdk/vkid/com.vk.id.refresh/-v-k-i-d-refresh-token-fail/index.html).
