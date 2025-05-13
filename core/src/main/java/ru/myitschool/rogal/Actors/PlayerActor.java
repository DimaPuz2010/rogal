package ru.myitschool.rogal.Actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ru.myitschool.rogal.Abilities.Abilitis.EnergyBullet;
import ru.myitschool.rogal.Abilities.Abilitis.FrostAuraAbility;
import ru.myitschool.rogal.Abilities.Abilitis.HealingAuraAbility;
import ru.myitschool.rogal.Abilities.Abilitis.LightningChainAbility;
import ru.myitschool.rogal.Abilities.Abilitis.OrbitingBladeAbility;
import ru.myitschool.rogal.Abilities.Abilitis.Relsatron;
import ru.myitschool.rogal.Abilities.Ability;
import ru.myitschool.rogal.Abilities.AbilityManager;
import ru.myitschool.rogal.CustomHelpers.Helpers.HitboxHelper;
import ru.myitschool.rogal.CustomHelpers.Vectors.Vector2Helpers;
import ru.myitschool.rogal.CustomHelpers.utils.GameUI;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
import ru.myitschool.rogal.CustomHelpers.utils.PlayerData;
import ru.myitschool.rogal.Main;
import ru.myitschool.rogal.Screens.GameScreen;

public class PlayerActor extends Actor {
    TextureRegion texture;
    Polygon hitbox;
    public static final float SCALE = 0.1f;

    private static final int MAX_SHIP_LEVEL = 5; // Максимальный уровень текстуры корабля

    // Атрибуты персонажа
    private int maxHealth = 120;
    private int currentHealth = maxHealth;
    private float healthRegeneration = 0.5f;
    private float healthRegenCounter = 0f;
    private float healthRegenAccumulator = 0f; // Аккумулятор для накопления дробных значений регенерации
    private float cooldownMultiplier = 1.0f;
    private int level = 1;
    private int experience = 0;
    private int experienceToNextLevel = 100;

    // Максимальный уровень персонажа
    private static final int MAX_LEVEL = 30;

    // Максимальный уровень способности
    private static final int MAX_ABILITY_LEVEL = 5;

    private static final float MAX_SPEED = 3.0f; // Максимальная скорость игрока
    private static float SPEED = 2f;
    private final Touchpad touchpad;
    private GameUI gameUI;
    // Переменные для управления с помощью касания
    private boolean touchControlMode = false; // Режим управления касанием
    private final Vector2 touchTarget = new Vector2(); // Целевая точка для движения
    private boolean isMovingToTarget = false; // Флаг движения к цели

    // Управление способностями
    private final AbilityManager abilityManager;
    private final Random random = new Random();

    // Время неуязвимости после получения урона
    private float invulnerabilityTime = 0.2f;
    private static final float MAX_INVULNERABILITY_TIME = 0.6f;

    // Константы для прокачки персонажа
    private static final float HEALTH_PER_LEVEL = 20f;
    private static final float REGEN_PER_LEVEL = 0.15f;
    private static final float ENERGY_PER_LEVEL = 10f;
    private static final float ENERGY_REGEN_PER_LEVEL = 0.15f;
    private static final float SPEED_PER_LEVEL = 0.04f;
    private static final float COOLDOWN_REDUCTION_PER_LEVEL = 0.015f;
    private static final float EXP_SCALING = 1.2f;

    // Время последнего получения урона
    private float lastDamageTime = 0f;
    private float gameTime = 0f;
    private static final int LEVELS_PER_SHIP_UPGRADE = 6; // Количество уровней игрока на одну текстуру
    // Добавим переменную для хранения текущей активной текстуры
    private TextureRegion currentActiveTexture;
    // Добавляем переменные для системы текстур в зависимости от уровня
    private int shipLevel = 1; // Текущий уровень корабля (от 1 до 5)
    private Animation<TextureRegion> exhaustAnimation; // Анимация выхлопа двигателя
    private float animationTime = 0f; // Время для анимации
    private Texture shipTexture; // Текстура корабля

    private interface AbilityConstructor {
        Ability create();
    }

    private final ArrayList<AbilityConstructor> availableAbilities = new ArrayList<>();

    // Добавляем атрибуты для энергии
    private int maxEnergy = 100;
    private int currentEnergy = maxEnergy;
    private float energyRegeneration = 1.5f;
    private float energyRegenCounter = 0f;
    private float energyRegenAccumulator = 0f;

    // Улучшение для системы неуязвимости - батарея энергии для резкого повышения регенерации
    private boolean energyBatteryActive = false;
    private float energyBatteryTime = 0f;
    private static final float ENERGY_BATTERY_DURATION = 5.0f;
    private static final float ENERGY_BATTERY_BOOST = 2.0f;

    // Система дебаффов
    private final HashMap<String, PlayerDebuff> activeDebuffs = new HashMap<>();
    // Обработчик смерти
    private DeathHandler deathHandler;

