package ru.myitschool.rogal.Abilities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.Actors.PlayerActor;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

/**
 * Базовый абстрактный класс для всех способностей игры
 */
public abstract class Ability {
    // Основные свойства способности
    protected String name;             // Название способности
    protected String description;      // Описание способности
    protected Texture icon;            // Иконка для отображения в UI
    protected int level = 1;           // Уровень способности
    protected float cooldown;          // Базовое время перезарядки в секундах
    protected float range;             // Радиус действия способности
    protected AbilityType abilityType; // Тип способности
    protected float energyCost = 0f;     // Стоимость энергии для активации способности
    protected boolean autoUse = true; // Автоматическое использование способности
    protected float autoUseTimer = 0f; // Таймер для автоматического использования

    // Состояние способности
    protected float currentCooldown = 0f;  // Текущее оставшееся время перезарядки
    protected boolean isActive = false;    // Активна ли способность сейчас
    protected float activeTime = 0f;       // Время, в течение которого способность активна
    public PlayerActor owner;           // Владелец способности

    /**
     * Конструктор способности
     * @param name название способности
     * @param description описание способности
     * @param iconPath путь к файлу иконки
     * @param cooldown время перезарядки в секундах
     * @param range радиус действия
     */
    public Ability(String name, String description, String iconPath, float cooldown, float range) {
        this.name = name;
        this.description = description;
        this.cooldown = cooldown;
        this.range = range;

    }

    /**
     * Устанавливает владельца способности
     * @param owner игрок, которому принадлежит способность
     */
    public void setOwner(PlayerActor owner) {
        this.owner = owner;
    }

    /**
     * Метод активации способности
     * @param position позиция применения способности
     * @return true если способность активирована, false если нет (например, на кулдауне)
     */
    public boolean activate(Vector2 position) {
        if (!canActivate()) {
            return false;
        }

        // Проверка наличия достаточного количества энергии (если есть стоимость)
        if (energyCost > 0 && owner != null) {
            if (owner.getCurrentEnergy() < energyCost) {
                return false;
            }

            owner.useEnergy(energyCost);
        }

        boolean success = use(position);

        if (success) {
            if (owner != null) {
                currentCooldown = cooldown * owner.getCooldownMultiplier();
            } else {
                currentCooldown = cooldown;
            }

            isActive = true;
            activeTime = 0f;
        }

        return success;
    }

    /**
     * Метод проверки возможности активации способности
     * @return true если способность может быть использована
     */
    public boolean canActivate() {
        return currentCooldown <= 0 && (owner == null || owner.getCurrentHealth() > 0);
    }

    /**
     * Обновление состояния способности
     * @param delta время между кадрами
     */
    public void update(float delta) {
        if (currentCooldown > 0) {
            currentCooldown -= delta;
            if (currentCooldown < 0) {
                currentCooldown = 0;
            }
        }

        if (isActive) {
            activeTime += delta;
            updateActive(delta);
        }

        if (autoUse && canActivate() && owner != null) {
            tryAutoActivate(delta);
        }
    }

    /**
     * Метод для обновления стоимости энергии при повышении уровня
     */
    protected void updateEnergyCostOnLevelUp() {
        // По умолчанию стоимость увеличивается на 15% с каждым уровнем
        // Способности могут переопределить этот метод для другого соотношения
        if (level > 1) {
            float baseCost = getEnergyCost();
            float levelMultiplier = 1.0f + ((level - 1) * 0.15f);
            setEnergyCost(Math.round(baseCost * levelMultiplier));

            // Логгируем изменение стоимости
            LogHelper.debug("Ability", name + " level " + level + " energy cost updated to " + energyCost);
        }
    }

    /**
     * Повышает уровень способности
     * @return новый уровень способности или -1, если достигнут максимум
     */
    public int levelUp() {
        if (owner == null) return -1;

        int maxLevel = owner.getMaxAbilityLevel();
        if (level >= maxLevel) {
            return -1;
        }

        level++;
        onLevelUp();
        updateEnergyCostOnLevelUp();

        LogHelper.log("Ability", name + " leveled up to " + level);
        return level;
    }

