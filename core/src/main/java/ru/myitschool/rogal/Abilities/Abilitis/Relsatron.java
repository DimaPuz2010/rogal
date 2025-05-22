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

public class Relsatron extends Ability {
    private float projectileSpeed = 10f;
    private float projectileDamage = 100f;
    private final float projectileLifespan = 5f;
    private final float projectileSize = 0.05f;
    private final String projectileTexturePath = "abilities/relsatron.png";
    private final float autoActivateInterval = 0.4f;
    private float timeSinceLastActivation = 0f;
    private final int projectileCount = 1; // Количество пуль
    private float projectileSpreadAngle = 0f; // Угол разброса между пулями в градусах

    public Relsatron() {
        super("Relsatron", "Создаёт энергетический снаряд, который автоматически направляется к ближайшему врагу и наносит урон",
            "abilities/relsatron.png", 1.0f, 500f);

        this.abilityType = AbilityType.ATTACK;
        this.energyCost = 50f;
        this.cooldown = 15f;
        this.range = 1000f;

        if (Gdx.files.internal(projectileTexturePath).exists()) {
            this.icon = new Texture(Gdx.files.internal(projectileTexturePath));
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);

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
            LogHelper.error("Relsatron", "Cannot use ability: owner or stage is missing");
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
                        angleOffset = (i == 0) ? -projectileSpreadAngle / 2 : projectileSpreadAngle / 2;
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

                Relsatron.BulletProjectile projectile = new Relsatron.BulletProjectile(
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
        range += 100f;
        projectileDamage += 10f;

        if (level == 2) {
            projectileDamage += 20f;
            range += 100f;
        } else if (level == 3) {
            cooldown -= 3f;
            projectileSpeed += 5f;
            projectileSpreadAngle = 15f;
        } else if (level == 4) {
            cooldown -= 2f;
            projectileSpeed += 10f;
        } else if (level == 5) {
            cooldown -= 5f;
            projectileDamage += 100f;
        }
    }

    @Override
    public String getDescription() {
        if (level == 1){
            return description;
        } else if (level+1 == 3){
            return "Уменьшает перезарядку, увеличивает скорость и урон снаряда.";
        } else if (level+1 == 4) {
            return "Уменьшает перезарядку и увеличивает скорость";
        } else if (level+1 == 5) {
            return "Уменьшает перезарядку, значительно увеличивает урон снаряда";
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

    @Override
    public void dispose() {
        if (icon != null) {
            icon.dispose();
            icon = null;
        }
        super.dispose();
    }

    /**
     * Внутренний класс для снаряда
     */
    private class BulletProjectile extends Actor {
        private final float speed;
        private final float damage;
        private final float maxLifespan;
        private final TextureRegion texture;
        private final Polygon hitbox;
        private final float turnSpeed = 0.5f;
        private final Vector2 direction;
        private float lifespan;
        private EnemyActor targetEnemy;
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
            setPosition(position.x - getWidth() / 2, position.y - getHeight() / 2);
            setOrigin(getWidth() / 2, getHeight() / 2);

            this.direction = new Vector2(target).sub(position).nor();
            float angle = Vector2Helpers.getRotationByVector(direction.x, direction.y);
            setRotation(angle);

            hitbox = HitboxHelper.createCircleHitbox(getWidth() / 2, 8);
            hitbox.setOrigin(getWidth() / 2, getHeight() / 2);
            updateHitbox();

            findTarget();
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.draw(texture, getX(), getY(), getOriginX(), getOriginY(),
                getWidth(), getHeight(), 1, 5, getRotation() + 90);
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
}
