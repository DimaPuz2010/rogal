package ru.myitschool.rogal.networking;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

/**
 * API для взаимодействия с сервером таблицы лидеров
 */
public class LeaderboardAPI {
    /**
     * Константы для серверных настроек по умолчанию
     */
    private static final String DEFAULT_SERVER_PROTOCOL = "http";
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int DEFAULT_SERVER_PORT = 433;
    private static final String DEFAULT_SERVER_PATH = "/api/leaderboard";
    // Ключи для сохранения настроек
    private static final String PREF_KEY_SERVER_HOST = "leaderboard_server_host";
    private static final String PREF_KEY_SERVER_PORT = "leaderboard_server_port";
    private static final String PREF_KEY_SERVER_PROTOCOL = "leaderboard_server_protocol";
    private static final String PREF_KEY_SERVER_PATH = "leaderboard_server_path";
    /**
     * Текущие настройки сервера
     */
    private static String SERVER_PROTOCOL = DEFAULT_SERVER_PROTOCOL;
    private static String SERVER_HOST = DEFAULT_SERVER_HOST;
    private static int SERVER_PORT = DEFAULT_SERVER_PORT;
    private static String SERVER_PATH = DEFAULT_SERVER_PATH;

    /**
     * Возвращает базовый URL сервера без эндпоинта
     *
     * @return базовый URL сервера
     */
    private static String getServerUrl() {
        return SERVER_PROTOCOL + "://" + SERVER_HOST + ":" + SERVER_PORT;
    }

    private static String getLeaderboardUrl() {
        return getServerUrl() + SERVER_PATH;
    }

    /**
     * Инициализирует настройки сервера из сохраненных предпочтений
     */
    public static void initialize() {
        try {
            loadServerSettings();
        } catch (Exception e) {
            LogHelper.error("LeaderboardAPI", "Ошибка при инициализации: " + e.getMessage());
        }
    }

    /**
     * Устанавливает новый адрес и порт сервера и сохраняет настройки
     *
     * @param host адрес сервера
     * @param port порт сервера
     */
    public static void setServerAddress(String host, String port) {
        SERVER_HOST = host;
        SERVER_PORT = Integer.parseInt(port);

        try {
            // Сохраняем настройки через универсальный метод сохранения
            saveServerSettings();

            LogHelper.log("LeaderboardAPI", "Настройки сервера обновлены: " + host + ":" + port);
        } catch (Exception e) {
            LogHelper.error("LeaderboardAPI", "Ошибка при сохранении настроек: " + e.getMessage());
        }
    }

    /**
     * Получает текущий адрес сервера
     */
    public static String getServerHost() {
        return SERVER_HOST;
    }

    public static void setServerHost(String host) {
        SERVER_HOST = host;
        saveServerSettings();
    }

    /**
     * Получает текущий порт сервера
     */
    public static int getServerPort() {
        return SERVER_PORT;
    }

    public static void setServerPort(int port) {
        SERVER_PORT = port;
        saveServerSettings();
    }

