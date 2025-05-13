package ru.myitschool.rogal.Abilities.Abilitis;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;

import ru.myitschool.rogal.Abilities.AbilityType;
import ru.myitschool.rogal.Abilities.AreaOfEffectAbility;
import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.Actors.PlayerActor;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

public class FrostAuraAbility extends AreaOfEffectAbility {

    private float damageAmount = 8f;          // Базовый урон от ауры
    private float slowAmount = 30f;           // Замедление в процентах (30%)
    private float slowDuration = 1.5f;        // Длительность замедления после выхода из ауры
    private final float effectDuration = 10f;        // Длительность действия ауры
    private final float effectFrequency = 0.5f;     // Частота нанесения урона (раз в 0.5 сек)
    private float effectTimer = 0f;           // Таймер для эффекта

    private FrostAuraVisual auraVisual;       // Визуальный эффект ауры
    private final Array<SlowEffect> activeSlowEffects = new Array<>();  // Активные эффекты замедления

    /**
     * Конструктор способности Ледяной Ауры
     */
    public FrostAuraAbility() {
        super("Frost Aura",
            "Создаёт ледяную ауру вокруг игрока, которая замедляет врагов и наносит им урон.\n" +
                "Уровень 2: Увеличивает урон и эффект замедления.\n" +
                "Уровень 3: Расширяет область действия и снижает перезарядку.\n" +
                "Уровень 4: Значительно увеличивает урон и длительность замедления.\n" +
                "Уровень 5: Существенно увеличивает урон и полностью замораживает врагов на короткое время.",
              "abilities/frost_aura.png",
              12f,    // Кулдаун
              130f,   // Радиус действия
              120f,   // Радиус визуального эффекта
              7.0f,   // Длительность эффекта
              8f,     // Базовый урон
              "abilities/frost_aura.png");

        abilityType = AbilityType.ATTACK;
        energyCost = 20f;

        try {
            this.icon = new Texture(Gdx.files.internal("abilities/frost_aura.png"));
        } catch (Exception e) {
            LogHelper.error("FrostAuraAbility", "Failed to load icon", e);
        }
    }

    @Override
    protected boolean use(Vector2 position) {
        if (owner == null) return false;

        clearExistingVisuals();

        isActive = true;
        activeTime = 0f;
        effectTimer = 0f;
        currentPosition = new Vector2(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);

        if (owner.getStage() != null) {
            auraVisual = new FrostAuraVisual(areaRadius);
            owner.getStage().addActor(auraVisual);
        }

        LogHelper.log("FrostAuraAbility", "Frost aura activated!");
        return true;
    }

    /**
     * Удаляет все существующие визуальные эффекты ауры
     */
    private void clearExistingVisuals() {
        if (auraVisual != null && auraVisual.getStage() != null) {
            auraVisual.remove();
        }

        if (owner != null && owner.getStage() != null) {
            Array<Actor> actors = owner.getStage().getActors();
            for (int i = actors.size - 1; i >= 0; i--) {
                Actor actor = actors.get(i);
                if (actor instanceof FrostAuraVisual) {
                    actor.remove();
                }
            }
        }
    }

    @Override
    protected void updateActive(float delta) {
        if (!isActive || owner == null) return;

        activeTime += delta;

        if (activeTime >= effectDuration) {
            isActive = false;
            if (auraVisual != null) {
                auraVisual.fadeOut();
            }
            return;
        }

        currentPosition.set(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);

        effectTimer += delta;
        if (effectTimer >= effectFrequency) {
            effectTimer = 0f;
            applyEffect(delta);
        }

        if (auraVisual != null) {
            auraVisual.setPosition(
                owner.getX() + owner.getWidth()/2 - auraVisual.getWidth()/2,
                owner.getY() + owner.getHeight()/2 - auraVisual.getHeight()/2
            );
        }

        updateSlowEffects(delta);
    }

    /**
     * Обновляет эффекты замедления на врагах
     */
    private void updateSlowEffects(float delta) {
        for (int i = activeSlowEffects.size - 1; i >= 0; i--) {
            SlowEffect effect = activeSlowEffects.get(i);
            effect.remainingTime -= delta;

            if (effect.remainingTime <= 0) {
                resetEnemySpeed(effect.enemy);
                activeSlowEffects.removeIndex(i);
            }
        }
    }

