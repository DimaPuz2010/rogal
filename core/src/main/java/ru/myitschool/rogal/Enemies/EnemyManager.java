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
    private int enemiesPerWave;
    private float waveDifficultyMultiplier;
    
    // Настройки радиуса появления
    private float minSpawnDistance = 300f;
    private float maxSpawnDistance = 600f;
    
    // Настройки переспавна
    private float maxEnemyDistance = 2000f; // Максимальное расстояние от игрока
    private float respawnCheckInterval = 5f; // Интервал проверки расстояния
    private float respawnCheckTimer = 0f;
    
    // Настройки сложности
    private float baseEnemyHealth;
    private float baseEnemyDamage;
    private float baseEnemySpeed;
    private float baseEnemyReward;
    
    // Настройки разнообразия врагов
    private float enemyHealthVariation = 0.2f; // ±20% вариация здоровья
    private float enemyDamageVariation = 0.15f; // ±15% вариация урона
    private float enemySpeedVariation = 0.25f; // ±25% вариация скорости
    
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
    private float eliteEnemyChance = 0.05f; // 5% шанс элитного врага
    
    // Множители для различных типов врагов
    private float fastEnemyHealthMult = 0.7f;
    private float fastEnemyDamageMult = 0.8f;
    private float fastEnemySpeedMult = 1.7f;
    
    private float tankEnemyHealthMult = 2.2f;
    private float tankEnemyDamageMult = 1.0f;
    private float tankEnemySpeedMult = 0.6f;
    
    private float eliteEnemyHealthMult = 2.0f;
    private float eliteEnemyDamageMult = 1.8f;
    private float eliteEnemySpeedMult = 1.2f;
    
    // Множители прогрессии урона для разных волн
    private float earlyGameDamageScale = 1.1f;  // Волны 1-5
    private float midGameDamageScale = 1.3f;    // Волны 6-15
    private float lateGameDamageScale = 1.5f;   // Волны 16+
    
    // Пороговые значения волн
    private static final int MID_GAME_WAVE = 6;
    private static final int LATE_GAME_WAVE = 16;
    
    // Лимит на количество врагов
    private int maxEnemiesOnScreen = 12;
    
    public EnemyManager(PlayerActor player, Stage stage) {
        this.player = player;
        this.stage = stage;
        this.enemies = new Array<>();
        
        // Инициализация настроек
        this.spawnTimer = 0;
        this.spawnInterval = 3.5f; // Сократил интервал между появлениями
        this.currentWave = 1;
        this.enemiesPerWave = 3; // Больше врагов в начальной волне
        this.waveDifficultyMultiplier = 1.15f; // Увеличил множитель сложности
        
        // Базовые характеристики противников
        this.baseEnemyHealth = 80f; // Снижено для более быстрого убийства в начале
        this.baseEnemyDamage = 8f; // Снижено для баланса
        this.baseEnemySpeed = 0.8f; // Немного быстрее
        this.baseEnemyReward = 15f; // Увеличена награда
        
        LogHelper.log("EnemyManager", "Enemy manager initialized with balanced settings");
    }
    
    public void update(float delta) {
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
        
        // Создаем врага с определенными характеристиками
        EnemyActor enemy = new EnemyActor(health, damage, speed, reward, spawnPosition, player);
        
        // Логируем информацию о созданном враге
        LogHelper.log("EnemyManager", "Created " + enemyType + " enemy: HP=" + health + 
                     ", DMG=" + damage + ", SPD=" + speed + ", REW=" + reward);
        
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
     * Рассчитывает базовое здоровье с учетом текущей волны
     */
    private float calculateBaseHealthForWave() {
        return baseEnemyHealth * (float)Math.pow(waveDifficultyMultiplier, currentWave - 1);
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
    
    private Vector2 generateSpawnPosition() {
        // Генерируем позицию в пределах настроенного диапазона
        float angle = MathUtils.random(0, 360);
        float distance = MathUtils.random(minSpawnDistance, maxSpawnDistance);
        
        float x = player.getX() + player.getWidth() / 2 + MathUtils.cosDeg(angle) * distance;
        float y = player.getY() + player.getHeight() / 2 + MathUtils.sinDeg(angle) * distance;
        
        return new Vector2(x, y);
    }
    
    private int calculateEnemiesToSpawn() {
        // Увеличиваем количество противников с каждой волной, но с ограничением
        return Math.min(enemiesPerWave + (currentWave - 1), 8);
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
    
    public void nextWave() {
        currentWave++;
        
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
        
        LogHelper.log("EnemyManager", "Starting wave " + currentWave + 
                       ", spawn interval: " + spawnInterval + 
                       ", max enemies: " + maxEnemiesOnScreen);
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