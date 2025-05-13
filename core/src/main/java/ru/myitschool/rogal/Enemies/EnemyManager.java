package ru.myitschool.rogal.Enemies;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.Actors.PlayerActor;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

public class EnemyManager implements Disposable {
    private final Array<EnemyActor> enemies;
    private final PlayerActor player;
    private final Stage stage;

    // Настройки появления противников
    private float spawnTimer;
    private float spawnInterval;
    private int currentWave;
    private final int enemiesPerWave;
    private final float waveDifficultyMultiplier;

    // Настройки длительности волны
    private boolean useWaveTimer = true;
    private float waveTimer; // Текущее время волны
    private float waveDuration; // Продолжительность волны в секундах
    private float waveBreakDuration = 5f; // Перерыв между волнами в секундах
    private float waveBreakTimer = 0f; // Таймер перерыва между волнами
    private boolean isWaveBreak = false; // Флаг перерыва между волнами
    private WaveListener waveListener;
    private EnemyKillListener killListener;

    // Настройки радиуса появления
    private float minSpawnDistance = 300f;
    private float maxSpawnDistance = 600f;

    // Настройки переспавна
    private float maxEnemyDistance = 2000f; // Максимальное расстояние от игрока
    private final float respawnCheckInterval = 5f; // Интервал проверки расстояния
    private float respawnCheckTimer = 0f;

    // Настройки сложности
    private float baseEnemyHealth;
    private float baseEnemyDamage;
    private final float baseEnemySpeed;
    private final float baseEnemyReward;

    // Настройки разнообразия врагов
    private final float enemyHealthVariation = 0.2f; // ±20% вариация здоровья
    private final float enemyDamageVariation = 0.15f; // ±15% вариация урона
    private final float enemySpeedVariation = 0.25f; // ±25% вариация скорости

    // Настройки типов врагов
    private enum EnemyType {
        NORMAL,   // Обычный враг
        FAST,     // Быстрый, но слабый враг
        TANK,     // Медленный, но с высоким здоровьем враг
        ELITE     // Элитный враг с повышенными характеристиками
    }

    // Вероятности появления различных типов врагов
    private float fastEnemyChance = 0.25f;
    private float tankEnemyChance = 0.15f;
    private final float eliteEnemyChance = 0.05f; // 5% шанс элитного врага

    // Множители для различных типов врагов
    private final float fastEnemyHealthMult = 0.7f;
    private final float fastEnemyDamageMult = 0.8f;
    private final float fastEnemySpeedMult = 1.7f;

    private final float tankEnemyHealthMult = 2.2f;
    private final float tankEnemyDamageMult = 1.0f;
    private final float tankEnemySpeedMult = 0.6f;

    private final float eliteEnemyHealthMult = 2.0f;
    private final float eliteEnemyDamageMult = 1.8f;
    private final float eliteEnemySpeedMult = 1.2f;

    // Множители прогрессии урона для разных волн
    private final float earlyGameDamageScale = 1.1f;  // Волны 1-5
    private final float midGameDamageScale = 1.3f;    // Волны 6-15
    private final float lateGameDamageScale = 1.5f;   // Волны 16+

    // Пороговые значения волн
    private static final int MID_GAME_WAVE = 6;
    private static final int LATE_GAME_WAVE = 16;

    // Лимит на количество врагов
    private int maxEnemiesOnScreen = 12;
    // Адаптивная система сложности
    private boolean adaptiveDifficulty = true;
    private float playerPerformanceRating = 1.0f;
    private int playerDeathCount = 0;
    private int consecutiveWavesClearedWithoutDamage = 0;
    public EnemyManager(PlayerActor player, Stage stage) {
        this.player = player;
        this.stage = stage;
        this.enemies = new Array<>();

        // Инициализация настроек
        this.spawnTimer = 0;
        this.spawnInterval = 3.5f; // Сократил интервал между появлениями
        this.currentWave = 1;
        this.enemiesPerWave = 3; // Больше врагов в начальной волне
        this.waveDifficultyMultiplier = 1.2f; // Увеличил множитель сложности

        // Инициализация таймера волны
        this.waveDuration = calculateWaveDuration(currentWave);
        this.waveTimer = waveDuration;

        // Базовые характеристики противников
        this.baseEnemyHealth = 80f;
        this.baseEnemyDamage = 15f;
        this.baseEnemySpeed = 0.5f;
        this.baseEnemyReward = 15f;

        // Инициализация оптимизированного пула врагов
        initEnemyPool();

        LogHelper.log("EnemyManager", "Enemy manager initialized with balanced settings");
    }

