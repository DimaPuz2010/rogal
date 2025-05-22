package ru.myitschool.rogal.Abilities.Abilitis;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;

import ru.myitschool.rogal.Abilities.Ability;
import ru.myitschool.rogal.Abilities.AbilityType;
import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.CustomHelpers.Helpers.HitboxHelper;
import ru.myitschool.rogal.CustomHelpers.Vectors.Vector2Helpers;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

public class EnergyBullet extends Ability {
    // Параметры снаряда
    private final float projectileSpeed = 5.5f;
    private float projectileDamage = 15f;
    private final float projectileLifespan = 5f;
    private final float projectileSize = 0.05f;
    private final String projectileTexturePath = "abilities/fireball.png";
    private final float autoActivateInterval = 0.4f;
    private float timeSinceLastActivation = 0f;
    private int projectileCount = 1; // Количество пуль
    private final float projectileSpreadAngle = 15f; // Угол разброса между пулями в градусах

    public EnergyBullet() {
        super("Energy Bullet", "Создаёт энергетический снаряд, который автоматически летит к врагу и наносит урон\n",
              "abilities/fireball.png", 1.0f, 500f);

        this.abilityType = AbilityType.ATTACK;
        this.energyCost = 8f;
        this.cooldown = 0.8f;
        this.range = 550f;

        if (Gdx.files.internal("abilities/fireball.png").exists()) {
            this.icon = new Texture(Gdx.files.internal("abilities/fireball.png"));
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        timeSinceLastActivation += delta;

        if (timeSinceLastActivation >= autoActivateInterval) {
            timeSinceLastActivation = 0f;
            tryAutoActivate(delta);
        }
    }

    @Override
    protected boolean use(Vector2 position) {
        if (owner == null || owner.getStage() == null) {
            return false;
        }

        Vector2 startPos = new Vector2(owner.getX() + owner.getOriginX(),
                                     owner.getY() + owner.getOriginY());

        Vector2 baseDirection = new Vector2(position).sub(startPos).nor();

        try {
            for (int i = 0; i < projectileCount; i++) {
                float angleOffset = 0;

                if (projectileCount > 1) {
                    if (projectileCount == 2) {
                        angleOffset = (i == 0) ? -projectileSpreadAngle/2 : projectileSpreadAngle/2;
                    } else {
                        angleOffset = (i - 1) * projectileSpreadAngle;
                    }
                }

                Vector2 projectileDirection = new Vector2(baseDirection);
                projectileDirection.rotateDeg(angleOffset);

                Vector2 targetPos = new Vector2(startPos).add(
                    projectileDirection.x * range,
                    projectileDirection.y * range
                );

                BulletProjectile projectile = new BulletProjectile(
                    startPos,
                    targetPos,
                    projectileSpeed,
                    projectileDamage,
                    projectileLifespan
                );
                owner.getStage().addActor(projectile);
            }

            return true;
        } catch (Exception e) {
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
        projectileDamage += 2.5f;
        if (level == 3) {
            projectileCount += 2;
            projectileDamage += 5;
        } else if (level == 5) {
            projectileCount += 1;
            projectileDamage += 10;
        }

    }

    @Override
    public String getDescription() {
        if (level == 1){
            return description;
        } else if (level+1 == 3){
            return "Выпускает 2 снаряда";
        } else if (level+1 == 4) {
            return "Улучшает способность";
        } else if (level+1 == 5) {
            return "Выпускает 3 снаряда и при попадании создаёт мини-молнию, поражающую ближайших врагов";
        }
        return description;
    }

    @Override
    protected Vector2 findTargetPosition() {
        if (owner == null || owner.getStage() == null) return null;

        Vector2 playerPos = new Vector2(owner.getX() + owner.getOriginX(),
                                      owner.getY() + owner.getOriginY());
        Stage stage = owner.getStage();
        Array<Actor> actors = stage.getActors();
        Array<EnemyActor> enemies = new Array<>();

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
                return nearestEnemyPos;
            }
        }

        return null;
    }

    protected void tryAutoActivate() {
        if (owner == null) return;

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
            return;
        }

        if (energyCost > 0 && owner.getCurrentEnergy() < energyCost) {
            return;
        }

        Vector2 targetPosition = findTargetPosition();

