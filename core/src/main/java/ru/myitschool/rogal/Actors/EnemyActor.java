package ru.myitschool.rogal.Actors;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

import ru.myitschool.rogal.CustomHelpers.Helpers.HitboxHelper;
import ru.myitschool.rogal.CustomHelpers.Vectors.Vector2Helpers;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

public class EnemyActor extends Actor {
    // Текстура и полигон для столкновений
    private final TextureRegion texture;
    private final Polygon hitbox;

    // Атрибуты врага
    private int maxHealth = 30;
    private int currentHealth = 30;
    private float baseSpeed = 0.7f; // Базовая скорость врага
    private float currentSpeed; // Текущая скорость с учетом дебаффов
    private int collisionDamage = 10;
    private int rewardExp = 15; // Опыт за убийство

    // Время неуязвимости после получения урона
    private float invulnerabilityTime = 0f;
    private static final float MAX_INVULNERABILITY_TIME = 0.0f;

    // Анимация и состояние
    public static final float SCALE = 0.08f;
    private boolean isDead = false;

    // Отслеживание игрока
    private PlayerActor targetPlayer;
    private final Vector2 randomDirection = new Vector2(1, 0);

    private boolean isSlowed = false; // Флаг замедления

    // Расстояние до игрока
    private float distanceToPlayer = 0;
    private DeathHandler deathHandler;
    private BehaviorType behaviorType = BehaviorType.DIRECT;
    private float behaviorTimer = 0;
    private float behaviorDuration = 3.0f;
    private boolean hasSpecialAttack = false;
    private float specialAttackCooldown = 0;
    /**
     * Новый конструктор для совместимости с EnemyManager
     * @param health здоровье
     * @param damage урон
     * @param speed скорость
     * @param reward награда за убийство
     * @param position начальная позиция
     * @param target цель (игрок)
     */
    public EnemyActor(int health, int damage, float speed, int reward, Vector2 position, PlayerActor target) {
        // Используем базовую текстуру для противника
        Texture tex = new Texture("Damaged_Ship_03.png");
        texture = new TextureRegion(tex);

        // Настройка размеров и начального положения
        setWidth(texture.getTexture().getWidth() * SCALE);
        setHeight(texture.getTexture().getHeight() * SCALE);
        setOriginX(getWidth() / 2);
        setOriginY(getHeight() / 2);

        // Установка позиции
        setPosition(position.x, position.y);

        // Создание хитбокса
        hitbox = HitboxHelper.createHitboxFromTexture(tex, SCALE, 20);
        hitbox.setOrigin(getOriginX(), getOriginY());


        // Установка характеристик
        this.maxHealth = health;
        this.currentHealth = health;
        this.collisionDamage = damage;
        this.baseSpeed = speed;
        this.currentSpeed = speed;
        this.rewardExp = reward;

        // Установка цели
        this.targetPlayer = target;

        LogHelper.log("EnemyActor", "Created new enemy with health: " + health);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (isDead) return;

        // Обновление таймера неуязвимости
        if (invulnerabilityTime > 0) {
            invulnerabilityTime -= delta;
        }

        // Поиск игрока, если еще не найден
        if (targetPlayer == null) {
            findPlayer();
        }

        // Движение врага
        if (targetPlayer != null) {
            moveTowardsPlayer(delta);
        }

        // Обновление хитбокса
        updateHitbox();

        // Обновляем таймер поведения
        behaviorTimer += delta;
        if (behaviorTimer >= behaviorDuration) {
            updateBehavior();
        }

        // Обновляем кулдаун специальной атаки
        if (hasSpecialAttack && specialAttackCooldown > 0) {
            specialAttackCooldown -= delta;

            // Если кулдаун истек и игрок в зоне поражения, используем специальную атаку
            if (specialAttackCooldown <= 0 && targetPlayer != null && distanceToPlayer < 200) {
                performSpecialAttack();
                specialAttackCooldown = MathUtils.random(5.0f, 10.0f);
            }
        }

        // Выбираем направление движения в зависимости от типа поведения
        chooseMovementDirection(delta);
    }

