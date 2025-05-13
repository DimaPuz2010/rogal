package ru.myitschool.rogal.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;

import ru.myitschool.rogal.CustomHelpers.utils.ButtonCreator;
import ru.myitschool.rogal.CustomHelpers.utils.FontManager;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
import ru.myitschool.rogal.Main;
import ru.myitschool.rogal.networking.LeaderboardAPI;
import ru.myitschool.rogal.networking.LeaderboardEntry;

/**
 * Экран для отображения таблицы лидеров
 */
public class LeaderboardScreen implements Screen {
    private final Main game;
    private final Stage stage;
    private final Texture backgroundTexture;
    private Table leaderboardTable;
    private Label statusLabel;
    private boolean isLoading = false;
    private boolean fullscreen = true;

    // Режим отображения: обычная таблица или лучшие результаты
    private boolean showOnlyBestScores = false;

    // Текущая страница и размер страницы для пагинации
    private int currentPage = 1;
    private int PAGE_SIZE = 50;

    // Сохраняем загруженные записи
    private List<LeaderboardEntry> leaderboardEntries = new ArrayList<>();

    // Статус сервера
    private boolean isServerAvailable = false;
    private Label serverInfoLabel;

    public LeaderboardScreen(final Main game) {
        this.game = game;
        stage = new Stage(new FitViewport(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        backgroundTexture = new Texture("BG.png");

        createUI();
        checkServerStatus();
    }

    private void createUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);

        // Создаем заголовок
        Label.LabelStyle titleStyle = new Label.LabelStyle(FontManager.getTitleFont(), Color.WHITE);
        Label titleLabel = new Label("ТАБЛИЦА ЛИДЕРОВ", titleStyle);
        titleLabel.setAlignment(Align.center);

        // Создаем статус загрузки
        Label.LabelStyle statusStyle = new Label.LabelStyle(FontManager.getRegularFont(), Color.YELLOW);
        statusLabel = new Label("Загрузка...", statusStyle);
        statusLabel.setAlignment(Align.center);

        // Создаем информацию о текущем сервере
        Label.LabelStyle serverInfoStyle = new Label.LabelStyle(FontManager.getSmallFont(), Color.LIGHT_GRAY);
        String serverInfo = LeaderboardAPI.getServerProtocol() + "://" +
            LeaderboardAPI.getServerHost() + ":" +
            LeaderboardAPI.getServerPort() +
            LeaderboardAPI.getServerPath();
        serverInfoLabel = new Label("Сервер: " + serverInfo, serverInfoStyle);
        serverInfoLabel.setAlignment(Align.center);

        // Создаем таблицу лидеров
        leaderboardTable = new Table();
        leaderboardTable.pad(10);

        // Добавляем заголовки столбцов
        Label.LabelStyle headerStyle = new Label.LabelStyle(FontManager.getRegularFont(), new Color(0.8f, 0.8f, 1f, 1f));

        Table headerTable = new Table();
        headerTable.add(new Label("МЕСТО", headerStyle)).width(100).padRight(5);
        headerTable.add(new Label("ИГРОК", headerStyle)).width(200).padRight(5);
        headerTable.add(new Label("СЧЕТ", headerStyle)).width(150).padRight(5);
        headerTable.add(new Label("ВОЛНА", headerStyle)).width(100).padRight(5);
        headerTable.add(new Label("УБИЙСТВА", headerStyle)).width(150).padRight(5);
        headerTable.add(new Label("ДАТА", headerStyle)).width(200);

        mainTable.add(titleLabel).padTop(20).padBottom(20).row();
        mainTable.add(statusLabel).padBottom(10).row();
        mainTable.add(serverInfoLabel).padBottom(10).row();
        mainTable.add(headerTable).row();

        // Создаем прокрутку для таблицы
        ScrollPane scrollPane = new ScrollPane(leaderboardTable);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setupFadeScrollBars(0f, 0f);
        scrollPane.setScrollBarPositions(false, true);
        scrollPane.setScrollbarsVisible(true);
        scrollPane.setScrollbarsOnTop(false);

        // Устанавливаем стиль скроллбара чтобы он был более заметным
        Skin skin = new Skin();
        Pixmap pixmap = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.4f, 0.4f, 0.8f, 1));
        pixmap.fill();
        skin.add("scrollbar", new Texture(pixmap));
        pixmap.dispose();

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        scrollPaneStyle.vScrollKnob = skin.getDrawable("scrollbar");
        scrollPane.setStyle(scrollPaneStyle);

        // Добавляем скролл-панель с настроенным размером
        mainTable.add(scrollPane).expand().fill().padBottom(20).row();

        // Кнопка переключения режима отображения
        TextButton toggleViewButton = ButtonCreator.createButton("ЛУЧШИЕ РЕЗУЛЬТАТЫ", FontManager.getRegularFont());
        toggleViewButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showOnlyBestScores = !showOnlyBestScores;
                if (showOnlyBestScores) {
                    toggleViewButton.setText("ВСЕ РЕЗУЛЬТАТЫ");
                } else {
                    toggleViewButton.setText("ЛУЧШИЕ РЕЗУЛЬТАТЫ");
                }

                // Перезагружаем данные с новым режимом
                loadLeaderboard(1, PAGE_SIZE);
            }
        });

        // Кнопка настройки сервера
        TextButton serverSettingsButton = ButtonCreator.createButton("НАСТРОЙКИ СЕРВЕРА", FontManager.getRegularFont());
        serverSettingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showServerSettingsDialog();
            }
        });

        // Кнопка возврата в главное меню
        TextButton backButton = ButtonCreator.createButton("НАЗАД В МЕНЮ", FontManager.getRegularFont());
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new StartScreen(game));
                dispose();
            }
        });

        // Создаем нижнюю панель с кнопками
        Table bottomPanel = new Table();
        bottomPanel.add(toggleViewButton).width(300).height(50).padRight(10);
        bottomPanel.add(serverSettingsButton).width(300).height(50).padRight(10);
        bottomPanel.add(backButton).width(300).height(50);

        mainTable.add(bottomPanel).padBottom(20).row();

        stage.addActor(mainTable);

        LogHelper.log("LeaderboardScreen", "UI created");
    }

    /**
     * Проверяет доступность сервера
     */
    private void checkServerStatus() {
        statusLabel.setText("Проверка сервера...");

        LeaderboardAPI.checkServerStatus(new LeaderboardAPI.ServerStatusListener() {
            @Override
            public void onSuccess(boolean isAvailable) {
                isServerAvailable = true;
                String serverInfo = LeaderboardAPI.getServerProtocol() + "://" +
                    LeaderboardAPI.getServerHost() + ":" +
                    LeaderboardAPI.getServerPort() +
                    LeaderboardAPI.getServerPath();

                serverInfoLabel.setText("Сервер: " + serverInfo + " (доступен)");
                serverInfoLabel.setColor(new Color(0.5f, 1f, 0.5f, 1f));

                // Сервер доступен, загружаем данные
                loadLeaderboard(currentPage, PAGE_SIZE);
            }

            @Override
            public void onError(String error) {
                isServerAvailable = false;
                String serverInfo = LeaderboardAPI.getServerProtocol() + "://" +
                    LeaderboardAPI.getServerHost() + ":" +
                    LeaderboardAPI.getServerPort() +
                    LeaderboardAPI.getServerPath();

                serverInfoLabel.setText("Сервер: " + serverInfo + " (недоступен)");
                serverInfoLabel.setColor(new Color(1f, 0.5f, 0.5f, 1f));

                statusLabel.setText("Ошибка: сервер недоступен");
                LogHelper.error("LeaderboardScreen", "Server check failed: " + error);
            }
        });
    }

    /**
     * Загружает данные таблицы лидеров с сервера
     */
    private void loadLeaderboard(final int page, final int size) {
        if (isLoading) return;

        // Если сервер недоступен, просто показываем сообщение
        if (!isServerAvailable) {
            statusLabel.setText("Сервер недоступен. Настройте подключение.");
            return;
        }

        isLoading = true;
        statusLabel.setText("Загрузка...");
        leaderboardTable.clear();

        LogHelper.log("LeaderboardScreen", "Loading leaderboard page " + page + ", best scores only: " + showOnlyBestScores);

        if (showOnlyBestScores) {
            // Загружаем только лучшие результаты
            LeaderboardAPI.getBestScores(page, size, new LeaderboardAPI.LeaderboardResponseListener() {
                @Override
                public void onSuccess(List<LeaderboardEntry> entries, int totalEntries, int currentPageResponse, int totalPagesResponse) {
                    handleLeaderboardResponse(entries, totalEntries, currentPageResponse);
                }

                @Override
                public void onError(String error) {
                    handleLeaderboardError(error);
                }
            });
        } else {
            // Загружаем обычную таблицу лидеров
            LeaderboardAPI.getLeaderboard(page, size, new LeaderboardAPI.LeaderboardResponseListener() {
                @Override
                public void onSuccess(List<LeaderboardEntry> entries, int totalEntries, int currentPageResponse, int totalPagesResponse) {
                    handleLeaderboardResponse(entries, totalEntries, currentPageResponse);
                }

                @Override
                public void onError(String error) {
                    handleLeaderboardError(error);
                }
            });
        }
    }

    /**
     * Обрабатывает успешный ответ сервера с данными таблицы лидеров
     */
    private void handleLeaderboardResponse(List<LeaderboardEntry> entries, int totalEntries, int currentPageResponse) {
        Gdx.app.postRunnable(() -> {
            isLoading = false;
            leaderboardEntries = entries;
            PAGE_SIZE = entries.size();
            currentPage = currentPageResponse;

            // Обновляем UI
            updateLeaderboardTable();

            // Обновляем статус
            if (entries.isEmpty()) {
                statusLabel.setText("Нет данных");
            } else {
                statusLabel.setText("Загружено записей: " + entries.size() + " из " + totalEntries);
            }

            LogHelper.log("LeaderboardScreen", "Loaded " + entries.size() + " entries");
        });
    }

    /**
     * Обрабатывает ошибку при загрузке таблицы лидеров
     */
    private void handleLeaderboardError(String error) {
        Gdx.app.postRunnable(() -> {
            isLoading = false;
            statusLabel.setText("Ошибка: " + error);
            LogHelper.error("LeaderboardScreen", "Error loading leaderboard: " + error);
        });
    }

    /**
     * Обновляет таблицу лидеров с загруженными данными
     */
    private void updateLeaderboardTable() {
        leaderboardTable.clear();

        Label.LabelStyle regularStyle = new Label.LabelStyle(FontManager.getSmallFont(), Color.WHITE);
        Label.LabelStyle scoreStyle = new Label.LabelStyle(FontManager.getSmallFont(), new Color(1f, 0.8f, 0.2f, 1f));

        int startRank = (currentPage - 1) * PAGE_SIZE + 1;

        for (int i = 0; i < leaderboardEntries.size(); i++) {
            LeaderboardEntry entry = leaderboardEntries.get(i);
            int rank = startRank + i;

            Table row = new Table();
            row.pad(5);

            // Выделяем лучшие результаты особым цветом
            if (entry.isBestScore()) {
                row.setBackground(ButtonCreator.createBackground(new Color(0.3f, 0.3f, 0.5f, 0.7f)));
            } else if (i % 2 == 0) {
                row.setBackground(ButtonCreator.createBackground(new Color(0.2f, 0.2f, 0.3f, 0.5f)));
            } else {
                row.setBackground(ButtonCreator.createBackground(new Color(0.3f, 0.3f, 0.4f, 0.5f)));
            }

            // Создаем метки для каждого поля
            Label rankLabel = new Label(String.valueOf(rank), regularStyle);
            Label nameLabel = new Label(entry.getPlayerName(), regularStyle);
            Label scoreLabel = new Label(String.valueOf(entry.getScore()), scoreStyle);
            Label waveLabel = new Label(String.valueOf(entry.getWave()), regularStyle);
            Label killsLabel = new Label(String.valueOf(entry.getKills()), regularStyle);
            Label dateLabel = new Label(entry.getTimestamp() != null ? entry.getTimestamp() : "-", regularStyle);

            // Добавляем метки в строку
            row.add(rankLabel).width(100).padRight(5);
            row.add(nameLabel).width(200).padRight(5);
            row.add(scoreLabel).width(150).padRight(5);
            row.add(waveLabel).width(100).padRight(5);
            row.add(killsLabel).width(150).padRight(5);
            row.add(dateLabel).width(200);

            leaderboardTable.add(row).expandX().fillX().row();
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        handleFullscreenToggle();
        ScreenUtils.clear(0, 0, 0, 1);

        // Рисуем фон
        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0, 0, Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT);
        stage.getBatch().end();

        // Рисуем интерфейс
        stage.act(delta);
        stage.draw();
    }

    private void handleFullscreenToggle() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            fullscreen = !fullscreen;
            if (fullscreen) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } else {
                Gdx.graphics.setWindowedMode(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
    }

    /**
     * Показывает диалог настройки сервера
     */
    private void showServerSettingsDialog() {
        // Создаем затемненный фон
        final Table darkBackground = new Table();
        darkBackground.setFillParent(true);
        darkBackground.setBackground(ButtonCreator.createBackground(new Color(0, 0, 0, 0.7f)));

        // Создаем диалоговое окно
        Table dialog = new Table();
        dialog.setBackground(ButtonCreator.createBackground(new Color(0.2f, 0.2f, 0.3f, 0.9f)));
        dialog.pad(20);

        // Создаем заголовок
        Label.LabelStyle titleStyle = new Label.LabelStyle(FontManager.getRegularFont(), Color.WHITE);
        Label titleLabel = new Label("НАСТРОЙКИ СЕРВЕРА", titleStyle);
        titleLabel.setAlignment(Align.center);

        // Создаем стиль для текстовых полей
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = FontManager.getRegularFont();
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = ButtonCreator.createBackground(new Color(0.1f, 0.1f, 0.2f, 1f));
        textFieldStyle.cursor = ButtonCreator.createBackground(Color.WHITE);
        textFieldStyle.selection = ButtonCreator.createBackground(new Color(0.3f, 0.3f, 0.7f, 1f));

        // Поле ввода протокола
        Label protocolLabel = new Label("Протокол:", titleStyle);
        final TextField protocolField = new TextField(LeaderboardAPI.getServerProtocol(), textFieldStyle);
        protocolField.setTextFieldFilter(new TextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField textField, char c) {
                return Character.isLetterOrDigit(c);
            }
        });

        // Поле ввода хоста
        Label hostLabel = new Label("Хост:", titleStyle);
        final TextField hostField = new TextField(LeaderboardAPI.getServerHost(), textFieldStyle);

        // Поле ввода порта
        Label portLabel = new Label("Порт:", titleStyle);
        final TextField portField = new TextField(String.valueOf(LeaderboardAPI.getServerPort()), textFieldStyle);
        portField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());

        // Поле ввода пути API
        Label pathLabel = new Label("Путь API:", titleStyle);
        final TextField pathField = new TextField(LeaderboardAPI.getServerPath(), textFieldStyle);

        // Кнопки
        TextButton saveButton = ButtonCreator.createButton("СОХРАНИТЬ", FontManager.getRegularFont());
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    String protocol = protocolField.getText().trim();
                    String host = hostField.getText().trim();
                    String port = portField.getText().trim();
                    String path = pathField.getText().trim();

                    // Проверка и установка значений по умолчанию
                    if (protocol.isEmpty()) {
                        protocol = "http";
                    }

                    if (host.isEmpty()) {
                        host = "localhost";
                    }

                    if (Integer.parseInt(port) <= 0) {
                        port = "8080";
                    }

                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }

                    // Сохраняем все настройки
                    LeaderboardAPI.setServerProtocol(protocol);
                    LeaderboardAPI.setServerHost(host);
                    LeaderboardAPI.setServerPort(Integer.parseInt(port));
                    LeaderboardAPI.setServerPath(path);

                    darkBackground.remove();

                    // Обновляем интерфейс
                    serverInfoLabel.setText("Сервер: " + protocol + "://" + host + ":" + port + path);
                    serverInfoLabel.setColor(Color.LIGHT_GRAY);

                    // Проверяем доступность сервера
                    checkServerStatus();

                    LogHelper.log("LeaderboardScreen", "Настройки сервера сохранены: " + protocol + "://" + host + ":" + port + path);
                } catch (Exception e) {
                    LogHelper.error("LeaderboardScreen", "Ошибка при сохранении настроек сервера: " + e.getMessage());
                }
            }
        });

        TextButton cancelButton = ButtonCreator.createButton("ОТМЕНА", FontManager.getRegularFont());
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                darkBackground.remove();
            }
        });

        // Добавляем элементы в диалог
        dialog.add(titleLabel).colspan(2).pad(10).row();

        dialog.add(protocolLabel).padRight(10).align(Align.right);
        dialog.add(protocolField).width(200).padBottom(10).row();

        dialog.add(hostLabel).padRight(10).align(Align.right);
        dialog.add(hostField).width(200).padBottom(10).row();

        dialog.add(portLabel).padRight(10).align(Align.right);
        dialog.add(portField).width(200).padBottom(10).row();

        dialog.add(pathLabel).padRight(10).align(Align.right);
        dialog.add(pathField).width(200).padBottom(20).row();

        Table buttonTable = new Table();
        buttonTable.add(saveButton).width(140).height(50).padRight(20);
        buttonTable.add(cancelButton).width(140).height(50);

        dialog.add(buttonTable).colspan(2).padTop(10);

        // Центрируем диалог
        darkBackground.add(dialog);

        // Добавляем диалог на сцену
        stage.addActor(darkBackground);

        // Устанавливаем фокус на поле хоста
        stage.setKeyboardFocus(hostField);
    }
}
