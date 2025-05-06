package ru.myitschool.rogal.Abilities.Abilitis;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

import ru.myitschool.rogal.Abilities.AbilityType;
import ru.myitschool.rogal.Abilities.AreaOfEffectAbility;
import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.Actors.PlayerActor;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

/**
 * Способность "Исцеляющая аура" - базовая лечащая способность
 */
public class HealingAuraAbility extends AreaOfEffectAbility {
    private float healAmount = 15f; // Уменьшено базовое количество восстанавливаемого здоровья
    private float damageAmount = 8f; // Уменьшено базовое количество урона
    private float auraDuration = 6f; // Увеличена длительность ауры в секундах

    // Дополнительные эффекты
    private boolean reducesDebuffs = false; // Снижает ли отрицательные эффекты (открывается на 3 уровне)
    private boolean boostsSpeed = false;   // Повышает ли скорость движения (открывается на 5 уровне)
    private float speedBoostPercent = 20f; // Процент увеличения скорости
    private final float speedBoostDuration = 3.0f;    // Увеличена длительность увеличения скорости
    private final float debuffReductionPercent = 30f; // Увеличен процент снижения длительности дебаффов

    // Отслеживание активных бустов
    private float speedBoostTimer = 0f;
    private boolean speedBoostActive = false;
    private float baseSpeed = 0f;
    private float boostedSpeed = 0f;

    // Система дебаффов
    private final HashMap<String, Float> activeDebuffsReduction = new HashMap<>();
    private float debuffCheckTimer = 0f;
    private static final float DEBUFF_CHECK_INTERVAL = 0.5f;

    // Ссылка на визуальный эффект ауры
    private HealingAuraVisual auraVisual;

    // Визуальные эффекты бустов
    private final Array<SpeedBoostParticle> speedParticles = new Array<>();
    private final Array<DebuffReductionParticle> debuffParticles = new Array<>();

    /**
     * Конструктор способности ауры исцеления
     */
    public HealingAuraAbility() {
        super("Healing Aura",
            "Создаёт лечащую ауру вокруг игрока, которая восстанавливает здоровье и наносит урон врагам.\n" +
                "Уровень 2: Увеличивает лечение и урон. Снижает перезарядку.\n" +
                "Уровень 3: Увеличивает область действия и количество лечения.\n" +
                "Уровень 4: Значительно увеличивает силу лечения и снижает перезарядку.\n" +
                "Уровень 5: Существенно увеличивает лечение, урон и область действия.",
                "abilities/healing_aura_effect.png",
                14f,     // Слегка уменьшенный кулдаун
                180f,    // Увеличенный радиус действия
                180f,    // Увеличенный радиус визуального эффекта
                6.0f,    // Увеличенная длительность эффекта
                15f,     // Уменьшенное базовое значение исцеления
                "abilities/healing_aura_effect.png");

        abilityType = AbilityType.DEFENSIVE;
        energyCost = 25f;
        healAmount = 15f;
        damageAmount = 8f;
        auraDuration = 6f;

        try {
            if (Gdx.files.internal("abilities/healing_aura_effect.png").exists()) {
                this.icon = new Texture(Gdx.files.internal("abilities/healing_aura_effect.png"));
                LogHelper.log("HealingAuraAbility", "Icon loaded successfully");
            } else {
                LogHelper.error("HealingAuraAbility", "Icon file doesn't exist");
            }
        } catch (Exception e) {
            LogHelper.error("HealingAuraAbility", "Failed to load icon", e);
        }
    }

    @Override
    protected boolean use(Vector2 position) {
        if (owner == null) return false;

        auraVisual = (HealingAuraVisual) createEffectVisual(position);
        if (auraVisual != null && owner.getStage() != null) {
            owner.getStage().addActor(auraVisual);
        }

        isActive = true;
        activeTime = 0f;
        currentPosition = position;

        // Очищаем предыдущие частицы
        clearParticles();

        // Сохраняем исходную скорость игрока для использования в буфах
        if (owner instanceof PlayerActor) {
            PlayerActor player = owner;
            baseSpeed = player.getSpeed();
        }

        LogHelper.log("HealingAuraAbility", "Healing aura activated!");
        return true;
    }

    /**
     * Очищает все частицы эффектов
     */
    private void clearParticles() {
        for (SpeedBoostParticle particle : speedParticles) {
            if (particle.getStage() != null) {
                particle.remove();
            }
        }
        speedParticles.clear();

        for (DebuffReductionParticle particle : debuffParticles) {
            if (particle.getStage() != null) {
                particle.remove();
            }
        }
        debuffParticles.clear();
    }

