package ru.myitschool.rogal.Abilities.Abilitis;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;

import ru.myitschool.rogal.Abilities.AbilityType;
import ru.myitschool.rogal.Abilities.AreaOfEffectAbility;
import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

/**
 * Способность "Вращающееся лезвие" - создает вращающийся объект вокруг игрока,
 * наносящий урон врагам при соприкосновении
 */
public class OrbitingBladeAbility extends AreaOfEffectAbility {

    // Параметры лезвия
    private float orbitRadius = 100f;       // Радиус орбиты лезвия
    private float rotationSpeed = 270f;    // Скорость вращения в градусах в секунду
    private float damageAmount = 25f;      // Базовый урон от лезвия
    private float currentAngle = 0f;       // Текущий угол на орбите

    // Визуальные элементы
    private OrbitingBladeVisual bladeVisual;
    private final Array<HitEnemy> hitEnemies = new Array<>(); // Список врагов, которых уже ударило лезвие
    private final float hitCooldown = 0.5f;      // Кулдаун между возможными повторными ударами по врагу

    /**
     * Конструктор способности вращающегося лезвия
     */
    public OrbitingBladeAbility() {
        super("Orbiting Blade",
            "Создаёт лезвие, которое вращается вокруг игрока и наносит урон врагам.\n" +
                "Уровень 2: Увеличивает урон и скорость вращения.\n" +
                "Уровень 3: Добавляет второе лезвие.\n" +
                "Уровень 4: Значительно увеличивает урон и размер.\n" +
                "Уровень 5: Увеличивает радиус орбиты и добавляет третье лезвие.",
              "abilities/blade_icon.png",
              7f,      // Кулдаун
              200f,    // Радиус действия
              15f,     // Размер лезвия
              10.0f,   // Длительность эффекта
              25f,     // Базовый урон
              "abilities/blade_icon.png");

        abilityType = AbilityType.ATTACK;
        energyCost = 20f;
        damageAmount = 25f;

        try{
            this.icon = new Texture(Gdx.files.internal("abilities/blade_icon.png"));
        } catch (Exception e) {
            LogHelper.error("OrbitingBladeAbility", "Failed to load icon", e);
        }
    }

    @Override
    protected boolean use(Vector2 position) {
        if (owner == null) return false;

        clearBlades();

        createBladeVisuals();

        isActive = true;
        activeTime = 0f;
        currentPosition = new Vector2(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);
        hitEnemies.clear();

        LogHelper.log("OrbitingBladeAbility", "Orbiting blade activated!");
        return true;
    }

    /**
     * Создает и добавляет визуальные элементы лезвий на сцену
     */
    private void createBladeVisuals() {
        if (owner == null || owner.getStage() == null) return;

        int bladeCount = 1;
        if (level >= 3) bladeCount = 2;
        if (level >= 5) bladeCount = 3;

        float angleStep = 360f / bladeCount;

        for (int i = 0; i < bladeCount; i++) {
            OrbitingBladeVisual blade = new OrbitingBladeVisual(areaRadius);
            blade.offsetAngle = i * angleStep;
            owner.getStage().addActor(blade);

            if (i == 0) {
                bladeVisual = blade;
            }
        }

        LogHelper.log("OrbitingBladeAbility", "Created " + bladeCount + " blades with angle step " + angleStep);
    }

    /**
     * Очищает все визуальные элементы лезвий
     */
    private void clearBlades() {
        if (bladeVisual != null && bladeVisual.getStage() != null) {
            bladeVisual.remove();
        }

        if (owner != null && owner.getStage() != null) {
            Array<Actor> actors = owner.getStage().getActors();
            for (int i = actors.size - 1; i >= 0; i--) {
                Actor actor = actors.get(i);
                if (actor instanceof OrbitingBladeVisual) {
                    actor.remove();
                }
            }
        }
    }

    @Override
    protected void updateActive(float delta) {
        if (!isActive || owner == null || owner.getStage() == null) return;

        activeTime += delta;

        if (activeTime >= effectDuration) {
            isActive = false;
            clearBlades();
            return;
        }

        currentAngle = (currentAngle + rotationSpeed * delta) % 360f;

        currentPosition.set(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);

        for (int i = hitEnemies.size - 1; i >= 0; i--) {
            HitEnemy hitEnemy = hitEnemies.get(i);
            hitEnemy.timer -= delta;
            if (hitEnemy.timer <= 0) {
                hitEnemies.removeIndex(i);
            }
        }

        applyEffect(delta);
    }

