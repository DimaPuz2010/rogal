package ru.myitschool.rogal.Abilities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import ru.myitschool.rogal.Actors.PlayerActor;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

/**
 * Класс для управления списком способностей игрока
 */
public class AbilityManager implements Disposable {

    // Список способностей игрока
    private final Array<Ability> abilities;

    // Активные в данный момент способности
    private final Array<Ability> activeAbilities;

    // Максимальное количество способностей, которые может иметь игрок
    public static final int MAX_ABILITIES = 5;

    // Ссылка на игрока - владельца способностей
    private final PlayerActor owner;

    /**
     * Конструктор менеджера способностей
     * @param owner игрок, которому принадлежат способности
     */
    public AbilityManager(PlayerActor owner) {
        this.owner = owner;
        this.abilities = new Array<>(MAX_ABILITIES);
        this.activeAbilities = new Array<>();

        LogHelper.log("AbilityManager", "Ability manager created for player");
    }

    /**
     * Добавляет новую способность в список
     * @param ability способность для добавления
     * @return true если способность успешно добавлена
     */
    public boolean addAbility(Ability ability) {
        if (abilities.size >= MAX_ABILITIES) {
            LogHelper.log("AbilityManager", "Cannot add ability: max abilities reached (" + MAX_ABILITIES + ")");
            return false;
        }

        // Устанавливаем владельца способности
        ability.setOwner(owner);

        // Добавляем способность в список
        abilities.add(ability);
        LogHelper.log("AbilityManager", "Added ability: " + ability.getName() + " (Level " + ability.getLevel() + ")");
        return true;
    }

    /**
     * Активирует способность по индексу
     * @param index индекс способности в списке
     * @param targetPosition позиция применения
     * @return true если способность успешно активирована
     */
    public boolean activateAbility(int index, Vector2 targetPosition) {
        if (index < 0 || index >= abilities.size) {
            return false;
        }

        Ability ability = abilities.get(index);
        boolean activated = ability.activate(targetPosition);

        if (activated) {
            // Добавляем способность в список активных, если она еще не там
            if (!activeAbilities.contains(ability, true)) {
                activeAbilities.add(ability);
            }
        }

        return activated;
    }

    /**
     * Обновляет состояние всех способностей
     * @param delta время между кадрами
     */
    public void update(float delta) {
        // Создаем копию списка способностей для безопасной итерации
        Array<Ability> abilitiesCopy = new Array<>(abilities);

        // Обновляем каждую способность
        for (Ability ability : abilitiesCopy) {
            if (ability != null) {
                ability.update(delta);
            }
        }

        // Создаем новый список активных способностей
        Array<Ability> newActiveAbilities = new Array<>();
        for (Ability ability : abilitiesCopy) {
            if (ability != null && ability.isActive()) {
                newActiveAbilities.add(ability);
            }
        }

        // Обновляем список активных способностей
        activeAbilities.clear();
        activeAbilities.addAll(newActiveAbilities);
    }

    /**
     * Повышает уровень способности
     * @param index индекс способности
     * @return новый уровень способности или -1 в случае ошибки
     */
    public int levelUpAbility(int index) {
        if (index < 0 || index >= abilities.size) {
            return -1;
        }

        return abilities.get(index).levelUp();
    }

    /**
     * Возвращает список всех способностей
     * @return массив способностей
     */
    public Array<Ability> getAbilities() {
        return abilities;
    }

    /**
     * Возвращает способность по индексу
     * @param index индекс способности
     * @return способность или null, если индекс некорректен
     */
    public Ability getAbility(int index) {
        if (index < 0 || index >= abilities.size) {
            return null;
        }
        return abilities.get(index);
    }

    /**
     * Возвращает количество способностей
     * @return количество способностей у игрока
     */
    public int getAbilityCount() {
        return abilities.size;
    }

    /**
     * Проверяет, может ли игрок получить новую способность
     * @return true если еще есть место для новой способности
     */
    public boolean canAddAbility() {
        return abilities.size < MAX_ABILITIES;
    }

    /**
     * Возвращает максимальное количество способностей
     * @return максимальное количество слотов для способностей
     */
    public int getMaxAbilities() {
        return MAX_ABILITIES;
    }

    /**
     * Сбрасывает все кулдауны способностей (для тестирования)
     */
    public void resetAllCooldowns() {
        for (Ability ability : abilities) {
            if (ability != null) {
                ability.resetCooldown();
            }
        }
    }

    /**
     * Включает автоматическое использование для всех способностей
     */
    public void enableAutoUseForAll() {
        for (Ability ability : abilities) {
            if (ability != null) {
                ability.setAutoUse(true);
            }
        }
    }

    /**
     * Отключает автоматическое использование для всех способностей
     */
    public void disableAutoUseForAll() {
        for (Ability ability : abilities) {
            if (ability != null) {
                ability.setAutoUse(false);
            }
        }
    }

    /**
     * Проверяет, включено ли автоматическое использование для всех способностей
     * @return true если хотя бы одна способность использует автоматическое применение
     */
    public boolean isAutoUseEnabledForAll() {
        for (Ability ability : abilities) {
            if (ability != null && ability.isAutoUse()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет, хватает ли энергии для всех автоматически используемых способностей
     * @return true если энергии достаточно
     */
    public boolean hasEnoughEnergyForAutoUse() {
        if (owner == null) return false;

        float totalEnergyCost = 0;
        for (Ability ability : abilities) {
            if (ability != null && ability.isAutoUse() && ability.canActivate()) {
                totalEnergyCost += ability.getEnergyCost();
            }
        }

        return owner.getCurrentEnergy() >= totalEnergyCost;
    }

    /**
     * Освобождает ресурсы, используемые способностями
     */
    @Override
    public void dispose() {
        // Освобождаем ресурсы каждой способности
        for (Ability ability : abilities) {
            if (ability != null) {
                ability.dispose();
            }
        }
        abilities.clear();
        activeAbilities.clear();
    }
}

