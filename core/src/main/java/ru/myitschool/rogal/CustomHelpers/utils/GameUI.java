package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.function.Consumer;

import ru.myitschool.rogal.Abilities.Ability;
import ru.myitschool.rogal.Abilities.AbilityManager;
import ru.myitschool.rogal.Actors.PlayerActor;
import ru.myitschool.rogal.Enemies.EnemyManager.WaveListener;
import ru.myitschool.rogal.Main;
import ru.myitschool.rogal.Screens.GameScreen;

/**
 * Класс для создания и управления игровым интерфейсом с использованием UIHelper
 */
public class GameUI implements Disposable {
    private final Stage stage;
    private final UIHelper uiHelper;
    private PlayerActor player;
    private AbilityManager abilityManager;
    private final Skin skin;
    private float uiScale = 1.0f;
    private final Texture skillSlot;

    // UI элементы
    private Table mainTable;
    private Table leftSection;
    private Table rightSection;
    private ProgressBar healthBar;
    private ProgressBar energyBar;
    private Label healthLabel;
    private Label energyLabel;
    private Label healthRegenLabel;
    private Label energyRegenLabel;
    private Label levelLabel;
    private Label expLabel;
    private Label difficultyLabel;
    private TextButton autoUseButton;
    private TextButton pauseButton; // Кнопка паузы

    // Контейнер для масштабирования
    private Table uiContainer;

    // Элементы меню паузы
    private Window pauseWindow;

    // Добавляем новые поля для хранения UI элементов способностей
    private Array<Stack> abilitySlots; // Стеки для отображения способностей
    private Array<Image> abilityIcons; // Иконки способностей
    private Array<Label> cooldownLabels; // Метки для отображения кулдаунов
    private Array<Image> cooldownOverlays; // Затемнения для отображения кулдаунов
    private Array<Label> levelLabels; // Метки для отображения уровней способностей
    private Window abilityInfoWindow; // Окно с информацией о способности

    // Элементы для отображения таймера волны
    private Label waveTimerLabel;
    private ProgressBar waveTimerBar;
    private Label waveBreakLabel;

    // Элементы интерфейса для режима управления касанием
    private final TextureRegion targetMarker;
    private boolean showTargetMarker = false;
    private final Vector2 targetPosition = new Vector2();

    // Приватное поле для фона кнопки паузы
    private Table pauseButtonBackground;

    public GameUI(Stage stage, PlayerActor player) {
        this.stage = stage;
        this.player = player;
        this.abilityManager = player.getAbilityManager();
        this.uiHelper = new UIHelper(stage);
        this.skin = UISkinHelper.createSkin();

        // Создаем текстуру для слотов способностей
        Pixmap slotPixmap = new Pixmap(60, 60, Pixmap.Format.RGBA8888);
        slotPixmap.setColor(Color.DARK_GRAY);
        slotPixmap.fillRectangle(0, 0, 60, 60);
        slotPixmap.setColor(Color.LIGHT_GRAY);
        slotPixmap.drawRectangle(0, 0, 60, 60);
        skillSlot = new Texture(slotPixmap);
        slotPixmap.dispose();

        // Создаем окно для информации о способностях
        createAbilityInfoWindow();

        // Добавляем интерфейс таймера волны
        createWaveTimerUI();

        // Добавляем кнопку паузы
        createPauseButton();

        // Загружаем текстуру для маркера целевой точки
        targetMarker = new TextureRegion(new Texture("target-marker.png"));

        // Проверяем режим управления
        showTargetMarker = PlayerData.getControlMode() == 1;

        createUI();
    }

    private void createUI() {
        // Создаем основной контейнер
        mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.bottom().left();
        mainTable.pad(10);

        // Создаем контейнер для масштабирования
        uiContainer = new Table();
        uiContainer.setTransform(true);
        uiContainer.setOrigin(Align.bottomLeft);

        // Создаем фон
        Table backgroundPanel = uiHelper.createPanel(800, 200, new Color(0.1f, 0.1f, 0.2f, 0.8f));
        backgroundPanel.pad(10);

        // Создаем горизонтальный layout
        Table horizontalLayout = new Table();

        // Левая секция
        leftSection = new Table();
        createLeftSection();

        // Правая секция
        rightSection = new Table();
        createRightSection();

        // Добавляем секции в горизонтальный layout
        horizontalLayout.add(leftSection).padRight(20);
        horizontalLayout.add(rightSection).padLeft(10);

        // Собираем интерфейс
        backgroundPanel.add(horizontalLayout);
        uiContainer.add(backgroundPanel);
        mainTable.add(uiContainer);
        stage.addActor(mainTable);
    }