    @Override
    protected Actor createEffectVisual(Vector2 position) {
        return new OrbitingBladeVisual(areaRadius);
    }

    @Override
    protected void applyEffect(float delta) {
        if (owner == null || owner.getStage() == null) return;

        Stage stage = owner.getStage();

        Array<EnemyActor> enemies = new Array<>();
        Array<OrbitingBladeVisual> blades = new Array<>();

        Array<Actor> actors = stage.getActors();
        for (int i = 0; i < actors.size; i++) {
            Actor actor = actors.get(i);
            if (actor instanceof EnemyActor) {
                enemies.add((EnemyActor)actor);
            } else if (actor instanceof OrbitingBladeVisual) {
                blades.add((OrbitingBladeVisual)actor);
            }
        }

        for (EnemyActor enemy : enemies) {
            boolean recentlyHit = false;
            for (HitEnemy hitEnemy : hitEnemies) {
                if (hitEnemy.enemy == enemy) {
                    recentlyHit = true;
                    break;
                }
            }

            if (!recentlyHit) {
                for (OrbitingBladeVisual blade : blades) {
                    if (checkBladeCollision(enemy, blade)) {
                        enemy.takeDamage(Math.round(damageAmount));
                        hitEnemies.add(new HitEnemy(enemy, hitCooldown));
                        blade.playHitEffect();
                        LogHelper.log("OrbitingBladeAbility", "Enemy hit with orbiting blade! Damage: " + Math.round(damageAmount));
                        break;
                    }
                }
            }
        }
    }

    /**
     * Проверяет, если лезвие пересекается с врагом
     * @param enemy враг для проверки столкновения
     * @param blade лезвие для проверки столкновения
     * @return true если есть столкновение
     */
    private boolean checkBladeCollision(EnemyActor enemy, OrbitingBladeVisual blade) {
        float enemyCenterX = enemy.getX() + enemy.getWidth()/2;
        float enemyCenterY = enemy.getY() + enemy.getHeight()/2;

        float bladeCenterX = blade.getX() + blade.getWidth()/2;
        float bladeCenterY = blade.getY() + blade.getHeight()/2;

        float distanceSquared = (enemyCenterX - bladeCenterX) * (enemyCenterX - bladeCenterX)
                              + (enemyCenterY - bladeCenterY) * (enemyCenterY - bladeCenterY);

        float collisionRadius = (enemy.getWidth() + blade.getWidth()) / 2.5f;

        return distanceSquared <= collisionRadius * collisionRadius;
    }

    @Override
    protected void onLevelUp() {
        cooldown = Math.max(3.0f, cooldown - 0.5f);
        damageAmount *= 1.3f;

        if (level == 2) {
            rotationSpeed *= 1.25f;
            LogHelper.log("OrbitingBladeAbility", "Rotation speed increased at level 2");
        }

        if (level == 3) {
            LogHelper.log("OrbitingBladeAbility", "Second blade added at level 3");

            if (isActive) {
                clearBlades();
                createBladeVisuals();
            }
        }

        if (level == 4) {
            damageAmount *= 1.5f;
            areaRadius *= 1.3f;
            LogHelper.log("OrbitingBladeAbility", "Damage and size significantly increased at level 4");
        }

        if (level == 5) {
            orbitRadius *= 1.5f;
            LogHelper.log("OrbitingBladeAbility", "Orbit radius increased and third blade added at level 5");

            if (isActive) {
                clearBlades();
                createBladeVisuals();
            }
        }
    }