    public PlayerActor(final String texturePath, final Touchpad touchpad) {
        // Определяем начальный уровень корабля на основе уровня игрока в PlayerData
        int playerLevel = PlayerData.getPlayerLevel();
        if (playerLevel <= 0) playerLevel = 1; // Защита от некорректных данных

        // Вычисляем уровень корабля на основе уровня игрока
        this.shipLevel = Math.min(((playerLevel - 1) / LEVELS_PER_SHIP_UPGRADE) + 1, MAX_SHIP_LEVEL);

        // Загружаем текстуры для нужного уровня корабля
        loadShipTextures();

        // Теперь texture инициализируется в loadShipTextures()
        this.touchpad = touchpad;

        // Устанавливаем режим управления из настроек
        this.touchControlMode = PlayerData.getControlMode() == 1;
        if (touchControlMode) {
            LogHelper.log("PlayerActor", "Touch control mode enabled");
        }

        setWidth(this.texture.getTexture().getWidth()*SCALE);
        setHeight(this.texture.getTexture().getHeight()*SCALE);
        setOriginX(getWidth()/2);
        setOriginY(getHeight()/2);

        // Создаем хитбокс на основе текстуры корабля
        hitbox = HitboxHelper.createHitboxFromTexture(shipTexture, SCALE, 12);
        hitbox.setOrigin(getOriginX(), getOriginY());

        // Применяем улучшения из сохраненных данных
        PlayerData.applyUpgradesToPlayer(this);

        abilityManager = new AbilityManager(this);
        initAvailableAbilities();

        unlockInitialAbility();

        if (abilityManager != null) {
            abilityManager.enableAutoUseForAll();
        }

        LogHelper.log("PlayerActor", "Created player with ship level " + shipLevel);
    }

    /**
     * Загружает текстуры корабля и анимацию выхлопа для текущего уровня корабля
     */
    private void loadShipTextures() {
        // Очищаем предыдущие текстуры, если есть
        if (shipTexture != null) {
            shipTexture.dispose();
        }

        // Загружаем статичную текстуру корабля для текущего уровня
        String shipTexturePath = "Texture/Player/lv" + shipLevel + "/Ship.png";
        shipTexture = new Texture(Gdx.files.internal(shipTexturePath));
        this.texture = new TextureRegion(shipTexture);

        // Устанавливаем текущую активную текстуру
        this.currentActiveTexture = this.texture;

        // Загружаем кадры анимации Exhaust (полное изображение корабля с выхлопом)
        Array<TextureRegion> exhaustFrames = new Array<>();

        // Определяем правильный префикс файла, для 3-го уровня это Exhaust_3_1_, для остальных Exhaust_X_2_
        String filePattern = (shipLevel == 3) ?
            "Exhaust_" + shipLevel + "_1_" :
            "Exhaust_" + shipLevel + "_2_";

        LogHelper.log("PlayerActor", "Searching for animation frames with pattern: " + filePattern);

        for (int i = 0; i < 10; i++) {
            String framePath = "Texture/Player/lv" + shipLevel + "/" + filePattern + String.format("%03d", i) + ".png";
            if (Gdx.files.internal(framePath).exists()) {
                Texture frameTexture = new Texture(Gdx.files.internal(framePath));
                exhaustFrames.add(new TextureRegion(frameTexture));
                LogHelper.log("PlayerActor", "Found frame: " + framePath);
            } else {
                LogHelper.error("PlayerActor", "Missing exhaust animation frame: " + framePath);
            }
        }

        // Устанавливаем начальные размеры и хитбокс на основе статичной текстуры
        // Увеличим количество точек аппроксимации для больших текстур
        updateHitbox(this.texture);

        // Создаем анимацию из кадров только если есть хотя бы один кадр
        if (exhaustFrames.size > 0) {
            exhaustAnimation = new Animation<>(0.05f, exhaustFrames, Animation.PlayMode.LOOP);
            LogHelper.log("PlayerActor", "Loaded textures for ship level " + shipLevel +
                " with " + exhaustFrames.size + " animation frames");
        } else {
            // Если нет кадров анимации, установим анимацию в null
            exhaustAnimation = null;
            LogHelper.error("PlayerActor", "No animation frames found for ship level " + shipLevel);
        }
    }

    /**
     * Инициализирует список доступных конструкторов способностей
     */
    private void initAvailableAbilities() {
        availableAbilities.add(EnergyBullet::new);
        availableAbilities.add(HealingAuraAbility::new);
        availableAbilities.add(OrbitingBladeAbility::new);
        availableAbilities.add(LightningChainAbility::new);
        availableAbilities.add(FrostAuraAbility::new);
        availableAbilities.add(Relsatron::new);
    }