    /**
     * Стандартный конструктор для существующего кода
     */
    public EnemyActor(String texturePath) {
        Texture tex = new Texture(texturePath);
        texture = new TextureRegion(tex);

        // Настройка размеров и начального положения
        setWidth(texture.getTexture().getWidth() * SCALE);
        setHeight(texture.getTexture().getHeight() * SCALE);
        setOriginX(getWidth() / 2);
        setOriginY(getHeight() / 2);

        // Создание хитбокса с использованием утилиты
        hitbox = HitboxHelper.createHitboxFromTexture(tex, SCALE, 10);
        hitbox.setOrigin(getOriginX(), getOriginY());

        // Начальное случайное направление
        float randomAngle = MathUtils.random(360f);
        randomDirection.set(MathUtils.cosDeg(randomAngle), MathUtils.sinDeg(randomAngle)).nor();

        this.currentSpeed = baseSpeed;
    }

    /**
     * Дополнительный конструктор для создания врага с упрощенным хитбоксом
     * @param texturePath путь к файлу текстуры
     * @param hitboxType тип хитбокса: "circle", "triangle" или "rectangle"
     */
    public EnemyActor(String texturePath, String hitboxType) {
        Texture tex = new Texture(texturePath);
        texture = new TextureRegion(tex);

        // Настройка размеров и начального положения
        setWidth(texture.getTexture().getWidth() * SCALE);
        setHeight(texture.getTexture().getHeight() * SCALE);
        setOriginX(getWidth() / 2);
        setOriginY(getHeight() / 2);

        // Создание хитбокса в зависимости от выбранного типа
        switch (hitboxType.toLowerCase()) {
            case "circle":
                float radius = Math.min(getWidth(), getHeight()) / 2;
                hitbox = HitboxHelper.createCircleHitbox(radius, 8);
                break;
            case "triangle":
                hitbox = HitboxHelper.createTriangleHitbox(getWidth(), getHeight());
                break;
            case "rectangle":
            default:
                hitbox = HitboxHelper.createRectangleHitbox(getWidth(), getHeight(), getOriginX(), getOriginY());
                break;
        }

        hitbox.setOrigin(getOriginX(), getOriginY());


        // Начальное случайное направление
        float randomAngle = MathUtils.random(360f);
        randomDirection.set(MathUtils.cosDeg(randomAngle), MathUtils.sinDeg(randomAngle)).nor();

        this.currentSpeed = baseSpeed;
    }