    @Override
    protected void updateActive(float delta) {
        if (!isActive || owner == null || owner.getStage() == null) return;

        activeTime += delta;

        if (activeTime >= auraDuration) {
            isActive = false;
            if (auraVisual != null) {
                auraVisual.remove();
            }
            clearParticles();
            return;
        }

        if (auraVisual != null) {
            auraVisual.setPosition(
                owner.getX() + owner.getWidth()/2 - auraVisual.getWidth()/2,
                owner.getY() + owner.getHeight()/2 - auraVisual.getHeight()/2
            );
        }

        updateSpeedBoost(delta);
        updateDebuffReduction(delta);

        updateParticles(delta);

        applyEffect(delta);
    }

    /**
     * Обновляет частицы эффектов
     * @param delta время между кадрами
     */
    private void updateParticles(float delta) {
        for (int i = speedParticles.size - 1; i >= 0; i--) {
            SpeedBoostParticle particle = speedParticles.get(i);
            if (particle.getStage() == null) {
                speedParticles.removeIndex(i);
            }
        }

        for (int i = debuffParticles.size - 1; i >= 0; i--) {
            DebuffReductionParticle particle = debuffParticles.get(i);
            if (particle.getStage() == null) {
                debuffParticles.removeIndex(i);
            }
        }

        if (speedBoostActive && owner.getStage() != null) {
            if (Math.random() < 0.1) {
                SpeedBoostParticle particle = new SpeedBoostParticle(owner);
                owner.getStage().addActor(particle);
                speedParticles.add(particle);
            }
        }

        if (reducesDebuffs && !activeDebuffsReduction.isEmpty() && owner.getStage() != null) {
            if (Math.random() < 0.1) {
                DebuffReductionParticle particle = new DebuffReductionParticle(owner);
                owner.getStage().addActor(particle);
                debuffParticles.add(particle);
            }
        }
    }

    /**
     * Обновление эффекта увеличения скорости
     * @param delta время между кадрами
     */
    private void updateSpeedBoost(float delta) {
        if (speedBoostActive) {
            speedBoostTimer -= delta;

            if (speedBoostTimer <= 0) {
                // Время действия буфа закончилось
                speedBoostActive = false;
                resetPlayerSpeed();
                LogHelper.log("HealingAuraAbility", "Speed boost expired");
            }
        }
    }

    /**
     * Сбрасывает скорость игрока к базовому значению
     */
    private void resetPlayerSpeed() {
        if (owner instanceof PlayerActor) {
            PlayerActor player = owner;
            player.setSpeed(baseSpeed);
            LogHelper.log("HealingAuraAbility", "Player speed reset to " + baseSpeed);
        }
    }

    /**
     * Обновление эффекта снижения дебаффов
     * @param delta время между кадрами
     */
    private void updateDebuffReduction(float delta) {
        if (reducesDebuffs) {
            debuffCheckTimer -= delta;

            if (debuffCheckTimer <= 0) {
                debuffCheckTimer = DEBUFF_CHECK_INTERVAL;

                // Проверяем и уменьшаем активные дебаффы
                processActiveDebuffs();
            }
        }
    }

    /**
     * Обрабатывает активные дебаффы игрока
     */
    private void processActiveDebuffs() {
        if (owner instanceof PlayerActor) {
            // Здесь предполагаем, что у PlayerActor есть метод getActiveDebuffs()
            HashMap<String, Float> playerDebuffs = getPlayerDebuffs();

            // Очищаем наши записи о дебаффах, которые уже не активны
            activeDebuffsReduction.keySet().removeIf(debuffId -> !playerDebuffs.containsKey(debuffId));

            // Обрабатываем текущие дебаффы игрока
            for (String debuffId : playerDebuffs.keySet()) {
                if (!activeDebuffsReduction.containsKey(debuffId)) {
                    // Это новый дебафф, уменьшаем его длительность
                    float originalDuration = playerDebuffs.get(debuffId);
                    float reduction = originalDuration * (debuffReductionPercent / 100f);

                    // Записываем, сколько мы уменьшили для этого дебаффа
                    activeDebuffsReduction.put(debuffId, reduction);

                    // Уменьшаем длительность дебаффа
                    reduceDebuffDuration(debuffId, reduction);
                    LogHelper.log("HealingAuraAbility", "Reduced debuff " + debuffId + " duration by " + reduction + " seconds");
                }
            }
        }
    }

    /**
     * Получает все активные дебаффы игрока
     * @return карта дебаффов с их оставшейся длительностью
     */
    private HashMap<String, Float> getPlayerDebuffs() {
        if (owner instanceof PlayerActor) {
            return owner.getActiveDebuffs();
        }
        return new HashMap<>();
    }