    /**
     * Сбрасывает скорость врага к нормальной
     */
    private void resetEnemySpeed(EnemyActor enemy) {
        if (enemy == null) return;

        enemy.resetSpeed();
        LogHelper.log("FrostAuraAbility", "Enemy speed restored to normal");
    }

    @Override
    protected Actor createEffectVisual(Vector2 position) {
        return new FrostAuraVisual(areaRadius);
    }

    @Override
    protected void applyEffect(float delta) {
        if (owner == null || owner.getStage() == null) return;

        Stage stage = owner.getStage();

        // Получаем врагов в радиусе действия и применяем эффекты
        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;

                float enemyCenterX = enemy.getX() + enemy.getWidth()/2;
                float enemyCenterY = enemy.getY() + enemy.getHeight()/2;

                float distance = new Vector2(currentPosition.x - enemyCenterX, currentPosition.y - enemyCenterY).len();

                if (distance <= areaRadius) {
                    enemy.takeDamage(Math.round(damageAmount));

                    applySlowToEnemy(enemy);

                    createFrostParticle(enemy);
                }
            }
        }
    }

    /**
     * Применяет эффект замедления к врагу
     */
    private void applySlowToEnemy(EnemyActor enemy) {
        boolean alreadySlowed = false;

        for (SlowEffect effect : activeSlowEffects) {
            if (effect.enemy == enemy) {
                effect.remainingTime = slowDuration;
                alreadySlowed = true;
                break;
            }
        }

        if (!alreadySlowed) {
            enemy.applySlowEffect(slowAmount / 100f);

            activeSlowEffects.add(new SlowEffect(enemy, slowDuration));

            LogHelper.log("FrostAuraAbility", "Enemy slowed by " + slowAmount + "%");
        }
    }

    /**
     * Создает частицу морозного эффекта на враге
     */
    private void createFrostParticle(EnemyActor enemy) {
        if (owner == null || owner.getStage() == null) return;

        FrostParticle particle = new FrostParticle(enemy);
        owner.getStage().addActor(particle);
    }

    @Override
    protected void onLevelUp() {
        damageAmount *= 1.25f;

        if (level == 2) {
            slowAmount = 40f;
            LogHelper.log("FrostAuraAbility", "Level 2: Damage and slow effect increased");
        }

        if (level == 3) {
            areaRadius *= 1.3f;
            cooldown = 10f;
            LogHelper.log("FrostAuraAbility", "Level 3: Area of effect increased and cooldown reduced");
        }

        if (level == 4) {
            damageAmount *= 1.5f;
            slowDuration = 2.5f;
            LogHelper.log("FrostAuraAbility", "Level 4: Damage significantly increased and slow duration extended");
        }

        if (level == 5) {
            damageAmount *= 1.5f;
            slowAmount = 50f;
            LogHelper.log("FrostAuraAbility", "Level 5: Damage greatly increased and enemies are now almost completely frozen");
        }
    }

    public void tryAutoActivate() {
        if (!autoUse || currentCooldown > 0 || isActive) return;
        if (owner == null || !(owner instanceof PlayerActor)) return;

        // Активируем ауру, если рядом есть враги
        boolean enemiesNearby = false;

        if (owner.getStage() != null) {
            Vector2 playerPos = new Vector2(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);

            for (Actor actor : owner.getStage().getActors()) {
                if (actor instanceof EnemyActor) {
                    EnemyActor enemy = (EnemyActor) actor;
                    float enemyCenterX = enemy.getX() + enemy.getWidth()/2;
                    float enemyCenterY = enemy.getY() + enemy.getHeight()/2;

                    float distance = new Vector2(playerPos.x - enemyCenterX, playerPos.y - enemyCenterY).len();

                    if (distance <= areaRadius * 1.5f) {
                        enemiesNearby = true;
                        break;
                    }
                }
            }
        }

        if (enemiesNearby) {
            Vector2 playerPos = new Vector2(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);
            activate(playerPos);
        }
    }

    /**
     * Класс для хранения информации об эффекте замедления
     */
    private static class SlowEffect {
        EnemyActor enemy;
        float remainingTime;

        public SlowEffect(EnemyActor enemy, float duration) {
            this.enemy = enemy;
            this.remainingTime = duration;
        }
    }

    /**
     * Визуальное представление ледяной ауры
     */
    private class FrostAuraVisual extends Actor {
        private final TextureRegion texture;
        private float angle = 0f;
        private float alpha = 0.6f;
        private float pulseTime = 0f;
        private boolean isFadingOut = false;

        /**
         * Создает визуальное представление ауры
         * @param radius радиус ауры
         */
        public FrostAuraVisual(float radius) {
            try {
                Texture tex = new Texture(Gdx.files.internal(effectTexturePath));
                this.texture = new TextureRegion(tex);
            } catch (Exception e) {
                LogHelper.error("FrostAuraVisual", "Failed to load texture", e);
                throw new RuntimeException("Failed to load frost aura texture", e);
            }

            setSize(radius * 2, radius * 2);
            setOrigin(getWidth() / 2, getHeight() / 2);

            if (owner != null) {
                setPosition(
                    owner.getX() + owner.getWidth()/2 - getWidth()/2,
                    owner.getY() + owner.getHeight()/2 - getHeight()/2
                );
            }

            // Плавное появление
            setColor(1, 1, 1, 0);
            addAction(Actions.fadeIn(0.5f));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (texture == null) return;

            batch.setColor(0.7f, 0.8f, 1f, alpha * parentAlpha);
            batch.draw(
                texture,
                getX(), getY(),
                getOriginX(), getOriginY(),
                getWidth(), getHeight(),
                getScaleX(), getScaleY(),
                angle
            );
            batch.setColor(1f, 1f, 1f, 1f);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            // Медленное вращение
            angle -= 5f * delta;
            if (angle < 0) angle += 360f;

            // Эффект пульсации
            pulseTime += delta;
            float pulseScale = 1.0f + 0.05f * (float)Math.sin(pulseTime * 2f);
            setScale(pulseScale);

            // Затухание при деактивации
            if (isFadingOut) {
                alpha = Math.max(0, alpha - delta * 1.5f);
                if (alpha <= 0) {
                    remove();
                }
            }
        }

        /**
         * Запускает анимацию затухания ауры
         */
        public void fadeOut() {
            isFadingOut = true;
            addAction(Actions.sequence(
                Actions.fadeOut(0.5f),
                Actions.removeActor()
            ));
        }
    }

    /**
     * Класс для визуального эффекта мороза на врагах
     */
    private class FrostParticle extends Actor {
        private final TextureRegion texture;
        private float lifetime;
        private final EnemyActor target;
        private final float scale;
        private final float alpha;

        public FrostParticle(EnemyActor target) {
            this.target = target;

            try {
                Texture tex = new Texture(Gdx.files.internal(effectTexturePath));
                this.texture = new TextureRegion(tex);
            } catch (Exception e) {
                LogHelper.error("FrostParticle", "Failed to load texture", e);
                throw new RuntimeException("Failed to load frost particle texture", e);
            }

            lifetime = 0.3f + MathUtils.random(0.3f);
            scale = 0.2f + MathUtils.random(0.3f);
            alpha = 0.4f + MathUtils.random(0.4f);

            setSize(20, 20);
            setOrigin(getWidth() / 2, getHeight() / 2);

            float offsetX = MathUtils.random(-target.getWidth()/3, target.getWidth()/3);
            float offsetY = MathUtils.random(-target.getHeight()/3, target.getHeight()/3);

            setPosition(
                target.getX() + target.getWidth()/2 + offsetX - getWidth()/2,
                target.getY() + target.getHeight()/2 + offsetY - getHeight()/2
            );

            setScale(scale);
            setRotation(MathUtils.random(360f));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.setColor(0.8f, 0.9f, 1f, alpha * parentAlpha * (lifetime / 0.6f));
            batch.draw(
                texture,
                getX(), getY(),
                getOriginX(), getOriginY(),
                getWidth(), getHeight(),
                getScaleX(), getScaleY(),
                getRotation()
            );
            batch.setColor(1f, 1f, 1f, 1f);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            lifetime -= delta;
            if (lifetime <= 0) {
                remove();
                return;
            }

            setPosition(
                target.getX() + getX() - target.getX(),
                target.getY() + getY() - target.getY()
            );
        }
    }
}