    /**
     * Получает таблицу лидеров с сервера
     *
     * @param page     номер страницы (с 1)
     * @param size     размер страницы
     * @param listener слушатель результата
     */
    public static void getLeaderboard(int page, int size, final LeaderboardResponseListener listener) {
        try {
            // Запрашиваем все результаты - используем временно /api/leaderboard/top с большим лимитом
            // Т.к. эндпоинт для всех результатов с пагинацией отсутствует на сервере
            String url = getLeaderboardUrl() + "/top?limit=" + (size * 5);

            HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
            Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

            LogHelper.log("LeaderboardAPI", "Запрос таблицы лидеров: " + url);

            Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    try {
                        String responseString;
                        try {
                            responseString = new String(httpResponse.getResult(), StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            LogHelper.error("LeaderboardAPI", "ID-GL-001: Ошибка чтения результата: " + e.getMessage());
                            responseString = httpResponse.getResultAsString();
                        }

                        int statusCode = httpResponse.getStatus().getStatusCode();

                        LogHelper.log("LeaderboardAPI", "Получен ответ: код=" + statusCode + ", ответ=" + responseString);

                        if (statusCode == 200) {
                            try {
                                JsonReader jsonReader = new JsonReader();
                                JsonValue root = jsonReader.parse(responseString);

                                List<LeaderboardEntry> entries = new ArrayList<>();

                                // Проверяем формат ответа
                                if (root.isArray()) {
                                    // Ответ в виде массива - простая обработка
                                    Json json = new Json();
                                    for (JsonValue entryJson : root) {
                                        try {
                                            LeaderboardEntry entry = json.fromJson(LeaderboardEntry.class, entryJson.toString());
                                            entries.add(entry);
                                        } catch (Exception e) {
                                            LogHelper.error("LeaderboardAPI", "ID-GL-002: Ошибка парсинга записи: " + e.getMessage());
                                        }
                                    }
                                } else if (root.has("content") && root.get("content").isArray()) {
                                    // Ответ в формате с пагинацией
                                    try {
                                        int totalElements = root.getInt("totalElements", 0);
                                        int totalPages = root.getInt("totalPages", 0);
                                        int currentPage = root.getInt("number", 0) + 1; // API использует 0-индексацию

                                        JsonValue contentArray = root.get("content");
                                        Json json = new Json();
                                        for (JsonValue entryJson : contentArray) {
                                            try {
                                                LeaderboardEntry entry = json.fromJson(LeaderboardEntry.class, entryJson.toString());
                                                entries.add(entry);
                                            } catch (Exception e) {
                                                LogHelper.error("LeaderboardAPI", "ID-GL-003: Ошибка парсинга записи в массиве content: " + e.getMessage());
                                            }
                                        }

                                        // Вызываем колбэк успешного выполнения с пагинацией
                                        Gdx.app.postRunnable(() -> {
                                            try {
                                                listener.onSuccess(entries, totalElements, currentPage, totalPages);
                                            } catch (Exception e) {
                                                LogHelper.error("LeaderboardAPI", "ID-GL-004: Ошибка в колбэке onSuccess: " + e.getMessage());
                                            }
                                        });
                                        return;
                                    } catch (Exception e) {
                                        LogHelper.error("LeaderboardAPI", "ID-GL-005: Ошибка обработки формата с пагинацией: " + e.getMessage());
                                    }
                                }

                                // Эмулируем пагинацию для ответа без пагинации
                                try {
                                    int totalEntries = entries.size();
                                    int totalPages = (int) Math.ceil((double) totalEntries / size);

                                    // Ограничиваем результаты текущей страницей
                                    int startIndex = (page - 1) * size;
                                    int endIndex = Math.min(startIndex + size, entries.size());

                                    // Только если есть индексы в пределах массива
                                    if (startIndex < entries.size()) {
                                        List<LeaderboardEntry> pageEntries = entries.subList(startIndex, endIndex);

                                        // Вызываем колбэк успешного выполнения с эмуляцией пагинации
                                        Gdx.app.postRunnable(() -> {
                                            try {
                                                listener.onSuccess(pageEntries, totalEntries, page, totalPages);
                                            } catch (Exception e) {
                                                LogHelper.error("LeaderboardAPI", "ID-GL-006: Ошибка в колбэке onSuccess: " + e.getMessage());
                                            }
                                        });
                                    } else {
                                        // Пустой список для страниц за пределами данных
                                        Gdx.app.postRunnable(() -> {
                                            try {
                                                listener.onSuccess(new ArrayList<>(), totalEntries, page, totalPages);
                                            } catch (Exception e) {
                                                LogHelper.error("LeaderboardAPI", "ID-GL-007: Ошибка в колбэке onSuccess: " + e.getMessage());
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    LogHelper.error("LeaderboardAPI", "ID-GL-008: Ошибка при эмуляции пагинации: " + e.getMessage());
                                    throw e;
                                }
                            } catch (Exception e) {
                                Gdx.app.postRunnable(() -> {
                                    try {
                                        listener.onError("Ошибка обработки данных: " + e.getMessage());
                                    } catch (Exception ex) {
                                        LogHelper.error("LeaderboardAPI", "ID-GL-009: Ошибка в колбэке onError: " + ex.getMessage());
                                    }
                                });
                                LogHelper.error("LeaderboardAPI", "ID-GL-010: Ошибка парсинга ответа: " + e.getMessage() + ", JSON: " + responseString);
                            }
                        } else {
                            Gdx.app.postRunnable(() -> {
                                try {
                                    listener.onError("Сервер вернул ошибку: " + statusCode);
                                } catch (Exception e) {
                                    LogHelper.error("LeaderboardAPI", "ID-GL-011: Ошибка в колбэке onError: " + e.getMessage());
                                }
                            });
                            LogHelper.error("LeaderboardAPI", "ID-GL-012: Ошибка сервера: " + statusCode + ", ответ: " + responseString);
                        }
                    } catch (Exception e) {
                        LogHelper.error("LeaderboardAPI", "ID-GL-013: Критическая ошибка обработки ответа: " + e.getMessage());
                        Gdx.app.postRunnable(() -> {
                            try {
                                listener.onError("Критическая ошибка: " + e.getMessage());
                            } catch (Exception ex) {
                                LogHelper.error("LeaderboardAPI", "ID-GL-014: Ошибка в колбэке onError: " + ex.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void failed(Throwable t) {
                    try {
                        Gdx.app.postRunnable(() -> {
                            try {
                                listener.onError("Не удалось подключиться к серверу: " + t.getMessage());
                            } catch (Exception e) {
                                LogHelper.error("LeaderboardAPI", "ID-GL-015: Ошибка в колбэке onError: " + e.getMessage());
                            }
                        });
                        LogHelper.error("LeaderboardAPI", "ID-GL-016: Ошибка соединения: " + t.getMessage());
                    } catch (Exception e) {
                        LogHelper.error("LeaderboardAPI", "ID-GL-017: Критическая ошибка в методе failed: " + e.getMessage());
                    }
                }

                @Override
                public void cancelled() {
                    try {
                        Gdx.app.postRunnable(() -> {
                            try {
                                listener.onError("Запрос был отменен");
                            } catch (Exception e) {
                                LogHelper.error("LeaderboardAPI", "ID-GL-018: Ошибка в колбэке onError: " + e.getMessage());
                            }
                        });
                        LogHelper.error("LeaderboardAPI", "ID-GL-019: Запрос отменен");
                    } catch (Exception e) {
                        LogHelper.error("LeaderboardAPI", "ID-GL-020: Критическая ошибка в методе cancelled: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            LogHelper.error("LeaderboardAPI", "ID-GL-021: Критическая ошибка при подготовке запроса: " + e.getMessage());
            Gdx.app.postRunnable(() -> {
                try {
                    listener.onError("Критическая ошибка при подготовке запроса: " + e.getMessage());
                } catch (Exception ex) {
                    LogHelper.error("LeaderboardAPI", "ID-GL-022: Ошибка в колбэке onError: " + ex.getMessage());
                }
            });
        }
    }

    /**
     * Отправляет результат игры на сервер
     *
     * @param playerName имя игрока
     * @param wave       волна, до которой дошел игрок
     * @param kills      количество убитых врагов
     * @param listener   слушатель результата
     */
    public static void submitScore(String playerName, int wave, int kills, final SubmitScoreListener listener) {
        try {
            int score = calculateScore(wave, kills);

            // Формируем JSON для отправки с экранированием спецсимволов
            String jsonContent = "{"
                + "\"playerName\":\"" + playerName.replace("\"", "\\\"") + "\","
                + "\"wave\":" + wave + ","
                + "\"kills\":" + kills + ","
                + "\"score\":" + score
                + "}";

            HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
            Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST)
                .url(getLeaderboardUrl() + "/submit")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .content(jsonContent)
                .build();

            LogHelper.log("LeaderboardAPI", "Отправка результата на сервер: " + getLeaderboardUrl() + "/submit");
            LogHelper.log("LeaderboardAPI", "Данные: " + jsonContent);

            Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    try {
                        String responseString;
                        try {
                            responseString = new String(httpResponse.getResult(), StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            LogHelper.error("LeaderboardAPI", "ID-SS-001: Ошибка чтения результата: " + e.getMessage());
                            responseString = httpResponse.getResultAsString();
                        }

                        int statusCode = httpResponse.getStatus().getStatusCode();
                        LogHelper.log("LeaderboardAPI", "Получен ответ: код=" + statusCode + ", ответ=" + responseString);

                        if (statusCode == 201 || statusCode == 200) { // Created or OK
                            try {
                                Json json = new Json();
                                JsonReader jsonReader = new JsonReader();
                                JsonValue root = jsonReader.parse(responseString);

                                // Для обратной совместимости проверяем разные форматы ответа
                                boolean success = true;
                                try {
                                    success = root.getBoolean("success", true); // По умолчанию true, если поля нет
                                } catch (Exception e) {
                                    LogHelper.error("LeaderboardAPI", "ID-SS-002: Ошибка при проверке success: " + e.getMessage());
                                }

                                if (success) {
                                    try {
                                        // Пытаемся получить данные о сохраненном счете в разных форматах
                                        JsonValue scoreJson = null;
                                        try {
                                            scoreJson = root.get("score");
                                            if (scoreJson == null) {
                                                scoreJson = root.get("data"); // Альтернативное поле
                                            }

                                            if (scoreJson == null) {
                                                // Если нет вложенных объектов, используем корневой
                                                scoreJson = root;
                                            }
                                        } catch (Exception e) {
                                            LogHelper.error("LeaderboardAPI", "ID-SS-003: Ошибка при получении JSON счета: " + e.getMessage());
                                            // Создаем пустой объект для избежания NPE
                                            scoreJson = new JsonValue(JsonValue.ValueType.object);
                                        }

                                        // Используем безопасный способ парсинга
                                        LeaderboardEntry createdEntryTemp = new LeaderboardEntry();
                                        try {
                                            createdEntryTemp = json.fromJson(LeaderboardEntry.class, scoreJson.toString());
                                        } catch (Exception e) {
                                            LogHelper.error("LeaderboardAPI", "ID-SS-004: Ошибка парсинга JSON: " + e.getMessage() + ", JSON: " + scoreJson);
                                            // Создаем объект с минимальной информацией
                                            createdEntryTemp.setPlayerName(playerName);
                                            createdEntryTemp.setScore(score);
                                            createdEntryTemp.setWave(wave);
                                            createdEntryTemp.setKills(kills);
                                        }

                                        final LeaderboardEntry createdEntry = createdEntryTemp;
                                        Gdx.app.postRunnable(() -> {
                                            try {
                                                listener.onSuccess(createdEntry);
                                            } catch (Exception e) {
                                                LogHelper.error("LeaderboardAPI", "ID-SS-005: Ошибка в колбэке onSuccess: " + e.getMessage());
                                            }
                                        });
                                    } catch (Exception e) {
                                        LogHelper.error("LeaderboardAPI", "ID-SS-006: Ошибка при обработке успеха: " + e.getMessage());
                                        throw e;
                                    }
                                } else {
                                    String errorMessage = "Неизвестная ошибка";
                                    try {
                                        errorMessage = root.getString("message", "Неизвестная ошибка");
                                    } catch (Exception e) {
                                        LogHelper.error("LeaderboardAPI", "ID-SS-007: Ошибка при получении сообщения: " + e.getMessage());
                                    }
                                    final String finalErrorMessage = errorMessage;
                                    Gdx.app.postRunnable(() -> {
                                        try {
                                            listener.onError(finalErrorMessage);
                                        } catch (Exception e) {
                                            LogHelper.error("LeaderboardAPI", "ID-SS-008: Ошибка в колбэке onError: " + e.getMessage());
                                        }
                                    });
                                    LogHelper.error("LeaderboardAPI", "ID-SS-009: Ошибка сервера: " + errorMessage);
                                }
                            } catch (Exception e) {
                                LogHelper.error("LeaderboardAPI", "ID-SS-010: Ошибка парсинга ответа: " + e.getMessage() + ", JSON: " + responseString);
                                // Создаем объект с минимальной информацией
                                LeaderboardEntry fallbackEntry = new LeaderboardEntry();
                                fallbackEntry.setPlayerName(playerName);
                                fallbackEntry.setScore(score);
                                fallbackEntry.setWave(wave);
                                fallbackEntry.setKills(kills);

                                Gdx.app.postRunnable(() -> {
                                    try {
                                        listener.onSuccess(fallbackEntry);
                                    } catch (Exception ex) {
                                        LogHelper.error("LeaderboardAPI", "ID-SS-011: Ошибка в колбэке onSuccess: " + ex.getMessage());
                                    }
                                });
                            }
                        } else {
                            Gdx.app.postRunnable(() -> {
                                try {
                                    listener.onError("Сервер вернул ошибку: " + statusCode);
                                } catch (Exception e) {
                                    LogHelper.error("LeaderboardAPI", "ID-SS-012: Ошибка в колбэке onError: " + e.getMessage());
                                }
                            });
                            LogHelper.error("LeaderboardAPI", "ID-SS-013: Ошибка сервера: " + statusCode + ", ответ: " + responseString);
                        }
                    } catch (Exception e) {
                        LogHelper.error("LeaderboardAPI", "ID-SS-014: Критическая ошибка обработки ответа: " + e.getMessage());
                        Gdx.app.postRunnable(() -> {
                            try {
                                listener.onError("Критическая ошибка: " + e.getMessage());
                            } catch (Exception ex) {
                                LogHelper.error("LeaderboardAPI", "ID-SS-015: Ошибка в колбэке onError: " + ex.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void failed(Throwable t) {
                    try {
                        Gdx.app.postRunnable(() -> {
                            try {
                                listener.onError("Не удалось подключиться к серверу: " + t.getMessage());
                            } catch (Exception e) {
                                LogHelper.error("LeaderboardAPI", "ID-SS-016: Ошибка в колбэке onError: " + e.getMessage());
                            }
                        });
                        LogHelper.error("LeaderboardAPI", "ID-SS-017: Ошибка соединения: " + t.getMessage());
                    } catch (Exception e) {
                        LogHelper.error("LeaderboardAPI", "ID-SS-018: Критическая ошибка в методе failed: " + e.getMessage());
                    }
                }

                @Override
                public void cancelled() {
                    try {
                        Gdx.app.postRunnable(() -> {
                            try {
                                listener.onError("Запрос был отменен");
                            } catch (Exception e) {
                                LogHelper.error("LeaderboardAPI", "ID-SS-019: Ошибка в колбэке onError: " + e.getMessage());
                            }
                        });
                        LogHelper.error("LeaderboardAPI", "ID-SS-020: Запрос отменен");
                    } catch (Exception e) {
                        LogHelper.error("LeaderboardAPI", "ID-SS-021: Критическая ошибка в методе cancelled: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            LogHelper.error("LeaderboardAPI", "ID-SS-022: Критическая ошибка при подготовке запроса: " + e.getMessage());
            Gdx.app.postRunnable(() -> {
                try {
                    listener.onError("Критическая ошибка при подготовке запроса: " + e.getMessage());
                } catch (Exception ex) {
                    LogHelper.error("LeaderboardAPI", "ID-SS-023: Ошибка в колбэке onError: " + ex.getMessage());
                }
            });
        }
    }

    /**
     * Рассчитывает итоговый счет игрока на основе волны и убийств
     *
     * @param wave  волна, до которой дошел игрок
     * @param kills количество убитых врагов
     * @return итоговый счет
     */
    private static int calculateScore(int wave, int kills) {
        return (wave * 100) + (kills);
    }

    /**
     * Отправляет текущий результат игрока на сервер
     *
     * @param playerName имя игрока
     * @param wave       волна, до которой дошел игрок
     * @param kills      количество убитых врагов
     * @param listener   слушатель результата
     */
    public static void submitCurrentPlayerScore(String playerName, int wave, int kills, final SubmitScoreListener listener) {
        try {
            // Получаем итоговый счет
            int finalScore = calculateScore(wave, kills);

            // Формируем простой JSON для отправки, без лишних полей
            String jsonContent = "{"
                + "\"playerName\":\"" + playerName.replace("\"", "\\\"") + "\","
                + "\"wave\":" + wave + ","
                + "\"kills\":" + kills + ","
                + "\"score\":" + finalScore
                + "}";

            HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
            Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST)
                .url(getLeaderboardUrl() + "/submit")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .content(jsonContent)
                .build();

            LogHelper.log("LeaderboardAPI", "Отправка результата на сервер: " + getLeaderboardUrl() + "/submit");
            LogHelper.log("LeaderboardAPI", "Данные: " + jsonContent);

            Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    try {
                        String responseString;
                        try {
                            responseString = new String(httpResponse.getResult(), StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            LogHelper.error("LeaderboardAPI", "ID-SCS-001: Ошибка чтения результата: " + e.getMessage());
                            responseString = httpResponse.getResultAsString();
                        }

                        int statusCode = httpResponse.getStatus().getStatusCode();

                        LogHelper.log("LeaderboardAPI", "Получен ответ от сервера: код=" + statusCode + ", ответ=" + responseString);

                        if (statusCode == 201 || statusCode == 200) { // Created or OK
                            try {
                                LeaderboardEntry entry = null;

                                try {
                                    Json json = new Json();
                                    JsonReader jsonReader = new JsonReader();
                                    try {
                                        JsonValue root = jsonReader.parse(responseString);

                                        // Для обратной совместимости проверяем разные форматы ответа
                                        boolean success = true;
                                        try {
                                            success = root.getBoolean("success", true); // По умолчанию true, если поля нет
                                        } catch (Exception e) {
                                            LogHelper.error("LeaderboardAPI", "ID-SCS-002: Ошибка при проверке success: " + e.getMessage());
                                        }

                                        if (success) {
                                            // Пытаемся получить данные о сохраненном счете в разных форматах
                                            JsonValue scoreJson = null;
                                            try {
                                                scoreJson = root.get("score");
                                                if (scoreJson == null) {
                                                    scoreJson = root.get("data"); // Альтернативное поле
                                                }

                                                if (scoreJson == null) {
                                                    // Если нет вложенных объектов, используем корневой
                                                    scoreJson = root;
                                                }
                                            } catch (Exception e) {
                                                LogHelper.error("LeaderboardAPI", "ID-SCS-003: Ошибка при получении JSON счета: " + e.getMessage());
                                                // Создаем пустой объект для избежания NPE
                                                scoreJson = new JsonValue(JsonValue.ValueType.object);
                                            }

                                            try {
                                                entry = json.fromJson(LeaderboardEntry.class, scoreJson.toString());
                                            } catch (Exception e) {
                                                LogHelper.error("LeaderboardAPI", "ID-SCS-004: Ошибка парсинга JSON: " + e.getMessage() + ", ответ: " + scoreJson);
                                                throw e;
                                            }
                                        } else {
                                            String errorMessage = "Неизвестная ошибка";
                                            try {
                                                errorMessage = root.getString("message", "Неизвестная ошибка");
                                            } catch (Exception e) {
                                                LogHelper.error("LeaderboardAPI", "ID-SCS-005: Ошибка при получении сообщения об ошибке: " + e.getMessage());
                                            }
                                            throw new Exception(errorMessage);
                                        }
                                    } catch (Exception e) {
                                        LogHelper.error("LeaderboardAPI", "ID-SCS-006: Ошибка парсинга JSON: " + e.getMessage() + ", ответ: " + responseString);
                                        throw e;
                                    }
                                } catch (Exception e) {
                                    LogHelper.error("LeaderboardAPI", "ID-SCS-007: Ошибка парсинга JSON: " + e.getMessage() + ", ответ: " + responseString);
                                    // Создаем объект с базовой информацией
                                    entry = new LeaderboardEntry();
                                    entry.setPlayerName(playerName);
                                    entry.setScore(finalScore);
                                    entry.setWave(wave);
                                    entry.setKills(kills);
                                }

                                final LeaderboardEntry finalEntry = entry;
                                Gdx.app.postRunnable(() -> {
                                    try {
                                        listener.onSuccess(finalEntry);
                                    } catch (Exception e) {
                                        LogHelper.error("LeaderboardAPI", "ID-SCS-008: Ошибка в колбэке onSuccess: " + e.getMessage());
                                    }
                                });
                            } catch (Exception e) {
                                LogHelper.error("LeaderboardAPI", "ID-SCS-009: Ошибка парсинга ответа: " + e.getMessage() + ", ответ: " + responseString);

                                // Создаем объект с базовой информацией в случае ошибки
                                final LeaderboardEntry fallbackEntry = new LeaderboardEntry();
                                fallbackEntry.setPlayerName(playerName);
                                fallbackEntry.setScore(finalScore);
                                fallbackEntry.setWave(wave);
                                fallbackEntry.setKills(kills);

                                Gdx.app.postRunnable(() -> {
                                    try {
                                        listener.onSuccess(fallbackEntry);
                                    } catch (Exception ex) {
                                        LogHelper.error("LeaderboardAPI", "ID-SCS-010: Ошибка в колбэке onSuccess: " + ex.getMessage());
                                    }
                                });
                            }
                        } else {
                            Gdx.app.postRunnable(() -> {
                                try {
                                    listener.onError("Сервер вернул ошибку: " + statusCode);
                                } catch (Exception e) {
                                    LogHelper.error("LeaderboardAPI", "ID-SCS-011: Ошибка в колбэке onError: " + e.getMessage());
                                }
                            });
                            LogHelper.error("LeaderboardAPI", "ID-SCS-012: Ошибка сервера: " + statusCode + ", ответ: " + responseString);
                        }
                    } catch (Exception e) {
                        LogHelper.error("LeaderboardAPI", "ID-SCS-013: Критическая ошибка обработки ответа: " + e.getMessage());
                        Gdx.app.postRunnable(() -> {
                            try {
                                listener.onError("Критическая ошибка обработки ответа: " + e.getMessage());
                            } catch (Exception ex) {
                                LogHelper.error("LeaderboardAPI", "ID-SCS-014: Ошибка в колбэке onError: " + ex.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void failed(Throwable t) {
                    try {
                        Gdx.app.postRunnable(() -> {
                            try {
                                listener.onError("Не удалось подключиться к серверу: " + t.getMessage());
                            } catch (Exception e) {
                                LogHelper.error("LeaderboardAPI", "ID-SCS-015: Ошибка в колбэке onError: " + e.getMessage());
                            }
                        });
                        LogHelper.error("LeaderboardAPI", "ID-SCS-016: Ошибка соединения: " + t.getMessage());
                    } catch (Exception e) {
                        LogHelper.error("LeaderboardAPI", "ID-SCS-017: Критическая ошибка в методе failed: " + e.getMessage());
                    }
                }

                @Override
                public void cancelled() {
                    try {
                        Gdx.app.postRunnable(() -> {
                            try {
                                listener.onError("Запрос был отменен");
                            } catch (Exception e) {
                                LogHelper.error("LeaderboardAPI", "ID-SCS-018: Ошибка в колбэке onError: " + e.getMessage());
                            }
                        });
                        LogHelper.error("LeaderboardAPI", "ID-SCS-019: Запрос отменен");
                    } catch (Exception e) {
                        LogHelper.error("LeaderboardAPI", "ID-SCS-020: Критическая ошибка в методе cancelled: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            LogHelper.error("LeaderboardAPI", "ID-SCS-021: Критическая ошибка при подготовке данных: " + e.getMessage());
            Gdx.app.postRunnable(() -> {
                try {
                    listener.onError("Ошибка подготовки данных: " + e.getMessage());
                } catch (Exception ex) {
                    LogHelper.error("LeaderboardAPI", "ID-SCS-022: Ошибка в колбэке onError: " + ex.getMessage());
                }
            });
        }
    }

    /**
     * Проверяет доступность сервера таблицы лидеров
     *
     * @param listener слушатель результата
     */
    public static void checkServerStatus(final ServerStatusListener listener) {
        String url = getLeaderboardUrl() + "/top?limit=1";

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(url)
            .timeout(3000) // Таймаут 3 секунды
            .build();

        LogHelper.log("LeaderboardAPI", "Проверка доступности сервера: " + url);

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                Gdx.app.postRunnable(() -> {
                    listener.onSuccess(true);
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> {
                    listener.onError("Сервер недоступен: " + t.getMessage());
                });
                LogHelper.error("LeaderboardAPI", "Сервер недоступен: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> {
                    listener.onError("Запрос отменен");
                });
            }
        });
    }

    /**
     * Получает лучший результат игрока с сервера
     *
     * @param playerName имя игрока
     * @param listener   слушатель результата
     */
    public static void getPlayerBestScore(String playerName, final GetBestScoreListener listener) {
        try {
            // URL-кодируем имя игрока для предотвращения ошибок с специальными символами
            String encodedPlayerName = java.net.URLEncoder.encode(playerName, "UTF-8");
            String url = getLeaderboardUrl() + "/player/" + encodedPlayerName + "/best";

            HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
            Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

            LogHelper.log("LeaderboardAPI", "Запрос лучшего результата игрока " + playerName + ": " + url);

            Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    String responseString = httpResponse.getResultAsString();
                    int statusCode = httpResponse.getStatus().getStatusCode();

                    if (statusCode == 200) {
                        try {
                            Json json = new Json();
                            LeaderboardEntry bestScore = json.fromJson(LeaderboardEntry.class, responseString);

                            // Вызываем колбэк успешного выполнения
                            Gdx.app.postRunnable(() -> {
                                listener.onSuccess(bestScore);
                            });
                        } catch (Exception e) {
                            Gdx.app.postRunnable(() -> {
                                listener.onError("Ошибка обработки данных: " + e.getMessage());
                            });
                            LogHelper.error("LeaderboardAPI", "Error parsing player best score: " + e.getMessage());
                        }
                    } else if (statusCode == 404) {
                        // Игрок не найден, возвращаем null как лучший результат
                        Gdx.app.postRunnable(() -> {
                            listener.onSuccess(null);
                        });
                    } else {
                        Gdx.app.postRunnable(() -> {
                            listener.onError("Сервер вернул ошибку: " + statusCode);
                        });
                        LogHelper.error("LeaderboardAPI", "Server error when getting player best score: " + statusCode + ", response: " + responseString);
                    }
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.postRunnable(() -> {
                        listener.onError("Не удалось подключиться к серверу: " + t.getMessage());
                    });
                    LogHelper.error("LeaderboardAPI", "Connection failed when getting player best score: " + t.getMessage());
                }

                @Override
                public void cancelled() {
                    Gdx.app.postRunnable(() -> {
                        listener.onError("Запрос был отменен");
                    });
                    LogHelper.error("LeaderboardAPI", "Request cancelled when getting player best score");
                }
            });
        } catch (Exception e) {
            LogHelper.error("LeaderboardAPI", "Error encoding player name: " + e.getMessage());
            Gdx.app.postRunnable(() -> {
                listener.onError("Ошибка кодирования имени игрока: " + e.getMessage());
            });
        }
    }

    /**
     * Получает список лучших результатов всех игроков
     *
     * @param page     номер страницы (с 1)
     * @param size     размер страницы
     * @param listener слушатель результата
     */
    public static void getBestScores(int page, int size, final LeaderboardResponseListener listener) {
        String url = getLeaderboardUrl() + "/bestscores?page=" + (page - 1) + "&size=" + size;

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build();

        LogHelper.log("LeaderboardAPI", "Запрос лучших результатов: " + url);

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String responseString;
                try {
                    responseString = new String(httpResponse.getResult(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    responseString = httpResponse.getResultAsString();
                }

                int statusCode = httpResponse.getStatus().getStatusCode();

                LogHelper.log("LeaderboardAPI", "Получен ответ (лучшие): код=" + statusCode + ", ответ=" + responseString);

                if (statusCode == 200) {
                    try {
                        JsonReader jsonReader = new JsonReader();
                        JsonValue root = jsonReader.parse(responseString);

                        // Парсим записи
                        List<LeaderboardEntry> entries = new ArrayList<>();

                        if (root.isArray()) {
                            Json json = new Json();
                            for (JsonValue entryJson : root) {
                                try {
                                    LeaderboardEntry entry = json.fromJson(LeaderboardEntry.class, entryJson.toString());
                                    entries.add(entry);
                                } catch (Exception e) {
                                    LogHelper.error("LeaderboardAPI", "Ошибка парсинга записи: " + e.getMessage() + ", JSON: " + entryJson.toString());
                                }
                            }
                        } else if (root.has("content") && root.get("content").isArray()) {
                            // Альтернативный формат с пагинацией
                            JsonValue contentArray = root.get("content");
                            int totalElements = root.getInt("totalElements", 0);
                            int totalPages = root.getInt("totalPages", 0);
                            int currentPage = root.getInt("number", 0) + 1;

                            Json json = new Json();
                            for (JsonValue entryJson : contentArray) {
                                try {
                                    LeaderboardEntry entry = json.fromJson(LeaderboardEntry.class, entryJson.toString());
                                    entries.add(entry);
                                } catch (Exception e) {
                                    LogHelper.error("LeaderboardAPI", "Ошибка парсинга записи: " + e.getMessage() + ", JSON: " + entryJson.toString());
                                }
                            }

                            // Вызываем колбэк с данными пагинации
                            final int finalTotalElements = totalElements;
                            final int finalCurrentPage = currentPage;
                            final int finalTotalPages = totalPages;

                            Gdx.app.postRunnable(() -> {
                                listener.onSuccess(entries, finalTotalElements, finalCurrentPage, finalTotalPages);
                            });
                            return;
                        }

                        // Для формата без пагинации
                        Gdx.app.postRunnable(() -> {
                            listener.onSuccess(entries, entries.size(), page, 1);
                        });
                    } catch (Exception e) {
                        Gdx.app.postRunnable(() -> {
                            listener.onError("Ошибка обработки данных: " + e.getMessage());
                        });
                        LogHelper.error("LeaderboardAPI", "Error parsing response: " + e.getMessage() + ", JSON: " + responseString);
                    }
                } else {
                    Gdx.app.postRunnable(() -> {
                        listener.onError("Сервер вернул ошибку: " + statusCode);
                    });
                    LogHelper.error("LeaderboardAPI", "Server error: " + statusCode + ", response: " + responseString);
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> {
                    listener.onError("Не удалось подключиться к серверу: " + t.getMessage());
                });
                LogHelper.error("LeaderboardAPI", "Connection failed: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> {
                    listener.onError("Запрос был отменен");
                });
                LogHelper.error("LeaderboardAPI", "Request cancelled");
            }
        });
    }

    /**
     * Сбрасывает настройки сервера к значениям по умолчанию
     */
    public static void reset() {
        SERVER_PROTOCOL = DEFAULT_SERVER_PROTOCOL;
        SERVER_HOST = DEFAULT_SERVER_HOST;
        SERVER_PORT = DEFAULT_SERVER_PORT;
        SERVER_PATH = DEFAULT_SERVER_PATH;
        saveServerSettings();
    }

    public static String getServerProtocol() {
        return SERVER_PROTOCOL;
    }

    public static void setServerProtocol(String protocol) {
        SERVER_PROTOCOL = protocol;
        saveServerSettings();
    }

    public static String getServerPath() {
        return SERVER_PATH;
    }

    public static void setServerPath(String path) {
        SERVER_PATH = path;
        saveServerSettings();
    }

    private static void loadServerSettings() {
        try {
            // Используем глобальные настройки приложения
            Preferences prefs = Gdx.app.getPreferences("LeaderboardSettings");

            // Загружаем все настройки с дефолтными значениями
            SERVER_PROTOCOL = prefs.getString(PREF_KEY_SERVER_PROTOCOL, DEFAULT_SERVER_PROTOCOL);
            SERVER_HOST = prefs.getString(PREF_KEY_SERVER_HOST, DEFAULT_SERVER_HOST);
            SERVER_PORT = prefs.getInteger(PREF_KEY_SERVER_PORT, DEFAULT_SERVER_PORT);
            SERVER_PATH = prefs.getString(PREF_KEY_SERVER_PATH, DEFAULT_SERVER_PATH);

            LogHelper.log("LeaderboardAPI", "Настройки сервера загружены: " + SERVER_PROTOCOL + "://" +
                SERVER_HOST + ":" + SERVER_PORT + SERVER_PATH);
        } catch (Exception e) {
            LogHelper.error("LeaderboardAPI", "Ошибка при загрузке настроек: " + e.getMessage());
            // В случае ошибки используем значения по умолчанию
            SERVER_PROTOCOL = DEFAULT_SERVER_PROTOCOL;
            SERVER_HOST = DEFAULT_SERVER_HOST;
            SERVER_PORT = DEFAULT_SERVER_PORT;
            SERVER_PATH = DEFAULT_SERVER_PATH;
        }
    }

    private static void saveServerSettings() {
        try {
            // Используем глобальные настройки приложения
            Preferences prefs = Gdx.app.getPreferences("LeaderboardSettings");

            // Сохраняем все настройки
            prefs.putString(PREF_KEY_SERVER_PROTOCOL, SERVER_PROTOCOL);
            prefs.putString(PREF_KEY_SERVER_HOST, SERVER_HOST);
            prefs.putInteger(PREF_KEY_SERVER_PORT, SERVER_PORT);
            prefs.putString(PREF_KEY_SERVER_PATH, SERVER_PATH);

            // Сразу сохраняем на диск
            prefs.flush();

            LogHelper.log("LeaderboardAPI", "Настройки сервера сохранены: " + SERVER_PROTOCOL + "://" +
                SERVER_HOST + ":" + SERVER_PORT + SERVER_PATH);
        } catch (Exception e) {
            LogHelper.error("LeaderboardAPI", "Ошибка при сохранении настроек: " + e.getMessage());
        }
    }

    /**
     * Интерфейс для получения результатов запроса таблицы лидеров
     */
    public interface LeaderboardResponseListener {
        void onSuccess(List<LeaderboardEntry> entries, int totalEntries, int currentPage, int totalPages);

        void onError(String error);
    }

    /**
     * Интерфейс для получения результата отправки результата
     */
    public interface SubmitScoreListener {
        void onSuccess(LeaderboardEntry entry);

        void onError(String error);
    }

    /**
     * Интерфейс для получения результата проверки сервера
     */
    public interface ServerStatusListener {
        void onSuccess(boolean isAvailable);

        void onError(String error);
    }

    /**
     * Интерфейс для получения лучшего результата игрока
     */
    public interface GetBestScoreListener {
        void onSuccess(LeaderboardEntry bestScore);

        void onError(String error);
    }
}
