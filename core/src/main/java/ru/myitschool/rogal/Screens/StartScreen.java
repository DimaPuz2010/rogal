package ru.myitschool.rogal.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import ru.myitschool.rogal.CustomHelpers.utils.ButtonCreator;
import ru.myitschool.rogal.CustomHelpers.utils.FontManager;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
import ru.myitschool.rogal.CustomHelpers.utils.PlayerData;
import ru.myitschool.rogal.Main;

public class StartScreen implements Screen {
    private final Main game;
    private final Stage stage;
    private final Texture backgroundTexture;
    private boolean fullscreen = true;

    public StartScreen(final Main game) {
        this.game = game;
        stage = new Stage(new FitViewport(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        backgroundTexture = new Texture("BG.png");

        createUI();
    }

    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);

        TextButton playButton = ButtonCreator.createButton("ИГРАТЬ", FontManager.getButtonFont());
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // При нажатии на кнопку переходим к игровому экрану
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });

        TextButton upgradeButton = ButtonCreator.createButton("УЛУЧШЕНИЯ", FontManager.getButtonFont());
        upgradeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Переходим в улучшения
                game.setScreen(new UpgradeScreen(game));
                dispose();
            }
        });

        TextButton settingsButton = ButtonCreator.createButton("НАСТРОЙКИ", FontManager.getButtonFont());
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Переходим к экрану настроек
                game.setScreen(new SettingsScreen(game));
                dispose();
            }
        });

        TextButton leaderboardButton = ButtonCreator.createButton("ТАБЛИЦА ЛИДЕРОВ", FontManager.getButtonFont());
        leaderboardButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Переходим к экрану таблицы лидеров
                game.setScreen(new LeaderboardScreen(game));
                dispose();
            }
        });

        TextButton exitButton = ButtonCreator.createButton("ВЫХОД", FontManager.getButtonFont());
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Выход из игры
                Gdx.app.exit();
            }
        });

        Label.LabelStyle titleStyle = new Label.LabelStyle(FontManager.getTitleFont(), Color.WHITE);
        Label titleLabel = new Label("ROGAL", titleStyle);
        titleLabel.setAlignment(Align.center);

        Label.LabelStyle currencyStyle = new Label.LabelStyle(FontManager.getRegularFont(), Color.YELLOW);
        Label currencyLabel = new Label("Кредиты: " + PlayerData.getCurrency(), currencyStyle);
        currencyLabel.setAlignment(Align.center);

        Label.LabelStyle versionStyle = new Label.LabelStyle(FontManager.getSmallFont(), Color.LIGHT_GRAY);
        Label versionLabel = new Label("Версия: " + Main.VERSION, versionStyle);
        versionLabel.setAlignment(Align.center);

        table.add(titleLabel).padBottom(30).row();
        table.add(currencyLabel).padBottom(10).row();
        table.add(versionLabel).padBottom(30).row();
        table.add(playButton).width(300).height(80).padBottom(20).row();
        table.add(upgradeButton).width(300).height(80).padBottom(20).row();
        table.add(leaderboardButton).width(300).height(80).padBottom(20).row();
        table.add(settingsButton).width(300).height(80).padBottom(20).row();
        table.add(exitButton).width(300).height(80).row();

        stage.addActor(table);

        LogHelper.log("StartScreen", "UI created");
    }


    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        if (!PlayerData.hasPlayerName()) {
            showNameInputDialog();
        }

    }

    /**
     * Показывает диалог ввода имени игрока
     */
    private void showNameInputDialog() {
        // Создаем затемненный фон
        Table darkBackground = new Table();
        darkBackground.setFillParent(true);
        darkBackground.setBackground(ButtonCreator.createBackground(new Color(0, 0, 0, 0.7f)));

        // Создаем диалоговое окно
        Table dialog = new Table();
        dialog.setBackground(ButtonCreator.createBackground(new Color(0.2f, 0.2f, 0.3f, 0.9f)));
        dialog.pad(20);

        // Заголовок диалога
        Label.LabelStyle titleStyle = new Label.LabelStyle(FontManager.getRegularFont(), Color.WHITE);
        Label titleLabel = new Label("ВВЕДИТЕ ВАШЕ ИМЯ", titleStyle);

        // Текст с описанием
        Label.LabelStyle descStyle = new Label.LabelStyle(FontManager.getSmallFont(), Color.LIGHT_GRAY);
        Label descLabel = new Label("Это имя будет использоваться для таблицы лидеров", descStyle);

        // Настройка стиля текстового поля
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = FontManager.getRegularFont();
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = ButtonCreator.createBackground(new Color(0.1f, 0.1f, 0.2f, 1f));
        textFieldStyle.cursor = ButtonCreator.createBackground(Color.WHITE);
        textFieldStyle.selection = ButtonCreator.createBackground(new Color(0.3f, 0.3f, 0.7f, 1f));

        // Поле ввода имени
        final TextField nameField = new TextField("Player", textFieldStyle);
        nameField.setMaxLength(15); // Ограничим длину имени

        // Кнопка ОК
        TextButton okButton = ButtonCreator.createButton("СОХРАНИТЬ", FontManager.getRegularFont());
        okButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Получаем введенное имя, проверяем на пустоту и сохраняем
                String playerName = nameField.getText().trim();
                if (playerName.isEmpty()) {
                    playerName = "Player" + System.currentTimeMillis() % 10000;
                }

                PlayerData.setPlayerName(playerName);
                darkBackground.remove();
                LogHelper.log("StartScreen", "Player name set to: " + playerName);
            }
        });

        // Добавляем элементы в диалог
        dialog.add(titleLabel).colspan(1).pad(10).row();
        dialog.add(descLabel).colspan(1).pad(10).row();
        dialog.add(nameField).width(300).padBottom(20).row();
        dialog.add(okButton).width(200).height(50).padTop(10);

        // Центрируем диалог
        darkBackground.add(dialog);

        // Добавляем диалог на сцену
        stage.addActor(darkBackground);

        // Устанавливаем фокус на поле ввода
        stage.setKeyboardFocus(nameField);
        nameField.selectAll(); // Выделяем текст по умолчанию для удобства
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
}
