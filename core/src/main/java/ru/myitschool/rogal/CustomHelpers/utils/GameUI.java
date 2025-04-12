package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import ru.myitschool.rogal.Actors.PlayerActor;
import ru.myitschool.rogal.Abilities.Ability;
import ru.myitschool.rogal.Abilities.AbilityManager;
import java.util.ArrayList;
import java.util.function.Consumer;
import com.badlogic.gdx.utils.Array;

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
    private Texture skillSlot;

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

    // Контейнер для масштабирования
    private Table uiContainer;

    // Добавляем новые поля для хранения UI элементов способностей
    private Array<Stack> abilitySlots; // Стеки для отображения способностей
    private Array<Image> abilityIcons; // Иконки способностей
    private Array<Label> cooldownLabels; // Метки для отображения кулдаунов
    private Array<Image> cooldownOverlays; // Затемнения для отображения кулдаунов
    private Array<Label> levelLabels; // Метки для отображения уровней способностей
    private Window abilityInfoWindow; // Окно с информацией о способности

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
                    Label label = (Label) actor;
                    if (label.getText().toString().equals("A")) {
                        label.setVisible(newAutoStatus);
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
                        Label label = (Label) actor;
                        if (label.getText().toString().equals("A")) {
                            label.setVisible(ability.isAutoUse());
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
                        Label label = (Label) actor;
                        if (label.getText().toString().equals("A")) {
                            label.setVisible(false);
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

    public void updateDifficultyLevel(int level) {
        difficultyLabel.setText("Уровень " + level);
    }

    public void setUIScaleFromSettings(int scalePercent) {
        float newScale = Math.max(0.8f, Math.min(1.5f, scalePercent / 100f));
        if (newScale != uiScale) {
            uiScale = newScale;
            applyUIScale();
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

                    // Закрываем окно
                    window.remove();
                }
            });

            choicesTable.add(abilityTable).pad(10);
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
        System.out.println("Создание слота для способности: " + ability.getName());
        System.out.println("Иконка способности: " + (ability.getIcon() != null ? "есть" : "отсутствует"));

        if (ability.getIcon() != null) {
            try {
                TextureRegionDrawable iconDrawable = new TextureRegionDrawable(new TextureRegion(ability.getIcon()));
                Image abilityIcon = new Image(iconDrawable);
                abilityIcon.setFillParent(true);
                iconStack.add(abilityIcon);
                System.out.println("Иконка успешно добавлена");
            } catch (Exception e) {
                System.out.println("Ошибка при загрузке иконки: " + e.getMessage());
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
        Label levelLabel = new Label("Уровень " + ability.getLevel(), skin);
        levelLabel.setColor(1f, 0.8f, 0.2f, 1f);

        // Описание способности
        Label descriptionLabel = new Label(ability.getDescription(), skin);
        descriptionLabel.setWrap(true);

        // Проверяем, есть ли уже такая способность у игрока
        if (player != null && player.getAbilityManager() != null) {
            boolean hasAbility = false;
            for (Ability playerAbility : player.getAbilityManager().getAbilities()) {
                if (playerAbility.getClass() == ability.getClass()) {
                    hasAbility = true;
                    break;
                }
            }
            if (hasAbility) {
                // Выделяем имеющуюся способность
                abilityTable.setBackground(skin.getDrawable("button-down"));
                Label ownedLabel = new Label("Уже имеется", skin);
                ownedLabel.setColor(0.2f, 1f, 0.2f, 1f);
                abilityTable.add(ownedLabel).colspan(2).padBottom(5).row();
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
}