    public void tryAutoActivate() {
        if (!autoUse) return;

        if (currentCooldown > 0 || isActive) return;

        if (owner == null || owner.getStage() == null) return;

        Stage stage = owner.getStage();
        boolean enemiesNearby = false;

        float checkRadius = 200f;
        Vector2 playerPos = new Vector2(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);

        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;

                Vector2 enemyPos = new Vector2(enemy.getX() + enemy.getWidth()/2, enemy.getY() + enemy.getHeight()/2);
                float distance = playerPos.dst(enemyPos);

                if (distance <= checkRadius) {
                    enemiesNearby = true;
                    break;
                }
            }
        }

        if (enemiesNearby) {
            activate(playerPos);
        }
    }

    /**
     * Класс для отслеживания врагов, которым недавно был нанесен урон
     */
    private static class HitEnemy {
        EnemyActor enemy;
        float timer;

        public HitEnemy(EnemyActor enemy, float timer) {
            this.enemy = enemy;
            this.timer = timer;
        }
    }

    /**
     * Внутренний класс для визуального представления лезвия
     */
    private class OrbitingBladeVisual extends Actor {
        private final TextureRegion texture;
        private final Polygon hitbox;
        private float offsetAngle = 0f; // Угол смещения для дополнительных лезвий
        private boolean isAppearing = true; // Флаг появления лезвия
        private float appearProgress = 0f; // Прогресс анимации появления (0-1)
        private static final float APPEAR_DURATION = 0.7f; // Длительность анимации появления в секундах

        public OrbitingBladeVisual(float size) {
            Texture tex = new Texture(Gdx.files.internal(effectTexturePath));
            this.texture = new TextureRegion(tex);

            setWidth(size * 2);
            setHeight(size);
            setOrigin(getWidth() / 2, getHeight() / 2);

            if (owner != null) {
                setPosition(
                    owner.getX() + owner.getWidth()/2 - getWidth()/2,
                    owner.getY() + owner.getHeight()/2 - getHeight()/2
                );
            }

            hitbox = new Polygon(new float[]{
                0, 0,
                getWidth(), 0,
                getWidth(), getHeight(),
                0, getHeight()
            });
            hitbox.setOrigin(getOriginX(), getOriginY());

            setScale(0.1f);
            setColor(1, 1, 1, 0);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float alpha = getColor().a * parentAlpha;

            batch.setColor(1f, 1f, 1f, alpha);

            batch.draw(
                texture,
                getX(),
                getY(),
                getOriginX(),
                getOriginY(),
                getWidth(),
                getHeight(),
                getScaleX(),
                getScaleY(),
                getRotation() - 180f
            );

            batch.setColor(1f, 1f, 1f, 1f);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            if (owner == null) return;

            if (isAppearing) {
                appearProgress += delta / APPEAR_DURATION;
                if (appearProgress >= 1f) {
                    appearProgress = 1f;
                    isAppearing = false;
                }

                setColor(1f, 1f, 1f, appearProgress);

                setScale(0.1f + 0.9f * appearProgress);

                float targetAngle = currentAngle + offsetAngle;
                float currentRadius = orbitRadius * appearProgress;

                float x = owner.getX() + owner.getWidth()/2 + currentRadius * (float)Math.cos(Math.toRadians(targetAngle)) - getWidth()/2;
                float y = owner.getY() + owner.getHeight()/2 + currentRadius * (float)Math.sin(Math.toRadians(targetAngle)) - getHeight()/2;

                setPosition(x, y);
                setRotation(targetAngle);
            } else {
                float angle = currentAngle + offsetAngle;
                float x = owner.getX() + owner.getWidth()/2 + orbitRadius * (float)Math.cos(Math.toRadians(angle)) - getWidth()/2;
                float y = owner.getY() + owner.getHeight()/2 + orbitRadius * (float)Math.sin(Math.toRadians(angle)) - getHeight()/2;

                setPosition(x, y);
                setRotation(angle);
            }

            hitbox.setPosition(getX(), getY());
            hitbox.setRotation(getRotation());

            if (!isActive && !hasActions()) {
                float centerX = owner.getX() + owner.getWidth()/2 - getWidth()/2;
                float centerY = owner.getY() + owner.getHeight()/2 - getHeight()/2;

                addAction(Actions.sequence(
                    Actions.parallel(
                        Actions.moveTo(centerX, centerY, 0.5f),
                        Actions.fadeOut(0.5f),
                        Actions.scaleTo(0.1f, 0.1f, 0.5f)
                    ),
                    Actions.removeActor()
                ));
            }
        }

        /**
         * Активирует эффект удара для лезвия
         */
        public void playHitEffect() {
            addAction(Actions.sequence(
                Actions.alpha(0.5f, 0.1f),
                Actions.alpha(1f, 0.1f)
            ));
        }

    }
}