    /**
     * Устанавливает слушателя событий волны
     *
     * @param listener слушатель событий
     */
    public void setWaveListener(WaveListener listener) {
        this.waveListener = listener;
    }

    /**
     * Включает или выключает таймер волны
     *
     * @param useTimer использовать ли таймер
     */
    public void setUseWaveTimer(boolean useTimer) {
        this.useWaveTimer = useTimer;
    }

    /**
     * Рассчитывает продолжительность волны в зависимости от номера
     *
     * @param waveNumber номер волны
     * @return продолжительность волны в секундах
     */
    private float calculateWaveDuration(int waveNumber) {
        // Базовая продолжительность - 90 секунд
        float baseDuration = 90f;

        // Волны постепенно становятся длиннее
        float durationMultiplier;
        if (waveNumber <= 5) {
            // Первые волны короче для быстрого входа в игру
            durationMultiplier = 0.7f + (waveNumber * 0.06f); // 70% - 100%
        } else if (waveNumber <= 15) {
            // Средние волны стандартной длины с небольшим ростом
            durationMultiplier = 1.0f + ((waveNumber - 5) * 0.03f); // 100% - 130%
        } else {
            // Поздние волны длиннее для большего вызова
            durationMultiplier = 1.3f + ((waveNumber - 15) * 0.02f); // 130%+
            // Ограничиваем максимальную длину
            durationMultiplier = Math.min(durationMultiplier, 2.0f);
        }

        return baseDuration * durationMultiplier;
    }

    /**
     * Устанавливает слушателя убийств врагов
     *
     * @param listener слушатель
     */
    public void setEnemyKillListener(EnemyKillListener listener) {
        this.killListener = listener;
    }

    /**
     * Включает или выключает адаптивную сложность
     *
     * @param adaptive использовать ли адаптивную сложность
     */
    public void setAdaptiveDifficulty(boolean adaptive) {
        this.adaptiveDifficulty = adaptive;
    }

    /**
     * Уведомляет менеджер о смерти игрока для адаптации сложности
     */
    public void notifyPlayerDeath() {
        if (!adaptiveDifficulty) return;

        playerDeathCount++;
        playerPerformanceRating = Math.max(0.7f, playerPerformanceRating - 0.1f);
        LogHelper.log("EnemyManager", "Player died, adjusting difficulty: " + playerPerformanceRating);
    }

    /**
     * Инициализирует пул врагов для оптимизации создания
     */
    private void initEnemyPool() {
        // Реализация пула врагов для переиспользования
        LogHelper.log("EnemyManager", "Enemy pool initialized");
    }

    public void update(float delta) {
        // Обрабатываем перерыв между волнами
        if (isWaveBreak) {
            updateWaveBreak(delta);
            return;
        }

        // Обновляем таймер волны, если он включен
        if (useWaveTimer) {
            waveTimer -= delta;

            // Уведомляем слушателя об обновлении таймера волны
            if (waveListener != null) {
                waveListener.onWaveTimerUpdate(waveTimer, waveDuration);
            }

            // Если таймер истек, завершаем волну
            if (waveTimer <= 0) {
                endWave();
                return;
            }
        }

        // Обновляем таймер появления
        spawnTimer += delta;

        // Проверяем, нужно ли создать новых противников
        if (spawnTimer >= spawnInterval && enemies.size < maxEnemiesOnScreen) {
            spawnEnemies();
            spawnTimer = 0;
        }

        // Обновляем таймер проверки переспавна
        respawnCheckTimer += delta;
        if (respawnCheckTimer >= respawnCheckInterval) {
            checkEnemiesDistance();
            respawnCheckTimer = 0;
        }

        // Обновляем существующих противников
        for (EnemyActor enemy : enemies) {
            if (enemy != null) {
                enemy.act(delta);
            }
        }

        // Удаляем мертвых противников
        removeDeadEnemies();

        // Проверяем, завершена ли волна, если все враги убиты
        if (!isWaveBreak && useWaveTimer && enemies.size == 0 && waveTimer > 0) {

            // Если игрок очистил волну без получения урона, увеличиваем счетчик
            if (player.getLastDamageTime() < waveDuration - waveTimer) {
                consecutiveWavesClearedWithoutDamage++;

                // Увеличиваем рейтинг производительности игрока
                if (adaptiveDifficulty) {
                    playerPerformanceRating = Math.min(1.5f, playerPerformanceRating + 0.05f);
                    LogHelper.log("EnemyManager", "Wave cleared without damage, adjusting difficulty: " + playerPerformanceRating);
                }
            } else {
                consecutiveWavesClearedWithoutDamage = 0;
            }
        }
    }

