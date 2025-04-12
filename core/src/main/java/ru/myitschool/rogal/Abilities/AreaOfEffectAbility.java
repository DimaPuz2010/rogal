package ru.myitschool.rogal.Abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;

import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

/**
 * Абстрактный класс для способностей с областью действия
 */
public abstract class AreaOfEffectAbility extends Ability {
    
    // Параметры области действия
    protected float areaRadius;            // Радиус области действия
    protected float effectDuration;        // Длительность эффекта области
    protected float baseEffectValue;       // Базовое значение эффекта (урон/лечение и т.д.)
    protected String effectTexturePath;    // Путь к текстуре эффекта
    protected Circle effectArea;           // Круговая область действия эффекта
    
    // Текущее состояние
    protected Vector2 currentPosition;     // Текущая позиция области
    
    /**
     * Конструктор для способности с областью действия
     * @param name название способности
     * @param description описание способности
     * @param iconPath путь к файлу иконки
     * @param cooldown время перезарядки в секундах
     * @param range радиус действия способности
     * @param areaRadius радиус области действия
     * @param effectDuration длительность эффекта
     * @param baseEffectValue базовое значение эффекта
     * @param effectTexturePath путь к текстуре эффекта
     */
    public AreaOfEffectAbility(String name, String description, String iconPath,
                            float cooldown, float range,
                            float areaRadius, float effectDuration, float baseEffectValue,
                            String effectTexturePath) {
        super(name, description, iconPath, cooldown, range);
        
        this.areaRadius = areaRadius;
        this.effectDuration = effectDuration;
        this.baseEffectValue = baseEffectValue;
        this.effectTexturePath = effectTexturePath;
        this.effectArea = new Circle();
        
        // Загрузка иконки
        try {
            this.icon = new Texture(Gdx.files.internal(iconPath));
        } catch (Exception e) {
            LogHelper.error("AreaOfEffectAbility", "Не удалось загрузить иконку: " + iconPath);
        }
    }
    
    @Override
    protected boolean use(Vector2 position) {
        if (owner == null || owner.getStage() == null) {
            return false;
        }
        
        // Применяем ограничение на дальность
        Vector2 playerPos = new Vector2(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);
        float distance = playerPos.dst(position);
        
        if (distance > range) {
            // Если цель слишком далеко, ограничиваем радиусом действия
            Vector2 direction = new Vector2(position).sub(playerPos).nor();
            position = new Vector2(playerPos).add(direction.scl(range));
        }
        
        // Устанавливаем область эффекта
        currentPosition = position;
        effectArea.set(position.x, position.y, areaRadius);
        
        // Создаем визуальный эффект на сцене
        Actor effectVisual = createEffectVisual(position);
        if (effectVisual != null) {
            owner.getStage().addActor(effectVisual);
        }
        
        return true;
    }
    
    @Override
    protected void updateActive(float delta) {
        // Проверяем время активности
        if (activeTime >= effectDuration) {
            isActive = false;
            return;
        }
        
        // Если способность активна, выполняем эффект области
        applyEffect(delta);
    }
    
    @Override
    protected void onLevelUp() {
        // Увеличиваем базовые характеристики с повышением уровня
        baseEffectValue *= 1.25f;  // Увеличение силы эффекта на 25%
        
        // Улучшения с некоторыми порогами уровней
        if (level == 3) {
            areaRadius *= 1.2f;  // На 3-м уровне увеличиваем радиус на 20%
        }
        if (level == 5) {
            effectDuration *= 1.3f;  // На 5-м уровне увеличиваем длительность на 30%
        }
    }
    
    /**
     * Создает визуальное представление эффекта
     * @param position позиция эффекта
     * @return актор визуального эффекта
     */
    protected abstract Actor createEffectVisual(Vector2 position);
    
    /**
     * Применяет эффект в области действия
     * @param delta время между кадрами
     */
    protected abstract void applyEffect(float delta);
    
    /**
     * Проверяет, находится ли актор в области действия способности
     * @param actor проверяемый актор
     * @return true если актор в зоне действия
     */
    protected boolean isActorInArea(Actor actor) {
        Vector2 actorCenter = new Vector2(
            actor.getX() + actor.getWidth() / 2,
            actor.getY() + actor.getHeight() / 2
        );
        
        return effectArea.contains(actorCenter);
    }
    
    /**
     * Возвращает текущую область действия способности
     * @return круг, представляющий область действия
     */
    public Circle getEffectArea() {
        return effectArea;
    }
    
    /**
     * Возвращает текущую силу эффекта, учитывая уровень способности
     * @return значение эффекта
     */
    public float getEffectValue() {
        return baseEffectValue;
    }
    
    /**
     * Возвращает длительность эффекта
     * @return длительность в секундах
     */
    public float getEffectDuration() {
        return effectDuration;
    }
} 