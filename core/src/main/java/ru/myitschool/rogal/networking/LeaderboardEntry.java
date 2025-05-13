package ru.myitschool.rogal.networking;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Класс, представляющий запись в таблице лидеров
 */
public class LeaderboardEntry implements Json.Serializable {
    private String id;
    private String playerName;
    private int score;
    private int wave;
    private int kills;
    private String timestamp;
    private boolean isBestScore;
    private int totalKills;
    private int gamesPlayed;
    private int bestWave;

    public LeaderboardEntry() {
        // Конструктор по умолчанию для десериализации
    }

    // Геттеры и сеттеры
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getWave() {
        return wave;
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isBestScore() {
        return isBestScore;
    }

    public void setBestScore(boolean bestScore) {
        isBestScore = bestScore;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getBestWave() {
        return bestWave;
    }

    public void setBestWave(int bestWave) {
        this.bestWave = bestWave;
    }

    /**
     * Метод для ручной десериализации JSON, учитывающий возможные различия в структуре
     */
    @Override
    public void read(Json json, JsonValue jsonData) {
        // Проверка на вложенные объекты (например, "score": {...})
        JsonValue idValue = jsonData.get("id");
        if (idValue != null) {
            if (idValue.isString()) {
                id = idValue.asString();
            } else if (idValue.isNumber()) {
                id = String.valueOf(idValue.asInt());
            }
        }

        // Чтение имени игрока
        JsonValue playerNameValue = jsonData.get("playerName");
        if (playerNameValue != null) {
            playerName = playerNameValue.asString();
        }

        // Чтение очков
        JsonValue scoreValue = jsonData.get("score");
        if (scoreValue != null) {
            if (scoreValue.isNumber()) {
                score = scoreValue.asInt();
            } else if (scoreValue.isObject()) {
                // Если "score" - это объект, извлекаем данные из него
                extractScoreObjectData(scoreValue);
            }
        }

        // Чтение волны
        JsonValue waveValue = jsonData.get("wave");
        if (waveValue != null && waveValue.isNumber()) {
            wave = waveValue.asInt();
        }

        // Чтение количества убийств
        JsonValue killsValue = jsonData.get("kills");
        if (killsValue != null && killsValue.isNumber()) {
            kills = killsValue.asInt();
        }

        // Чтение времени
        JsonValue timestampValue = jsonData.get("timestamp");
        if (timestampValue != null) {
            timestamp = timestampValue.asString();
        }

        // Проверяем альтернативное поле для времени (submittedAt)
        JsonValue submittedAtValue = jsonData.get("submittedAt");
        if (submittedAtValue != null) {
            timestamp = submittedAtValue.asString();
        }

        // Чтение флага лучшего результата
        JsonValue bestScoreValue = jsonData.get("bestScore");
        if (bestScoreValue != null && bestScoreValue.isBoolean()) {
            isBestScore = bestScoreValue.asBoolean();
        }

        // Чтение общего количества убийств
        JsonValue totalKillsValue = jsonData.get("totalKills");
        if (totalKillsValue != null && totalKillsValue.isNumber()) {
            totalKills = totalKillsValue.asInt();
        }

        // Чтение количества сыгранных игр
        JsonValue gamesPlayedValue = jsonData.get("gamesPlayed");
        if (gamesPlayedValue != null && gamesPlayedValue.isNumber()) {
            gamesPlayed = gamesPlayedValue.asInt();
        }

        // Чтение лучшей волны
        JsonValue bestWaveValue = jsonData.get("bestWave");
        if (bestWaveValue != null && bestWaveValue.isNumber()) {
            bestWave = bestWaveValue.asInt();
        }
    }

    /**
     * Извлекает данные из вложенного объекта score
     */
    private void extractScoreObjectData(JsonValue scoreObj) {
        // ID
        JsonValue nestedId = scoreObj.get("id");
        if (nestedId != null) {
            if (nestedId.isString()) {
                id = nestedId.asString();
            } else if (nestedId.isNumber()) {
                id = String.valueOf(nestedId.asInt());
            }
        }

        // Score
        JsonValue actualScore = scoreObj.get("score");
        if (actualScore != null && actualScore.isNumber()) {
            score = actualScore.asInt();
        }

        // PlayerName
        JsonValue nameValue = scoreObj.get("playerName");
        if (nameValue != null) {
            playerName = nameValue.asString();
        }

        // Wave
        JsonValue waveValue = scoreObj.get("wave");
        if (waveValue != null && waveValue.isNumber()) {
            wave = waveValue.asInt();
        }

        // Kills
        JsonValue killsValue = scoreObj.get("kills");
        if (killsValue != null && killsValue.isNumber()) {
            kills = killsValue.asInt();
        }

        // SubmittedAt (timestamp)
        JsonValue submittedAtValue = scoreObj.get("submittedAt");
        if (submittedAtValue != null) {
            timestamp = submittedAtValue.asString();
        }

        // BestScore
        JsonValue bestScoreValue = scoreObj.get("bestScore");
        if (bestScoreValue != null && bestScoreValue.isBoolean()) {
            isBestScore = bestScoreValue.asBoolean();
        }

        // TotalKills
        JsonValue totalKillsValue = scoreObj.get("totalKills");
        if (totalKillsValue != null && totalKillsValue.isNumber()) {
            totalKills = totalKillsValue.asInt();
        }

        // GamesPlayed
        JsonValue gamesPlayedValue = scoreObj.get("gamesPlayed");
        if (gamesPlayedValue != null && gamesPlayedValue.isNumber()) {
            gamesPlayed = gamesPlayedValue.asInt();
        }

        // BestWave
        JsonValue bestWaveValue = scoreObj.get("bestWave");
        if (bestWaveValue != null && bestWaveValue.isNumber()) {
            bestWave = bestWaveValue.asInt();
        }
    }

    @Override
    public void write(Json json) {
        json.writeValue("id", id);
        json.writeValue("playerName", playerName);
        json.writeValue("score", score);
        json.writeValue("wave", wave);
        json.writeValue("kills", kills);
        json.writeValue("timestamp", timestamp);
        json.writeValue("bestScore", isBestScore);
        json.writeValue("totalKills", totalKills);
        json.writeValue("gamesPlayed", gamesPlayed);
        json.writeValue("bestWave", bestWave);
    }

    @Override
    public String toString() {
        return "LeaderboardEntry{" +
            "id='" + id + '\'' +
            ", playerName='" + playerName + '\'' +
            ", score=" + score +
            ", wave=" + wave +
            ", kills=" + kills +
            ", timestamp='" + timestamp + '\'' +
            ", isBestScore=" + isBestScore +
            ", totalKills=" + totalKills +
            ", gamesPlayed=" + gamesPlayed +
            ", bestWave=" + bestWave +
            '}';
    }
}
