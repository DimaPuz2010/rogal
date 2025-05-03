package ru.myitschool.rogal;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.Properties;

import ru.myitschool.rogal.CustomHelpers.utils.FontManager;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
import ru.myitschool.rogal.CustomHelpers.utils.PlayerData;
import ru.myitschool.rogal.CustomHelpers.utils.UpdateManager;
import ru.myitschool.rogal.Screens.StartScreen;
import ru.myitschool.rogal.networking.LeaderboardAPI;

public class Main extends Game {
    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 720;

    public static String VERSION = "1.0.7";

    @Override
    public void create() {

        // Загружаем версию из gradle.properties
        loadVersion();

        // Инициализация ресурсов
        FontManager.initialize();

        // Инициализация данных игрока
        PlayerData.initialize();

        // Инициализация API таблицы лидеров
        LeaderboardAPI.initialize();

        // Инициализация системы обновлений
        UpdateManager.getInstance().checkForUpdates();

        // Переход на начальный экран
        setScreen(new StartScreen(this));

        // Логируем информацию о запуске
        LogHelper.log("Main", "Game initialized with version " + VERSION);
    }

    /**
     * Загружает версию из файла gradle.properties
     */
    private void loadVersion() {
        try {
            // Пробуем загрузить из разных источников по приоритету
            FileHandle propertiesFile = Gdx.files.internal("gradle.properties");

            // Если не нашли, пробуем в ресурсах
            if (!propertiesFile.exists()) {
                propertiesFile = Gdx.files.internal("resources/gradle.properties");
                LogHelper.log("Main", "Checking in resources folder");
            }

            // Если не нашли, попробуем version.properties
            if (!propertiesFile.exists()) {
                propertiesFile = Gdx.files.internal("version.properties");
                LogHelper.log("Main", "Checking version.properties file");
            }

            if (propertiesFile.exists()) {
                Properties properties = new Properties();
                properties.load(propertiesFile.reader());

                // Пробуем получить значение либо из projectVersion, либо из version (для совместимости)
                String projectVersion = properties.getProperty("projectVersion");
                if (projectVersion == null || projectVersion.isEmpty()) {
                    projectVersion = properties.getProperty("version");
                }

                if (projectVersion != null && !projectVersion.isEmpty()) {
                    VERSION = projectVersion;
                    LogHelper.log("Main", "Загружена версия из файла " + propertiesFile.path() + ": " + VERSION);
                } else {
                    LogHelper.log("Main", "В файле " + propertiesFile.path() + " не найдена версия, используется значение по умолчанию: " + VERSION);
                }
            } else {
                LogHelper.log("Main", "Файлы конфигурации не найдены, используется версия по умолчанию: " + VERSION);
            }
        } catch (Exception e) {
            LogHelper.log("Main", "Ошибка при загрузке версии: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        // Освобождаем ресурсы
        FontManager.dispose();

        LogHelper.log("Main", "Game disposed");
    }
}