    /**
     * Возвращает текущий процент перезарядки (0.0 - 1.0)
     * @return значение от 0.0 (готова) до 1.0 (полностью на кулдауне)
     */
    public float getCooldownPercent() {
        if (cooldown <= 0) {
            return 0f;
        }
        return Math.max(0f, currentCooldown / cooldown);
    }

    /**
     * Возвращает описание способности с учетом текущего уровня
     * @return строка с описанием
     */
    public String getDescription() {
        return description + "\nУровень: " + level;
    }

    /**
     * Возвращает описание для следующего уровня способности
     * @return строка с описанием улучшения
     */
    public String getUpgradeDescription() {
        return description + "\nУровень: " + level + " -> " + (level + 1) + "\n(Улучшение)";
    }

    /**
     * Проверяет, можно ли улучшить способность
     * @param maxLevel максимальный возможный уровень
     * @return true если способность можно улучшить
     */
    public boolean canUpgrade(int maxLevel) {
        return level < maxLevel;
    }

    /**
     * Возвращает название способности
     * @return название способности
     */
    public String getName() {
        return name;
    }

    /**
     * Возвращает иконку способности
     * @return текстура иконки
     */
    public Texture getIcon() {
        return icon;
    }

    /**
     * Возвращает текущий уровень способности
     * @return текущий уровень
     */
    public int getLevel() {
        return level;
    }

    /**
     * Устанавливает уровень способности
     *
     * @param level новый уровень
     */
    public void setLevel(int level) {
        this.level = level;

        updateEnergyCostOnLevelUp();
    }

    /**
     * Возвращает текущее время перезарядки
     * @return оставшееся время в секундах
     */
    public float getCurrentCooldown() {
        return currentCooldown;
    }

    /**
     * Возвращает базовое время перезарядки способности
     * @return базовое время в секундах
     */
    public float getCooldown() {
        return cooldown;
    }

    /**
     * Проверяет, активна ли способность
     * @return true если способность активна
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Позволяет вручную сбросить кулдаун способности
     */
    public void resetCooldown() {
        currentCooldown = 0f;
    }

    /**
     * Освобождает ресурсы, используемые способностью
     */
    public void dispose() {
        if (icon != null) {
            icon.dispose();
        }
    }

    /**
     * Реализация применения способности - должна быть переопределена в дочерних классах
     * @param position позиция применения
     * @return true если способность успешно применена
     */
    protected abstract boolean use(Vector2 position);

    /**
     * Обновление активного состояния способности - должно быть переопределено если способность имеет длительность
     * @param delta время между кадрами
     */
    protected abstract void updateActive(float delta);

    /**
     * Действия при повышении уровня способности - должны быть переопределены в дочерних классах
     */
    protected abstract void onLevelUp();

    // Новый метод для получения стоимости энергии
    public float getEnergyCost() {
        return energyCost;
    }

    // Новый метод для установки стоимости энергии
    public void setEnergyCost(float energyCost) {
        this.energyCost = energyCost;
    }

    /**
     * Активирует способность без указания позиции
     * @return true если способность была активирована, false если на перезарядке или не может быть активирована
     */
    public boolean activate() {
        if (!canActivate()) {
            return false;
        }

        if (energyCost > 0 && owner != null) {
            if (owner.getCurrentEnergy() < energyCost) {
                return false;
            }
        }

        return activate(new Vector2(owner.getX() + owner.getOriginX(), owner.getY() + owner.getOriginY()));
    }

    /**
     * Включает или выключает автоматическое использование способности
     * @param auto true для включения автоматического использования
     */
    public void setAutoUse(boolean auto) {
        this.autoUse = auto;
    }

    /**
     * Проверяет, включено ли автоматическое использование способности
     * @return true если способность используется автоматически
     */
    public boolean isAutoUse() {
        return autoUse;
    }