    @Override
    public void act(final float delta) {
        super.act(delta);

        gameTime += delta;
        // Обновляем время анимации
        animationTime += delta;

        // Определяем, какую текстуру использовать в данный момент
        updateActiveTexture();

        boolean isAbilitySelectionActive = false;
        if (gameUI != null) {
            Main game = (Main) Gdx.app.getApplicationListener();
            if (game != null && game.getScreen() instanceof GameScreen) {
                isAbilitySelectionActive = ((GameScreen) game.getScreen()).isAbilitySelectionPaused();
            }
        }

        // Обработка перемещения в зависимости от режима управления
        // Не двигаемся, если выбор способности активен
        if (!isAbilitySelectionActive) {
            if (touchControlMode) {
                // Режим управления касанием
                // Если есть активная цель для движения
                if (isMovingToTarget) {
                    float dx = touchTarget.x - (getX() + getOriginX());
                    float dy = touchTarget.y - (getY() + getOriginY());
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    // Если мы достигли цели (с небольшим порогом), останавливаемся
                    if (distance < 5) {
                        isMovingToTarget = false;
                    } else {
                        float moveX = dx / distance * SPEED;
                        float moveY = dy / distance * SPEED;

                        // Перемещаем игрока
                        moveBy(moveX, moveY);

                        float angle = Vector2Helpers.getRotationByVector(moveX, moveY);
                        if (angle != 0) {
                            setRotation(angle);
                        }
                    }
                }
            } else {
                // Стандартный режим управления джойстиком
                float nextX = touchpad.getKnobPercentX() * SPEED;
                float nextY = touchpad.getKnobPercentY() * SPEED;

                // Ограничиваем скорость максимальным значением
                float currentMoveSpeed = (float) Math.sqrt(nextX * nextX + nextY * nextY);
                if (currentMoveSpeed > MAX_SPEED) {
                    float scaleFactor = MAX_SPEED / currentMoveSpeed;
                    nextX *= scaleFactor;
                    nextY *= scaleFactor;
                }

                moveBy(nextX, nextY);

                float angle = Vector2Helpers.getRotationByVector(nextX, nextY);
                if (angle != 0) {
                    setRotation(angle);
                }
            }
        }

        hitbox.setPosition(getX(), getY());
        hitbox.setRotation(getRotation() + 90);

        healthRegenCounter += delta;
        if (healthRegenCounter >= 1f) {
            regenerateHealth();
            healthRegenCounter = 0f;
        }

        energyRegenCounter += delta;
        if (energyRegenCounter >= 1f) {
            regenerateEnergy();
            energyRegenCounter = 0f;
        }

        if (invulnerabilityTime > 0) {
            invulnerabilityTime -= delta;
        }

        if (energyBatteryActive) {
            energyBatteryTime -= delta;
            if (energyBatteryTime <= 0) {
                energyBatteryActive = false;
                LogHelper.log("PlayerActor", "Energy battery effect ended");
            }
        }

        updateDebuffs(delta);

        checkOverlap();

        if (abilityManager != null) {
            abilityManager.update(delta);
        }

        gameUI.act(delta);
    }

    /**
     * Обновляет активную текстуру на основе состояния движения
     */
    private void updateActiveTexture() {
        // Всегда используем анимацию выхлопа, если она доступна
        TextureRegion newActiveTexture = texture;

        if (exhaustAnimation != null) {
            try {
                newActiveTexture = exhaustAnimation.getKeyFrame(animationTime);
            } catch (Exception e) {
                LogHelper.error("PlayerActor", "Error getting animation frame: " + e.getMessage());
                // В случае ошибки анимации используем статичную текстуру
                exhaustAnimation = null;
            }
        }

        // Если текстура изменилась или активная текстура еще не установлена, обновляем размеры и хитбокс
        if (currentActiveTexture != newActiveTexture || currentActiveTexture == null) {
            currentActiveTexture = newActiveTexture;
            // Обновляем размеры объекта на основе текущей текстуры
            setWidth(currentActiveTexture.getRegionWidth() * SCALE);
            setHeight(currentActiveTexture.getRegionHeight() * SCALE);
            setOriginX(getWidth() / 2);
            setOriginY(getHeight() / 2);

            hitbox.setOrigin(getOriginX(), getOriginY());
            hitbox.setPosition(getX(), getY());
            hitbox.setRotation(getRotation() + 90);
        }
    }

    /**
     * Обновляет размеры и хитбокс в соответствии с текущей текстурой
     *
     * @param textureRegion текстура для обновления размеров
     */
    private void updateHitbox(TextureRegion textureRegion) {
        if (textureRegion == null) return;

        // Создаем новый хитбокс на основе текущей текстуры
        if (hitbox != null) {
            // Получаем текстуру для создания хитбокса
            Texture hitboxTexture = textureRegion.getTexture();

            // Увеличиваем количество точек аппроксимации для больших текстур
            int width = hitboxTexture.getWidth();
            // Чем больше текстура, тем больше точек аппроксимации, но не меньше 12
            int approximation = Math.max(20, width / 50);

            // Создаем хитбокс с адаптивным количеством точек аппроксимации
            hitbox = HitboxHelper.createHitboxFromTexture(hitboxTexture, SCALE, approximation);
            hitbox.setOrigin(getOriginX(), getOriginY());
            // Устанавливаем позицию хитбокса
            hitbox.setPosition(getX(), getY());
            hitbox.setRotation(getRotation() + 90);

            LogHelper.log("PlayerActor", "Created hitbox with approximation: " + approximation +
                " for texture size: " + width + "x" + hitboxTexture.getHeight());
        }
    }

    /**
     * Добавляет дебафф игроку
     * @param id уникальный идентификатор дебаффа
     * @param name название дебаффа
     * @param duration длительность дебаффа в секундах
     * @param effectValue значение эффекта дебаффа
     * @param type тип дебаффа
     * @return true если дебафф успешно добавлен
     */
    public boolean addDebuff(String id, String name, float duration, int effectValue, PlayerData.DebuffType type) {
        // Если такой дебафф уже есть, обновляем его длительность
        if (activeDebuffs.containsKey(id)) {
            PlayerDebuff existingDebuff = activeDebuffs.get(id);
            float newDuration = Math.max(existingDebuff.getRemainingTime(), duration);
            existingDebuff.setRemainingTime(newDuration);
            LogHelper.log("PlayerActor", "Updated debuff " + name + " duration to " + newDuration);
            return true;
        }

        // Создаем новый дебафф
        PlayerDebuff debuff = new PlayerDebuff(id, name, duration, effectValue, type);
        activeDebuffs.put(id, debuff);
        LogHelper.log("PlayerActor", "Added debuff " + name + " with duration " + duration);

        // Применяем эффект дебаффа
        applyDebuffEffect(debuff, true);

        return true;
    }