    private void createLeftSection() {
        // Портрет игрока
        Image portrait = new Image(new Texture("Ship_LVL_1.png"));
        leftSection.add(portrait).size(100).padBottom(15).row();

        // Процент опыта
        expLabel = uiHelper.createLabel("0%", new Color(1f, 1f, 0.3f, 1f));
        expLabel.setAlignment(Align.center);
        expLabel.setFontScale(1.5f);
        leftSection.add(expLabel).padBottom(10).row();

        // Уровень
        levelLabel = uiHelper.createLabel("1", Color.WHITE);
        levelLabel.setAlignment(Align.center);
        leftSection.add(levelLabel).padBottom(10).row();

        // Сложность
        difficultyLabel = uiHelper.createLabel("Уровень 1", Color.WHITE);
        leftSection.add(difficultyLabel).padBottom(10).row();

        // Кнопка АВТО
        autoUseButton = uiHelper.createButton("Auto: OFF", new Color(0.3f, 0.3f, 0.5f, 0.8f));
        autoUseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleAutoUse();
            }
        });
        leftSection.add(autoUseButton).size(70, 35);
    }

    private void createRightSection() {
        // Полоса здоровья
        healthBar = uiHelper.createProgressBar(450, 20, 0, 100, new Color(0.2f, 0.9f, 0.2f, 1f));
        healthLabel = uiHelper.createLabel("100 / 100", Color.WHITE);
        healthRegenLabel = uiHelper.createLabel("+0.5", new Color(0.3f, 1f, 0.3f, 1f));

        Table healthTable = new Table();
        healthTable.add(healthBar).growX().height(28).padRight(10);
        healthTable.add(healthRegenLabel).padLeft(5).width(40);
        rightSection.add(healthTable).growX().padBottom(10).row();
        rightSection.add(healthLabel).left().padBottom(15).row();

        // Полоса энергии
        energyBar = uiHelper.createProgressBar(450, 20, 0, 100, new Color(0.2f, 0.5f, 1f, 1f));
        energyLabel = uiHelper.createLabel("100 / 100", Color.WHITE);
        energyRegenLabel = uiHelper.createLabel("+0.5", new Color(0.3f, 0.6f, 1f, 1f));

        Table energyTable = new Table();
        energyTable.add(energyBar).growX().height(28).padRight(10);
        energyTable.add(energyRegenLabel).padLeft(5).width(40);
        rightSection.add(energyTable).growX().padBottom(10).row();
        rightSection.add(energyLabel).left().padBottom(15).row();

        // Создаем слоты способностей
        createAbilitySlots();
    }

    private void createAbilitySlots() {
        abilitySlots = new Array<>(AbilityManager.MAX_ABILITIES);
        abilityIcons = new Array<>(AbilityManager.MAX_ABILITIES);
        cooldownLabels = new Array<>(AbilityManager.MAX_ABILITIES);
        cooldownOverlays = new Array<>(AbilityManager.MAX_ABILITIES);
        levelLabels = new Array<>(AbilityManager.MAX_ABILITIES);

        Table abilitiesTable = new Table();
        abilitiesTable.pad(5);

        // Заголовок
        Label abilitiesTitle = uiHelper.createLabel("СПОСОБНОСТИ", new Color(1f, 0.8f, 0.2f, 1f));
        abilitiesTable.add(abilitiesTitle).colspan(5).padBottom(10).row();

        // Создаем слоты для способностей
        for (int i = 0; i < AbilityManager.MAX_ABILITIES; i++) {
            // Создаем основу слота
            Stack slotStack = new Stack();

            // Фон слота (нижний слой)
            Image slotBg = new Image(skillSlot);
            slotStack.add(slotBg);

            // Создаем иконку способности (изначально пустую)
            Image abilityIcon = new Image();
            abilityIcon.setFillParent(true);
            abilityIcons.add(abilityIcon);
            slotStack.add(abilityIcon);

            // Создаем оверлей для отображения кулдауна
            Image cooldownOverlay = new Image(skillSlot);
            cooldownOverlay.setColor(0, 0, 0, 0.7f);
            cooldownOverlay.setVisible(false);
            cooldownOverlay.setFillParent(true);
            cooldownOverlays.add(cooldownOverlay);
            slotStack.add(cooldownOverlay);

            // Создаем метку для отображения оставшегося времени кулдауна
            Label cooldownLabel = uiHelper.createLabel("", Color.WHITE);
            cooldownLabel.setAlignment(Align.center);
            cooldownLabel.setFontScale(1.2f);
            cooldownLabel.setVisible(false);
            cooldownLabels.add(cooldownLabel);
            slotStack.add(cooldownLabel);

            // Создаем метку для уровня способности
            Label levelLabel = uiHelper.createLabel("", new Color(1f, 0.8f, 0.2f, 1f));
            levelLabel.setAlignment(Align.bottomRight);
            levelLabel.setFontScale(0.9f);
            levelLabel.setVisible(false);
            levelLabels.add(levelLabel);
            slotStack.add(levelLabel);

            // Добавляем индикатор авто-использования
            Label autoLabel = uiHelper.createLabel("A", new Color(0.2f, 1f, 0.2f, 1f));
            autoLabel.setAlignment(Align.topRight);
            autoLabel.setFontScale(0.8f);
            autoLabel.setVisible(false);
            autoLabel.setPosition(autoLabel.getX() + 5, autoLabel.getY() + 5);
            slotStack.add(autoLabel);

            // Горячая клавиша
            Label hotkeyLabel = uiHelper.createLabel(String.valueOf(i + 1), Color.WHITE);
            hotkeyLabel.setAlignment(Align.topLeft);
            hotkeyLabel.setFontScale(0.9f);
            hotkeyLabel.setPosition(hotkeyLabel.getX() + 5, hotkeyLabel.getY() + 5);
            slotStack.add(hotkeyLabel);

            // Добавляем слот в список и на таблицу
            abilitySlots.add(slotStack);
            abilitiesTable.add(slotStack).size(65).pad(5);

            // После 5 слотов переходим на новую строку
            if ((i + 1) % 5 == 0 && i < AbilityManager.MAX_ABILITIES - 1) {
                abilitiesTable.row();
            }

            // Добавляем обработчик нажатия для слота
            final int slotIndex = i;
            slotStack.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (player != null) {
                        // Если нажата правая кнопка мыши, переключаем режим автоиспользования
                        if (event.getButton() == Input.Buttons.RIGHT) {
                            toggleAutoUseForSlot(slotIndex);
                        } else {
                            // Получаем позицию мыши в координатах мира для цели способности
                            Vector2 targetPos = new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
                            targetPos = stage.screenToStageCoordinates(targetPos);

                            // Активируем способность
                            player.useAbility(slotIndex, targetPos);
                        }
                    }
                }

                // Показываем информацию о способности при наведении
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (player != null && player.getAbilityManager() != null) {
                        Ability ability = player.getAbilityManager().getAbility(slotIndex);
                        if (ability != null) {
                            Vector2 pos = new Vector2(event.getStageX() + 10, event.getStageY() + 10);
                            showAbilityInfo(ability, pos.x, pos.y);
                        }
                    }
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    hideAbilityInfo();
                }
            });
        }

        rightSection.add(abilitiesTable);
    }

    private void createAbilityInfoWindow() {
        abilityInfoWindow = new Window("", skin);
        abilityInfoWindow.setMovable(false);
        abilityInfoWindow.setResizable(false);
        abilityInfoWindow.setVisible(false);
        abilityInfoWindow.setModal(false);
        abilityInfoWindow.setKeepWithinStage(true);
        abilityInfoWindow.pad(10);

        // Добавляем окно на сцену
        stage.addActor(abilityInfoWindow);
    }

    private void showAbilityInfo(Ability ability, float x, float y) {
        if (ability == null) {
            abilityInfoWindow.setVisible(false);
            return;
        }

        // Обновляем содержимое окна
        abilityInfoWindow.clear();
        abilityInfoWindow.getTitleLabel().setText(ability.getName() + " (Lvl. " + ability.getLevel() + ")");

        // Таблица для содержимого окна
        Table contentTable = new Table();
        contentTable.pad(5);

        // Добавляем иконку способности
        if (ability.getIcon() != null) {
            Image abilityIcon = new Image(new TextureRegionDrawable(new TextureRegion(ability.getIcon())));
            contentTable.add(abilityIcon).size(50).padRight(10);
        }

        // Добавляем таблицу с информацией
        Table infoTable = new Table();

        // Добавляем описание
        Label descriptionLabel = new Label(ability.getDescription(), skin);
        descriptionLabel.setWrap(true);
        infoTable.add(descriptionLabel).width(250).padBottom(5).row();

        // Добавляем стоимость энергии
        if (ability.getEnergyCost() > 0) {
            Label energyLabel = new Label("Energy: " + ability.getEnergyCost(), skin);
            energyLabel.setColor(0.2f, 0.6f, 1f, 1);
            infoTable.add(energyLabel).left().padBottom(3).row();
        }

        // Добавляем кулдаун
        Label cooldownLabel = new Label("Cooldown: " + ability.getCurrentCooldown() + " sec", skin);
        cooldownLabel.setColor(0.8f, 0.8f, 0.8f, 1);
        infoTable.add(cooldownLabel).left().row();

        contentTable.add(infoTable);
        abilityInfoWindow.add(contentTable);

        // Устанавливаем позицию окна
        float windowX = Math.min(x, stage.getWidth() - abilityInfoWindow.getWidth() - 10);
        float windowY = Math.min(y, stage.getHeight() - abilityInfoWindow.getHeight() - 10);
        abilityInfoWindow.setPosition(windowX, windowY);

        // Показываем окно
        abilityInfoWindow.setVisible(true);
    }

    private void hideAbilityInfo() {
        if (abilityInfoWindow != null) {
            abilityInfoWindow.setVisible(false);
        }
    }

    private void toggleAutoUse() {
        if (abilityManager == null) return;

        boolean isAutoUseEnabled = abilityManager.isAutoUseEnabledForAll();
        if (isAutoUseEnabled) {
            abilityManager.disableAutoUseForAll();
            autoUseButton.setText("Auto: OFF");
            showFloatingMessage("Auto-use disabled");
        } else {
            abilityManager.enableAutoUseForAll();
            autoUseButton.setText("Auto: ON");
            showFloatingMessage("Auto-use enabled");
        }
    }

    private void toggleAutoUseForSlot(int slotIndex) {
        if (player == null || player.getAbilityManager() == null) return;

        AbilityManager abilityManager = player.getAbilityManager();
        Ability ability = abilityManager.getAbility(slotIndex);

        if (ability != null) {
            boolean newAutoStatus = !ability.isAutoUse();
            ability.setAutoUse(newAutoStatus);

            Stack slot = abilitySlots.get(slotIndex);
            for (Actor actor : slot.getChildren()) {
                if (actor instanceof Label) {
                    if (((Label) actor).getText().toString().equals("A")) {
                        actor.setVisible(newAutoStatus);
                    }
                }
            }

            String message = newAutoStatus ?
                            "Ability \"" + ability.getName() + "\" will be used automatically" :
                            "Auto use disabled for ability \"" + ability.getName() + "\"";
            showFloatingMessage(message);
        }
    }

    public boolean keyDown(int keycode) {
        // Обработка горячих клавиш для способностей
        if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
            int index = keycode - Input.Keys.NUM_1;
            if (index < AbilityManager.MAX_ABILITIES && player != null && player.getAbilityManager() != null) {
                Ability ability = player.getAbilityManager().getAbility(index);
                if (ability != null) {
                    // Используем способность на позиции курсора мыши
                    Vector2 targetPos = new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
                    targetPos = stage.screenToStageCoordinates(targetPos);
                    player.useAbility(index, targetPos);
                    return true;
                }
            }
        }

        return false;
    }

    public void updateAbilities() {
        if (player == null || player.getAbilityManager() == null || abilitySlots == null) return;

        AbilityManager abilityManager = player.getAbilityManager();
        Array<Ability> abilities = abilityManager.getAbilities();

        // Обновляем каждый слот способности
        for (int i = 0; i < AbilityManager.MAX_ABILITIES; i++) {
            if (i >= abilitySlots.size) break;

            // Получаем компоненты слота
            Stack slot = abilitySlots.get(i);
            Image icon = abilityIcons.get(i);
            Label cooldownLabel = cooldownLabels.get(i);
            Image cooldownOverlay = cooldownOverlays.get(i);
            Label levelLabel = levelLabels.get(i);

            // Проверяем, есть ли способность для этого слота
            if (i < abilities.size && abilities.get(i) != null) {
                Ability ability = abilities.get(i);

                // Устанавливаем иконку способности
                if (ability.getIcon() != null) {
                    icon.setDrawable(new TextureRegionDrawable(new TextureRegion(ability.getIcon())));
                    icon.setVisible(true);
                } else {
                    icon.setVisible(false);
                }

                // Обновляем кулдаун
                float cooldown = ability.getCurrentCooldown();
                boolean onCooldown = cooldown > 0f;
                cooldownLabel.setText(String.format("%.1f", cooldown));
                cooldownLabel.setVisible(onCooldown);
                cooldownOverlay.setVisible(onCooldown);

                // Обновляем метку уровня способности
                int abilityLevel = ability.getLevel();
                levelLabel.setText(String.valueOf(abilityLevel));
                levelLabel.setVisible(abilityLevel > 1);

                // Обновляем индикатор автоиспользования
                for (Actor actor : slot.getChildren()) {
                    if (actor instanceof Label) {
                        if (((Label) actor).getText().toString().equals("A")) {
                            actor.setVisible(ability.isAutoUse());
                        }
                    }
                }
            } else {
                // Слот пуст, скрываем все элементы
                icon.setVisible(false);
                cooldownLabel.setVisible(false);
                cooldownOverlay.setVisible(false);
                levelLabel.setVisible(false);

                // Скрываем индикатор автоиспользования
                for (Actor actor : slot.getChildren()) {
                    if (actor instanceof Label) {
                        if (((Label) actor).getText().toString().equals("A")) {
                            actor.setVisible(false);
                        }
                    }
                }
            }
        }

        // Заставляем UI обновиться сразу
        stage.getRoot().getColor().a = 1;
    }

    public void update() {
        if (player != null) {
            // Обновляем здоровье
            healthBar.setRange(0, player.getMaxHealth());
            healthBar.setValue(player.getCurrentHealth());
            healthLabel.setText(Math.round(player.getCurrentHealth()) + " / " + Math.round(player.getMaxHealth()));
            healthRegenLabel.setText("+" + String.format("%.1f", player.getHealthRegeneration()));

            // Обновляем энергию
            energyBar.setRange(0, player.getMaxEnergy());
            energyBar.setValue(player.getCurrentEnergy());
            energyLabel.setText(player.getCurrentEnergy() + " / " + player.getMaxEnergy());
            energyRegenLabel.setText("+" + String.format("%.1f", player.getEnergyRegeneration()));

            // Обновляем уровень и опыт
            levelLabel.setText(String.valueOf(player.getLevel()));
            int expPercentage = (int)((float)player.getExperience() / player.getExperienceToNextLevel() * 100);
            expLabel.setText(expPercentage + "%");

            // Обновляем слоты способностей
            updateAbilitySlots();
        }
    }

    /**
     * Обновляет информацию о текущем уровне сложности (волна)
     *
     * @param level номер текущей волны
     */
    public void updateDifficultyLevel(int level) {
        if (difficultyLabel != null) {
            difficultyLabel.setText("Волна " + level);
        }
    }

    public void setUIScaleFromSettings(int scalePercent) {
        float newScale = Math.max(0.5f, Math.min(1.5f, scalePercent / 100f));
        if (newScale != uiScale) {
            uiScale = newScale;
            applyUIScale();
            // Пересоздаем кнопку паузы при изменении масштаба
            createPauseButton();
            showFloatingMessage("Размер интерфейса: " + Math.round(uiScale * 100) + "%");
        }
    }

    public void showFloatingMessage(String message) {
        Label messageLabel = uiHelper.createLabel(message, Color.WHITE);
        messageLabel.setPosition(stage.getWidth() / 2 - messageLabel.getWidth() / 2, stage.getHeight() * 0.7f);
        stage.addActor(messageLabel);

        messageLabel.addAction(Actions.sequence(
            Actions.fadeIn(0.2f),
            Actions.delay(2f),
            Actions.fadeOut(0.5f),
            Actions.removeActor()
        ));
    }

    public void showAbilityChoiceDialog(ArrayList<Ability> abilities, Consumer<Ability> abilitySelectedCallback) {
        if (abilities == null || abilities.isEmpty()) return;

        // Приостанавливаем игру
        pauseGameForAbilitySelection(true);

        // Создаем окно для выбора способности
        final Window window = new Window("Выберите способность", skin);
        window.setModal(true);
        window.setMovable(false);
        window.setResizable(false);
        window.pad(20);

        // Заголовок
        Label titleLabel = new Label("Выберите одну из трех способностей:", skin, "title");
        window.add(titleLabel).colspan(3).center().pad(10).row();

        // Создаем слоты для выбора способностей
        final Table choicesTable = new Table();
        choicesTable.pad(10);

        // Добавляем способности на выбор
        for (final Ability ability : abilities) {
            Table abilityTable = createAbilityChoiceSlot(ability);

            // Добавляем обработчик нажатия
            abilityTable.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // Вызываем callback с выбранной способностью
                    if (abilitySelectedCallback != null) {
                        abilitySelectedCallback.accept(ability);
                    }

                    // Возобновляем игру
                    pauseGameForAbilitySelection(false);

                    // Закрываем окно
                    window.remove();
                }
            });

            choicesTable.add(abilityTable).pad(10).top();
        }

        // Добавляем таблицу с выбором в окно
        ScrollPane scrollPane = new ScrollPane(choicesTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setFlickScroll(false);
        window.add(scrollPane).colspan(3).fillX().expandX().minHeight(300).row();

        // Добавляем подсказку
        Label hintLabel = new Label("Нажмите на способность, чтобы выбрать её", skin);
        window.add(hintLabel).colspan(3).center().pad(10);

        // Настраиваем размеры и позицию окна
        window.setSize(stage.getWidth() * 0.8f, stage.getHeight() * 0.7f);
        window.setPosition(
            (stage.getWidth() - window.getWidth()) / 2,
            (stage.getHeight() - window.getHeight()) / 2
        );

        // Обработчик удаления окна, чтобы восстановить игру, если окно будет закрыто не через выбор
        window.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    pauseGameForAbilitySelection(false);
                    window.remove();
                    return true;
                }
                return false;
            }
        });

        // Добавляем окно на сцену
        stage.addActor(window);
    }

    public void showAbilityUnlockMessage(String abilityName) {
        showFloatingMessage("Получена новая способность: " + abilityName);
    }

    public void showLevelUpMessage(int level) {
        showFloatingMessage("Повышение уровня! Теперь вы " + level + " уровня");
    }

    private void applyUIScale() {
        if (uiContainer != null) {
            uiContainer.setScale(uiScale);
        }
    }

    private Table createAbilityChoiceSlot(Ability ability) {
        Table abilityTable = new Table();
        abilityTable.setBackground(skin.getDrawable("button-up"));
        abilityTable.pad(10);

        // Иконка способности
        Stack iconStack = new Stack();
        Image slotBg = new Image(skillSlot);
        iconStack.add(slotBg);

        // Отладочная информация
        LogHelper.log(this.getClass().toString(), "Создание слота для способности: " + ability.getName());
        LogHelper.log(this.getClass().toString(), "Иконка способности: " + (ability.getIcon() != null ? "есть" : "отсутствует"));

        if (ability.getIcon() != null) {
            try {
                TextureRegionDrawable iconDrawable = new TextureRegionDrawable(new TextureRegion(ability.getIcon()));
                Image abilityIcon = new Image(iconDrawable);
                abilityIcon.setFillParent(true);
                iconStack.add(abilityIcon);
                LogHelper.log(this.getClass().toString(), "Иконка успешно добавлена");
            } catch (Exception e) {
                LogHelper.log(this.getClass().toString(), "Ошибка при загрузке иконки: " + e.getMessage());
                // Добавляем заглушку для иконки
                Image placeholderIcon = new Image(skillSlot);
                placeholderIcon.setColor(0.5f, 0.5f, 0.5f, 1f);
                placeholderIcon.setFillParent(true);
                iconStack.add(placeholderIcon);
            }
        } else {
            // Добавляем заглушку для иконки
            Image placeholderIcon = new Image(skillSlot);
            placeholderIcon.setColor(0.5f, 0.5f, 0.5f, 1f);
            placeholderIcon.setFillParent(true);
            iconStack.add(placeholderIcon);
        }

        // Добавляем оверлей для отображения кулдауна
        Image cooldownOverlay = new Image(skillSlot);
        cooldownOverlay.setColor(0, 0, 0, 0.7f);
        cooldownOverlay.setVisible(ability.getCurrentCooldown() > 0);
        cooldownOverlay.setFillParent(true);
        iconStack.add(cooldownOverlay);

        // Добавляем метку для отображения оставшегося времени кулдауна
        Label cooldownLabel = new Label(String.format("%.1f", ability.getCurrentCooldown()), skin);
        cooldownLabel.setAlignment(Align.center);
        cooldownLabel.setVisible(ability.getCurrentCooldown() > 0);
        iconStack.add(cooldownLabel);

        // Название способности
        Label nameLabel = new Label(ability.getName(), skin, "title");

        // Уровень способности
        String levelText = "Уровень " + ability.getLevel();

        // Проверяем, есть ли уже такая способность у игрока и может ли она быть улучшена
        boolean hasAbility = false;
        boolean isUpgradable = false;
        int currentLevel = 0;

        if (player != null && player.getAbilityManager() != null) {
            for (Ability playerAbility : player.getAbilityManager().getAbilities()) {
                if (playerAbility.getClass() == ability.getClass()) {
                    hasAbility = true;
                    currentLevel = playerAbility.getLevel();
                    isUpgradable = currentLevel < player.getMaxAbilityLevel();

                    // Если это улучшение существующей способности, показываем новый уровень
                    if (isUpgradable) {
                        levelText = "Уровень " + currentLevel + " -> " + (currentLevel + 1);
                    } else {
                        levelText = "Уровень " + currentLevel + " (макс.)";
                    }

                    break;
                }
            }
        }

        Label levelLabel = new Label(levelText, skin);
        levelLabel.setColor(1f, 0.8f, 0.2f, 1f);

        // Описание способности
        Label descriptionLabel = new Label(ability.getDescription(), skin);
        descriptionLabel.setWrap(true);

        // Если у игрока уже есть эта способность
        if (hasAbility) {
            // Выделяем имеющуюся способность
            abilityTable.setBackground(skin.getDrawable("button-down"));

            if (isUpgradable) {
                Label upgradeLabel = new Label("Улучшение", skin);
                upgradeLabel.setColor(0.2f, 1f, 0.2f, 1f);
                abilityTable.add(upgradeLabel).colspan(2).padBottom(5).row();
            } else {
                Label maxLevelLabel = new Label("Максимальный уровень", skin);
                maxLevelLabel.setColor(1f, 0.5f, 0.2f, 1f);
                abilityTable.add(maxLevelLabel).colspan(2).padBottom(5).row();
            }
        }

        // Добавляем элементы в таблицу
        abilityTable.add(iconStack).size(80).padBottom(10).row();
        abilityTable.add(nameLabel).padBottom(5).row();
        abilityTable.add(levelLabel).padBottom(5).row();
        abilityTable.add(descriptionLabel).width(200).row();

        return abilityTable;
    }

    public void setPlayer(PlayerActor player) {
        this.player = player;
        if (player != null) {
            this.abilityManager = player.getAbilityManager();
        }
    }

    public void act(float delta) {
        update();
    }

    public void updateHealth(int currentHealth, int maxHealth, float healthRegeneration) {
        if (healthBar != null) {
            healthBar.setRange(0, maxHealth);
            healthBar.setValue(currentHealth);
            healthLabel.setText(currentHealth + " / " + maxHealth);
            healthRegenLabel.setText("+" + String.format("%.1f", healthRegeneration));
        }
    }

    public void updateLevel(int level) {
        if (levelLabel != null) {
            levelLabel.setText(String.valueOf(level));
        }
    }

    public void updateExperience(int experience, int experienceToNextLevel) {
        if (expLabel != null) {
            int expPercentage = (int)((float)experience / experienceToNextLevel * 100);
            expLabel.setText(expPercentage + "%");
        }
    }

    public void updateEnergy(int currentEnergy, int maxEnergy, float energyRegeneration) {
        if (energyBar != null) {
            energyBar.setRange(0, maxEnergy);
            energyBar.setValue(currentEnergy);
            energyLabel.setText(currentEnergy + " / " + maxEnergy);
            energyRegenLabel.setText("+" + String.format("%.1f", energyRegeneration));
        }
    }

    private void updateAbilitySlots() {
        if (player == null || player.getAbilityManager() == null) return;

        Array<Ability> abilities = player.getAbilityManager().getAbilities();

        // Обновляем каждый слот
        for (int i = 0; i < abilitySlots.size; i++) {
            Image icon = abilityIcons.get(i);
            Label cooldownLabel = cooldownLabels.get(i);
            Image cooldownOverlay = cooldownOverlays.get(i);
            Label levelLabel = levelLabels.get(i);

            // Проверяем, есть ли способность для этого слота
            if (i < abilities.size) {
                Ability ability = abilities.get(i);

                // Обновляем иконку
                if (ability.getIcon() != null) {
                    icon.setDrawable(new TextureRegionDrawable(new TextureRegion(ability.getIcon())));
                    icon.setVisible(true);
                } else {
                    icon.setVisible(false);
                }

                // Обновляем кулдаун
                float cooldown = ability.getCurrentCooldown();
                if (cooldown > 0) {
                    cooldownLabel.setText(String.format("%.1f", cooldown));
                    cooldownLabel.setVisible(true);
                    cooldownOverlay.setVisible(true);
                } else {
                    cooldownLabel.setVisible(false);
                    cooldownOverlay.setVisible(false);
                }

                // Обновляем уровень
                int level = ability.getLevel();
                if (level > 1) {
                    levelLabel.setText(String.valueOf(level));
                    levelLabel.setVisible(true);
                } else {
                    levelLabel.setVisible(false);
                }
            } else {
                // Слот пуст
                icon.setVisible(false);
                cooldownLabel.setVisible(false);
                cooldownOverlay.setVisible(false);
                levelLabel.setVisible(false);
            }
        }
    }

    /**
     * Создает интерфейс для отображения таймера волны
     */
    private void createWaveTimerUI() {
        // Создаем контейнер для таймера волны
        Table waveTimerTable = new Table();
        waveTimerTable.top().right();
        waveTimerTable.setPosition(Main.SCREEN_WIDTH - 210, Main.SCREEN_HEIGHT - 30);

        // Стиль метки для таймера
        Label.LabelStyle timerStyle = new Label.LabelStyle(FontManager.getRegularFont(), Color.WHITE);

        // Метка для отображения оставшегося времени
        waveTimerLabel = new Label("Волна 1: 00:00", timerStyle);

        // Стиль для прогресс-бара
        ProgressBar.ProgressBarStyle progressBarStyle = new ProgressBar.ProgressBarStyle();
        Pixmap pixmap = new Pixmap(1, 10, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.3f, 0.3f, 0.7f, 1f));
        pixmap.fill();
        progressBarStyle.background = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));

        pixmap.setColor(new Color(0.2f, 0.6f, 1f, 1f));
        pixmap.fill();
        progressBarStyle.knob = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));

        pixmap.dispose();

        // Создаем прогресс-бар для визуального отображения таймера
        waveTimerBar = new ProgressBar(0, 1, 0.01f, false, progressBarStyle);
        waveTimerBar.setValue(1);
        waveTimerBar.setWidth(200);

        // Метка для отображения перерыва между волнами
        waveBreakLabel = new Label("Следующая волна через: 5", timerStyle);
        waveBreakLabel.setVisible(false);

        // Добавляем элементы в таблицу
        waveTimerTable.add(waveTimerLabel).padBottom(5).right().row();
        waveTimerTable.add(waveTimerBar).width(200).height(10).padBottom(5).right().row();
        waveTimerTable.add(waveBreakLabel).right();

        // Добавляем таблицу в стейдж
        this.stage.addActor(waveTimerTable);
    }

    /**
     * Создает слушателя событий волны для EnemyManager
     *
     * @return слушатель событий волны
     */
    public WaveListener createWaveListener() {
        return new WaveListener() {
            @Override
            public void onWaveStart(int waveNumber) {
                // Обновляем отображение при начале волны
                waveTimerLabel.setVisible(true);
                waveTimerBar.setVisible(true);
                waveBreakLabel.setVisible(false);

                // Обновляем номер волны в UI
                updateDifficultyLevel(waveNumber);

                // Показываем сообщение о начале волны
                showFloatingMessage("Волна " + waveNumber + " началась!");
            }

            @Override
            public void onWaveEnd(int waveNumber) {
                // Показываем сообщение об окончании волны
                showFloatingMessage("Волна " + waveNumber + " завершена!");
            }

            @Override
            public void onWaveTimerUpdate(float remainingTime, float totalTime) {
                // Обновляем таймер волны
                updateWaveTimer(remainingTime, totalTime);
            }

            @Override
            public void onWaveBreakUpdate(float remainingTime, float totalTime) {
                // Обновляем таймер перерыва между волнами
                updateWaveBreakTimer(remainingTime, totalTime);
            }
        };
    }

    /**
     * Обновляет отображение таймера волны
     *
     * @param remainingTime оставшееся время в секундах
     * @param totalTime     общее время волны в секундах
     */
    private void updateWaveTimer(float remainingTime, float totalTime) {
        // Форматируем время в минуты:секунды
        int minutes = (int) (remainingTime / 60);
        int seconds = (int) (remainingTime % 60);
        String timeString = String.format("%02d:%02d", minutes, seconds);

        // Обновляем текст
        String waveText = difficultyLabel.getText().toString();
        String waveNumber = "1";
        if (waveText != null && waveText.contains(" ")) {
            String[] parts = waveText.split(" ");
            if (parts.length > 1) {
                waveNumber = parts[1];
            }
        }
        waveTimerLabel.setText("Волна " + waveNumber + ": " + timeString);

        // Обновляем прогресс-бар
        waveTimerBar.setValue(remainingTime / totalTime);

        // Отображаем соответствующие элементы
        waveTimerLabel.setVisible(true);
        waveTimerBar.setVisible(true);
        waveBreakLabel.setVisible(false);
    }

    /**
     * Обновляет отображение таймера перерыва между волнами
     *
     * @param remainingTime оставшееся время перерыва в секундах
     * @param totalTime     общее время перерыва в секундах
     */
    private void updateWaveBreakTimer(float remainingTime, float totalTime) {
        // Форматируем оставшееся время перерыва
        int seconds = (int) Math.ceil(remainingTime);

        // Обновляем текст
        waveBreakLabel.setText("Следующая волна через: " + seconds);

        // Отображаем соответствующие элементы
        waveTimerLabel.setVisible(false);
        waveTimerBar.setVisible(false);
        waveBreakLabel.setVisible(true);
    }

    /**
     * Приостанавливает или возобновляет игру для выбора способности
     *
     * @param pause true для приостановки, false для возобновления
     */
    private void pauseGameForAbilitySelection(boolean pause) {
        if (player == null || player.getStage() == null) {
            return;
        }

        // Получаем ссылку на Game
        Main game = (Main) Gdx.app.getApplicationListener();
        if (game != null && game.getScreen() instanceof GameScreen) {
            ((GameScreen) game.getScreen()).setAbilitySelectionPaused(pause);
            LogHelper.log("GameUI", "Game " + (pause ? "paused" : "resumed") + " for ability selection");
        } else {
            LogHelper.error("GameUI", "Could not find GameScreen to pause/resume");
        }
    }

    /**
     * Устанавливает целевую точку для режима управления касанием
     *
     * @param x координата X
     * @param y координата Y
     */
    public void setTargetPosition(float x, float y) {
        targetPosition.set(x, y);
    }

    /**
     * Показывает или скрывает маркер целевой точки
     *
     * @param show true - показать, false - скрыть
     */
    public void showTargetMarker(boolean show) {
        showTargetMarker = show;
    }

    /**
     * Обновляет настройки UI при смене режима управления
     */
    public void updateControlMode() {
        showTargetMarker = PlayerData.getControlMode() == 1;
    }

    /**
     * Рисует маркер целевой точки в режиме управления касанием
     *
     * @param batch объект для отрисовки
     */
    public void drawTargetMarker(Batch batch) {
        // В режиме управления касанием рисуем маркер целевой точки
        if (showTargetMarker && player != null && player.isMovingToTarget()) {
            float width = targetMarker.getRegionWidth() * uiScale * 0.25f;
            float height = targetMarker.getRegionHeight() * uiScale * 0.25f;

            batch.draw(
                targetMarker,
                targetPosition.x - width / 2,  // Центрируем маркер с учетом масштаба
                targetPosition.y - height / 2, // Центрируем маркер с учетом масштаба
                width,                         // Масштабированная ширина
                height                         // Масштабированная высота
            );
        }
    }

    @Override
    public void dispose() {
        if (skillSlot != null) {
            skillSlot.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        if (abilityInfoWindow != null) {
            abilityInfoWindow.remove();
        }
    }

    /**
     * Показывает меню паузы с указанными действиями
     *
     * @param resumeAction     действие при нажатии "Продолжить"
     * @param exitToMenuAction действие при нажатии "Выход в меню"
     */
    public void showPauseMenu(final Runnable resumeAction, final Runnable exitToMenuAction) {
        // Если окно уже существует, удаляем его
        if (pauseWindow != null) {
            pauseWindow.remove();
        }

        // Создаем окно с заголовком "Пауза"
        pauseWindow = new Window("ПАУЗА", skin);
        pauseWindow.setMovable(false);
        pauseWindow.setModal(true);
        pauseWindow.setSize(300, 250);
        pauseWindow.setPosition((stage.getWidth() - pauseWindow.getWidth()) / 2,
            (stage.getHeight() - pauseWindow.getHeight()) / 2);

        // Создаем кнопки
        TextButton resumeButton = uiHelper.createButton("Продолжить", new Color(0.3f, 0.5f, 0.3f, 0.8f));
        TextButton exitToMenuButton = uiHelper.createButton("Выход в меню", new Color(0.5f, 0.3f, 0.3f, 0.8f));
        TextButton exitButton = uiHelper.createButton("Выход из игры", new Color(0.7f, 0.2f, 0.2f, 0.8f));

        // Добавляем обработчики нажатий
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hidePauseMenu();
                if (resumeAction != null) {
                    resumeAction.run();
                }
            }
        });

        exitToMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hidePauseMenu();
                if (exitToMenuAction != null) {
                    exitToMenuAction.run();
                }
            }
        });

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Создаем таблицу для размещения кнопок
        Table buttonTable = new Table();
        buttonTable.defaults().pad(10).size(200, 50).fill();
        buttonTable.add(resumeButton).row();
        buttonTable.add(exitToMenuButton).row();
        buttonTable.add(exitButton).row();

        // Добавляем таблицу в окно
        pauseWindow.add(buttonTable).expand().fill();

        // Добавляем окно на сцену
        this.stage.addActor(pauseWindow);

        // Добавляем обработчик ввода для клавиши Escape
        pauseWindow.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    hidePauseMenu();
                    if (resumeAction != null) {
                        resumeAction.run();
                    }
                    return true;
                }
                return super.keyDown(event, keycode);
            }
        });

        // Запрашиваем фокус ввода
        this.stage.setKeyboardFocus(pauseWindow);
    }

    /**
     * Скрывает меню паузы
     */
    public void hidePauseMenu() {
        if (pauseWindow != null) {
            pauseWindow.remove();
            pauseWindow = null;
        }
    }

    // Создает кнопку паузы
    private void createPauseButton() {
        // Определяем, является ли устройство мобильным
        boolean isMobile = Gdx.app.getType() == Application.ApplicationType.Android ||
            Gdx.app.getType() == Application.ApplicationType.iOS;

        // Выбираем базовый размер и отступ в зависимости от типа устройства
        // Уменьшаем базовый размер кнопки для десктопа
        float baseButtonSize = isMobile ? 120 : 50;
        float basePadding = isMobile ? 25 : 15;
        float baseFontScale = isMobile ? 2.5f : 1.2f;

        // Применяем масштабирование интерфейса
        float buttonSize = baseButtonSize * uiScale;
        float buttonPadding = basePadding * uiScale;
        float fontScale = baseFontScale * uiScale;

        // Удаляем существующие элементы, если они уже есть на сцене
        if (pauseButton != null && pauseButton.getStage() != null) pauseButton.remove();
        if (pauseButtonBackground != null && pauseButtonBackground.getStage() != null)
            pauseButtonBackground.remove();

        // Создаем кнопку паузы с текстовым символом паузы
        pauseButton = uiHelper.createButton("II", new Color(0.3f, 0.3f, 0.8f, 1.0f));
        pauseButton.setSize(buttonSize, buttonSize);
        pauseButton.getLabel().setFontScale(fontScale);

        // Размещаем кнопку в правом верхнем углу
        pauseButton.setPosition(
            stage.getWidth() - buttonSize - buttonPadding,
            stage.getHeight() - buttonSize - buttonPadding);

        // Добавляем фон для лучшей видимости - немного больше чем кнопка
        float bgPadding = 10 * uiScale;
        pauseButtonBackground = uiHelper.createPanel(buttonSize + bgPadding * 2, buttonSize + bgPadding * 2,
            new Color(0.1f, 0.1f, 0.3f, 0.95f));
        pauseButtonBackground.setPosition(pauseButton.getX() - bgPadding, pauseButton.getY() - bgPadding);

        // Добавляем обработчик нажатия
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Находим GameScreen и вызываем его метод для паузы
                if (player != null && player.getStage() != null) {
                    Main game = (Main) Gdx.app.getApplicationListener();
                    if (game != null && game.getScreen() instanceof GameScreen) {
                        GameScreen gameScreen = (GameScreen) game.getScreen();
                        gameScreen.togglePause();
                        LogHelper.log("GameUI", "Кнопка паузы нажата");
                    }
                }
            }
        });

        // Добавляем фон и кнопку на сцену
        stage.addActor(pauseButtonBackground);
        stage.addActor(pauseButton);

        LogHelper.log("GameUI", "Кнопка паузы создана. Размер: " + buttonSize + ", Позиция: " +
            pauseButton.getX() + "," + pauseButton.getY() + ", Мобильное: " + isMobile + ", Масштаб UI: " + uiScale);
    }

    /**
     * Обновляет позиции элементов UI при изменении размера экрана
     *
     * @param width  новая ширина экрана
     * @param height новая высота экрана
     */
    public void resize(int width, int height) {
        LogHelper.log("GameUI", "Resize вызван: ширина=" + width + ", высота=" + height);

        // Гарантированно перессоздаем кнопку паузы с правильными размерами
        createPauseButton();
        LogHelper.log("GameUI", "Кнопка паузы обновлена и размещена поверх других элементов");

        // Обновляем другие элементы UI если необходимо
        // ...
    }
}
