package ru.myitschool.rogal.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.Actors.PlayerActor;
import ru.myitschool.rogal.CustomHelpers.Controls.TouchpadHelpers;
import ru.myitschool.rogal.CustomHelpers.Helpers.HitboxHelper;
import ru.myitschool.rogal.CustomHelpers.Helpers.ParallaxBackground;
import ru.myitschool.rogal.CustomHelpers.utils.GameUI;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
import ru.myitschool.rogal.CustomHelpers.utils.PlayerData;
import ru.myitschool.rogal.Enemies.EnemyManager;
import ru.myitschool.rogal.Main;

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

    // Текущая волна
    private final int currentWave = 1;
    // Количество убитых врагов
    private int enemiesKilled = 0;
    // Флаг окончания игры
    private final boolean gameOver = false;

    // Флаг для отслеживания перехода на экран поражения
    private boolean transitioning = false;

    // Ссылка на главный класс игры
    private final Main game;

    // Флаг для паузы игры во время выбора способности
    private boolean isAbilitySelectionPaused = false;

    // Флаг для паузы игры
    private boolean isPaused = false;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        gameStage = new Stage(new FitViewport(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT));
        uiStage = new Stage(new FitViewport(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT));

        background = new ParallaxBackground("BG.png", (OrthographicCamera) gameStage.getCamera());
        background.setScale(1.5f);

        // Получаем масштаб UI из настроек
        Preferences preferences = Gdx.app.getPreferences("rogal_settings");
        float uiScale = preferences.getFloat("ui_scale", 1.0f);

        // Создаем джойстик с учетом масштаба UI
        Touchpad touchpad = TouchpadHelpers.createScaledTouchpad("joystick-background.png", "joystick-knob.png", uiScale);

        // Позиционируем джойстик с учетом масштаба UI
        float posX = Main.SCREEN_WIDTH - touchpad.getWidth() - 20 * uiScale;
        float posY = 20 * uiScale;
        touchpad.setPosition(posX, posY);

        boolean touchControlMode = PlayerData.getControlMode() == 1;
        // В режиме управления касанием джойстик не нужен
        if (touchControlMode) {
            touchpad.setVisible(false);
        }

        player = new PlayerActor("Ship_LVL_1.png", touchpad);
        // Устанавливаем обработчик смерти игрока
        player.setDeathHandler(new PlayerActor.DeathHandler() {
            @Override
            public void onPlayerDeath() {
                handlePlayerDeath();
            }
        });

        gameStage.addActor(player);
        uiStage.addActor(touchpad);

        gameUI = new GameUI(uiStage, player);
        player.setGameUI(gameUI);

        // Настраиваем обработчик ввода с использованием InputMultiplexer
        // для обработки как UI, так и игровых касаний
        InputMultiplexer inputMultiplexer = new InputMultiplexer();

        // UI всегда получает ввод первым, независимо от режима управления
        inputMultiplexer.addProcessor(uiStage);

        // В режиме управления касанием добавляем gameStage как второй обработчик
        if (touchControlMode) {
            // Создаем специальный InputProcessor для gameStage, который будет
            // обрабатывать касания только для движения игрока
            InputProcessor gameInputProcessor = new InputProcessor() {
                @Override
                public boolean keyDown(int keycode) {
                    return false;
                }

                @Override
                public boolean keyUp(int keycode) {
                    return false;
                }

                @Override
                public boolean keyTyped(char character) {
                    return false;
                }

                @Override
                public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                    // Проверяем, активен ли выбор способности
                    if (isAbilitySelectionPaused) {
                        return false;
                    }

                    // Проверяем, попало ли касание на элемент UI
                    Actor hitActor = uiStage.hit(screenX, screenY, true);
                    // Если касание не попало на UI элемент (hitActor == null) или попало на корневой элемент,
                    // то обрабатываем его для движения игрока
                    if (hitActor == null) {
                        // Обработка касания для движения игрока
                        handleTouchForPlayerMovement(screenX, screenY);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                    return false;
                }

                @Override
                public boolean touchDragged(int screenX, int screenY, int pointer) {
                    // Обрабатываем перетаскивание только если не выбор способности и UI не обработал его
                    // Проверяем, активен ли выбор способности
                    if (isAbilitySelectionPaused) {
                        return false;
                    }

                    // Проверяем, попало ли касание на элемент UI
                    Actor hitActor = uiStage.hit(screenX, screenY, true);
                    // Если касание не попало на UI элемент (hitActor == null) или попало на корневой элемент,
                    // то обрабатываем его для движения игрока
                    if (hitActor == null) {
                        // Обработка перетаскивания для движения игрока
                        handleTouchForPlayerMovement(screenX, screenY);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean mouseMoved(int screenX, int screenY) {
                    return false;
                }

                @Override
                public boolean scrolled(float amountX, float amountY) {
                    return false;
                }

                @Override
                public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
                    return false;
                }
            };

            inputMultiplexer.addProcessor(gameInputProcessor);
        }

        Gdx.input.setInputProcessor(inputMultiplexer);

        enemyManager = new EnemyManager(player, gameStage);

        enemyManager.setSpawnDistanceRange(350f, 700f);
        enemyManager.setMaxEnemyDistance(1500f);
        enemyManager.setMaxEnemiesOnScreen(10);
        enemyManager.setWaveListener(gameUI.createWaveListener());

        enemyManager.setEnemyKillListener(new EnemyManager.EnemyKillListener() {
            @Override
            public void onEnemyKilled() {
                enemiesKilled++;
            }
        });

        if (gameUI != null) {
            gameUI.setUIScaleFromSettings(Math.round(uiScale * 100));
        }

        Gdx.input.setCatchKey(Input.Keys.F11, true);
    }

    @Override
    public void render(float delta) {
        if (gameOver && !transitioning) {
            transitioning = true;
            handlePlayerDeath();
            return;
        }

        handleInput();
        handleFullscreenToggle();

        ScreenUtils.clear(0, 0, 0, 1);

        updateCamera();

        background.render();

        if (!isAbilitySelectionPaused && !isPaused) {
            gameStage.act(delta);

            if (enemyManager != null) {
                enemyManager.update(delta);
            }
        }

        gameStage.draw();

        // Отрисовка маркера целевой точки для режима управления касанием
        // Не отображаем маркер если выбор способности или пауза
        if (PlayerData.getControlMode() == 1 && player != null && player.isMovingToTarget()
            && gameUI != null && !isAbilitySelectionPaused && !isPaused) {
            gameStage.getBatch().begin();
            gameUI.drawTargetMarker(gameStage.getBatch());
            gameStage.getBatch().end();
        }

        uiStage.act(delta);
        uiStage.draw();
    }

    private void handleInput() {
        // Обработка клавиши Escape для вызова/скрытия меню паузы
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
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
        gameStage.getViewport().update(width, height, true);
        uiStage.getViewport().update(width, height, true);

        if (gameUI != null) {
            LogHelper.log("GameScreen", "Обновляем gameUI");
            gameUI.resize(width, height);
        } else {
            LogHelper.log("GameScreen", "gameUI is null");
        }
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

    /**
     * Обрабатывает смерть игрока и подготавливает данные для экрана поражения
     */
    private void handlePlayerDeath() {
        LogHelper.log("GameScreen", "Player died, transitioning to game over screen");
        LogHelper.log("GameScreen", "Final score: wave=" + enemyManager.getCurrentWave() + ", kills=" + enemiesKilled);

        // Рассчитываем награду валюты
        int currencyReward = 50 + (enemyManager.getCurrentWave() * 10) + enemiesKilled;

        PlayerData.addCurrency(currencyReward);
        PlayerData.updateStats(enemiesKilled, enemyManager.getCurrentWave());

        game.setScreen(new GameOverScreen(game, enemyManager.getCurrentWave(), enemiesKilled, currencyReward));
    }

    /**
     * Проверяет, находится ли игра в режиме паузы для выбора способности
     *
     * @return true если в режиме паузы для выбора способности
     */
    public boolean isAbilitySelectionPaused() {
        return isAbilitySelectionPaused;
    }

    /**
     * Устанавливает паузу при выборе способности
     *
     * @param paused true для паузы, false для возобновления
     */
    public void setAbilitySelectionPaused(boolean paused) {
        isAbilitySelectionPaused = paused;
        LogHelper.log("GameScreen", "Game " + (paused ? "paused" : "resumed") + " for ability selection");
    }

    /**
     * Возвращает игрока для доступа к его методам
     *
     * @return объект игрока
     */
    public PlayerActor getPlayer() {
        return player;
    }

    /**
     * Обновляет режим управления игрока и связанные настройки
     */
    public void updateControlMode() {
        if (player != null) {
            player.updateControlMode();

            boolean touchControlMode = PlayerData.getControlMode() == 1;

            for (Actor actor : uiStage.getActors()) {
                if (actor instanceof Touchpad) {
                    actor.setVisible(!touchControlMode);
                }
            }

            // Обновляем обработчик ввода
            InputMultiplexer inputMultiplexer = new InputMultiplexer();
            inputMultiplexer.addProcessor(uiStage);

            if (touchControlMode) {
                // Создаем специальный InputProcessor для gameStage
                InputProcessor gameInputProcessor = new InputProcessor() {
                    @Override
                    public boolean keyDown(int keycode) {
                        return false;
                    }

                    @Override
                    public boolean keyUp(int keycode) {
                        return false;
                    }

                    @Override
                    public boolean keyTyped(char character) {
                        return false;
                    }

                    @Override
                    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                        if (isAbilitySelectionPaused) {
                            return false;
                        }

                        // Проверяем, попало ли касание на элемент UI
                        Actor hitActor = uiStage.hit(screenX, screenY, true);
                        if (hitActor == null) {
                            handleTouchForPlayerMovement(screenX, screenY);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                        return false;
                    }

                    @Override
                    public boolean touchDragged(int screenX, int screenY, int pointer) {
                        if (isAbilitySelectionPaused) {
                            return false;
                        }

                        // Проверяем, попало ли касание на элемент UI
                        Actor hitActor = uiStage.hit(screenX, screenY, true);
                        if (hitActor == null) {
                            handleTouchForPlayerMovement(screenX, screenY);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean mouseMoved(int screenX, int screenY) {
                        return false;
                    }

                    @Override
                    public boolean scrolled(float amountX, float amountY) {
                        return false;
                    }

                    @Override
                    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
                        return false;
                    }
                };

                inputMultiplexer.addProcessor(gameInputProcessor);
            }

            Gdx.input.setInputProcessor(inputMultiplexer);

            LogHelper.log("GameScreen", "Control mode updated: " + (touchControlMode ? "touch" : "joystick"));
        }
    }

    /**
     * Обрабатывает касание экрана для движения игрока
     *
     * @param screenX координата X касания
     * @param screenY координата Y касания
     */
    private void handleTouchForPlayerMovement(int screenX, int screenY) {
        if (player != null && !isAbilitySelectionPaused) {
            // Получаем координаты касания в мировых координатах
            Vector3 touchPos = new Vector3(screenX, screenY, 0);
            Camera camera = gameStage.getCamera();
            camera.unproject(touchPos);

            // Передаем координаты касания в игрока и в UI
            player.setTouchTarget(touchPos.x, touchPos.y);
            if (gameUI != null) {
                gameUI.setTargetPosition(touchPos.x, touchPos.y);
            }
        }
    }

    /**
     * Переключает состояние паузы игры и отображает/скрывает меню паузы
     */
    public void togglePause() {
        isPaused = !isPaused;

        if (isPaused) {
            // Показываем меню паузы
            gameUI.showPauseMenu(new Runnable() {
                @Override
                public void run() {
                    // Продолжение игры
                    isPaused = false;
                }
            }, new Runnable() {
                @Override
                public void run() {
                    // Выход в главное меню
                    game.setScreen(new StartScreen(game));
                }
            });
        } else {
            gameUI.hidePauseMenu();
        }
    }
}