    /**
     * Разблокирует начальную способность игрока
     */
    private void unlockInitialAbility() {
        if (abilityManager == null) {
            LogHelper.error("PlayerActor", "Cannot unlock initial ability: AbilityManager is null");
            return;
        }

        if (abilityManager.getAbilities().size > 0) {
            LogHelper.log("PlayerActor", "Initial abilities already unlocked");
            return;
        }

        ArrayList<Ability> allInitialAbilities = new ArrayList<>();
        for (AbilityConstructor constructor : availableAbilities) {
            allInitialAbilities.add(constructor.create());
        }

        // Выбираем только 3 случайные способности для предложения игроку
        ArrayList<Ability> choiceAbilities = new ArrayList<>();
        Collections.shuffle(allInitialAbilities, random);
        for (int i = 0; i < 3; i++) {
            choiceAbilities.add(allInitialAbilities.get(i));
        }

        // Предлагаем выбор через UI
        if (gameUI != null && !choiceAbilities.isEmpty()) {
            gameUI.showAbilityChoiceDialog(choiceAbilities, ability -> {
                abilityManager.addAbility(ability);
                if (gameUI != null) {
                    gameUI.showAbilityUnlockMessage(ability.getName());
                    LogHelper.log("PlayerActor", "Выбрана начальная способность: " + ability.getName());
                    gameUI.updateAbilities();
                }
            });
        } else {
            LogHelper.error("PlayerActor", "Cannot show ability choice: GameUI is null or no abilities available");
        }
        updateUIData();
    }

    public void setGameUI(GameUI gameUI) {
        this.gameUI = gameUI;
        if (gameUI != null) {
            gameUI.setPlayer(this);
            LogHelper.log("PlayerActor", "Connected to GameUI");

            // Проверяем, нужно ли показать выбор начальной способности
            if (abilityManager != null && abilityManager.getAbilities().size == 0) {
                unlockInitialAbility();
            }
        }
    }

    @Override
    public void draw(final Batch batch, final float parentAlpha) {
        // Используем текущую активную текстуру, которая была обновлена в методе act
        batch.draw(
            currentActiveTexture,
            getX(),
            getY(),
            getOriginX(),
            getOriginY(),
            getWidth(),
            getHeight(),
            1,
            1,
            getRotation()-90
        );
    }

    /**
     * Наносит урон персонажу
     * @param damage величина урона
     * @return true если урон был нанесен, false если персонаж неуязвим
     */
    public boolean takeDamage(int damage) {
        if (invulnerabilityTime > 0) {
            return false;
        }

        lastDamageTime = gameTime;

        currentHealth -= damage;
        invulnerabilityTime = MAX_INVULNERABILITY_TIME;

        if (random.nextFloat() < 0.3f) {
            activateEnergyBattery();
        }

        if (currentHealth <= 0) {
            currentHealth = 0;
            onDeath();
        }

        return true;
    }

    /**
     * Регенерирует здоровье персонажа
     */
    private void regenerateHealth() {
        if (currentHealth < maxHealth) {
            healthRegenAccumulator += healthRegeneration;

            if (healthRegenAccumulator >= 1) {
                int regenAmount = (int)healthRegenAccumulator;
                healthRegenAccumulator -= regenAmount;

                currentHealth = Math.min(currentHealth + regenAmount, maxHealth);
                updateUIData();
            }
        }
    }

    /**
     * Обновляет данные в пользовательском интерфейсе
     */
    private void updateUIData() {
        if (gameUI != null) {
            gameUI.updateHealth(currentHealth, maxHealth, healthRegeneration);
            gameUI.updateLevel(level);
            gameUI.updateExperience(experience, experienceToNextLevel);
            gameUI.updateEnergy(currentEnergy, maxEnergy, energyRegeneration);
        }
    }

    /**
     * Повышает уровень персонажа
     */
    private void levelUp() {
        if (level >= MAX_LEVEL) {
            return;
        }

        level++;

        // Увеличиваем атрибуты персонажа
        maxHealth += HEALTH_PER_LEVEL;
        currentHealth = maxHealth; // Восстанавливаем здоровье при повышении уровня
        healthRegeneration += REGEN_PER_LEVEL;
        maxEnergy += ENERGY_PER_LEVEL;
        currentEnergy = maxEnergy; // Восстанавливаем энергию при повышении уровня
        energyRegeneration += ENERGY_REGEN_PER_LEVEL;
        SPEED += SPEED_PER_LEVEL;
        cooldownMultiplier -= COOLDOWN_REDUCTION_PER_LEVEL; // Уменьшаем кулдаун

        // Шкалирование опыта для следующего уровня
        experienceToNextLevel = (int) (experienceToNextLevel * EXP_SCALING);

        // Получаем доступные для улучшения способности
        ArrayList<Ability> upgradableAbilities = getUpgradableAbilities();

        // Проверяем, обновилась ли текстура корабля
        checkShipLevelUpgrade();

        if (!upgradableAbilities.isEmpty() && gameUI != null) {
            // Предлагаем выбор улучшения способности
            offerAbilityChoice();
        } else if (upgradableAbilities.isEmpty() && gameUI != null) {
            // Если нельзя улучшить существующие способности, предлагаем новые
            if (abilityManager.getAbilities().size < AbilityManager.MAX_ABILITIES) {
                offerAbilityChoice();
            }
        }

        LogHelper.log("PlayerActor", "Level UP! Current level: " + level);

        // Сохраняем данные игрока
        PlayerData.savePlayerLevel(level);
        updateUIData();
    }