    /**
     * Обновляет перерыв между волнами
     *
     * @param delta время между кадрами
     */
    private void updateWaveBreak(float delta) {
        waveBreakTimer -= delta;

        // Уведомляем слушателя об обновлении таймера перерыва
        if (waveListener != null) {
            waveListener.onWaveBreakUpdate(waveBreakTimer, waveBreakDuration);
        }

        // Если таймер перерыва истек, начинаем новую волну
        if (waveBreakTimer <= 0) {
            startNextWave();
        }
    }

    /**
     * Завершает текущую волну и начинает перерыв
     */
    private void endWave() {
        // Уведомляем о завершении волны
        if (waveListener != null) {
            waveListener.onWaveEnd(currentWave);
        }

        LogHelper.log("EnemyManager", "Wave " + currentWave + " ended");

        // Начинаем перерыв между волнами
        isWaveBreak = true;
        waveBreakTimer = waveBreakDuration;

    }

    /**
     * Начинает следующую волну после перерыва
     */
    private void startNextWave() {
        // Увеличиваем номер волны
        currentWave++;

        // Настраиваем параметры новой волны
        updateWaveParameters();

        // Сбрасываем флаг перерыва
        isWaveBreak = false;

        // Устанавливаем таймер волны
        waveDuration = calculateWaveDuration(currentWave);
        waveTimer = waveDuration;

        // Уведомляем о начале новой волны
        if (waveListener != null) {
            waveListener.onWaveStart(currentWave);
        }

        LogHelper.log("EnemyManager", "Starting wave " + currentWave +
            ", duration: " + waveDuration +
            ", spawn interval: " + spawnInterval +
            ", max enemies: " + maxEnemiesOnScreen);
    }

    /**
     * Обновляет параметры волны при переходе к следующей
     */
    private void updateWaveParameters() {
        // Уменьшаем интервал между появлениями, но не ниже минимума
        spawnInterval = Math.max(0.8f, spawnInterval - 0.2f);

        // Увеличиваем лимит врагов на экране с ростом волн, но не более 20
        maxEnemiesOnScreen = Math.min(12 + (currentWave / 2), 20);

        // Увеличиваем шансы появления особых врагов с ростом волн
        if (currentWave >= LATE_GAME_WAVE) {
            // В поздней игре больше танков и быстрых врагов
            fastEnemyChance = Math.min(0.35f, fastEnemyChance + 0.01f);
            tankEnemyChance = Math.min(0.25f, tankEnemyChance + 0.01f);
        } else if (currentWave >= MID_GAME_WAVE) {
            // В средней игре постепенно увеличиваем шансы
            fastEnemyChance = Math.min(0.30f, fastEnemyChance + 0.005f);
            tankEnemyChance = Math.min(0.20f, tankEnemyChance + 0.005f);
        }

        // Применяем адаптивную сложность
        if (adaptiveDifficulty) {
            float difficultyAdjustment = playerPerformanceRating;

            // Если игрок умирал слишком часто, немного снижаем сложность
            if (playerDeathCount > currentWave / 3) {
                difficultyAdjustment *= 0.85f;
            }

            // Если игрок легко проходит волны, увеличиваем сложность
            if (consecutiveWavesClearedWithoutDamage >= 2) {
                difficultyAdjustment *= 1.15f;
            }

            // Применяем корректировку к базовым параметрам
            baseEnemyHealth *= difficultyAdjustment;
            baseEnemyDamage *= difficultyAdjustment;

            LogHelper.log("EnemyManager", "Adaptive difficulty applied: " + difficultyAdjustment);
        }
    }

