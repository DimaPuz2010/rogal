package ru.myitschool.rogal.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.Preferences;

import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.Actors.PlayerActor;
import ru.myitschool.rogal.CustomHelpers.Helpers.HitboxHelper;
import ru.myitschool.rogal.CustomHelpers.Helpers.ParallaxBackground;
import ru.myitschool.rogal.CustomHelpers.Controls.TouchpadHelpers;
import ru.myitschool.rogal.CustomHelpers.utils.GameUI;
import ru.myitschool.rogal.Enemies.EnemyManager;
import ru.myitschool.rogal.Main;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
public class GameScreen implements Screen {
    private Stage gameStage;
    private Stage uiStage;
    private ParallaxBackground background;
    private boolean fullscreen = true;
    private GameUI gameUI;
    private PlayerActor player;
    private EnemyManager enemyManager;

    // Отладочная информация
    private boolean debugMode = false;


    @Override
    public void show() {
        gameStage = new Stage(new FitViewport(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT));
        uiStage = new Stage(new FitViewport(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT));

        background = new ParallaxBackground("BG.png", (OrthographicCamera) gameStage.getCamera());
        background.setScale(1.5f);

        Touchpad touchpad = TouchpadHelpers.createTouchpad("joystick-background.png", "joystick-knob.png");
        touchpad.setPosition(Main.SCREEN_WIDTH - touchpad.getWidth(), touchpad.getY());

        player = new PlayerActor("Ship_LVL_1.png", touchpad);
        gameStage.addActor(player);
        uiStage.addActor(touchpad);

        gameUI = new GameUI(uiStage, player);
        player.setGameUI(gameUI);

        // Инициализируем менеджер противников
        enemyManager = new EnemyManager(player, gameStage);
        
        // Настраиваем диапазон появления противников
        enemyManager.setSpawnDistanceRange(350f, 700f);
        
        // Настраиваем максимальное расстояние для переспавна
        enemyManager.setMaxEnemyDistance(1500f);
        
        // Ограничиваем количество врагов на экране для лучшего баланса
        enemyManager.setMaxEnemiesOnScreen(10);

        // Загружаем настройки и применяем масштаб интерфейса
        Preferences preferences = Gdx.app.getPreferences("rogal_settings");
        float uiScale = preferences.getFloat("ui_scale", 1.0f);

        // Применяем масштаб к интерфейсу, если он был создан
        if (gameUI != null) {
            gameUI.setUIScaleFromSettings(Math.round(uiScale * 100));
        }

        // Устанавливаем обработчики клавиш
        Gdx.input.setInputProcessor(uiStage);
        Gdx.input.setCatchKey(Input.Keys.F11, true);
        Gdx.input.setCatchKey(Input.Keys.X, true); // Клавиша для добаления опыта(50)
        Gdx.input.setCatchKey(Input.Keys.Z, true); // Клавиша для нанесения урона игроку(20)
        Gdx.input.setCatchKey(Input.Keys.E, true); // Клавиша для спавна врага
        Gdx.input.setCatchKey(Input.Keys.D, true); // Клавиша для включения режима отладки
    }

    @Override
    public void render(float delta) {
        handleInput();
        handleFullscreenToggle();

        // Обновляем менеджер противников
        enemyManager.update(delta);

        ScreenUtils.clear(Color.CLEAR);
        updateCamera();
        background.render();
        gameStage.act(delta);
        gameStage.draw();
        uiStage.act(delta);
        uiStage.draw();

        // Отрисовка хитбоксов в режиме отладки
        if (debugMode) {
            renderHitboxes();
        }
    }

    /**
     * Отрисовывает хитбоксы всех объектов для отладки
     */
    private void renderHitboxes() {
        // Получаем камеру игрового мира
        OrthographicCamera camera = (OrthographicCamera) gameStage.getCamera();

        // Отрисовка хитбокса игрока
        if (player != null) {
            HitboxHelper.renderHitbox(player.getHitbox(), Color.GREEN, camera);
        }

        // Отрисовка хитбоксов врагов
        for (Actor actor : gameStage.getActors()) {
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor)actor;
                HitboxHelper.renderHitbox(enemy.getHitbox(), Color.RED, camera);
            }
        }
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.X) && player != null) {
            player.addExperience(50);
            LogHelper.log("GameScreen", "Added 50 XP to player");
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Z) && player != null) {
            boolean damaged = player.takeDamage(20);
            LogHelper.log("GameScreen", "Damage taken: " + (damaged ? "Yes" : "No (invulnerable)"));
        }

        // Переключение режима отладки клавишей D
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            debugMode = !debugMode;
            LogHelper.log("GameScreen", "Debug mode: " + (debugMode ? "ON" : "OFF"));
        }
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
        gameStage.getViewport().update(width, height);
        uiStage.getViewport().update(width, height);
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
        gameStage.dispose();
        uiStage.dispose();
        background.dispose();
        HitboxHelper.dispose();
        if (gameUI != null) {
            gameUI.dispose();
        }
        if (enemyManager != null) {
            enemyManager.dispose();
        }
    }

    private void updateCamera() {
        if (player != null) {
            gameStage.getCamera().position.x = player.getX() + player.getWidth() / 2;
            gameStage.getCamera().position.y = player.getY() + player.getHeight() / 2;
            gameStage.getCamera().update();
            return;
        }

        for (Actor actor : gameStage.getActors()) {
            if (actor instanceof PlayerActor) {
                player = (PlayerActor) actor;
                gameStage.getCamera().position.x = player.getX() + player.getWidth() / 2;
                gameStage.getCamera().position.y = player.getY() + player.getHeight() / 2;
                gameStage.getCamera().update();
                break;
            }
        }
    }
}