    /**
     * Добавляет опыт персонажу
     * @param exp количество опыта
     */
    public void addExperience(int exp) {
        // Если достигнут максимальный уровень, опыт не добавляется
        if (level >= MAX_LEVEL) return;

        experience += exp;
        updateUIData();

        // Проверяем, достаточно ли опыта для нового уровня
        while (experience >= experienceToNextLevel && level < MAX_LEVEL) {
            experience -= experienceToNextLevel;
            levelUp();
        }

        // Если достигнут максимальный уровень, устанавливаем опыт в максимум
        if (level >= MAX_LEVEL) {
            experience = experienceToNextLevel;
        }
    }

    /**
     * Возвращает список способностей игрока, которые можно улучшить
     * @return список способностей, не достигших максимального уровня
     */
    private ArrayList<Ability> getUpgradableAbilities() {
        ArrayList<Ability> upgradable = new ArrayList<>();
        int countMaxLevel = 0;

        Array<Ability> playerAbilities = abilityManager.getAbilities();
        LogHelper.log("PlayerActor", "Проверка улучшаемых способностей, всего: " + playerAbilities.size);

        for (Ability ability : playerAbilities) {
            if (ability.getLevel() < MAX_ABILITY_LEVEL) {
                try {
                    Ability abilityCopy = ability.getClass().getDeclaredConstructor().newInstance();

                    abilityCopy.setLevel(ability.getLevel());

                    upgradable.add(abilityCopy);

                    LogHelper.log("PlayerActor", "Добавлена для улучшения способность: " +
                        ability.getName() + " (уровень " + ability.getLevel() + ")");
                } catch (Exception e) {
                    Gdx.app.error("PlayerActor", "Ошибка при создании копии способности", e);
                }
            } else {
                countMaxLevel++;
                LogHelper.log("PlayerActor", "Способность на максимальном уровне: " +
                    ability.getName() + " (уровень " + ability.getLevel() + ")");
            }
        }

        LogHelper.log("PlayerActor", "Найдено способностей для улучшения: " + upgradable.size() +
            ", на максимальном уровне: " + countMaxLevel);

        return upgradable;
    }

    /**
     * Предлагает выбор из 3 случайных способностей после повышения уровня
     */
    private void offerAbilityChoice() {
        ArrayList<Ability> choices = generateRandomAbilityChoices(3);

        if (gameUI != null && !choices.isEmpty()) {
            LogHelper.log("PlayerActor", "Предлагаются следующие способности (" + choices.size() + "):");
            for (Ability ability : choices) {
                boolean isUpgrade = findAbilityIndex(ability) >= 0;
                LogHelper.log("PlayerActor", "- " + ability.getName() +
                    " (уровень " + ability.getLevel() + ")" +
                    (isUpgrade ? " [улучшение]" : " [новая]"));
            }

            gameUI.showAbilityChoiceDialog(choices, ability -> {
                int abilityIndex = findAbilityIndex(ability);

                if (abilityIndex >= 0) {
                    int newLevel = upgradeAbility(abilityIndex);
                    Gdx.app.log("PlayerActor", "Улучшена способность: " + ability.getName() + " до уровня " + newLevel);
                } else {
                    abilityManager.addAbility(ability);
                    Gdx.app.log("PlayerActor", "Получена новая способность: " + ability.getName());
                }
            });
        } else {
            LogHelper.log("PlayerActor", "Нет доступных способностей для выбора");
        }
    }