    /**
     * Проверяет расстояние от врагов до игрока и переспавнит тех, кто слишком далеко
     */
    private void checkEnemiesDistance() {
        for (EnemyActor enemy : enemies) {
            if (enemy != null && !enemy.isDead()) {
                float distance = Vector2.dst(
                    enemy.getX() + enemy.getWidth() / 2,
                    enemy.getY() + enemy.getHeight() / 2,
                    player.getX() + player.getWidth() / 2,
                    player.getY() + player.getHeight() / 2
                );

                if (distance > maxEnemyDistance) {
                    // Создаем новую позицию для переспавна
                    Vector2 newPosition = generateSpawnPosition();
                    enemy.setPosition(newPosition.x, newPosition.y);
                    LogHelper.log("EnemyManager", "Enemy respawned due to distance: " + distance);
                }
            }
        }
    }

    private void spawnEnemies() {
        int enemiesToSpawn = calculateEnemiesToSpawn();

        // Ограничиваем количество врагов на экране
        enemiesToSpawn = Math.min(enemiesToSpawn, maxEnemiesOnScreen - enemies.size);

        for (int i = 0; i < enemiesToSpawn; i++) {
            EnemyActor enemy = createEnemy();
            if (enemy != null) {
                enemies.add(enemy);
                stage.addActor(enemy);
            }
        }

        LogHelper.log("EnemyManager", "Spawned " + enemiesToSpawn + " enemies in wave " + currentWave);
    }

    private EnemyActor createEnemy() {
        // Определяем тип врага
        EnemyType enemyType = determineEnemyType();

        // Получаем базовые характеристики с учетом волны
        float baseHealth = calculateBaseHealthForWave();
        float baseDamage = calculateBaseDamageForWave();
        float baseSpeed = calculateBaseSpeedForWave();
        float baseReward = calculateBaseRewardForWave();

        // Модифицируем характеристики в зависимости от типа врага
        float healthMultiplier = getHealthMultiplierForType(enemyType);
        float damageMultiplier = getDamageMultiplierForType(enemyType);
        float speedMultiplier = getSpeedMultiplierForType(enemyType);
        float rewardMultiplier = getRewardMultiplierForType(enemyType);

        // Применяем множители типа врага
        baseHealth *= healthMultiplier;
        baseDamage *= damageMultiplier;
        baseSpeed *= speedMultiplier;
        baseReward *= rewardMultiplier;

        // Добавляем вариацию для разнообразия врагов
        float healthVariation = MathUtils.random(-enemyHealthVariation, enemyHealthVariation);
        float damageVariation = MathUtils.random(-enemyDamageVariation, enemyDamageVariation);
        float speedVariation = MathUtils.random(-enemySpeedVariation, enemySpeedVariation);

        // Применяем вариацию
        int health = Math.max(10, Math.round(baseHealth * (1 + healthVariation)));
        int damage = Math.max(2, Math.round(baseDamage * (1 + damageVariation)));
        float speed = Math.max(0.5f, baseSpeed * (1 + speedVariation));
        int reward = Math.round(baseReward * (1 + (healthVariation + damageVariation + speedVariation) / 3));

        // Генерируем случайную позицию появления
        Vector2 spawnPosition = generateSpawnPosition();

        // Выбираем текстуру в зависимости от типа врага
        String texturePath;
        switch (enemyType) {
            case FAST:
                texturePath = "Texture\\Enemy\\Ship_06.png"; // Быстрый, но слабый враг
                break;
            case TANK:
                texturePath = "Texture\\Enemy\\Ship_04.png"; // Медленный, но с высоким здоровьем враг
                break;
            case ELITE:
                texturePath = "Texture\\Enemy\\Ship_03.png"; // Элитный враг с повышенными характеристиками
                break;
            default:
                texturePath = "Texture\\Enemy\\Ship_01.png"; // Обычный враг
                break;
        }

        // Создаем врага с определенными характеристиками
        EnemyActor enemy = new EnemyActor(texturePath);

        // Настраиваем характеристики
        enemy.setHealth(health);
        enemy.setCollisionDamage(damage);
        enemy.setSpeed(speed);
        enemy.setReward(reward);
        enemy.setPosition(spawnPosition.x, spawnPosition.y);

        // Устанавливаем цель (игрок)
        if (player != null) {
            enemy.setTarget(player);
        }

        // Устанавливаем обработчик смерти
        enemy.setDeathHandler(new EnemyActor.DeathHandler() {
            @Override
            public void onEnemyDeath(EnemyActor deadEnemy) {
                handleEnemyDeath(deadEnemy);
            }
        });

        // Логируем информацию о созданном враге
        LogHelper.log("EnemyManager", "Created " + enemyType + " enemy: HP=" + health +
            ", DMG=" + damage + ", SPD=" + speed + ", REW=" + reward + ", Texture: " + texturePath);

        return enemy;
    }

