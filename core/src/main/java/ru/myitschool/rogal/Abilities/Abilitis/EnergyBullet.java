package ru.myitschool.rogal.Abilities.Abilitis;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;

import ru.myitschool.rogal.Abilities.Ability;
import ru.myitschool.rogal.Abilities.AbilityType;
import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.CustomHelpers.Helpers.HitboxHelper;
import ru.myitschool.rogal.CustomHelpers.Vectors.Vector2Helpers;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

public class EnergyBullet extends Ability {
    // Параметры снаряда
    private float projectileSpeed = 5.5f;
    private float projectileDamage = 15f;
    private float projectileLifespan = 5f;
    private float projectileSize = 0.05f;
    private String projectileTexturePath = "abilities/fireball.png";
    private float autoActivateInterval = 0.4f;
    private float timeSinceLastActivation = 0f;
    private int projectileCount = 1; // Количество пуль
    private float projectileSpreadAngle = 15f; // Угол разброса между пулями в градусах

    public EnergyBullet() {
        super("Energy Bullet", "Creates an energy projectile that automatically flies towards an enemy and deals damage\n" +
              "Level 3: Fires 2 projectiles\n" +
              "Level 5: Fires 3 projectiles",
              "abilities/fireball.png", 1.0f, 500f);

        this.abilityType = AbilityType.ATTACK;
        this.energyCost = 8f;
        this.cooldown = 0.8f;
        this.range = 550f;

        // Загружаем иконку для отображения в UI
        try {
            if (Gdx.files.internal("abilities/fireball.png").exists()) {
                this.icon = new Texture(Gdx.files.internal("abilities/fireball.png"));
                LogHelper.log("EnergyBullet", "Icon loaded successfully");
            } else {
                LogHelper.error("EnergyBullet", "Icon file doesn't exist");
            }
        } catch (Exception e) {
            LogHelper.error("EnergyBullet", "Failed to load icon", e);
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        // Обновляем таймер автоматической активации
        timeSinceLastActivation += delta;

        // Проверяем, нужно ли активировать способность
        if (timeSinceLastActivation >= autoActivateInterval) {
            timeSinceLastActivation = 0f;
            tryAutoActivate();
        }
    }

    @Override
    protected boolean use(Vector2 position) {
        if (owner == null || owner.getStage() == null) {
            LogHelper.error("EnergyBullet", "Cannot use ability: owner or stage is missing");
            return false;
        }

        // Получаем позицию игрока
        Vector2 startPos = new Vector2(owner.getX() + owner.getOriginX(),
                                     owner.getY() + owner.getOriginY());

        // Вычисляем базовое направление выстрела
        Vector2 baseDirection = new Vector2(position).sub(startPos).nor();

        try {
            // Создаем нужное количество снарядов с учетом угла разброса
            for (int i = 0; i < projectileCount; i++) {
                float angleOffset = 0;

                // Рассчитываем угол смещения для каждой пули
                if (projectileCount > 1) {
                    // Для 2 пуль: -spreadAngle/2, +spreadAngle/2
                    // Для 3 пуль: -spreadAngle, 0, +spreadAngle
                    if (projectileCount == 2) {
                        angleOffset = (i == 0) ? -projectileSpreadAngle/2 : projectileSpreadAngle/2;
                    } else {
                        angleOffset = (i - 1) * projectileSpreadAngle;
                    }
                }

                // Создаем новый вектор направления с учетом смещения угла
                Vector2 projectileDirection = new Vector2(baseDirection);
                projectileDirection.rotateDeg(angleOffset);

                // Вычисляем целевую позицию для этого направления
                Vector2 targetPos = new Vector2(startPos).add(
                    projectileDirection.x * range,
                    projectileDirection.y * range
                );

                // Создаем и добавляем снаряд
                BulletProjectile projectile = new BulletProjectile(
                    startPos,
                    targetPos,
                    projectileSpeed,
                    projectileDamage,
                    projectileLifespan
                );
                owner.getStage().addActor(projectile);
            }

            LogHelper.log("EnergyBullet", "Energy projectiles launched: " + projectileCount);
            return true;
        } catch (Exception e) {
            LogHelper.error("EnergyBullet", "Error creating projectiles", e);
            return false;
        }
    }

    @Override
    protected void updateActive(float delta) {
        isActive = false;
    }

    @Override
    protected void onLevelUp() {
        cooldown = Math.max(0.8f, cooldown - 0.3f);
        range += 20f;
        projectileDamage += 5f;

        // Увеличиваем количество пуль на определенных уровнях
        if (level == 3) {
            projectileCount = 2;
            LogHelper.log("EnergyBullet", "Level 3 upgrade: Now fires 2 projectiles!");
        } else if (level == 5) {
            projectileCount = 3;
            LogHelper.log("EnergyBullet", "Level 5 upgrade: Now fires 3 projectiles!");
        }
    }

    @Override
    protected Vector2 findTargetPosition() {
        if (owner == null || owner.getStage() == null) return null;

        Vector2 playerPos = new Vector2(owner.getX() + owner.getOriginX(),
                                      owner.getY() + owner.getOriginY());
        Stage stage = owner.getStage();
        Array<Actor> actors = stage.getActors();
        Array<EnemyActor> enemies = new Array<>();

        // Собираем всех живых врагов
        for (Actor actor : actors) {
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;
                if (enemy.getCurrentHealth() > 0) {
                    enemies.add(enemy);
                }
            }
        }

        if (enemies.size > 0) {
            float minDistance = Float.MAX_VALUE;
            Vector2 nearestEnemyPos = null;

            // Находим ближайшего врага
            for (EnemyActor enemy : enemies) {
                Vector2 enemyPos = new Vector2(enemy.getX() + enemy.getOriginX(),
                                             enemy.getY() + enemy.getOriginY());
                float distance = playerPos.dst(enemyPos);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearestEnemyPos = enemyPos;
                }
            }

            if (nearestEnemyPos != null) {
                LogHelper.debug("EnergyBullet", "Found nearest enemy at distance: " + minDistance);
                return nearestEnemyPos;
            }
        }