    /**
     * Находит индекс способности в списке игрока
     * @param ability способность для поиска
     * @return индекс способности или -1, если не найдена
     */
    private int findAbilityIndex(Ability ability) {
        Array<Ability> playerAbilities = abilityManager.getAbilities();
        for (int i = 0; i < playerAbilities.size; i++) {
            if (playerAbilities.get(i).getClass().getName().equals(ability.getClass().getName())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Генерирует список случайных способностей для выбора
     * @param count количество способностей для генерации
     * @return список случайных способностей (новых и существующих для улучшения)
     */
    private ArrayList<Ability> generateRandomAbilityChoices(int count) {
        ArrayList<Ability> result = new ArrayList<>();

        ArrayList<Ability> upgradableAbilities = getUpgradableAbilities();

        ArrayList<Ability> newAbilities = generateNewAbilities();

        boolean allSlotsOccupied = abilityManager.getAbilityCount() >= AbilityManager.MAX_ABILITIES;

        ArrayList<Ability> allPossibleChoices = new ArrayList<>();

        // Если все слоты заняты, предлагаем только улучшения существующих способностей
        if (allSlotsOccupied) {
            allPossibleChoices.addAll(upgradableAbilities);

            // Если нет способностей для улучшения, возвращаем пустой список
            if (allPossibleChoices.isEmpty()) {
                LogHelper.log("PlayerActor", "Все способности на максимальном уровне, новые способности не предлагаются");
                return result;
            }
        } else {
            // Если есть свободные слоты, предлагаем как новые способности, так и улучшения
            allPossibleChoices.addAll(upgradableAbilities);
            allPossibleChoices.addAll(newAbilities);
        }

        if (allPossibleChoices.size() <= count) {
            return allPossibleChoices;
        }

        // Перемешиваем список возможных способностей и выбираем случайные
        Collections.shuffle(allPossibleChoices, random);
        for (int i = 0; i < count; i++) {
            if (i < allPossibleChoices.size()) {
                result.add(allPossibleChoices.get(i));
            }
        }

        return result;
    }

    /**
     * Устанавливает обработчик смерти игрока
     *
     * @param handler обработчик смерти
     */
    public void setDeathHandler(DeathHandler handler) {
        this.deathHandler = handler;
        LogHelper.log("PlayerActor", "Death handler set");
    }

    /**
     * Генерирует список новых способностей, которых еще нет у игрока
     * @return список новых способностей
     */
    private ArrayList<Ability> generateNewAbilities() {
        ArrayList<Ability> newAbilities = new ArrayList<>();

        // Получаем текущие способности игрока
        Array<Ability> playerAbilities = abilityManager.getAbilities();

        for (AbilityConstructor constructor : availableAbilities) {
            Ability testAbility = constructor.create();
            boolean playerHasAbility = false;

            for (Ability playerAbility : playerAbilities) {
                if (playerAbility.getClass().getName().equals(testAbility.getClass().getName())) {
                    playerHasAbility = true;
                    break;
                }
            }

            if (!playerHasAbility) {
                newAbilities.add(testAbility);
            }
        }

        return newAbilities;
    }

    /**
     * Действия при смерти персонажа
     */
    private void onDeath() {
        LogHelper.log("PlayerActor", "Player died!");

        if (deathHandler != null) {
            deathHandler.onPlayerDeath();
        }
    }

    /**
     * Получает максимальное здоровье игрока
     *
     * @return максимальное здоровье
     */
    public int getMaxHealth() {
        return maxHealth;
    }

    private void checkOverlap(){
        Stage stage = getStage();
        if (stage == null) return;

        Array<Actor> actors = stage.getActors();
        Array<EnemyActor> enemiesToRemove = new Array<>();

        for (int i = 0; i < actors.size; i++) {
            Actor actor = actors.get(i);
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;

                boolean isOverlap = Intersector.overlapConvexPolygons(this.hitbox, enemy.getHitbox());

                if (isOverlap) {
                    takeDamage(enemy.getCollisionDamage());
                    enemiesToRemove.add(enemy);

                    Gdx.app.debug("PlayerActor", "Collision detected with enemy");
                }
            }
        }

        for (EnemyActor enemy : enemiesToRemove) {
            enemy.die();
        }
    }

    public Polygon getHitbox() {
        return this.hitbox;
    }

    /**
     * Устанавливает максимальное здоровье игрока
     *
     * @param maxHealth новое максимальное здоровье
     */
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = Math.min(currentHealth, maxHealth);
        updateUIData();
    }

    /**
     * Получает текущее здоровье игрока
     * @return текущее здоровье
     */
    public int getCurrentHealth() {
        return currentHealth;
    }

    /**
     * Устанавливает текущее здоровье игрока
     *
     * @param health новое значение здоровья
     */
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.min(health, maxHealth);
        updateUIData();
    }

    /**
     * Получает максимальную энергию игрока
     *
     * @return максимальная энергия
     */
    public int getMaxEnergy() {
        return maxEnergy;
    }