    /**
     * Уменьшает длительность дебаффа игрока
     * @param debuffId идентификатор дебаффа
     * @param reduction количество секунд для уменьшения
     */
    private void reduceDebuffDuration(String debuffId, float reduction) {
        if (owner instanceof PlayerActor) {
            owner.reduceDebuffDuration(debuffId, reduction);
        }
    }

    @Override
    protected void onLevelUp() {
        cooldown = Math.max(2.0f, cooldown - 0.5f); // Уменьшаем кулдаун
        healAmount *= 1.125f; // Увеличиваем лечение на 12.5%
        damageAmount *= 1.125f; // Увеличиваем урон на 12.5%

        // Специальные улучшения на определенных уровнях
        if (level == 3) {
            reducesDebuffs = true;
            LogHelper.log("HealingAuraAbility", "Debuff reduction unlocked at level 3");
        }
        if (level == 4) {
            healAmount *= 1.5f;
            damageAmount *= 1.5f;
            LogHelper.log("HealingAuraAbility", "Healing and damage increased at level 4");
        }
        if (level == 5) {
            boostsSpeed = true;
            speedBoostPercent = 20f;
            LogHelper.log("HealingAuraAbility", "Speed boost unlocked at level 5");
        }
    }

    @Override
    protected Actor createEffectVisual(Vector2 position) {
        return new HealingAuraVisual(position.x, position.y, areaRadius);
    }

    @Override
    protected void applyEffect(float delta) {
        if (owner == null || owner.getStage() == null) return;

        // Получаем позицию игрока
        Vector2 playerPos = new Vector2(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);
        effectArea.setPosition(playerPos.x, playerPos.y);
        effectArea.setRadius(areaRadius);

        // Лечим игрока
        float currentHealAmount = healAmount * delta;
        int currentHealth = owner.getCurrentHealth();
        int maxHealth = owner.getMaxHealth();

        if (currentHealth < maxHealth) {
            owner.setCurrentHealth(Math.min(currentHealth + Math.round(currentHealAmount), maxHealth));
        }

        // Наносим урон врагам в радиусе
        for (Actor actor : owner.getStage().getActors()) {
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;
                if (isActorInArea(enemy)) {
                    float currentDamageAmount = damageAmount * delta;
                    enemy.takeDamage(Math.round(currentDamageAmount));
                }
            }
        }

        // Применяем дополнительные эффекты
        if (boostsSpeed && !speedBoostActive && owner instanceof PlayerActor) {
            applySpeedBoost();
        }

