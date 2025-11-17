package ru.myitschool.rogal.networking;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

/**
 * API для взаимодействия с Supabase базой данных
 */
public class SupabaseAPI {
    // Supabase конфигурация
    private static final String SUPABASE_URL = "https://ghdljgzefsiuffjtwrwv.supabase.co";
    private static final String SUPABASE_KEY = "sb_secret_de3zhsKlpHZ51MhQyUXfKg_OYIkqkta";

    /**
     * Получает таблицу лидеров с Supabase
     *
     * @param page     номер страницы (с 1)
     * @param size     размер страницы
     * @param listener слушатель результата
     */
    public static void getLeaderboard(int page, int size, final LeaderboardResponseListener listener) {
        try {
            // Supabase использует limit и offset для пагинации
            int limit = size;
            int offset = (page - 1) * size;

            String url = SUPABASE_URL + "/rest/v1/leaderboard?select=*&order=score.desc&limit=" + limit + "&offset=" + offset;

            HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
            Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

            LogHelper.log("SupabaseAPI", "Запрос таблицы лидеров: " + url);

            Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    try {
                        String responseString = new String(httpResponse.getResult(), StandardCharsets.UTF_8);
                        int statusCode = httpResponse.getStatus().getStatusCode();

                        LogHelper.log("SupabaseAPI", "Получен ответ: код=" + statusCode + ", ответ=" + responseString);

                        if (statusCode == 200) {
                            try {
                                JsonReader jsonReader = new JsonReader();
                                JsonValue root = jsonReader.parse(responseString);

                                List<LeaderboardEntry> entries = new ArrayList<>();

                                if (root.isArray()) {
                                    for (JsonValue entryJson : root) {
                                        try {
                                            LeaderboardEntry entry = parseSupabaseEntry(entryJson);
                                            entries.add(entry);
                                        } catch (Exception e) {
                                            LogHelper.error("SupabaseAPI", "Ошибка парсинга записи: " + e.getMessage());
                                        }
                                    }
                                }

                                // Получаем общее количество записей для пагинации
                                getTotalCount(new TotalCountListener() {
                                    @Override
                                    public void onSuccess(int totalCount) {
                                        int totalPages = (int) Math.ceil((double) totalCount / size);
                                        Gdx.app.postRunnable(() -> {
                                            listener.onSuccess(entries, totalCount, page, totalPages);
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {
                                        // Если не удалось получить общее количество, используем текущие данные
                                        Gdx.app.postRunnable(() -> {
                                            listener.onSuccess(entries, entries.size(), page, 1);
                                        });
                                    }
                                });

                            } catch (Exception e) {
                                Gdx.app.postRunnable(() -> {
                                    listener.onError("Ошибка обработки данных: " + e.getMessage());
                                });
                                LogHelper.error("SupabaseAPI", "Ошибка парсинга ответа: " + e.getMessage());
                            }
                        } else {
                            Gdx.app.postRunnable(() -> {
                                listener.onError("Сервер вернул ошибку: " + statusCode);
                            });
                            LogHelper.error("SupabaseAPI", "Ошибка сервера: " + statusCode);
                        }
                    } catch (Exception e) {
                        LogHelper.error("SupabaseAPI", "Критическая ошибка обработки ответа: " + e.getMessage());
                        Gdx.app.postRunnable(() -> {
                            listener.onError("Критическая ошибка: " + e.getMessage());
                        });
                    }
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.postRunnable(() -> {
                        listener.onError("Не удалось подключиться к серверу: " + t.getMessage());
                    });
                    LogHelper.error("SupabaseAPI", "Ошибка соединения: " + t.getMessage());
                }

                @Override
                public void cancelled() {
                    Gdx.app.postRunnable(() -> {
                        listener.onError("Запрос был отменен");
                    });
                    LogHelper.error("SupabaseAPI", "Запрос отменен");
                }
            });
        } catch (Exception e) {
            LogHelper.error("SupabaseAPI", "Критическая ошибка при подготовке запроса: " + e.getMessage());
            Gdx.app.postRunnable(() -> {
                listener.onError("Критическая ошибка при подготовке запроса: " + e.getMessage());
            });
        }
    }

