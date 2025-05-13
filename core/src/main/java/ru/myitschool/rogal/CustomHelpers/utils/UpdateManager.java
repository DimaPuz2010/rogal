package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.myitschool.rogal.Main;

/**
 * Менеджер обновлений для проверки и загрузки обновлений игры с GitHub.
 */
public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/DimaPuz2010/rogal/releases/latest";

    private static final UpdateManager instance = new UpdateManager();

    private final String currentVersion;
    private String latestVersion = "";
    private String downloadUrl = "";
    private boolean updateAvailable = false;
    private double downloadProgress = 0;
    private final AtomicBoolean isDownloading = new AtomicBoolean(false);

    private UpdateListener listener;

    private UpdateManager() {
        // Приватный конструктор для синглтона
        currentVersion = Main.VERSION;
    }

    public static UpdateManager getInstance() {
        return instance;
    }


    /**
     * Получает текущую версию приложения
     */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Получает последнюю доступную версию приложения
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Проверяет наличие обновлений
     */
    public void checkForUpdates() {
        LogHelper.log(TAG, "Проверка обновлений...");
        LogHelper.log(TAG, "URL для проверки: " + GITHUB_API_URL);
        LogHelper.log(TAG, "Текущая версия: " + currentVersion);

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(GITHUB_API_URL);
        request.setHeader("User-Agent", "libGDX");

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    int statusCode = httpResponse.getStatus().getStatusCode();
                    LogHelper.log(TAG, "Код ответа: " + statusCode);

                    if (statusCode == 200) {
                        String result = httpResponse.getResultAsString();
                        LogHelper.log(TAG, "Получен ответ: " + (result.length() > 100 ? result.substring(0, 100) + "..." : result));
                        parseReleaseInfo(result);
                    } else {
                        LogHelper.log(TAG, "Ошибка проверки обновлений. Код статуса: " + statusCode);
                        if (listener != null) {
                            Gdx.app.postRunnable(() -> listener.onUpdateCheckFailed("Ошибка сервера: " + statusCode));
                        }
                    }
                } catch (Exception e) {
                    LogHelper.log(TAG, "Ошибка при обработке ответа: " + e.getMessage());
                    if (listener != null) {
                        Gdx.app.postRunnable(() -> listener.onUpdateCheckFailed(e.getMessage()));
                    }
                }
            }

            @Override
            public void failed(Throwable t) {
                LogHelper.log(TAG, "Ошибка при проверке обновлений: " + t.getMessage());
                if (listener != null) {
                    Gdx.app.postRunnable(() -> listener.onUpdateCheckFailed(t.getMessage()));
                }
            }

            @Override
            public void cancelled() {
                LogHelper.log(TAG, "Проверка обновлений отменена");
            }
        });
    }

    /**
     * Сравнивает версии в формате semantic versioning (например, "1.2.3")
     *
     * @param v1 первая версия
     * @param v2 вторая версия
     * @return -1 если v1 < v2, 0 если v1 = v2, 1 если v1 > v2
     */
    private int compareVersions(String v1, String v2) {
        // Удаляем префиксы вроде 'v' или 'version' если они есть
        v1 = v1.replaceAll("[^0-9.]", "");
        v2 = v2.replaceAll("[^0-9.]", "");

        LogHelper.log(TAG, "Сравнение версий: " + v1 + " и " + v2);

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        // Получаем максимальную длину для итерации
        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            // Если какая-то из версий закончилась, считаем что там 0
            int p1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            int p2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;

            // Если части не равны, возвращаем результат сравнения
            if (p1 != p2) {
                return p1 < p2 ? -1 : 1;
            }
        }

        // Если все части равны, версии равны
        return 0;
    }

    /**
     * Разбирает информацию о релизе из JSON
     */
    private void parseReleaseInfo(String json) {
        try {
            JsonValue root = new JsonReader().parse(json);
            latestVersion = root.getString("name");
            LogHelper.log(TAG, "Последняя версия: " + latestVersion);

            // Сравниваем версии семантически
            int comparisonResult = compareVersions(currentVersion, latestVersion);
            updateAvailable = comparisonResult < 0; // Обновление доступно если текущая версия меньше последней

            LogHelper.log(TAG, "Результат сравнения: " + comparisonResult + ", обновление доступно: " + updateAvailable);

            // Находим подходящий файл для загрузки в зависимости от платформы
            JsonValue assets = root.get("assets");
            if (assets != null && assets.size > 0) {
                LogHelper.log(TAG, "Доступные файлы для скачивания:");
                for (JsonValue asset : assets) {
                    String assetName = asset.getString("name");
                    String assetUrl = asset.getString("browser_download_url");
                    LogHelper.log(TAG, " - " + assetName + " (" + assetUrl + ")");

                    // Ищем подходящий файл для нашей платформы
                    boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
                    boolean isMatchingFile = false;

                    if (isAndroid && assetName.endsWith(".apk")) {
                        isMatchingFile = true;
                    } else if (!isAndroid && (assetName.endsWith(".exe") || assetName.endsWith(".jar") ||
                        assetName.endsWith(".zip") && assetName.contains("rogal"))) {
                        isMatchingFile = true;
                    }

                    if (isMatchingFile) {
                        downloadUrl = assetUrl;
                        LogHelper.log(TAG, "Найден подходящий файл для скачивания: " + assetName);
                    }
                }

                if (downloadUrl.isEmpty()) {
                    LogHelper.log(TAG, "Не найдены подходящие файлы для текущей платформы");
                    // Если нет подходящего файла для нашей платформы, используем первый доступный
                    if (assets.size > 0) {
                        downloadUrl = assets.get(0).getString("browser_download_url");
                        LogHelper.log(TAG, "Используем первый доступный файл: " + assets.get(0).getString("name"));
                    }
                }
            } else {
                LogHelper.log(TAG, "В релизе нет доступных файлов для скачивания");
            }

            LogHelper.log(TAG, "URL загрузки: " + downloadUrl);

            // Уведомляем о результате проверки
            if (listener != null) {
                Gdx.app.postRunnable(() -> {
                    if (updateAvailable) {
                        listener.onUpdateAvailable(latestVersion, downloadUrl);
                    } else {
                        listener.onNoUpdateAvailable();
                    }
                });
            }
        } catch (Exception e) {
            LogHelper.log(TAG, "Ошибка при разборе информации об обновлении: " + e.getMessage());
            if (listener != null) {
                Gdx.app.postRunnable(() -> listener.onUpdateCheckFailed(e.getMessage()));
            }
        }
    }

    /**
     * Загружает обновление по URL
     */
    public void downloadUpdate() {
        // Проверяем, есть ли URL для загрузки
        if (downloadUrl.isEmpty() || isDownloading.get()) {
            LogHelper.log(TAG, "Невозможно начать загрузку: URL пустой или загрузка уже идет");
            return;
        }
        // Если обновление не доступно, но пользователь хочет скачать файл принудительно
        if (!updateAvailable) {
            LogHelper.log(TAG, "Принудительная загрузка файла, хотя обновление не требуется: " + downloadUrl);
        }
        isDownloading.set(true);
        downloadProgress = 0;
        LogHelper.log(TAG, "Начало загрузки обновления: " + downloadUrl);
        try {
            // Получаем имя файла из URL
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
            // На десктопе сохраняем как .new.jar
            if (Gdx.app.getType() != Application.ApplicationType.Android && fileName.endsWith(".jar")) {
                fileName = fileName.replace(".jar", ".new.jar");
            }
            final File outputFile = getTempFile(fileName);
            outputFile.getParentFile().mkdirs();
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(downloadUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "libGDX");
                    connection.setConnectTimeout(10000); // 10 секунд на подключение
                    connection.setReadTimeout(30000);    // 30 секунд на чтение
                    connection.connect();
                    int fileLength = connection.getContentLength();
                    LogHelper.log(TAG, "Размер файла: " + fileLength + " байт");
                    try (java.io.InputStream in = connection.getInputStream();
                         java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int total = 0;
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            total += bytesRead;
                            if (fileLength > 0) {
                                final double progress = (double) total / fileLength;
                                downloadProgress = progress;
                                LogHelper.log(TAG, "Прогресс загрузки: " + (int) (progress * 100) + "%");
                            }
                        }
                    }
                    isDownloading.set(false);
                    downloadProgress = 1.0;
                    LogHelper.log(TAG, "Загрузка обновления завершена: " + outputFile.getAbsolutePath());
                    if (listener != null) {
                        final String filePath = outputFile.getAbsolutePath();
                        Gdx.app.postRunnable(() -> listener.onUpdateDownloaded(filePath));
                    }
                } catch (Exception e) {
                    LogHelper.log(TAG, "Ошибка при загрузке обновления: " + e.getMessage());
                    isDownloading.set(false);
                    if (listener != null) {
                        final String errorMessage = e.getMessage();
                        Gdx.app.postRunnable(() -> listener.onUpdateDownloadFailed(errorMessage));
                    }
                }
            }).start();
        } catch (Exception e) {
            LogHelper.log(TAG, "Ошибка при создании файла для загрузки: " + e.getMessage());
            isDownloading.set(false);
            if (listener != null) {
                listener.onUpdateDownloadFailed(e.getMessage());
            }
        }
    }

    /**
     * Создает временный файл для сохранения обновления
     */
    private File getTempFile(String fileName) throws IOException {
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            FileHandle externalDir = Gdx.files.external("Download");
            if (!externalDir.exists()) {
                externalDir.mkdirs();
            }
            return new File(externalDir.file(), fileName);
        } else {
            // На десктопе используем текущую директорию игры
            File currentDir = new File("").getAbsoluteFile();
            LogHelper.log(TAG, "Текущая директория игры: " + currentDir.getAbsolutePath());
            return new File(currentDir, fileName);
        }
    }

    /**
     * Устанавливает обновление
     * На Android автоматически открывает установщик APK
     * На десктопе уведомляет о необходимости перезапуска
     */
    public void installUpdate(String filePath) {
        LogHelper.log(TAG, "Установка обновления: " + filePath);
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            try {
                // Получаем контекст Android через рефлексию
                Class<?> androidApplicationClass = Class.forName("com.badlogic.gdx.backends.android.AndroidApplication");
                Object context = androidApplicationClass.getMethod("getContext").invoke(Gdx.app);

                // Создаем Intent для открытия установщика APK
                Class<?> intentClass = Class.forName("android.content.Intent");
                Object intent = intentClass.getConstructor(String.class)
                    .newInstance("android.intent.action.VIEW");

                // Создаем URI для файла APK
                Object apkUri;
                Class<?> buildVersionClass = Class.forName("android.os.Build$VERSION");
                int sdkInt = (int) buildVersionClass.getField("SDK_INT").get(null);

                if (sdkInt >= 24) { // Android 7.0 (API 24) и выше
                    // Используем FileProvider
                    Class<?> fileProviderClass = Class.forName("androidx.core.content.FileProvider");
                    apkUri = fileProviderClass.getMethod("getUriForFile",
                            Class.forName("android.content.Context"),
                            String.class,
                            File.class)
                        .invoke(null, context, context.getClass().getMethod("getPackageName").invoke(context) + ".provider", new File(filePath));
                } else {
                    // Для более старых версий Android
                    Class<?> uriClass = Class.forName("android.net.Uri");
                    apkUri = uriClass.getMethod("fromFile", File.class)
                        .invoke(null, new File(filePath));
                }

                // Устанавливаем данные и тип
                intentClass.getMethod("setDataAndType",
                        Class.forName("android.net.Uri"),
                        String.class)
                    .invoke(intent, apkUri, "application/vnd.android.package-archive");

                // Добавляем флаги
                int flagActivityNewTask = (int) intentClass.getField("FLAG_ACTIVITY_NEW_TASK").get(null);
                int flagGrantReadUriPermission = (int) intentClass.getField("FLAG_GRANT_READ_URI_PERMISSION").get(null);
                intentClass.getMethod("addFlags", int.class)
                    .invoke(intent, flagActivityNewTask | flagGrantReadUriPermission);

                // Запускаем установщик
                context.getClass().getMethod("startActivity", intentClass)
                    .invoke(context, intent);

                // Уведомляем пользователя
                if (listener != null) {
                    listener.onUpdateReadyToInstall(filePath);
                }
            } catch (Exception e) {
                LogHelper.log(TAG, "Ошибка при открытии установщика APK: " + e.getMessage());
                if (listener != null) {
                    listener.onUpdateInstallFailed(e.getMessage());
                }
            }
        } else {
            // На десктопе просто уведомляем пользователя
            if (listener != null) {
                listener.onUpdateReadyToInstall(filePath);
            }
        }
    }

    /**
     * Получает путь к текущему JAR-файлу игры
     */
    public String getCurrentJarPath() {
        try {
            // Получаем путь к текущему JAR-файлу
            String path = Main.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath();

            // Убираем URL-кодирование
            path = java.net.URLDecoder.decode(path, "UTF-8");

            // Убираем префикс "file:" если он есть
            if (path.startsWith("file:")) {
                path = path.substring(5);
            }

            LogHelper.log(TAG, "Путь к текущему JAR-файлу: " + path);
            return path;
        } catch (Exception e) {
            LogHelper.log(TAG, "Ошибка при определении пути к JAR-файлу: " + e.getMessage());
            return null;
        }
    }

    /**
     * Получает текущий прогресс загрузки
     */
    public double getDownloadProgress() {
        return downloadProgress;
    }

    /**
     * Проверяет, доступно ли обновление
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * Проверяет, выполняется ли в данный момент загрузка
     */
    public boolean isDownloading() {
        return isDownloading.get();
    }

    /**
     * Устанавливает слушателя событий обновления
     */
    public void setListener(UpdateListener listener) {
        this.listener = listener;
    }

    /**
     * Получает URL для загрузки обновления
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Проверяет и применяет обновление при запуске (для десктопа)
     */
    public void applyPendingUpdateIfExists() {
        if (Gdx.app.getType() == Application.ApplicationType.Android) return;
        try {
            String currentJarPath = getCurrentJarPath();
            if (currentJarPath == null) return;
            File currentJar = new File(currentJarPath);
            File dir = currentJar.getParentFile();
            // Ищем файл *.new.jar
            File[] newJars = dir.listFiles((d, name) -> name.endsWith(".new.jar"));
            if (newJars != null && newJars.length > 0) {
                File newJar = newJars[0];
                // Получаем имя новой версии (например, rogal-1.0.9.new.jar -> rogal-1.0.9.jar)
                String newJarName = newJar.getName().replace(".new.jar", ".jar");
                File targetJar = new File(dir, newJarName);
                // Создаем резервную копию текущего jar
                File backupFile = new File(dir, currentJar.getName() + ".backup");
                java.nio.file.Files.copy(currentJar.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                // Удаляем старый jar, если имя отличается
                if (!currentJar.getAbsolutePath().equals(targetJar.getAbsolutePath())) {
                    currentJar.delete();
                }
                // Переименовываем .new.jar в jar новой версии
                java.nio.file.Files.move(newJar.toPath(), targetJar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                LogHelper.log(TAG, "The update was applied at startup. New JAR: " + targetJar.getName() + ", backup: " + backupFile.getAbsolutePath());
            }
        } catch (Exception e) {
            LogHelper.log(TAG, "Error when applying the update at startup: " + e.getMessage());
        }
    }

    /**
     * Запускает новую версию приложения и завершает текущую (для десктопа)
     *
     * @param newFilePath   путь к новому jar/exe файлу
     * @param deleteOldFile удалить ли старый файл после запуска
     */
    public void launchNewVersionAndExit(String newFilePath, boolean deleteOldFile) {
        if (Gdx.app.getType() == Application.ApplicationType.Android) return;

        try {
            LogHelper.log(TAG, "Запуск новой версии: " + newFilePath);

            // Получаем путь к текущему файлу для удаления после запуска
            String currentPath = getCurrentJarPath();
            File currentFile = new File(currentPath);

            // Создаем скрипт обновления в зависимости от ОС
            String scriptExt = System.getProperty("os.name").toLowerCase().contains("win") ? "bat" : "sh";
            String scriptName = "update_script." + scriptExt;
            File scriptFile = new File(currentFile.getParentFile(), scriptName);

            LogHelper.log(TAG, "Создание скрипта обновления: " + scriptFile.getAbsolutePath());

            // Формируем содержимое скрипта
            StringBuilder scriptContent = new StringBuilder();

            if (scriptExt.equals("bat")) {
                // Windows BAT скрипт
                scriptContent.append("@echo off\n");
                scriptContent.append("echo Применение обновления...\n");
                scriptContent.append("timeout /t 1 /nobreak > nul\n");
                // Запуск нового файла
                scriptContent.append("start \"\" \"").append(newFilePath).append("\"\n");

                if (deleteOldFile && currentFile.exists()) {
                    // Удаление старого файла
                    scriptContent.append("timeout /t 2 /nobreak > nul\n");
                    scriptContent.append("del \"").append(currentPath).append("\"\n");
                }

                // Удаление самого скрипта
                scriptContent.append("timeout /t 2 /nobreak > nul\n");
                scriptContent.append("del \"%~f0\"\n");
            } else {
                // Unix SH скрипт
                scriptContent.append("#!/bin/sh\n");
                scriptContent.append("echo 'Применение обновления...'\n");
                scriptContent.append("sleep 1\n");
                // Запуск нового файла
                scriptContent.append("\"").append(newFilePath).append("\" &\n");

                if (deleteOldFile && currentFile.exists()) {
                    // Удаление старого файла
                    scriptContent.append("sleep 2\n");
                    scriptContent.append("rm \"").append(currentPath).append("\"\n");
                }

                // Удаление самого скрипта
                scriptContent.append("sleep 2\n");
                scriptContent.append("rm \"$0\"\n");
            }

            // Записываем скрипт
            try (java.io.FileWriter writer = new java.io.FileWriter(scriptFile)) {
                writer.write(scriptContent.toString());
            }

            // Делаем скрипт исполняемым (для Unix)
            if (!scriptExt.equals("bat")) {
                scriptFile.setExecutable(true);
            }

            // Запускаем скрипт обновления
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (scriptExt.equals("bat")) {
                processBuilder.command(scriptFile.getAbsolutePath());
            } else {
                processBuilder.command("sh", scriptFile.getAbsolutePath());
            }

            Process process = processBuilder.start();
            LogHelper.log(TAG, "Скрипт обновления запущен");

            // Завершаем текущее приложение
            Thread.sleep(500); // Даем скрипту время запуститься
            System.exit(0);

        } catch (Exception e) {
            LogHelper.log(TAG, "Ошибка при запуске новой версии: " + e.getMessage());
        }
    }

    /**
     * Создает временный скрипт обновления
     *
     * @return путь к скрипту обновления или null в случае ошибки
     */
    public String createUpdateScript() {
        if (Gdx.app.getType() == Application.ApplicationType.Android) return null;

        try {
            // Получаем путь к текущему файлу
            String currentPath = getCurrentJarPath();
            if (currentPath == null) return null;

            File currentFile = new File(currentPath);
            File currentDir = currentFile.getParentFile();

            // Создаем скрипт обновления в зависимости от ОС
            String scriptExt = System.getProperty("os.name").toLowerCase().contains("win") ? "bat" : "sh";
            String scriptName = "update_helper." + scriptExt;
            File scriptFile = new File(currentDir, scriptName);

            LogHelper.log(TAG, "Создание скрипта обновления: " + scriptFile.getAbsolutePath());

            // Формируем содержимое скрипта
            StringBuilder scriptContent = new StringBuilder();

            if (scriptExt.equals("bat")) {
                // Windows BAT скрипт
                scriptContent.append("@echo off\n");
                scriptContent.append("echo Утилита для применения обновлений ROGAL\n");
                scriptContent.append("echo =======================================\n\n");

                scriptContent.append(":: Проверка наличия параметров\n");
                scriptContent.append("if \"%~1\"==\"\" (\n");
                scriptContent.append("    echo Ошибка: не указан файл новой версии\n");
                scriptContent.append("    echo Использование: %~nx0 путь_к_новому_файлу [старый_файл_для_удаления]\n");
                scriptContent.append("    pause\n");
                scriptContent.append("    exit /b 1\n");
                scriptContent.append(")\n\n");

                scriptContent.append(":: Проверка существования нового файла\n");
                scriptContent.append("if not exist \"%~1\" (\n");
                scriptContent.append("    echo Ошибка: файл новой версии не найден: %~1\n");
                scriptContent.append("    pause\n");
                scriptContent.append("    exit /b 1\n");
                scriptContent.append(")\n\n");

                scriptContent.append("echo Ожидание завершения работы предыдущей версии...\n");
                scriptContent.append("timeout /t 2 /nobreak > nul\n\n");

                scriptContent.append(":: Удаление старого файла, если он указан\n");
                scriptContent.append("if not \"%~2\"==\"\" (\n");
                scriptContent.append("    if exist \"%~2\" (\n");
                scriptContent.append("        echo Удаление старой версии: %~2\n");
                scriptContent.append("        del \"%~2\"\n");
                scriptContent.append("        if errorlevel 1 (\n");
                scriptContent.append("            echo Предупреждение: не удалось удалить старый файл\n");
                scriptContent.append("        )\n");
                scriptContent.append("    ) else (\n");
                scriptContent.append("        echo Старый файл не найден: %~2\n");
                scriptContent.append("    )\n");
                scriptContent.append(")\n\n");

                scriptContent.append(":: Запуск новой версии\n");
                scriptContent.append("echo Запуск новой версии: %~1\n");
                scriptContent.append("start \"\" \"%~1\"\n");
                scriptContent.append("if errorlevel 1 (\n");
                scriptContent.append("    echo Ошибка при запуске новой версии!\n");
                scriptContent.append("    pause\n");
                scriptContent.append("    exit /b 1\n");
                scriptContent.append(")\n\n");

                scriptContent.append("echo Обновление успешно применено.\n");
                scriptContent.append("timeout /t 3 /nobreak > nul\n");
                scriptContent.append("exit /b 0\n");
            } else {
                // Unix SH скрипт
                scriptContent.append("#!/bin/sh\n\n");
                scriptContent.append("echo \"Утилита для применения обновлений ROGAL\"\n");
                scriptContent.append("echo \"=======================================\"\n\n");

                scriptContent.append("# Проверка наличия параметров\n");
                scriptContent.append("if [ -z \"$1\" ]; then\n");
                scriptContent.append("    echo \"Ошибка: не указан файл новой версии\"\n");
                scriptContent.append("    echo \"Использование: $0 путь_к_новому_файлу [старый_файл_для_удаления]\"\n");
                scriptContent.append("    exit 1\n");
                scriptContent.append("fi\n\n");

                scriptContent.append("# Проверка существования нового файла\n");
                scriptContent.append("if [ ! -f \"$1\" ]; then\n");
                scriptContent.append("    echo \"Ошибка: файл новой версии не найден: $1\"\n");
                scriptContent.append("    exit 1\n");
                scriptContent.append("fi\n\n");

                scriptContent.append("# Ожидание завершения работы предыдущей версии...\n");
                scriptContent.append("sleep 2\n\n");

                scriptContent.append("# Удаление старого файла, если он указан\n");
                scriptContent.append("if [ ! -z \"$2\" ]; then\n");
                scriptContent.append("    if [ -f \"$2\" ]; then\n");
                scriptContent.append("        echo \"Удаление старой версии: $2\"\n");
                scriptContent.append("        rm \"$2\"\n");
                scriptContent.append("        if [ $? -ne 0 ]; then\n");
                scriptContent.append("            echo \"Предупреждение: не удалось удалить старый файл\"\n");
                scriptContent.append("        fi\n");
                scriptContent.append("    else\n");
                scriptContent.append("        echo \"Старый файл не найден: $2\"\n");
                scriptContent.append("    fi\n");
                scriptContent.append("fi\n\n");

                scriptContent.append("# Делаем новый файл исполняемым\n");
                scriptContent.append("chmod +x \"$1\"\n\n");

                scriptContent.append("# Запуск новой версии\n");
                scriptContent.append("echo \"Запуск новой версии: $1\"\n");
                scriptContent.append("\"$1\" &\n");
                scriptContent.append("if [ $? -ne 0 ]; then\n");
                scriptContent.append("    echo \"Ошибка при запуске новой версии!\"\n");
                scriptContent.append("    exit 1\n");
                scriptContent.append("fi\n\n");

                scriptContent.append("echo \"Обновление успешно применено.\"\n");
                scriptContent.append("sleep 3\n");
                scriptContent.append("exit 0\n");
            }

            // Записываем скрипт
            try (java.io.FileWriter writer = new java.io.FileWriter(scriptFile)) {
                writer.write(scriptContent.toString());
            }

            // Делаем скрипт исполняемым (для Unix)
            if (!scriptExt.equals("bat")) {
                scriptFile.setExecutable(true);
            }

            LogHelper.log(TAG, "Скрипт обновления создан: " + scriptFile.getAbsolutePath());
            return scriptFile.getAbsolutePath();

        } catch (Exception e) {
            LogHelper.log(TAG, "Ошибка при создании скрипта обновления: " + e.getMessage());
            return null;
        }
    }

    /**
     * Интерфейс для слушателя событий обновления
     */
    public interface UpdateListener {
        void onUpdateAvailable(String version, String downloadUrl);

        void onNoUpdateAvailable();

        void onUpdateCheckFailed(String errorMessage);

        void onUpdateDownloaded(String filePath);

        void onUpdateDownloadFailed(String errorMessage);

        void onUpdateReadyToInstall(String filePath);

        void onUpdateInstallFailed(String errorMessage);
    }
}