        // Плавно изменяем прозрачность ауры в зависимости от времени
        if (auraVisual != null && auraVisual.getStage() != null) {
            float progress = activeTime / auraDuration;
            if (progress > 0.7f) {
                // В конце эффекта плавно убираем ауру
                float fadeOut = 1f - ((progress - 0.7f) / 0.3f);
                auraVisual.setAlpha(fadeOut);
            }
        }
    }

    /**
     * Применяет буст скорости к игроку
     */
    private void applySpeedBoost() {
        if (owner instanceof PlayerActor) {
            PlayerActor player = owner;
            // Устанавливаем флаг активного буста
            speedBoostActive = true;
            speedBoostTimer = speedBoostDuration;

            // Сохраняем базовую скорость и вычисляем буст
            baseSpeed = player.getSpeed();
            boostedSpeed = baseSpeed * (1 + speedBoostPercent/100f);

            // Применяем буст
            player.setSpeed(boostedSpeed);

            LogHelper.log("HealingAuraAbility", "Speed boost applied: " + baseSpeed + " -> " + boostedSpeed);
        }
    }

    /**
     * Немедленно очищает все дебаффы игрока
     */
    public void clearAllDebuffs() {
        if (owner instanceof PlayerActor && level >= 5) {
            int clearedCount = owner.clearAllDebuffs();
            if (clearedCount > 0) {
                LogHelper.log("HealingAuraAbility", "Cleared " + clearedCount + " debuffs from player");

                // Добавляем визуальный эффект очистки дебаффов
                addDebuffClearingEffect();
            }
        }
    }

    /**
     * Добавляет визуальный эффект очистки дебаффов
     */
    private void addDebuffClearingEffect() {
        if (owner == null || owner.getStage() == null) return;

        for (int i = 0; i < 10; i++) {
            DebuffReductionParticle particle = new DebuffReductionParticle(owner);
            owner.getStage().addActor(particle);
            debuffParticles.add(particle);
        }
    }

    /**
     * Внутренний класс для визуального представления ауры исцеления
     */
    private class HealingAuraVisual extends Actor {
        private final TextureRegion texture;
        private float alpha = 0.7f; // Прозрачность ауры
        private float pulseTime = 1f; // Время для пульсирующего эффекта

        public HealingAuraVisual(float x, float y, float radius) {
            Texture tex = new Texture(Gdx.files.internal(effectTexturePath));
            this.texture = new TextureRegion(tex);

            setWidth(radius * 2);
            setHeight(radius * 2);
            setPosition(x - radius, y - radius);
            setOrigin(radius, radius);
            setScale(1.0f);

            addAction(Actions.sequence(
                Actions.alpha(0),
                Actions.fadeIn(0.5f)
            ));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.setColor(1f, 1f, 1f, alpha * parentAlpha);
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
                getRotation()
            );
            batch.setColor(1f, 1f, 1f, 1f);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            // Обновляем время для пульсирующего эффекта
            pulseTime += delta;

            // Создаем пульсирующий эффект, меняя масштаб
            float pulseScale = 1f + 0.05f * (float)Math.sin(pulseTime * 3);
            setScale(pulseScale);

            // Проверяем, активна ли еще способность
            if (!isActive) {
                // Если способность закончилась, удаляем ауру
                addAction(Actions.sequence(
                    Actions.fadeOut(0.5f),
                    Actions.removeActor()
                ));
            }
        }

        /**
         * Устанавливает прозрачность ауры
         * @param alpha значение прозрачности (0.0-1.0)
         */
        public void setAlpha(float alpha) {
            this.alpha = alpha;
        }
    }

    /**
     * Класс для отображения частиц увеличения скорости
     */
    private class SpeedBoostParticle extends Actor {
        private final TextureRegion texture;
        private float lifetime;
        private float angle;
        private final float speed;
        private final float baseAlpha;

        public SpeedBoostParticle(Actor target) {
            Texture tex = new Texture(Gdx.files.internal(effectTexturePath));
            this.texture = new TextureRegion(tex);


            lifetime = 0.3f + (float)Math.random() * 0.7f;
            angle = (float)(Math.random() * Math.PI * 2);
            speed = 10f + (float)Math.random() * 20f;
            baseAlpha = 0.3f + (float)Math.random() * 0.5f;

            float offsetX = (float)Math.cos(angle) * 5f;
            float offsetY = (float)Math.sin(angle) * 5f;

            setWidth(10);
            setHeight(10);
            setPosition(
                target.getX() + target.getWidth()/2 + offsetX - getWidth()/2,
                target.getY() + target.getHeight()/2 + offsetY - getHeight()/2
            );
            setOrigin(getWidth()/2, getHeight()/2);

            setScale(0.5f);
            addAction(Actions.scaleTo(1.5f, 1.5f, lifetime));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float alpha = baseAlpha * parentAlpha * (lifetime);
            batch.setColor(0.6f, 0.9f, 1.0f, alpha);
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
                angle * 57.3f
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

            // Движение частицы
            float moveX = (float)Math.cos(angle) * speed * delta;
            float moveY = (float)Math.sin(angle) * speed * delta;
            moveBy(moveX, moveY);

            // Вращение
            angle += delta;
        }
    }

    /**
     * Класс для отображения частиц снижения дебаффов
     */
    private class DebuffReductionParticle extends Actor {
        private final TextureRegion texture;
        private float lifetime;
        private final Actor target;
        private final float offsetX;
        private final float offsetY;
        private final float alpha;

        public DebuffReductionParticle(Actor target) {
            Texture tex = new Texture(Gdx.files.internal(effectTexturePath));
            this.texture = new TextureRegion(tex);

            this.target = target;

            lifetime = 0.5f + (float)Math.random() * 0.5f;
            offsetX = (float)(Math.random() * target.getWidth()) - target.getWidth()/2;
            offsetY = (float)(Math.random() * target.getHeight()) - target.getHeight()/2;
            alpha = 0.3f + (float)Math.random() * 0.4f;

            setWidth(15);
            setHeight(15);
            setPosition(
                target.getX() + target.getWidth()/2 + offsetX - getWidth()/2,
                target.getY() + target.getHeight()/2 + offsetY - getHeight()/2
            );
            setOrigin(getWidth()/2, getHeight()/2);

            setScale(0.1f);
            addAction(Actions.sequence(
                Actions.scaleTo(1.5f, 1.5f, 0.3f),
                Actions.scaleTo(0.1f, 0.1f, 0.2f),
                Actions.removeActor()
            ));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.setColor(0.8f, 0.2f, 0.8f, alpha * parentAlpha);
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
                getRotation()
            );
            batch.setColor(1f, 1f, 1f, 1f);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            // Следуем за целью
            setPosition(
                target.getX() + target.getWidth()/2 + offsetX - getWidth()/2,
                target.getY() + target.getHeight()/2 + offsetY - getHeight()/2
            );

            // Уменьшаем время жизни
            lifetime -= delta;
            if (lifetime <= 0) {
                remove();
            }
        }
    }
}
