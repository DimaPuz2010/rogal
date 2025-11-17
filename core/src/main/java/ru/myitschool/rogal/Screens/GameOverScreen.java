package ru.myitschool.rogal.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import ru.myitschool.rogal.CustomHelpers.utils.ButtonCreator;
import ru.myitschool.rogal.CustomHelpers.utils.FontManager;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
import ru.myitschool.rogal.CustomHelpers.utils.PlayerData;
import ru.myitschool.rogal.Main;
import ru.myitschool.rogal.networking.LeaderboardEntry;
import ru.myitschool.rogal.networking.SupabaseAPI;

/**
 * Экран поражения, показывается после смерти игрока
 */
public class GameOverScreen implements Screen {
    private final Main game;
    private final Stage stage;
    private final Texture backgroundTexture;

    // Данные о текущей игре
    private final int wave;
    private final int kills;
    private final int currencyEarned;

    // Статистика всех игр
    private final int totalKills;
    private final int gamesPlayed;
    private final int bestWave;

    // Статус отправки результата
    private Label scoreSubmitStatusLabel;
    private boolean scoreSubmitted = false;

    /**
     * Конструктор экрана поражения
     *
     * @param game           главный класс игры
     * @param wave           достигнутая волна
     * @param kills          количество убитых врагов
     * @param currencyEarned заработанная валюта
     */
    public GameOverScreen(final Main game, int wave, int kills, int currencyEarned) {
        this.game = game;
        this.wave = wave;
        this.kills = kills;
        this.currencyEarned = currencyEarned;

        // Загружаем общую статистику
        this.totalKills = PlayerData.getTotalKills();
        this.gamesPlayed = PlayerData.getGamesPlayed();
        this.bestWave = PlayerData.getBestWave();

        stage = new Stage(new FitViewport(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        backgroundTexture = new Texture("BG.png");

        createUI();

        // Отправляем результат на сервер
        submitScoreToLeaderboard();

        LogHelper.log("GameOverScreen", "Game over screen initialized");
    }

    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);

        // Заголовок "Игра окончена"
        Label.LabelStyle titleStyle = new Label.LabelStyle(FontManager.getTitleFont(), Color.RED);
        Label titleLabel = new Label("ИГРА ОКОНЧЕНА", titleStyle);
        titleLabel.setAlignment(Align.center);

        // Стиль для основного текста
        Label.LabelStyle textStyle = new Label.LabelStyle(FontManager.getRegularFont(), Color.WHITE);

        // Информация о текущей игре
        Label waveLabel = new Label("Волна: " + wave, textStyle);
        Label killsLabel = new Label("Убито врагов: " + kills, textStyle);
        Label currencyLabel = new Label("Получено кредитов: " + currencyEarned, textStyle);

        // Разделитель
        Label dividerLabel = new Label("-------------------------", textStyle);

        // Общая статистика
        Label statsHeaderLabel = new Label("ОБЩАЯ СТАТИСТИКА", textStyle);
        Label totalKillsLabel = new Label("Всего убито: " + totalKills, textStyle);
        Label gamesPlayedLabel = new Label("Игр сыграно: " + gamesPlayed, textStyle);
        Label bestWaveLabel = new Label("Лучшая волна: " + bestWave, textStyle);

        // Статус отправки результата на сервер
        scoreSubmitStatusLabel = new Label("Отправка результата...", textStyle);

        // Кнопка "улучшения"
        TextButton shopButton = ButtonCreator.createButton("УЛУЧШЕНИЯ", FontManager.getButtonFont());
        shopButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new UpgradeScreen(game));
                dispose();
            }
        });

        // Кнопка "Играть снова"
        TextButton playAgainButton = ButtonCreator.createButton("ИГРАТЬ СНОВА", FontManager.getButtonFont());
        playAgainButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });

        // Кнопка "Главное меню"
        TextButton mainMenuButton = ButtonCreator.createButton("ГЛАВНОЕ МЕНЮ", FontManager.getButtonFont());
        mainMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new StartScreen(game));
                dispose();
            }
        });

        // Добавляем элементы в таблицу
        table.add(titleLabel).colspan(2).padBottom(25).row();

        // Информация о текущей игре
        table.add(waveLabel).align(Align.left).padLeft(20).padBottom(5).row();
        table.add(killsLabel).align(Align.left).padLeft(20).padBottom(5).row();
        table.add(currencyLabel).align(Align.left).padLeft(20).padBottom(15).row();

        table.add(dividerLabel).align(Align.center).padBottom(15).row();

        // Общая статистика
        table.add(statsHeaderLabel).align(Align.center).padBottom(10).row();
        table.add(totalKillsLabel).align(Align.left).padLeft(20).padBottom(5).row();
        table.add(gamesPlayedLabel).align(Align.left).padLeft(20).padBottom(5).row();
        table.add(bestWaveLabel).align(Align.left).padLeft(20).padBottom(10).row();

        // Статус отправки результата
        table.add(scoreSubmitStatusLabel).align(Align.center).padBottom(15).row();

        // Кнопки
        table.add(shopButton).width(280).height(50).padBottom(10).row();
        table.add(playAgainButton).width(280).height(50).padBottom(10).row();
        table.add(mainMenuButton).width(280).height(50).row();

        stage.addActor(table);
    }

    /**
     * Отправляет результат игрока на сервер таблицы лидеров
     */
    private void submitScoreToLeaderboard() {
        if (scoreSubmitted) return;

        String playerNameTemp = PlayerData.getPlayerName();

        // Если имя не задано (что теоретически не должно произойти), используем временное имя
        if (playerNameTemp.isEmpty()) {
            playerNameTemp = "Player" + System.currentTimeMillis() % 10000;
            LogHelper.error("GameOverScreen", "Имя игрока не найдено, используем временное: " + playerNameTemp);
        }

        final String playerName = playerNameTemp;

        submitScore(playerName, wave, kills);
    }

    /**
     * Отправляет результат игры в таблицу лидеров
     *
     * @param playerName имя игрока
     * @param wave       последняя пройденная волна
     * @param kills      количество убитых врагов
     */
    private void submitScore(String playerName, int wave, int kills) {
        try {
            SupabaseAPI.submitScore(playerName, wave, kills, bestWave, new SupabaseAPI.SubmitScoreListener() {
                @Override
                public void onSuccess(LeaderboardEntry entry) {
                    scoreSubmitStatusLabel.setText("Результат отправлен! Счет: " + entry.getScore());
                    scoreSubmitted = true;
                    LogHelper.log("GameOverScreen", "Результат успешно отправлен: " + entry.getScore() + " очков");

                    // Проверяем, является ли текущий результат лучшим для игрока
                    checkIfBestScore(playerName, entry);
                }

                @Override
                public void onError(String error) {
                    scoreSubmitStatusLabel.setText("Ошибка отправки: " + error);
                }
            });
        } catch (Exception e) {
            scoreSubmitStatusLabel.setText("Не удалось отправить результат");
        }
    }

    /**
     * Проверяет, является ли результат лучшим для игрока
     *
     * @param playerName имя игрока
     * @param entry      отправленный результат
     */
    private void checkIfBestScore(String playerName, LeaderboardEntry entry) {
        SupabaseAPI.getPlayerBestScore(playerName, new SupabaseAPI.GetBestScoreListener() {
            @Override
            public void onSuccess(LeaderboardEntry bestScore) {
                if (bestScore != null) {

                    // Проверяем по флагу isBestScore или по ID записи
                    if (bestScore.isBestScore() && bestScore.getId() == entry.getId()) {
                        scoreSubmitStatusLabel.setText("Новый рекорд! Счет: " + entry.getScore());
                    }
                }
            }

            @Override
            public void onError(String error) {
                LogHelper.error("GameOverScreen", "Ошибка при получении лучшего результата: " + error);
            }
        });
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        // Рисуем фон
        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0, 0, Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT);
        stage.getBatch().end();

        // Рисуем интерфейс
        stage.act(delta);
        stage.draw();
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
}
