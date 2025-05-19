package ru.myitschool.rogal;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
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

    public static String VERSION = "1.0.9";

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        LogHelper.log("Main", "Игра запущена. Версия: " + VERSION);

        UpdateManager.getInstance().applyPendingUpdateIfExists();
        UpdateManager.getInstance().checkForUpdates();

        cleanupOldFiles();

        FontManager.initialize();
        loadVersion();
        PlayerData.initialize();
        LeaderboardAPI.initialize();

        setScreen(new StartScreen(this));

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
                }
            }
        } catch (Exception e) {
            LogHelper.log("Main", "Ошибка при загрузке версии: " + e.getMessage());
        }
    }

    /**
     * Очищает старые файлы после обновления
     */
    private void cleanupOldFiles() {
        if (Gdx.app.getType() == Application.ApplicationType.Android) return;

        try {
            // Получаем текущую директорию
            String currentJarPath = UpdateManager.getInstance().getCurrentJarPath();
            if (currentJarPath == null) return;

            File currentDir = new File(currentJarPath).getParentFile();
            LogHelper.log("Main", "Проверка файлов для очистки в директории: " + currentDir.getAbsolutePath());

            // Ищем резервные копии и временные файлы
            File[] oldFiles = currentDir.listFiles((dir, name) -> {
                return name.endsWith(".backup") || name.endsWith(".bak") ||
                    name.startsWith("update_script.") || name.contains(".old.");
            });

            if (oldFiles != null && oldFiles.length > 0) {
                LogHelper.log("Main", "Найдено " + oldFiles.length + " файлов для удаления");

                for (File file : oldFiles) {
                    try {
                        boolean deleted = file.delete();
                        LogHelper.log("Main", "Удаление файла " + file.getName() + ": " +
                            (deleted ? "успешно" : "не удалось"));
                    } catch (Exception e) {
                        LogHelper.log("Main", "Ошибка при удалении файла " + file.getName() + ": " + e.getMessage());
                    }
                }
            } else {
                LogHelper.log("Main", "Старых файлов не найдено");
            }
        } catch (Exception e) {
            LogHelper.log("Main", "Ошибка при очистке старых файлов: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        // Освобождаем ресурсы
        FontManager.dispose();
    }
}