    /**
     * Движение в направлении игрока
     * @param delta время между кадрами
     */
    private void moveTowardsPlayer(float delta) {
        // Проверяем, существует ли игрок
        if (targetPlayer == null) {
            return;
        }

        // Проверяем, существует ли еще игрок
        if (targetPlayer.getStage() == null) {
            targetPlayer = null;
            return;
        }

        // Рассчитываем вектор направления к игроку
        float targetX = targetPlayer.getX() + targetPlayer.getWidth() / 2;
        float targetY = targetPlayer.getY() + targetPlayer.getHeight() / 2;
        float enemyX = getX() + getWidth() / 2;
        float enemyY = getY() + getHeight() / 2;

        // Расстояние до игрока
        distanceToPlayer = Vector2.dst(enemyX, enemyY, targetX, targetY);

        // Создаем вектор направления к игроку и нормализуем его
        Vector2 direction = new Vector2(targetX - enemyX, targetY - enemyY).nor();

        // Вычисляем угол поворота (в градусах)
        float angle = Vector2Helpers.getRotationByVector(direction.x, direction.y);
        setRotation(angle);

        // Перемещение в сторону игрока
        moveBy(direction.x * currentSpeed, direction.y * currentSpeed);

        // Проверка столкновения с игроком (для совместимости с Enemy)
        if (distanceToPlayer < 32) {
            targetPlayer.takeDamage(collisionDamage);
        }
    }
    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (texture != null) {
            batch.draw(
                texture,
                getX(),
                getY(),
                getOriginX(),
                getOriginY(),
                getWidth(),
                getHeight(),
                0.75f,
                0.75f,
                getRotation()-90
            );
        }
    }

    /**
     * Действия при смерти врага
     */
    public void die() {
        // Устанавливаем флаг смерти
        isDead = true;

        // Удаляем со сцены
        remove();

        // Даем опыт игроку, если он существует
        if (targetPlayer != null) {
            int calculatedExp = calculateExperienceReward();
            targetPlayer.addExperience(calculatedExp);
            LogHelper.log("EnemyActor", "Enemy died, calculated reward: " + calculatedExp);
        }

        // Уведомляем обработчик о смерти
        if (deathHandler != null) {
            deathHandler.onEnemyDeath(this);
        }
    }

    /**
     * Обновляет позицию хитбокса
     */
    private void updateHitbox() {
        hitbox.setPosition(getX(), getY());
        hitbox.setRotation(getRotation() + 90);
    }

    /**
     * Ищет игрока на сцене
     */
    private void findPlayer() {
        Stage stage = getStage();
        if (stage == null) return;

        for (Actor actor : stage.getActors()) {
            if (actor instanceof PlayerActor) {
                targetPlayer = (PlayerActor) actor;
                break;
            }
        }
    }

    /**
     * Устанавливает обработчик смерти врага
     *
     * @param handler обработчик
     */
    public void setDeathHandler(DeathHandler handler) {
        this.deathHandler = handler;
    }


    /**
     * Наносит урон врагу
     * @param damage величина урона
     * @return true если урон был нанесен, false если враг неуязвим
     */
    public boolean takeDamage(int damage) {
        // Проверка на неуязвимость
        if (invulnerabilityTime > 0) {
            return false;
        }

        // Наносим урон
        currentHealth = Math.max(0, currentHealth - damage);

        // Устанавливаем период неуязвимости
        invulnerabilityTime = MAX_INVULNERABILITY_TIME;

        // Проверяем, жив ли враг
        if (currentHealth <= 0) {
            die();
            return true;
        }

        return true;
    }

    /**
     * Устанавливает тип поведения ИИ
     *
     * @param type тип поведения
     */
    public void setBehaviorType(BehaviorType type) {
        this.behaviorType = type;
        this.behaviorTimer = 0;

        // Генерируем длительность поведения
        this.behaviorDuration = MathUtils.random(2.0f, 5.0f);
    }

    /**
     * Рассчитывает награду опыта в зависимости от характеристик врага
     * @return количество опыта
     */
    private int calculateExperienceReward() {
        // Базовая награда
        int baseReward = rewardExp;

        // Множители для различных характеристик
        float healthMultiplier = maxHealth / 30f; // Относительно базового здоровья в 30
        float damageMultiplier = collisionDamage / 10f; // Относительно базового урона в 10
        float speedMultiplier = baseSpeed / 0.7f; // Относительно базовой скорости в 0.7

        // Рассчитываем финальную награду
        // Формула: базовая награда * (0.5 * множитель здоровья + 0.3 * множитель урона + 0.2 * множитель скорости)
        float totalMultiplier = 0.5f * healthMultiplier + 0.3f * damageMultiplier + 0.2f * speedMultiplier;

        // Минимальная награда - 5 опыта
        int finalReward = Math.max(5, Math.round(baseReward * totalMultiplier));

        LogHelper.log("EnemyActor", "Experience reward calculation: base=" + baseReward
            + ", health=" + maxHealth + "x" + healthMultiplier
            + ", damage=" + collisionDamage + "x" + damageMultiplier
            + ", speed=" + baseSpeed + "x" + speedMultiplier
            + ", total=" + finalReward);

        return finalReward;
    }

    /**
     * Проверяет, мертв ли враг (для совместимости с Enemy)
     */
    public boolean isDead() {
        return isDead;
    }

    /**
     * Возвращает процент оставшегося здоровья (для совместимости с Enemy)
     */
    public float getHealthPercentage() {
        return (float)currentHealth / maxHealth;
    }


    /**
     * Возвращает урон от столкновения с врагом
     */
    public int getCollisionDamage() {
        return collisionDamage;
    }

    /**
     * Возвращает хитбокс врага
     */
    public Polygon getHitbox() {
        return this.hitbox;
    }

    /**
     * Устанавливает здоровье врага
     */
    public void setHealth(int health) {
        this.maxHealth = health;
        this.currentHealth = health;
    }

    public void setSpeed(float speed) {
        this.baseSpeed = speed;
        this.currentSpeed = speed;
    }

    public void setCollisionDamage(int damage) {
        this.collisionDamage = damage;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    /**
     * Устанавливает опыт за убийство врага
     */
    public void setReward(int reward) {
        this.rewardExp = reward;
    }

    /**
     * Применяет эффект замедления к врагу
     * @param slowFactor коэффициент замедления (0-1)
     */
    public void applySlowEffect(float slowFactor) {
        if (!isSlowed) {
            isSlowed = true;
            float newSpeed = baseSpeed * (1 - slowFactor);
            currentSpeed = Math.max(0.1f, newSpeed); // Минимальная скорость 0.1
            LogHelper.log("EnemyActor", "Speed reduced from " + baseSpeed + " to " + currentSpeed);
        }
    }

    /**
     * Сбрасывает скорость врага к нормальной
     */
    public void resetSpeed() {
        if (isSlowed) {
            currentSpeed = baseSpeed;
            isSlowed = false;
            LogHelper.log("EnemyActor", "Speed reset to normal: " + baseSpeed);
        }
    }

    /**
     * Возвращает текущую скорость врага
     * @return текущая скорость
     */
    public float getCurrentSpeed() {
        return currentSpeed;
    }

    /**
     * Включает специальную атаку для врага (если доступна)
     *
     * @param hasAttack наличие специальной атаки
     */
    public void setHasSpecialAttack(boolean hasAttack) {
        this.hasSpecialAttack = hasAttack;
        this.specialAttackCooldown = MathUtils.random(3.0f, 8.0f);
    }

    /**
     * Обновляет поведение врага, случайно меняя тип
     */
    private void updateBehavior() {
        behaviorTimer = 0;
        behaviorDuration = MathUtils.random(3.0f, 6.0f);

        // Генерируем случайное поведение, с большим весом для прямого преследования
        float roll = MathUtils.random();

        if (currentHealth < maxHealth * 0.3f && roll < 0.4f) {
            // При малом здоровье повышаем шанс отступления
            behaviorType = BehaviorType.RETREAT;
        } else if (roll < 0.6f) {
            behaviorType = BehaviorType.DIRECT;
        } else if (roll < 0.8f) {
            behaviorType = BehaviorType.FLANKING;
        } else {
            behaviorType = BehaviorType.AMBUSH;
        }
    }

    /**
     * Выбирает направление движения в зависимости от типа поведения
     */
    private void chooseMovementDirection(float delta) {
        // Проверка на null, чтобы избежать NullPointerException
        if (targetPlayer == null) {
            // Если игрок не найден, делаем случайные движения
            if (MathUtils.randomBoolean(0.05f)) {
                randomDirection.set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor();
            }
            return;
        }

        Vector2 playerPosition = new Vector2(
            targetPlayer.getX() + targetPlayer.getWidth() / 2,
            targetPlayer.getY() + targetPlayer.getHeight() / 2
        );

        Vector2 myPosition = new Vector2(
            getX() + getWidth() / 2,
            getY() + getHeight() / 2
        );

        Vector2 direction = new Vector2();

        switch (behaviorType) {
            case DIRECT:
                // Прямое преследование игрока
                direction.set(playerPosition).sub(myPosition).nor();
                break;

            case FLANKING:
                // Пытаемся зайти сбоку от игрока
                direction.set(playerPosition).sub(myPosition);
                float length = direction.len();
                direction.nor();

                // Поворачиваем вектор на 45-90 градусов
                float angle = MathUtils.random(45, 90);
                if (MathUtils.randomBoolean()) angle = -angle;
                direction.rotate(angle);

                // Если враг достаточно близко к игроку, атакуем напрямую
                if (length < 100) {
                    direction.set(playerPosition).sub(myPosition).nor();
                }
                break;

            case AMBUSH:
                // Если враг далеко от игрока, то ждем на месте
                if (distanceToPlayer > 300) {
                    direction.set(0, 0);
                } else {
                    // Если игрок приблизился, начинаем преследование
                    direction.set(playerPosition).sub(myPosition).nor();
                    behaviorType = BehaviorType.DIRECT;
                }
                break;

            case RETREAT:
                // Отступаем от игрока
                direction.set(myPosition).sub(playerPosition).nor();

                // Если враг достаточно далеко, меняем поведение
                if (distanceToPlayer > 400) {
                    updateBehavior();
                }
                break;
        }

        // Применяем выбранное направление
        randomDirection.set(direction);
    }

    /**
     * Выполняет специальную атаку врага
     */
    private void performSpecialAttack() {
        // Здесь можно реализовать различные специальные атаки
        // Например, выстрел снарядом или временное ускорение
        LogHelper.log("EnemyActor", "Enemy performed special attack");
    }

    /**
     * Устанавливает цель для врага
     *
     * @param target игрок, на которого будет направлен враг
     */
    public void setTarget(PlayerActor target) {
        this.targetPlayer = target;
    }

    // Типы поведения ИИ врагов
    public enum BehaviorType {
        DIRECT,     // Прямое преследование игрока
        FLANKING,   // Попытка зайти сбоку
        AMBUSH,     // Подготовка засады
        RETREAT     // Отступление при малом здоровье
    }

    // Интерфейс для обработки смерти врага
    public interface DeathHandler {
        void onEnemyDeath(EnemyActor enemy);
    }
}
