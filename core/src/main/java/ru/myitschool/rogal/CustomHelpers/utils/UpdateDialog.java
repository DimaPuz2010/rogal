package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import java.io.File;

/**
 * Диалоговое окно для проверки обновлений и установки обновлений.
 */
public class UpdateDialog extends Dialog implements UpdateManager.UpdateListener {
    private final Skin skin;
    private final Stage stage;

    private Label statusLabel;
    private TextButton checkButton;
    private TextButton downloadButton;
    private TextButton installButton;
    private TextButton restartButton;
    private ProgressBar progressBar;

    private String downloadedFilePath;

    private boolean checkAutomatically = true;

    public UpdateDialog(Stage stage, Skin skin) {
        this(stage, skin, "Проверьте наличие обновлений");
    }

    public UpdateDialog(Stage stage, Skin skin, String initialMessage) {
        super("Обновление", skin);
        this.skin = skin;
        this.stage = stage;

        // Устанавливаем слушателя для UpdateManager
        UpdateManager.getInstance().setListener(this);

        createUI();

        // Устанавливаем начальное сообщение
        statusLabel.setText(initialMessage);
    }

    private void createUI() {
        // Основная таблица
        Table contentTable = getContentTable();
        contentTable.pad(20);

        // Статусная метка
        statusLabel = new Label("Проверьте наличие обновлений", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);
        contentTable.add(statusLabel).width(400).padBottom(20).row();

        // Прогресс-бар для отображения загрузки
        progressBar = new ProgressBar(0, 1, 0.01f, false, skin);
        progressBar.setVisible(false);
        contentTable.add(progressBar).width(400).height(30).padBottom(20).row();

        // Увеличиваем отступы таблицы кнопок и добавляем фоновую заливку для кнопок
        Table buttonTable = getButtonTable();
        buttonTable.pad(20);
        buttonTable.defaults().padRight(15).padLeft(15).minWidth(140).minHeight(40);

        // Создаем таблицу с кнопками
        Table buttonsTable = new Table();
        buttonsTable.defaults().pad(10).minWidth(140).minHeight(40);

        // Кнопка проверки обновлений
        checkButton = ButtonCreator.createButton("Проверить", FontManager.getButtonFont());
        checkButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                checkForUpdates();
            }
        });
        buttonsTable.add(checkButton).expandX().fill();

        // Кнопка загрузки
        downloadButton = ButtonCreator.createButton("Загрузить", FontManager.getButtonFont());
        downloadButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                downloadUpdate();
            }
        });
        downloadButton.setDisabled(true);
        buttonsTable.add(downloadButton).expandX().fill().row();

        // Кнопка установки
        installButton = ButtonCreator.createButton("Установить", FontManager.getButtonFont());
        installButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                installUpdate();
            }
        });
        installButton.setDisabled(true);
        buttonsTable.add(installButton).expandX().fill();

        // Кнопка перезапуска с новой версией
        restartButton = ButtonCreator.createButton("Перезапустить", FontManager.getButtonFont());
        restartButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                restartWithNewVersion();
            }
        });
        restartButton.setDisabled(true);
        restartButton.setVisible(false);

        // Добавляем кнопку перезапуска в отдельную строку
        buttonsTable.row();
        buttonsTable.add(restartButton).colspan(2).expandX().fill().padTop(10);

        // Кнопка отмены/закрытия
        TextButton closeButton = ButtonCreator.createButton("Закрыть", FontManager.getButtonFont());
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });
        buttonsTable.row();
        buttonsTable.add(closeButton).colspan(2).expandX().fill().padTop(10);

        // Добавляем таблицу с кнопками
        buttonTable.add(buttonsTable).fillX().expandX();

        // Устанавливаем размеры диалога с учетом всех элементов
        setWidth(520);
        setHeight(450);
        setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
    }

    /**
     * Проверяет наличие обновлений
     */
    private void checkForUpdates() {
        statusLabel.setText("Проверка обновлений...");
        statusLabel.setColor(Color.WHITE);
        checkButton.setDisabled(true);
        downloadButton.setDisabled(true);
        installButton.setDisabled(true);
        progressBar.setVisible(false);

        UpdateManager.getInstance().checkForUpdates();
    }

    /**
     * Загружает обновление
     */
    private void downloadUpdate() {
        statusLabel.setText("Подготовка к загрузке...");
        statusLabel.setColor(Color.WHITE);
        downloadButton.setDisabled(true);
        checkButton.setDisabled(true);
        installButton.setDisabled(true);

        // Сбрасываем и показываем прогресс-бар
        progressBar.setValue(0);
        progressBar.setVisible(true);

        // Запускаем загрузку с небольшой задержкой для обновления UI
        Gdx.app.postRunnable(() -> {
            // Запускаем загрузку
            UpdateManager.getInstance().downloadUpdate();
        });
    }

    /**
     * Устанавливает загруженное обновление
     */
    private void installUpdate() {
        if (downloadedFilePath != null) {
            UpdateManager.getInstance().installUpdate(downloadedFilePath);
        }
    }

    /**
     * Перезапускает приложение с новой версией
     */
    private void restartWithNewVersion() {
        if (downloadedFilePath != null) {
            // Проверяем, что файл существует
            File file = new File(downloadedFilePath);
            if (file.exists()) {
                try {
                    // Закрываем диалог
                    hide();

                    // Сначала пробуем создать скрипт обновления
                    String scriptPath = UpdateManager.getInstance().createUpdateScript();

                    if (scriptPath != null) {
                        // Получаем путь к текущему файлу
                        String currentPath = UpdateManager.getInstance().getCurrentJarPath();

                        // Запускаем скрипт с параметрами
                        LogHelper.log("UpdateDialog", "Запуск скрипта обновления: " + scriptPath);

                        ProcessBuilder processBuilder = new ProcessBuilder();

                        if (System.getProperty("os.name").toLowerCase().contains("win")) {
                            // Для Windows
                            processBuilder.command(scriptPath, downloadedFilePath, currentPath);
                        } else {
                            // Для Unix
                            processBuilder.command("sh", scriptPath, downloadedFilePath, currentPath);
                        }

                        Process process = processBuilder.start();

                        // Даем скрипту время запуститься и завершаем текущее приложение
                        Thread.sleep(500);
                        System.exit(0);
                    } else {
                        // Если создать скрипт не удалось, используем встроенный метод
                        UpdateManager.getInstance().launchNewVersionAndExit(downloadedFilePath, true);
                    }
                } catch (Exception e) {
                    LogHelper.log("UpdateDialog", "Ошибка при перезапуске: " + e.getMessage());
                    statusLabel.setText("Ошибка при перезапуске: " + e.getMessage());
                    statusLabel.setColor(Color.RED);
                }
            } else {
                statusLabel.setText("Ошибка: файл новой версии не найден");
                statusLabel.setColor(Color.RED);
            }
        }
    }

    /**
     * Отключает автоматическую проверку обновлений при показе диалога
     */
    public void disableAutomaticCheck() {
        checkAutomatically = false;
    }

    @Override
    public Dialog show(Stage stage) {
        Dialog dialog = super.show(stage);

        // Отключаем эффект затухания при появлении диалога
        this.setColor(1, 1, 1, 1);

        // Автоматически проверяем обновления при открытии диалога только если это разрешено
        if (checkAutomatically) {
            checkForUpdates();
        }

        return dialog;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        // Обновляем прогресс загрузки, если идет загрузка
        if (UpdateManager.getInstance().isDownloading() && progressBar.isVisible()) {
            float progress = (float) UpdateManager.getInstance().getDownloadProgress();
            progressBar.setValue(progress);

            // Обновляем текст с процентами скачивания
            int progressPercent = (int) (progress * 100);
            statusLabel.setText("Загрузка обновления: " + progressPercent + "%");

            // Изменяем цвет в зависимости от прогресса для лучшей наглядности
            if (progressPercent < 50) {
                statusLabel.setColor(1, 1, 0.6f, 1); // Светло-желтый
            } else if (progressPercent < 90) {
                statusLabel.setColor(0.6f, 1, 0.6f, 1); // Светло-зеленый
            } else {
                statusLabel.setColor(0, 1, 0, 1); // Зеленый
            }
        }
    }

    // Реализация методов интерфейса UpdateListener

    @Override
    public void onUpdateAvailable(String version, String downloadUrl) {
        statusLabel.setText("Доступно обновление: " + version + "\nТекущая версия: " +
            UpdateManager.getInstance().getCurrentVersion());
        statusLabel.setColor(Color.GREEN);
        checkButton.setDisabled(false);
        downloadButton.setDisabled(false);
        installButton.setDisabled(true);
    }

    @Override
    public void onNoUpdateAvailable() {
        statusLabel.setText("У вас установлена последняя версия" +
            "\nТекущая версия: " + UpdateManager.getInstance().getCurrentVersion() +
            "\nПоследняя версия: " + UpdateManager.getInstance().getLatestVersion());
        statusLabel.setColor(Color.WHITE);
        checkButton.setDisabled(false);

        // Позволим скачать файл даже если это та же версия
        if (!UpdateManager.getInstance().getDownloadUrl().isEmpty()) {
            downloadButton.setDisabled(false);
            statusLabel.setText(statusLabel.getText() + "\n\nВы можете повторно загрузить текущую версию при необходимости.");
        } else {
            downloadButton.setDisabled(true);
        }

        installButton.setDisabled(true);
        progressBar.setVisible(false);
    }

    @Override
    public void onUpdateCheckFailed(String errorMessage) {
        statusLabel.setText("Ошибка при проверке обновлений: " + errorMessage);
        statusLabel.setColor(Color.RED);
        checkButton.setDisabled(false);
        downloadButton.setDisabled(true);
        installButton.setDisabled(true);
        progressBar.setVisible(false);
    }

    @Override
    public void onUpdateDownloaded(String filePath) {
        downloadedFilePath = filePath;
        statusLabel.setText("Обновление загружено и готово к установке");
        statusLabel.setColor(Color.GREEN);
        checkButton.setDisabled(false);
        downloadButton.setDisabled(true);
        installButton.setDisabled(false);
        progressBar.setValue(1);

        // Активируем кнопку перезапуска, если мы не на Android
        if (Gdx.app.getType() != Application.ApplicationType.Android) {
            restartButton.setDisabled(false);
            restartButton.setVisible(true);
        }
    }

    @Override
    public void onUpdateDownloadFailed(String errorMessage) {
        statusLabel.setText("Ошибка при загрузке обновления: " + errorMessage);
        statusLabel.setColor(Color.RED);
        checkButton.setDisabled(false);
        downloadButton.setDisabled(false);
        installButton.setDisabled(true);
        progressBar.setVisible(false);
    }

    @Override
    public void onUpdateReadyToInstall(String filePath) {
        downloadedFilePath = filePath;

        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            // На Android нужно установить APK вручную
            statusLabel.setText("Обновление загружено. Перейдите в папку Download и установите APK-файл вручную.");
            statusLabel.setColor(Color.YELLOW);
        } else {
            // На десктопе показываем инструкции и предлагаем сразу перезапустить
            if (filePath.endsWith(".zip")) {
                statusLabel.setText("Обновление загружено. Распакуйте ZIP-файл и запустите игру: " + filePath);
                statusLabel.setColor(Color.YELLOW);
            } else {
                statusLabel.setText("Обновление готово к использованию!\n" +
                    "Вы можете нажать кнопку 'Перезапустить' для немедленного применения обновления или выполнить следующие шаги вручную:\n" +
                    "1. Закройте это окно\n" +
                    "2. Выйдите из игры\n" +
                    "3. Запустите игру снова\n" +
                    "\nОбновление расположено здесь:\n" + filePath);
                statusLabel.setColor(Color.GREEN);

                // Активируем кнопку перезапуска
                restartButton.setDisabled(false);
                restartButton.setVisible(true);

                // Отключаем другие кнопки действий
                checkButton.setDisabled(true);
                downloadButton.setDisabled(true);
                installButton.setDisabled(true);
            }
        }
    }

    @Override
    public void onUpdateInstallFailed(String errorMessage) {
        statusLabel.setText("Ошибка при установке обновления: " + errorMessage);
        statusLabel.setColor(Color.RED);
    }

    /**
     * Закрывает диалог и удаляет его со сцены
     */
    @Override
    public void hide() {
        // Отключаем слушателя обновлений, чтобы избежать утечек памяти
        UpdateManager.getInstance().setListener(null);

        // Удаляем диалог со сцены полностью
        this.remove();

        super.hide();
    }
}