    /**
     * Устанавливает максимальную энергию игрока
     *
     * @param maxEnergy новое значение максимальной энергии
     */
    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = maxEnergy;
        this.currentEnergy = Math.min(currentEnergy, maxEnergy);
        updateUIData();
    }

    /**
     * Получает текущую энергию игрока
     *
     * @return текущая энергия
     */
    public int getCurrentEnergy() {
        return currentEnergy;
    }

    /**
     * Устанавливает текущую энергию игрока
     *
     * @param energy новое значение энергии
     */
    public void setCurrentEnergy(int energy) {
        this.currentEnergy = Math.min(energy, maxEnergy);
        updateUIData();
    }

    /**
     * Получает скорость регенерации здоровья
     * @return скорость регенерации здоровья
     */
    public float getHealthRegeneration() {
        return healthRegeneration;
    }

    /**
     * Устанавливает скорость регенерации здоровья
     *
     * @param regeneration новая скорость регенерации
     */
    public void setHealthRegeneration(float regeneration) {
        this.healthRegeneration = regeneration;
        updateUIData();
    }

    /**
     * Получает скорость регенерации энергии
     *
     * @return скорость регенерации энергии
     */
    public float getEnergyRegeneration() {
        return energyRegeneration;
    }

    /**
     * Устанавливает скорость регенерации энергии
     *
     * @param regeneration новая скорость регенерации
     */
    public void setEnergyRegeneration(float regeneration) {
        this.energyRegeneration = regeneration;
        updateUIData();
    }

    /**
     * Восстанавливает энергию игрока
     */
    private void regenerateEnergy() {
        if (currentEnergy < maxEnergy) {
            float actualRegen = energyRegeneration;

            if (energyBatteryActive) {
                actualRegen *= ENERGY_BATTERY_BOOST;
            }

            energyRegenAccumulator += actualRegen;

            if (energyRegenAccumulator >= 1) {
                int regenAmount = (int)energyRegenAccumulator;
                energyRegenAccumulator -= regenAmount;

                currentEnergy = Math.min(currentEnergy + regenAmount, maxEnergy);
                updateUIData();
            }
        }
    }

    public int getLevel() {
        return level;
    }

    /**
     * Возвращает максимальный уровень персонажа
     * @return максимальный уровень
     */
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    /**
     * Возвращает максимальный уровень способности
     * @return максимальный уровень способности
     */
    public int getMaxAbilityLevel() {
        return MAX_ABILITY_LEVEL;
    }

    public float getCooldownMultiplier() {
        return cooldownMultiplier;
    }

    /**
     * Возвращает текущее количество опыта
     * @return текущий опыт
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Возвращает количество опыта, необходимое для следующего уровня
     * @return опыт до следующего уровня
     */
    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    /**
     * Возвращает менеджер способностей
     * @return менеджер способностей игрока
     */
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    /**
     * Использует способность по индексу
     * @param abilityIndex индекс способности (0-4)
     * @param targetPosition позиция использования
     * @return true если способность использована успешно
     */
    public boolean useAbility(int abilityIndex, Vector2 targetPosition) {
        if (abilityManager == null) {
            LogHelper.error("PlayerActor", "Cannot use ability: AbilityManager is null");
            return false;
        }

        boolean success = abilityManager.activateAbility(abilityIndex, targetPosition);

        if (success) {
            Ability ability = abilityManager.getAbility(abilityIndex);
            if (ability != null) {
                LogHelper.log("PlayerActor", "Used ability " + abilityIndex + ": " + ability.getName());
            }
        }

        return success;
    }

    /**
     * Повышает уровень способности
     * @param index индекс способности в списке
     * @return новый уровень способности или -1 в случае ошибки
     */
    public int upgradeAbility(int index) {
        if (abilityManager == null) return -1;
        return abilityManager.levelUpAbility(index);
    }

    /**
     * Использует энергию для способности
     * @param cost стоимость энергии
     * @return true если успешно потрачена энергия
     */
    public boolean useEnergy(float cost) {
        // Если включен режим бесплатных способностей, не тратим энергию
        if (PlayerData.isFreeAbilitiesEnabled()) {
            return true;
        }

        // Иначе проверяем, достаточно ли энергии
        int energyCost = Math.round(cost);
        if (currentEnergy >= energyCost) {
            currentEnergy -= energyCost;
            updateUIData();
            return true;
        }
        return false;
    }

    /**
     * Устанавливает скорость игрока
     * @param speed новая скорость
     */
    public void setSpeed(float speed) {
        SPEED = Math.min(speed, MAX_SPEED);
    }

    /**
     * Находит позицию ближайшего врага в пределах заданного радиуса
     * @param searchRadius радиус поиска врагов
     * @return позиция ближайшего врага или null, если враги не найдены
     */
    public Vector2 findNearestEnemyPosition(float searchRadius) {
        Stage stage = getStage();
        if (stage == null) return null;

        Vector2 playerPos = new Vector2(getX() + getOriginX(), getY() + getOriginY());
        Vector2 nearestEnemyPos = null;
        float minDistance = Float.MAX_VALUE;

        Array<Actor> actors = stage.getActors();

        for (int i = 0; i < actors.size; i++) {
            Actor actor = actors.get(i);
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;

                Vector2 enemyPos = new Vector2(enemy.getX() + enemy.getOriginX(),
                                             enemy.getY() + enemy.getOriginY());

                float distance = playerPos.dst(enemyPos);

                if (distance <= searchRadius && distance < minDistance) {
                    minDistance = distance;
                    nearestEnemyPos = enemyPos;
                }
            }
        }

        return nearestEnemyPos;
    }

    /**
     * Получает текущую скорость игрока
     * @return текущая скорость
     */
    public float getSpeed() {
        return SPEED;
    }

    /**
     * Получает максимальную скорость игрока
     * @return максимальная скорость
     */
    public float getMaxSpeed() {
        return MAX_SPEED;
    }

    /**
     * Возвращает время последнего получения урона
     *
     * @return время в секундах
     */
    public float getLastDamageTime() {
        return lastDamageTime;
    }

    /**
     * Класс для хранения информации о дебаффе игрока
     */
    public class PlayerDebuff {
        private final String id;
        private final String name;
        private final float duration;
        private float remainingTime;
        private final int effectValue;
        private final PlayerData.DebuffType type;

        public PlayerDebuff(String id, String name, float duration, int effectValue, PlayerData.DebuffType type) {
            this.id = id;
            this.name = name;
            this.duration = duration;
            this.remainingTime = duration;
            this.effectValue = effectValue;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public float getDuration() {
            return duration;
        }

        public float getRemainingTime() {
            return remainingTime;
        }

        public void setRemainingTime(float time) {
            this.remainingTime = Math.max(0, time);
        }

        public int getEffectValue() {
            return effectValue;
        }

        public PlayerData.DebuffType getType() {
            return type;
        }

        /**
         * Обновляет дебафф
         * @param delta время между кадрами
         * @return true если дебафф все еще активен
         */
        public boolean update(float delta) {
            remainingTime -= delta;
            return remainingTime > 0;
        }
    }

    /**
     * Удаляет дебафф у игрока
     * @param id идентификатор дебаффа
     * @return true если дебафф был удален
     */
    public boolean removeDebuff(String id) {
        if (activeDebuffs.containsKey(id)) {
            PlayerDebuff debuff = activeDebuffs.remove(id);
            LogHelper.log("PlayerActor", "Removed debuff " + debuff.getName());

            // Отменяем эффект дебаффа
            applyDebuffEffect(debuff, false);
            return true;
        }
        return false;
    }

    /**
     * Уменьшает длительность указанного дебаффа
     * @param id идентификатор дебаффа
     * @param reduction количество секунд для уменьшения
     * @return true если дебафф был изменен
     */
    public boolean reduceDebuffDuration(String id, float reduction) {
        if (activeDebuffs.containsKey(id)) {
            PlayerDebuff debuff = activeDebuffs.get(id);
            float currentTime = debuff.getRemainingTime();
            float newTime = Math.max(0, currentTime - reduction);

            debuff.setRemainingTime(newTime);
            LogHelper.log("PlayerActor", "Reduced debuff " + debuff.getName() + " duration: " +
                        currentTime + " -> " + newTime);

            if (newTime <= 0) {
                removeDebuff(id);
            }
            return true;
        }
        return false;
    }

    /**
     * Очищает все дебаффы игрока
     * @return количество удаленных дебаффов
     */
    public int clearAllDebuffs() {
        int count = activeDebuffs.size();
        if (count > 0) {
            for (PlayerDebuff debuff : activeDebuffs.values()) {
                applyDebuffEffect(debuff, false);
            }
            activeDebuffs.clear();
            LogHelper.log("PlayerActor", "Cleared all debuffs (" + count + ")");
        }
        return count;
    }

    /**
     * Возвращает все активные дебаффы игрока
     * @return карта дебаффов с их оставшейся длительностью
     */
    public HashMap<String, Float> getActiveDebuffs() {
        HashMap<String, Float> result = new HashMap<>();
        for (Map.Entry<String, PlayerDebuff> entry : activeDebuffs.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getRemainingTime());
        }
        return result;
    }

    /**
     * Обновляет все активные дебаффы
     * @param delta время между кадрами
     */
    private void updateDebuffs(float delta) {
        ArrayList<String> expiredDebuffs = new ArrayList<>();

        for (Map.Entry<String, PlayerDebuff> entry : activeDebuffs.entrySet()) {
            PlayerDebuff debuff = entry.getValue();
            if (!debuff.update(delta)) {
                // Дебафф истек
                expiredDebuffs.add(entry.getKey());
            } else {
                // Применяем периодический эффект дебаффа
                applyPeriodicDebuffEffect(debuff, delta);
            }
        }

        // Удаляем истекшие дебаффы
        for (String id : expiredDebuffs) {
            removeDebuff(id);
        }
    }

    /**
     * Применяет эффект дебаффа
     * @param debuff объект дебаффа
     * @param isApplying true если применяем, false если отменяем
     */
    private void applyDebuffEffect(PlayerDebuff debuff, boolean isApplying) {
        switch (debuff.getType()) {
            case SLOW:
                // Изменяем скорость
                float speedModifier = isApplying ?
                    (1.0f - debuff.getEffectValue() / 100.0f) :
                    (1.0f / (1.0f - debuff.getEffectValue() / 100.0f));
                SPEED *= speedModifier;
                break;
            case ARMOR_REDUCTION:
                // Можно реализовать в будущем
                break;
            case ATTACK_REDUCTION:
                // Можно реализовать в будущем
                break;
            case STUN:
                // Можно реализовать в будущем
                break;
            default:
                // Для других типов дебаффов
                break;
        }
    }

    /**
     * Применяет периодический эффект дебаффа
     * @param debuff объект дебаффа
     * @param delta время между кадрами
     */
    private void applyPeriodicDebuffEffect(PlayerDebuff debuff, float delta) {
        switch (debuff.getType()) {
            case DAMAGE_OVER_TIME:
                float damagePerSecond = debuff.getEffectValue();
                int damage = Math.round(damagePerSecond * delta);
                if (damage > 0) {
                    takeDamage(damage);
                }
                break;
            default:
                // Другие периодические эффекты
                break;
        }
    }

    /**
     * Полностью восстанавливает энергию игрока
     */
    public void restoreFullEnergy() {
        currentEnergy = maxEnergy;
        updateUIData();
        LogHelper.log("PlayerActor", "Energy fully restored: " + currentEnergy + "/" + maxEnergy);
    }

    /**
     * Активирует эффект "энергетической батареи", временно повышающий скорость регенерации энергии
     */
    public void activateEnergyBattery() {
        energyBatteryActive = true;
        energyBatteryTime = ENERGY_BATTERY_DURATION;
        LogHelper.log("PlayerActor", "Energy battery activated for " + ENERGY_BATTERY_DURATION + " seconds");
    }

    /**
     * Метод для обновления режима управления
     * Необходимо вызывать при изменении настроек управления
     */
    public void updateControlMode() {
        this.touchControlMode = PlayerData.getControlMode() == 1;
        // Сбрасываем флаг движения при смене режима
        isMovingToTarget = false;
        LogHelper.log("PlayerActor", "Control mode updated: " + (touchControlMode ? "touch" : "joystick"));
    }

    /**
     * Проверяет, движется ли игрок к целевой точке в режиме управления касанием
     *
     * @return true если игрок движется к цели
     */
    public boolean isMovingToTarget() {
        return isMovingToTarget;
    }

    /**
     * Устанавливает целевую точку для движения в режиме касания
     *
     * @param x координата X
     * @param y координата Y
     */
    public void setTouchTarget(float x, float y) {
        touchTarget.set(x, y);
        isMovingToTarget = true;
    }

    /**
     * Интерфейс для обработки смерти игрока
     */
    public interface DeathHandler {
        void onPlayerDeath();
    }

    /**
     * Проверяет необходимость изменения текстуры корабля при повышении уровня
     */
    private void checkShipLevelUpgrade() {
        // Вычисляем новый уровень корабля на основе уровня игрока
        int newShipLevel = Math.min(((level - 1) / LEVELS_PER_SHIP_UPGRADE) + 1, MAX_SHIP_LEVEL);

        // Если уровень корабля изменился, обновляем текстуры
        if (newShipLevel != shipLevel) {
            shipLevel = newShipLevel;
            loadShipTextures();

        }
    }
}