        if (targetPosition != null) {
            activate(targetPosition);
        }
    }

    /**
     * Внутренний класс для снаряда
     */
    private class BulletProjectile extends Actor {
        private final Vector2 direction;
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

            projectileTexture = new Texture(Gdx.files.internal(projectileTexturePath));
            this.texture = new TextureRegion(projectileTexture);

            setWidth(texture.getRegionWidth() * projectileSize);
            setHeight(texture.getRegionHeight() * projectileSize);
            setPosition(position.x - getWidth()/2, position.y - getHeight()/2);
            setOrigin(getWidth()/2, getHeight()/2);

            this.direction = new Vector2(target).sub(position).nor();
            float angle = Vector2Helpers.getRotationByVector(direction.x, direction.y);
            setRotation(angle);

            hitbox = HitboxHelper.createCircleHitbox(getWidth()/2, 8);
            hitbox.setOrigin(getWidth()/2, getHeight()/2);
            updateHitbox();

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

            lifespan -= delta;
            if (lifespan <= 0) {
                remove();
                return;
            }

            if (lifespan < maxLifespan * 0.2f) {
                float scale = lifespan / (maxLifespan * 0.2f);
                setScale(scale);
            }

            if (targetEnemy != null && targetEnemy.getStage() != null) {
                if (targetEnemy.getCurrentHealth() <= 0) {
                    targetEnemy = null;
                    findTarget();
                } else {
                    Vector2 projectilePos = new Vector2(getX() + getOriginX(), getY() + getOriginY());
                    Vector2 enemyPos = new Vector2(targetEnemy.getX() + targetEnemy.getOriginX(),
                                                 targetEnemy.getY() + targetEnemy.getOriginY());
                    Vector2 newDirection = new Vector2(enemyPos).sub(projectilePos).nor();

                    direction.lerp(newDirection, delta * turnSpeed);
                    direction.nor();

                    float angle = Vector2Helpers.getRotationByVector(direction.x, direction.y);
                    setRotation(angle);
                }
            } else {
                findTarget();
            }

            moveBy(direction.x * speed, direction.y * speed);
            updateHitbox();

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

                        // На 5 уровне создаем мини-молнию при попадании
                        if (level >= 5) {
                            createLightningChainEffect(enemy);
                        }

                        remove();
                        break;
                    }
                }
            }
        }

        /**
         * Создает эффект мини-молнии при попадании на 5 уровне
         *
         * @param hitEnemy враг, в которого попал снаряд
         */
        private void createLightningChainEffect(EnemyActor hitEnemy) {
            if (owner == null || owner.getStage() == null) return;

            // Параметры мини-молнии
            float miniLightningDamage = damage * 0.5f; // Половина урона от пули
            float miniLightningRange = 150f; // Меньшая дальность, чем у обычной молнии
            int miniLightningJumps = 2; // Меньше перескоков
            float miniLightningFalloff = 0.6f; // Быстрее падает урон

            // Используем статический метод из LightningChainAbility для создания мини-молнии
            LightningChainAbility.createMiniLightning(
                EnergyBullet.this, // передаем текущую способность
                hitEnemy, // враг, в которого попал снаряд
                miniLightningDamage, // урон молнии
                miniLightningRange, // дальность
                miniLightningJumps, // количество перескоков
                miniLightningFalloff // коэффициент уменьшения урона
            );

            LogHelper.log("EnergyBullet", "Created mini lightning chain effect");
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

    /**
     * Внутренний класс для визуального эффекта молнии
     * Упрощенная версия класса из LightningChainAbility
     */
    private class LightningEffect extends Actor {
        private final TextureRegion texture;
        private final Vector2 start = new Vector2();
        private final Vector2 end = new Vector2();
        private final float duration = 0.3f;
        private final int segments = 3;
        private final Vector2[] points;
        private final float jitterAmount = 5f;
        private final float jitterFrequency = 0.05f;
        private float jitterTimer = 0f;

        /**
         * Создает визуальный эффект молнии между двумя точками
         */
        public LightningEffect(Vector2 start, Vector2 end) {
            this.start.set(start);
            this.end.set(end);

            try {
                this.texture = new TextureRegion(new Texture(Gdx.files.internal("abilities/lightning.png")));
            } catch (Exception e) {
                LogHelper.error("LightningEffect", "Failed to load lightning texture", e);
                throw new RuntimeException("Failed to load lightning texture", e);
            }

            points = new Vector2[segments + 1];
            for (int i = 0; i <= segments; i++) {
                points[i] = new Vector2();
            }

            generateLightningPoints();

            float width = end.dst(start);
            float height = 15f;

            setWidth(width);
            setHeight(height);
            setPosition(start.x, start.y - height / 2);
            setColor(0.5f, 0.7f, 1f, 0.8f); // Другой цвет для мини-молнии

            addAction(Actions.sequence(
                Actions.delay(duration * 0.7f),
                Actions.fadeOut(duration * 0.3f),
                Actions.removeActor()
            ));
        }

        /**
         * Генерирует точки для сегментов молнии с случайными отклонениями
         */
        private void generateLightningPoints() {
            points[0].set(start);
            points[segments].set(end);

            for (int i = 1; i < segments; i++) {
                float t = (float) i / segments;

                float x = start.x + (end.x - start.x) * t;
                float y = start.y + (end.y - start.y) * t;

                float dx = end.x - start.x;
                float dy = end.y - start.y;
                float length = (float) Math.sqrt(dx * dx + dy * dy);

                if (length < 0.01f) continue;

                float nx = -dy / length;
                float ny = dx / length;

                float offset = MathUtils.random(-jitterAmount, jitterAmount);
                x += nx * offset;
                y += ny * offset;

                points[i].set(x, y);
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color color = getColor();
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

            for (int i = 0; i < segments; i++) {
                Vector2 p1 = points[i];
                Vector2 p2 = points[i + 1];

                float dx = p2.x - p1.x;
                float dy = p2.y - p1.y;
                float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                float length = Vector2.dst(p1.x, p1.y, p2.x, p2.y);

                batch.draw(texture,
                    p1.x, p1.y - getHeight() / 2,
                    0, getHeight() / 2,
                    length, getHeight(),
                    1, 1,
                    angle + 90);
            }

            batch.setColor(1, 1, 1, 1);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            jitterTimer += delta;
            if (jitterTimer >= jitterFrequency) {
                jitterTimer = 0;
                generateLightningPoints();
            }
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