    /**
     * Отправляет результат игры в Supabase
     *
     * @param playerName имя игрока
     * @param wave       волна, до которой дошел игрок
     * @param kills      количество убитых врагов
     * @param listener   слушатель результата
     */
    public static void submitScore(String playerName, int wave, int kills, int bestWave, final SubmitScoreListener listener) {
        try {
            int score = calculateScore(wave, kills);

            // Формируем JSON для отправки в Supabase
            String jsonContent = "{"

                + "\"playerName\":\"" + playerName.replace("\"", "\\\"") + "\","
                + "\"wave\":" + wave + ","
                + "\"kills\":" + kills + ","
                + "\"score\":" + score + ","
                + "\"totalKills\":\"" + kills + "\","
                + "\"gamesPlayed\":\"1\","
                + "\"bestWave\":\"" + bestWave + "\","
                + "\"bestScore\":true"
                + "}";

            HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
            Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST)
                .url(SUPABASE_URL + "/rest/v1/leaderboard")
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Prefer", "return=representation")
                .content(jsonContent)
                .build();

            LogHelper.log("SupabaseAPI", "Отправка результата в Supabase");
            LogHelper.log("SupabaseAPI", "Данные: " + jsonContent);

            Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    try {
                        String responseString = new String(httpResponse.getResult(), StandardCharsets.UTF_8);
                        int statusCode = httpResponse.getStatus().getStatusCode();

                        LogHelper.log("SupabaseAPI", "Получен ответ: код=" + statusCode + ", ответ=" + responseString);

                        if (statusCode == 201 || statusCode == 200) {
                            try {
                                JsonReader jsonReader = new JsonReader();
                                JsonValue root = jsonReader.parse(responseString);

                                // Supabase возвращает массив с одной записью
                                LeaderboardEntry entry = null;
                                if (root.isArray() && root.size > 0) {
                                    entry = parseSupabaseEntry(root.get(0));
                                } else {
                                    // Создаем объект с базовой информацией
                                    entry = new LeaderboardEntry();
                                    entry.setPlayerName(playerName);
                                    entry.setScore(score);
                                    entry.setWave(wave);
                                    entry.setKills(kills);
                                }

                                final LeaderboardEntry finalEntry = entry;
                                Gdx.app.postRunnable(() -> {
                                    listener.onSuccess(finalEntry);
                                });
                            } catch (Exception e) {
                                LogHelper.error("SupabaseAPI", "Ошибка парсинга ответа: " + e.getMessage());

                                // Создаем объект с базовой информацией
                                final LeaderboardEntry fallbackEntry = new LeaderboardEntry();
                                fallbackEntry.setPlayerName(playerName);
                                fallbackEntry.setScore(score);
                                fallbackEntry.setWave(wave);
                                fallbackEntry.setKills(kills);

                                Gdx.app.postRunnable(() -> {
                                    listener.onSuccess(fallbackEntry);
                                });
                            }
                        } else {
                            Gdx.app.postRunnable(() -> {
                                listener.onError("Сервер вернул ошибку: " + statusCode);
                            });
                            LogHelper.error("SupabaseAPI", "Ошибка сервера: " + statusCode);
                        }
                    } catch (Exception e) {
                        LogHelper.error("SupabaseAPI", "Критическая ошибка обработки ответа: " + e.getMessage());
                        Gdx.app.postRunnable(() -> {
                            listener.onError("Критическая ошибка: " + e.getMessage());
                        });
                    }
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.postRunnable(() -> {
                        listener.onError("Не удалось подключиться к серверу: " + t.getMessage());
                    });
                    LogHelper.error("SupabaseAPI", "Ошибка соединения: " + t.getMessage());
                }