    /**
     * Определяет тип создаваемого врага на основе вероятностей
     */
    private EnemyType determineEnemyType() {
        float roll = MathUtils.random();

        // Увеличение шанса элитных врагов с ростом волны
        float currentEliteChance = eliteEnemyChance;
        if (currentWave >= LATE_GAME_WAVE) {
            currentEliteChance = eliteEnemyChance * 3; // Утроенный шанс элиты в поздней игре
        } else if (currentWave >= MID_GAME_WAVE) {
            currentEliteChance = eliteEnemyChance * 2; // Удвоенный шанс элиты в средней игре
        }

        if (roll < currentEliteChance) {
            return EnemyType.ELITE;
        } else if (roll < currentEliteChance + tankEnemyChance) {
            return EnemyType.TANK;
        } else if (roll < currentEliteChance + tankEnemyChance + fastEnemyChance) {
            return EnemyType.FAST;
        } else {
            return EnemyType.NORMAL;
        }
    }

    /**
     * Рассчитывает базовый урон с учетом текущей волны и стадии игры
     */
    private float calculateBaseDamageForWave() {
        float damageWaveMultiplier;

        // Выбираем множитель в зависимости от стадии игры
        if (currentWave >= LATE_GAME_WAVE) {
            damageWaveMultiplier = lateGameDamageScale;
        } else if (currentWave >= MID_GAME_WAVE) {
            damageWaveMultiplier = midGameDamageScale;
        } else {
            damageWaveMultiplier = earlyGameDamageScale;
        }

        // Базовая формула с прогрессивным увеличением
        return baseEnemyDamage * (float)Math.pow(damageWaveMultiplier, currentWave - 1);
    }

    private Vector2 generateSpawnPosition() {
        // Генерируем позицию в пределах настроенного диапазона
        float angle = MathUtils.random(0, 360);
        float distance = MathUtils.random(minSpawnDistance, maxSpawnDistance);

        float x = player.getX() + player.getWidth() / 2 + MathUtils.cosDeg(angle) * distance;
        float y = player.getY() + player.getHeight() / 2 + MathUtils.sinDeg(angle) * distance;

        return new Vector2(x, y);
    }

    /**
     * Рассчитывает базовое здоровье с учетом текущей волны
     */
    private float calculateBaseHealthForWave() {
        return baseEnemyHealth * (float)Math.pow(waveDifficultyMultiplier, currentWave - 1);
    }

    private void removeDeadEnemies() {
        Array<EnemyActor> deadEnemies = new Array<>();

        for (EnemyActor enemy : enemies) {
            if (enemy != null && enemy.isDead()) {
                deadEnemies.add(enemy);
            }
        }

        for (EnemyActor enemy : deadEnemies) {
            enemies.removeValue(enemy, true);
            enemy.remove();
        }
    }

    /**
     * Рассчитывает базовую скорость с учетом текущей волны
     */
    private float calculateBaseSpeedForWave() {
        // Более медленная прогрессия скорости для лучшего баланса
        return baseEnemySpeed * (1 + (currentWave - 1) * 0.05f);
    }

    /**
     * Рассчитывает базовую награду с учетом текущей волны
     */
    private float calculateBaseRewardForWave() {
        return baseEnemyReward * (float)Math.pow(waveDifficultyMultiplier, currentWave - 1);
    }

    /**
     * Возвращает множитель здоровья для определенного типа врага
     */
    private float getHealthMultiplierForType(EnemyType type) {
        switch (type) {
            case FAST: return fastEnemyHealthMult;
            case TANK: return tankEnemyHealthMult;
            case ELITE: return eliteEnemyHealthMult;
            default: return 1.0f;
        }
    }

    /**
     * Возвращает множитель урона для определенного типа врага
     */
    private float getDamageMultiplierForType(EnemyType type) {
        switch (type) {
            case FAST: return fastEnemyDamageMult;
            case TANK: return tankEnemyDamageMult;
            case ELITE: return eliteEnemyDamageMult;
            default: return 1.0f;
        }
    }

    /**
     * Возвращает множитель скорости для определенного типа врага
     */
    private float getSpeedMultiplierForType(EnemyType type) {
        switch (type) {
            case FAST: return fastEnemySpeedMult;
            case TANK: return tankEnemySpeedMult;
            case ELITE: return eliteEnemySpeedMult;
            default: return 1.0f;
        }
    }