        return null;
    }

    protected void tryAutoActivate() {
        if (owner == null) return;

        // Проверяем, есть ли враги на сцене
        Stage stage = owner.getStage();
        boolean enemiesExist = false;

        if (stage != null) {
            for (Actor actor : stage.getActors()) {
                if (actor instanceof EnemyActor) {
                    enemiesExist = true;
                    break;
                }
            }
        }

        if (!enemiesExist) {
            LogHelper.debug("EnergyBullet", "No enemies on stage");
            return;
        }

        // Проверяем, хватает ли энергии
        if (energyCost > 0 && owner.getCurrentEnergy() < energyCost) {
            LogHelper.debug("EnergyBullet", "Not enough energy");
            return;
        }

        // Находим позицию ближайшего врага
        Vector2 targetPosition = findTargetPosition();

        if (targetPosition != null) {
            boolean success = activate(targetPosition);
            if (success) {
                LogHelper.log("EnergyBullet", "Auto-used successfully");
            } else {
                LogHelper.debug("EnergyBullet", "Failed to use ability");
            }
        }
    }

    /**
     * Внутренний класс для снаряда
     */
    private class BulletProjectile extends Actor {
        private Vector2 direction;
        private final float speed;
        private final float damage;
        private final float maxLifespan;
        private float lifespan;
        private final TextureRegion texture;
        private final Polygon hitbox;
        private EnemyActor targetEnemy;
        private final float turnSpeed = 3.0f;
        private Texture projectileTexture;

        public BulletProjectile(Vector2 position, Vector2 target, float speed, float damage, float lifespan) {
            this.speed = speed;
            this.damage = damage;
            this.maxLifespan = lifespan;
            this.lifespan = lifespan;

            // Загружаем текстуру снаряда
            projectileTexture = new Texture(Gdx.files.internal(projectileTexturePath));
            this.texture = new TextureRegion(projectileTexture);

            // Настраиваем размеры и позицию
            setWidth(texture.getRegionWidth() * projectileSize);
            setHeight(texture.getRegionHeight() * projectileSize);
            setPosition(position.x - getWidth()/2, position.y - getHeight()/2);
            setOrigin(getWidth()/2, getHeight()/2);

            // Рассчитываем начальное направление
            this.direction = new Vector2(target).sub(position).nor();
            float angle = Vector2Helpers.getRotationByVector(direction.x, direction.y);
            setRotation(angle);

            // Создаем хитбокс
            hitbox = HitboxHelper.createCircleHitbox(getWidth()/2, 8);
            hitbox.setOrigin(getWidth()/2, getHeight()/2);
            updateHitbox();

            // Находим цель для отслеживания
            findTarget();
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.draw(texture, getX(), getY(), getOriginX(), getOriginY(),
                      getWidth(), getHeight(), 1, 1, getRotation()+90);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            // Уменьшаем время жизни
            lifespan -= delta;
            if (lifespan <= 0) {
                remove();
                return;
            }

            // Анимация уменьшения размера
            if (lifespan < maxLifespan * 0.2f) {
                float scale = lifespan / (maxLifespan * 0.2f);
                setScale(scale);
            }

            // Отслеживание цели
            if (targetEnemy != null && targetEnemy.getStage() != null) {
                if (targetEnemy.getCurrentHealth() <= 0) {
                    targetEnemy = null;
                    findTarget();
                } else {
                    Vector2 projectilePos = new Vector2(getX() + getOriginX(), getY() + getOriginY());
                    Vector2 enemyPos = new Vector2(targetEnemy.getX() + targetEnemy.getOriginX(),
                                                 targetEnemy.getY() + targetEnemy.getOriginY());
                    Vector2 newDirection = new Vector2(enemyPos).sub(projectilePos).nor();

                    // Плавный поворот к цели
                    direction.lerp(newDirection, delta * turnSpeed);
                    direction.nor();

                    float angle = Vector2Helpers.getRotationByVector(direction.x, direction.y);
                    setRotation(angle);
                }
            } else {
                findTarget();
            }

            // Движение снаряда
            moveBy(direction.x * speed, direction.y * speed);
            updateHitbox();

            // Проверка коллизий
            checkCollisions();
        }

        private boolean findTarget() {
            Stage stage = owner.getStage();
            if (stage == null) return false;

            Vector2 projectilePos = new Vector2(getX() + getOriginX(), getY() + getOriginY());
            float closestDistance = Float.MAX_VALUE;
            targetEnemy = null;

            for (Actor actor : stage.getActors()) {
                if (actor instanceof EnemyActor) {
                    EnemyActor enemy = (EnemyActor) actor;
                    if (enemy.getCurrentHealth() <= 0) continue;

                    Vector2 enemyPos = new Vector2(enemy.getX() + enemy.getOriginX(),
                                                 enemy.getY() + enemy.getOriginY());
                    float distance = projectilePos.dst(enemyPos);

                    if (distance < closestDistance) {
                        closestDistance = distance;
                        targetEnemy = enemy;
                    }
                }
            }

            return targetEnemy != null;
        }

        private void updateHitbox() {
            hitbox.setPosition(getX(), getY());
            hitbox.setRotation(Vector2Helpers.getRotationByVector(direction));
        }

        private void checkCollisions() {
            Stage stage = getStage();
            if (stage == null) return;

            for (Actor actor : stage.getActors()) {
                if (actor instanceof EnemyActor) {
                    EnemyActor enemy = (EnemyActor) actor;
                    if (Intersector.overlapConvexPolygons(hitbox, enemy.getHitbox())) {
                        enemy.takeDamage(Math.round(damage));
                        remove();
                        break;
                    }
                }
            }
        }

        @Override
        public boolean remove() {
            if (projectileTexture != null) {
                projectileTexture.dispose();
                projectileTexture = null;
            }
            return super.remove();
        }
    }

    @Override
    public void dispose() {
        if (icon != null) {
            icon.dispose();
            icon = null;
        }
        super.dispose();
    }
}