                @Override
                public void cancelled() {
                    Gdx.app.postRunnable(() -> {
                        listener.onError("Запрос был отменен");
                    });
                    LogHelper.error("SupabaseAPI", "Запрос отменен");
                }
            });
        } catch (Exception e) {
            LogHelper.error("SupabaseAPI", "Критическая ошибка при подготовке запроса: " + e.getMessage());
            Gdx.app.postRunnable(() -> {
                listener.onError("Критическая ошибка при подготовке запроса: " + e.getMessage());
            });
        }
    }

    /**
     * Получает лучший результат игрока из Supabase
     *
     * @param playerName имя игрока
     * @param listener   слушатель результата
     */
    public static void getPlayerBestScore(String playerName, final GetBestScoreListener listener) {
        try {
            String url = SUPABASE_URL + "/rest/v1/leaderboard?select=*&playerName=eq." +
                playerName.replace(" ", "%20") + "&order=score.desc&limit=1";

            HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
            Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

            LogHelper.log("SupabaseAPI", "Запрос лучшего результата игрока: " + url);

            Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    try {
                        String responseString = new String(httpResponse.getResult(), StandardCharsets.UTF_8);
                        int statusCode = httpResponse.getStatus().getStatusCode();

                        if (statusCode == 200) {
                            try {
                                JsonReader jsonReader = new JsonReader();
                                JsonValue root = jsonReader.parse(responseString);

                                final LeaderboardEntry bestScore;
                                if (root.isArray() && root.size > 0) {
                                    bestScore = parseSupabaseEntry(root.get(0));
                                } else {
                                    bestScore = null;
                                }

                                Gdx.app.postRunnable(() -> {
                                    listener.onSuccess(bestScore);
                                });
                            } catch (Exception e) {
                                Gdx.app.postRunnable(() -> {
                                    listener.onError("Ошибка обработки данных: " + e.getMessage());
                                });
                                LogHelper.error("SupabaseAPI", "Ошибка парсинга ответа: " + e.getMessage());
                            }
                        } else {
                            Gdx.app.postRunnable(() -> {
                                listener.onError("Сервер вернул ошибку: " + statusCode);
                            });
                            LogHelper.error("SupabaseAPI", "Ошибка сервера: " + statusCode);
                        }
                    } catch (Exception e) {
                        LogHelper.error("SupabaseAPI", "Критическая ошибка обработки ответа: " + e.getMessage());
                        Gdx.app.postRunnable(() -> {
                            listener.onError("Критическая ошибка: " + e.getMessage());
                        });
                    }
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.postRunnable(() -> {
                        listener.onError("Не удалось подключиться к серверу: " + t.getMessage());
                    });
                    LogHelper.error("SupabaseAPI", "Ошибка соединения: " + t.getMessage());
                }

                @Override
                public void cancelled() {
                    Gdx.app.postRunnable(() -> {
                        listener.onError("Запрос был отменен");
                    });
                    LogHelper.error("SupabaseAPI", "Запрос отменен");
                }
            });
        } catch (Exception e) {
            LogHelper.error("SupabaseAPI", "Критическая ошибка при подготовке запроса: " + e.getMessage());
            Gdx.app.postRunnable(() -> {
                listener.onError("Критическая ошибка при подготовке запроса: " + e.getMessage());
            });
        }
    }

    /**
     * Проверяет доступность Supabase сервера
     *
     * @param listener слушатель результата
     */
    public static void checkServerStatus(final ServerStatusListener listener) {
        String url = SUPABASE_URL + "/rest/v1/leaderboard?select=id&limit=1";

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(url)
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer " + SUPABASE_KEY)
            .timeout(5000) // Таймаут 5 секунд
            .build();

        LogHelper.log("SupabaseAPI", "Проверка доступности сервера: " + url);

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
                LogHelper.error("SupabaseAPI", "Сервер недоступен: " + t.getMessage());
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
     * Парсит запись из Supabase в объект LeaderboardEntry
     */
    private static LeaderboardEntry parseSupabaseEntry(JsonValue entryJson) {
        LeaderboardEntry entry = new LeaderboardEntry();

        try {
            entry.setId(entryJson.getLong("id", 0));
            entry.setPlayerName(entryJson.getString("playerName", ""));
            entry.setScore(entryJson.getInt("score", 0));
            entry.setWave(entryJson.getInt("wave", 0));
            entry.setKills(entryJson.getInt("kills", 0));

            // Парсим дополнительные поля
            if (entryJson.has("submittedAt")) {
                entry.setSubmittedAt(entryJson.getString("submittedAt", ""));
            }
            if (entryJson.has("totalKills")) {
                entry.setTotalKills(entryJson.getString("totalKills", ""));
            }
            if (entryJson.has("gamesPlayed")) {
                entry.setGamesPlayed(entryJson.getString("gamesPlayed", ""));
            }
            if (entryJson.has("bestWave")) {
                entry.setBestWave(entryJson.getString("bestWave", ""));
            }
            if (entryJson.has("bestScore")) {
                entry.setBestScore(entryJson.getBoolean("bestScore", false));
            }
        } catch (Exception e) {
            LogHelper.error("SupabaseAPI", "Ошибка парсинга записи: " + e.getMessage());
        }

        return entry;
    }

    /**
     * Получает общее количество записей в таблице лидеров
     */
    private static void getTotalCount(final TotalCountListener listener) {
        String url = SUPABASE_URL + "/rest/v1/leaderboard?select=count";

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(url)
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer " + SUPABASE_KEY)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Prefer", "count=exact")
            .build();

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    int statusCode = httpResponse.getStatus().getStatusCode();

                    if (statusCode == 200) {
                        // Supabase возвращает количество в заголовке Content-Range
                        String contentRange = httpResponse.getHeader("Content-Range");
                        if (contentRange != null && contentRange.contains("/")) {
                            String[] parts = contentRange.split("/");
                            if (parts.length > 1) {
                                int totalCount = Integer.parseInt(parts[1]);
                                listener.onSuccess(totalCount);
                                return;
                            }
                        }
                    }
                    listener.onError("Не удалось получить общее количество");
                } catch (Exception e) {
                    listener.onError("Ошибка получения количества: " + e.getMessage());
                }
            }

            @Override
            public void failed(Throwable t) {
                listener.onError("Не удалось подключиться к серверу: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                listener.onError("Запрос был отменен");
            }
        });
    }

    /**
     * Рассчитывает итоговый счет игрока на основе волны и убийств
     */
    private static int calculateScore(int wave, int kills) {
        return (wave * 100) + kills;
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

    /**
     * Интерфейс для получения общего количества записей
     */
    private interface TotalCountListener {
        void onSuccess(int totalCount);

        void onError(String error);
    }
}
