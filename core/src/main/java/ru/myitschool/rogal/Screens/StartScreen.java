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
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import ru.myitschool.rogal.CustomHelpers.utils.ButtonCreator;
import ru.myitschool.rogal.Main;
import ru.myitschool.rogal.CustomHelpers.utils.FontManager;

public class StartScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Texture backgroundTexture;
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

        // Создаем кнопку "Играть" с пиксельным шрифтом
        TextButton playButton = ButtonCreator.createButton("ИГРАТЬ", FontManager.getButtonFont());
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // При нажатии на кнопку переходим к игровому экрану
                game.setScreen(new GameScreen());
                dispose();
            }
        });
        
        // Создаем кнопку "Настройки" с пиксельным шрифтом
        TextButton settingsButton = ButtonCreator.createButton("НАСТРОЙКИ", FontManager.getButtonFont());
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Переходим к экрану настроек
                game.setScreen(new SettingsScreen(game));
                dispose();
            }
        });
        
        // Создаем кнопку "Выход" с пиксельным шрифтом
        TextButton exitButton = ButtonCreator.createButton("ВЫХОД", FontManager.getButtonFont());
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Выход из игры
                Gdx.app.exit();
            }
        });

        // Создаем заголовок игры с пиксельным шрифтом
        Label.LabelStyle titleStyle = new Label.LabelStyle(FontManager.getTitleFont(), Color.WHITE);
        Label titleLabel = new Label("ROGAL", titleStyle);
        titleLabel.setAlignment(Align.center);

        // Добавляем элементы на сцену
        table.add(titleLabel).padBottom(50).row();
        table.add(playButton).width(300).height(80).padBottom(20).row();
        table.add(settingsButton).width(300).height(80).padBottom(20).row();
        table.add(exitButton).width(300).height(80).row();

        stage.addActor(table);
    }

    @Override
    public void show() {
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