    /**
     * Возвращает множитель награды для определенного типа врага
     */
    private float getRewardMultiplierForType(EnemyType type) {
        switch (type) {
            case FAST: return 0.8f; // Меньше опыта за быстрых врагов
            case TANK: return 1.5f; // Больше опыта за танков
            case ELITE: return 3.0f; // Значительно больше опыта за элиту
            default: return 1.0f;
        }
    }

    /**
     * Форсирует переход к следующей волне
     */
    public void forceNextWave() {
        endWave();
    }

    private int calculateEnemiesToSpawn() {
        // Увеличиваем количество противников с каждой волной, но с ограничением
        return Math.min(enemiesPerWave + (currentWave - 1), 8);
    }

    /**
     * Возвращает текущее время волны
     *
     * @return оставшееся время в секундах
     */
    public float getWaveTimer() {
        return waveTimer;
    }

    /**
     * Возвращает общую продолжительность текущей волны
     *
     * @return продолжительность в секундах
     */
    public float getWaveDuration() {
        return waveDuration;
    }

    /**
     * Возвращает время до начала следующей волны
     *
     * @return оставшееся время перерыва в секундах
     */
    public float getWaveBreakTimer() {
        return waveBreakTimer;
    }

    /**
     * Проверяет, идет ли сейчас перерыв между волнами
     *
     * @return true если идет перерыв
     */
    public boolean isInWaveBreak() {
        return isWaveBreak;
    }

    /**
     * Устанавливает продолжительность перерыва между волнами
     *
     * @param duration продолжительность в секундах
     */
    public void setWaveBreakDuration(float duration) {
        this.waveBreakDuration = duration;
    }

    /**
     * Обрабатывает уничтожение врага
     *
     * @param enemy уничтоженный враг
     */
    public void handleEnemyDeath(EnemyActor enemy) {
        if (enemy == null) return;


        // Уведомляем слушателя об убийстве
        if (killListener != null) {
            killListener.onEnemyKilled();
        }

        // Генерируем бонусы на месте смерти врага (случайный шанс)
        if (MathUtils.random() < 0.1f) { // 10% шанс
            spawnBonusAtLocation(enemy.getX(), enemy.getY());
        }
    }

    /**
     * Создает бонус на указанной позиции
     */
    private void spawnBonusAtLocation(float x, float y) {
        // Здесь можно реализовать появление бонусов (здоровье, энергия и т.д.)
        LogHelper.log("EnemyManager", "Bonus could be spawned at: " + x + ", " + y);
    }

    /**
     * Устанавливает диапазон расстояний для появления врагов
     * @param min минимальное расстояние от игрока
     * @param max максимальное расстояние от игрока
     */
    public void setSpawnDistanceRange(float min, float max) {
        this.minSpawnDistance = min;
        this.maxSpawnDistance = max;
        LogHelper.log("EnemyManager", "Spawn distance range set to " + min + "-" + max);
    }

    /**
     * Устанавливает максимальное расстояние, на котором враг может находиться от игрока
     * @param distance максимальное расстояние
     */
    public void setMaxEnemyDistance(float distance) {
        this.maxEnemyDistance = distance;
        LogHelper.log("EnemyManager", "Max enemy distance set to " + distance);
    }

    /**
     * Устанавливает максимальное количество врагов на экране
     * @param maxEnemies максимальное количество
     */
    public void setMaxEnemiesOnScreen(int maxEnemies) {
        this.maxEnemiesOnScreen = maxEnemies;
        LogHelper.log("EnemyManager", "Max enemies on screen set to " + maxEnemies);
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getEnemiesCount() {
        return enemies.size;
    }

    // Интерфейс для уведомления о событиях волны
    public interface WaveListener {
        void onWaveStart(int waveNumber);

        void onWaveEnd(int waveNumber);

        void onWaveTimerUpdate(float remainingTime, float totalTime);

        void onWaveBreakUpdate(float remainingTime, float totalTime);
    }

    // Интерфейс для уведомления об убийстве врага
    public interface EnemyKillListener {
        void onEnemyKilled();
    }

    @Override
    public void dispose() {
        for (EnemyActor enemy : enemies) {
            if (enemy != null) {
                enemy.remove();
            }
        }
        enemies.clear();
    }
}
