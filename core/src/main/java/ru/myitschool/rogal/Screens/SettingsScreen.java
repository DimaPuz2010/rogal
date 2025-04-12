package ru.myitschool.rogal.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.Preferences;

import ru.myitschool.rogal.CustomHelpers.utils.ButtonCreator;
import ru.myitschool.rogal.CustomHelpers.utils.FontManager;
import ru.myitschool.rogal.CustomHelpers.utils.UISkinHelper;
import ru.myitschool.rogal.Main;

public class SettingsScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Texture backgroundTexture;
    private boolean isFullscreen;
    private Skin skin;
    private Preferences preferences;
    
    // Размер UI
    private float uiScale = 1.0f;
    private Label uiScaleValueLabel;

    // Доступные разрешения экрана
    private static final Resolution[] RESOLUTIONS = {
        new Resolution(1280, 720),
        new Resolution(1366, 768),
        new Resolution(1600, 900),
        new Resolution(1920, 1080)
    };

    private static class Resolution {
        int width;
        int height;

        Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return width + "x" + height;
        }
    }

    public SettingsScreen(final Main game) {
        this.game = game;
        stage = new Stage(new FitViewport(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        backgroundTexture = new Texture("BG.png");

        skin = UISkinHelper.createSkin();

        isFullscreen = Gdx.graphics.isFullscreen();
        
        // Загружаем сохраненные настройки
        preferences = Gdx.app.getPreferences("rogal_settings");
        uiScale = preferences.getFloat("ui_scale", 1.0f);

        createUI();
    }

    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);

        Label.LabelStyle titleStyle = new Label.LabelStyle(FontManager.getTitleFont(), Color.WHITE);
        Label titleLabel = new Label("НАСТРОЙКИ", titleStyle);
        titleLabel.setAlignment(Align.center);

        Label.LabelStyle labelStyle = new Label.LabelStyle(FontManager.getRegularFont(), Color.WHITE);

        Label fullscreenLabel = new Label("Полный экран:", labelStyle);

        // Создаем чекбокс для полноэкранного режима
        final CheckBox fullscreenCheckbox = new CheckBox("", skin);
        fullscreenCheckbox.setChecked(isFullscreen);
        fullscreenCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean isChecked = fullscreenCheckbox.isChecked();
                if (isChecked) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                } else {
                    Gdx.graphics.setWindowedMode(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT);
                }
                isFullscreen = isChecked;
            }
        });

        Label resolutionLabel = new Label("Разрешение:", labelStyle);

        // Создаем выпадающий список для выбора разрешения
        final SelectBox<Resolution> resolutionSelect = new SelectBox<>(skin);
        Array<Resolution> resolutionsArray = new Array<>(RESOLUTIONS);
        resolutionSelect.setItems(resolutionsArray);

        // Устанавливаем текущее разрешение как выбранное
        int currentIndex = 0;
        for (int i = 0; i < RESOLUTIONS.length; i++) {
            if (RESOLUTIONS[i].width == Gdx.graphics.getWidth() &&
                RESOLUTIONS[i].height == Gdx.graphics.getHeight()) {
                currentIndex = i;
                break;
            }
        }
        resolutionSelect.setSelectedIndex(currentIndex);

        resolutionSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Resolution selectedResolution = resolutionSelect.getSelected();
                if (!isFullscreen) {
                    Gdx.graphics.setWindowedMode(selectedResolution.width, selectedResolution.height);
                }
            }
        });
        
        // Добавляем настройку размера интерфейса
        Label uiScaleLabel = new Label("Размер интерфейса:", labelStyle);
        
        // Слайдер для настройки размера UI (от 80% до 150%)
        final Slider uiScaleSlider = new Slider(0.8f, 1.5f, 0.1f, false, skin);
        uiScaleSlider.setValue(uiScale);
        
        // Метка для отображения текущего значения
        uiScaleValueLabel = new Label(Math.round(uiScale * 100) + "%", labelStyle);
        
        // Обработчик изменения слайдера
        uiScaleSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                uiScale = (float)Math.round(uiScaleSlider.getValue() * 10) / 10f; // Округляем до 0.1
                uiScaleValueLabel.setText(Math.round(uiScale * 100) + "%");
                
                // Сохраняем настройку
                preferences.putFloat("ui_scale", uiScale);
                preferences.flush();
            }
        });
        
        // Кнопка для возврата на главный экран
        TextButton backButton = ButtonCreator.createButton("НАЗАД", FontManager.getButtonFont());
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveSettings();
                game.setScreen(new StartScreen(game));
                //dispose();
            }
        });

        table.add(titleLabel).colspan(2).padBottom(50).row();

        table.add(fullscreenLabel).align(Align.left).padRight(20);
        table.add(fullscreenCheckbox).align(Align.left).row();

        table.add(resolutionLabel).align(Align.left).padRight(20).padTop(20);
        table.add(resolutionSelect).align(Align.left).padTop(20).row();
        
        // Добавляем настройку размера интерфейса в таблицу
        Table uiScaleTable = new Table();
        uiScaleTable.add(uiScaleSlider).width(200).padRight(10);
        uiScaleTable.add(uiScaleValueLabel).width(50);
        
        table.add(uiScaleLabel).align(Align.left).padRight(20).padTop(20);
        table.add(uiScaleTable).align(Align.left).padTop(20).row();

        table.add(backButton).colspan(2).width(300).height(80).padTop(50).row();

        stage.addActor(table);
    }
    
    /**
     * Сохраняет все настройки
     */
    private void saveSettings() {
        preferences.putBoolean("fullscreen", isFullscreen);
        preferences.putFloat("ui_scale", uiScale);
        preferences.flush();
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0, 0, Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT);
        stage.getBatch().end();

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
        skin.dispose();
        UISkinHelper.dispose();
    }
}