    /**
     * Пытается автоматически активировать способность
     * @param delta время между кадрами
     */
    public void tryAutoActivate(float delta) {
        if (!autoUse || !isUnlocked() || !canActivate() || owner == null) {
            return;
        }

        autoUseTimer += delta;

        if (autoUseTimer < 0.5f) {
            return;
        }

        autoUseTimer = 0f;

        Stage stage = owner.getStage();
        if (stage == null) {
            return;
        }

        int enemiesCount = 0;
        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyActor) {
                enemiesCount++;
            }
        }

        if (enemiesCount == 0) {
            LogHelper.debug("Ability", "No enemies on stage to use ability: " + name);
            return;
        }

        float totalAutoUseCost = 0;
        for (Ability ability : owner.getAbilityManager().getAbilities()) {
            if (ability.isAutoUse() && ability.isUnlocked() && ability.canActivate()) {
                totalAutoUseCost += ability.getEnergyCost();
            }
        }

        // Если текущей энергии меньше, чем суммарная стоимость всех авто-способностей,
        // то используем только дешевые способности или ждем восстановления
        if (owner.getCurrentEnergy() < totalAutoUseCost) {
            // Если эта способность относительно дешевая (менее 25% от общей стоимости),
            // то все равно пытаемся использовать её
            if (getEnergyCost() > totalAutoUseCost * 0.25f) {
                LogHelper.debug("Ability", "Not enough energy for all auto abilities. Prioritizing cheaper ones.");
                return;
            }
        }

        Vector2 targetPosition = findNearestEnemyPosition(range);

        if (targetPosition != null) {
            if (energyCost > 0 && owner.getCurrentEnergy() < energyCost) {
                LogHelper.debug("Ability", "Not enough energy for " + name + ": " +
                              owner.getCurrentEnergy() + "/" + energyCost);
                return;
            }

            activate(targetPosition);
        }
    }

    /**
     * Находит позицию для автоматического использования способности
     * @return позиция цели или null, если цель не найдена
     */
    protected Vector2 findTargetPosition() {
        if (owner == null || owner.getStage() == null) return null;

        Vector2 playerPos = new Vector2(owner.getX() + owner.getOriginX(), owner.getY() + owner.getOriginY());

        // Для атакующих способностей нужно найти ближайшего врага
        if (abilityType == AbilityType.ATTACK) {
            Vector2 enemyPos = owner.findNearestEnemyPosition(range);
            if (enemyPos != null) {
                return enemyPos;
            } else {
                // Если в пределах дальности нет врагов, пробуем найти любого врага
                // и проверим, можно ли его достать, если двигаться в его направлении
                Vector2 anyEnemyPos = owner.findNearestEnemyPosition(1000f); // Используем большой радиус
                if (anyEnemyPos != null) {
                    Vector2 direction = new Vector2(anyEnemyPos).sub(playerPos).nor();
                    return new Vector2(playerPos).add(direction.scl(range));
                }
            }
        }
        // Для защитных/лечащих способностей используем позицию игрока
        else if (abilityType == AbilityType.DEFENSIVE) {
            // Для защитных способностей проверяем, нужно ли их использовать
            // Например, если здоровье ниже 70%, то используем лечащую способность
            if (owner.getCurrentHealth() < owner.getMaxHealth() * 0.7f) {
                return playerPos;
            } else {
                // Если здоровье достаточное, не используем защитную способность
                return null;
            }
        }
        // Для остальных типов используем позицию игрока
        return playerPos;
    }

    /**
     * Проверяет, разблокирована ли способность
     * @return true, если способность разблокирована
     */
    public boolean isUnlocked() {
        return true; // По умолчанию все способности разблокированы
    }

    /**
     * Находит позицию ближайшего врага в пределах указанного радиуса
     * @param searchRange радиус поиска
     * @return Vector2 позиция ближайшего врага или null
     */
    protected Vector2 findNearestEnemyPosition(float searchRange) {
        if (owner == null || owner.getStage() == null) return null;

        Stage stage = owner.getStage();
        Vector2 playerPos = new Vector2(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);
        Vector2 closestEnemyPos = null;
        float closestDistance = searchRange;

        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;
                Vector2 enemyPos = new Vector2(
                    enemy.getX() + enemy.getWidth()/2,
                    enemy.getY() + enemy.getHeight()/2
                );

                float distance = playerPos.dst(enemyPos);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEnemyPos = enemyPos;
                }
            }
        }

        return closestEnemyPos;
    }
}
