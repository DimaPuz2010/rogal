package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import ru.myitschool.rogal.Actors.PlayerActor;

/**
 * Класс для управления данными игрока между игровыми сессиями
 */
public class PlayerData {
    private static final String PREFS_NAME = "rogal_player_data";

    // Константы для ключей сохранения
    private static final String PREF_KEY_CURRENCY = "currency";
    private static final String PREF_KEY_HEALTH_UPGRADE = "health_upgrade";
    private static final String PREF_KEY_ENERGY_UPGRADE = "energy_upgrade";
    private static final String PREF_KEY_SPEED_UPGRADE = "speed_upgrade";
    private static final String PREF_KEY_REGEN_UPGRADE = "regen_upgrade";
    private static final String PREF_KEY_ENERGY_REGEN_UPGRADE = "energy_regen_upgrade";
    private static final String PREF_KEY_TOTAL_KILLS = "total_kills";
    private static final String PREF_KEY_GAMES_PLAYED = "games_played";
    private static final String PREF_KEY_BEST_WAVE = "best_wave";
    private static final String PREF_KEY_FREE_ABILITIES = "free_abilities";
    private static final String PREF_KEY_PLAYER_NAME = "player_name";
    private static final String PREF_KEY_CONTROL_MODE = "control_mode";
    private static final String PREF_KEY_PLAYER_LEVEL = "player_level";

    // Стоимость улучшений (увеличивается с каждым уровнем)
    private static final int BASE_HEALTH_UPGRADE_COST = 100;
    private static final int BASE_ENERGY_UPGRADE_COST = 100;
    private static final int BASE_SPEED_UPGRADE_COST = 150;
    private static final int BASE_REGEN_UPGRADE_COST = 200;
    private static final int BASE_ENERGY_REGEN_UPGRADE_COST = 200;

    // Коэффициент увеличения стоимости улучшений с каждым уровнем
    private static final float UPGRADE_COST_MULTIPLIER = 1.5f;

    // Максимальный уровень улучшений
    private static final int MAX_UPGRADE_LEVEL = 10;

    // Значения улучшений за уровень
    private static final int HEALTH_PER_UPGRADE = 20;
    private static final int ENERGY_PER_UPGRADE = 15;
    private static final float SPEED_PER_UPGRADE = 0.1f;
    private static final float REGEN_PER_UPGRADE = 0.2f;
    private static final float ENERGY_REGEN_PER_UPGRADE = 0.3f;
    // Объект для работы с сохранениями
    private static Preferences prefs;

    /**
     * Инициализирует систему сохранений данных игрока
     */
    public static void initialize() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Проверка наличия данных и создание начальных, если их нет
        if (!prefs.contains(PREF_KEY_CURRENCY)) {
            resetToDefaults();
        }

        LogHelper.log("PlayerData", "Player data initialized");
    }

    /**
     * Сбрасывает данные игрока к начальным значениям
     */
    public static void resetToDefaults() {
        prefs.putInteger(PREF_KEY_CURRENCY, 0);
        prefs.putInteger(PREF_KEY_HEALTH_UPGRADE, 0);
        prefs.putInteger(PREF_KEY_ENERGY_UPGRADE, 0);
        prefs.putInteger(PREF_KEY_SPEED_UPGRADE, 0);
        prefs.putInteger(PREF_KEY_REGEN_UPGRADE, 0);
        prefs.putInteger(PREF_KEY_ENERGY_REGEN_UPGRADE, 0);
        prefs.putInteger(PREF_KEY_TOTAL_KILLS, 0);
        prefs.putInteger(PREF_KEY_GAMES_PLAYED, 0);
        prefs.putInteger(PREF_KEY_BEST_WAVE, 0);
        prefs.putBoolean(PREF_KEY_FREE_ABILITIES, false); // По умолчанию способности тратят энергию
        prefs.putInteger(PREF_KEY_CONTROL_MODE, 0); // По умолчанию используется режим джойстика
        prefs.putInteger(PREF_KEY_PLAYER_LEVEL, 1);
        prefs.flush();

        LogHelper.log("PlayerData", "Reset player data to defaults");
    }

    /**
     * Получает текущее количество валюты игрока
     */
    public static int getCurrency() {
        return prefs.getInteger(PREF_KEY_CURRENCY, 0);
    }

    /**
     * Добавляет валюту игроку
     *
     * @param amount количество валюты для добавления
     */
    public static void addCurrency(int amount) {
        int currentCurrency = getCurrency();
        prefs.putInteger(PREF_KEY_CURRENCY, currentCurrency + amount);
        prefs.flush();

        LogHelper.log("PlayerData", "Added " + amount + " currency. Total: " + (currentCurrency + amount));
    }

    /**
     * Тратит валюту, если у игрока достаточно средств
     *
     * @param amount количество валюты для траты
     * @return true если валюта успешно потрачена
     */
    public static boolean spendCurrency(int amount) {
        int currentCurrency = getCurrency();
        if (currentCurrency >= amount) {
            prefs.putInteger(PREF_KEY_CURRENCY, currentCurrency - amount);
            prefs.flush();
            LogHelper.log("PlayerData", "Spent " + amount + " currency. Remaining: " + (currentCurrency - amount));
            return true;
        }

        LogHelper.log("PlayerData", "Not enough currency to spend " + amount + ". Current: " + currentCurrency);
        return false;
    }

    /**
     * Получает текущий уровень улучшения здоровья
     */
    public static int getHealthUpgradeLevel() {
        return prefs.getInteger(PREF_KEY_HEALTH_UPGRADE, 0);
    }

    /**
     * Получает текущий уровень улучшения энергии
     */
    public static int getEnergyUpgradeLevel() {
        return prefs.getInteger(PREF_KEY_ENERGY_UPGRADE, 0);
    }

    /**
     * Получает текущий уровень улучшения скорости
     */
    public static int getSpeedUpgradeLevel() {
        return prefs.getInteger(PREF_KEY_SPEED_UPGRADE, 0);
    }

    /**
     * Получает текущий уровень улучшения регенерации здоровья
     */
    public static int getRegenUpgradeLevel() {
        return prefs.getInteger(PREF_KEY_REGEN_UPGRADE, 0);
    }

    /**
     * Получает текущий уровень улучшения регенерации энергии
     */
    public static int getEnergyRegenUpgradeLevel() {
        return prefs.getInteger(PREF_KEY_ENERGY_REGEN_UPGRADE, 0);
    }

    /**
     * Рассчитывает стоимость следующего улучшения здоровья
     */
    public static int getNextHealthUpgradeCost() {
        int level = getHealthUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return -1; // Достигнут максимальный уровень
        return Math.round(BASE_HEALTH_UPGRADE_COST * (float) Math.pow(UPGRADE_COST_MULTIPLIER, level));
    }

    /**
     * Рассчитывает стоимость следующего улучшения энергии
     */
    public static int getNextEnergyUpgradeCost() {
        int level = getEnergyUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return -1;
        return Math.round(BASE_ENERGY_UPGRADE_COST * (float) Math.pow(UPGRADE_COST_MULTIPLIER, level));
    }

    /**
     * Рассчитывает стоимость следующего улучшения скорости
     */
    public static int getNextSpeedUpgradeCost() {
        int level = getSpeedUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return -1;
        return Math.round(BASE_SPEED_UPGRADE_COST * (float) Math.pow(UPGRADE_COST_MULTIPLIER, level));
    }

    /**
     * Рассчитывает стоимость следующего улучшения регенерации здоровья
     */
    public static int getNextRegenUpgradeCost() {
        int level = getRegenUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return -1;
        return Math.round(BASE_REGEN_UPGRADE_COST * (float) Math.pow(UPGRADE_COST_MULTIPLIER, level));
    }

    /**
     * Рассчитывает стоимость следующего улучшения регенерации энергии
     */
    public static int getNextEnergyRegenUpgradeCost() {
        int level = getEnergyRegenUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return -1;
        return Math.round(BASE_ENERGY_REGEN_UPGRADE_COST * (float) Math.pow(UPGRADE_COST_MULTIPLIER, level));
    }

    /**
     * Покупает улучшение здоровья
     *
     * @return true если улучшение успешно куплено
     */
    public static boolean upgradeHealth() {
        int level = getHealthUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return false;

        int cost = getNextHealthUpgradeCost();
        if (spendCurrency(cost)) {
            prefs.putInteger(PREF_KEY_HEALTH_UPGRADE, level + 1);
            prefs.flush();
            LogHelper.log("PlayerData", "Upgraded health to level " + (level + 1));
            return true;
        }
        return false;
    }

    /**
     * Покупает улучшение энергии
     *
     * @return true если улучшение успешно куплено
     */
    public static boolean upgradeEnergy() {
        int level = getEnergyUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return false;

        int cost = getNextEnergyUpgradeCost();
        if (spendCurrency(cost)) {
            prefs.putInteger(PREF_KEY_ENERGY_UPGRADE, level + 1);
            prefs.flush();
            LogHelper.log("PlayerData", "Upgraded energy to level " + (level + 1));
            return true;
        }
        return false;
    }

    /**
     * Покупает улучшение скорости
     *
     * @return true если улучшение успешно куплено
     */
    public static boolean upgradeSpeed() {
        int level = getSpeedUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return false;

        int cost = getNextSpeedUpgradeCost();
        if (spendCurrency(cost)) {
            prefs.putInteger(PREF_KEY_SPEED_UPGRADE, level + 1);
            prefs.flush();
            LogHelper.log("PlayerData", "Upgraded speed to level " + (level + 1));
            return true;
        }
        return false;
    }

    /**
     * Покупает улучшение регенерации здоровья
     *
     * @return true если улучшение успешно куплено
     */
    public static boolean upgradeRegen() {
        int level = getRegenUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return false;

        int cost = getNextRegenUpgradeCost();
        if (spendCurrency(cost)) {
            prefs.putInteger(PREF_KEY_REGEN_UPGRADE, level + 1);
            prefs.flush();
            LogHelper.log("PlayerData", "Upgraded health regen to level " + (level + 1));
            return true;
        }
        return false;
    }

    /**
     * Покупает улучшение регенерации энергии
     *
     * @return true если улучшение успешно куплено
     */
    public static boolean upgradeEnergyRegen() {
        int level = getEnergyRegenUpgradeLevel();
        if (level >= MAX_UPGRADE_LEVEL) return false;

        int cost = getNextEnergyRegenUpgradeCost();
        if (spendCurrency(cost)) {
            prefs.putInteger(PREF_KEY_ENERGY_REGEN_UPGRADE, level + 1);
            prefs.flush();
            LogHelper.log("PlayerData", "Upgraded energy regen to level " + (level + 1));
            return true;
        }
        return false;
    }

    /**
     * Получает бонус к здоровью от улучшений
     */
    public static int getHealthBonus() {
        return getHealthUpgradeLevel() * HEALTH_PER_UPGRADE;
    }

    /**
     * Получает бонус к энергии от улучшений
     */
    public static int getEnergyBonus() {
        return getEnergyUpgradeLevel() * ENERGY_PER_UPGRADE;
    }

    /**
     * Получает бонус к скорости от улучшений
     */
    public static float getSpeedBonus() {
        return SPEED_PER_UPGRADE * getSpeedUpgradeLevel();
    }

    /**
     * Получает бонус к регенерации здоровья от улучшений
     */
    public static float getRegenBonus() {
        return getRegenUpgradeLevel() * REGEN_PER_UPGRADE;
    }

    /**
     * Получает бонус к регенерации энергии от улучшений
     */
    public static float getEnergyRegenBonus() {
        return getEnergyRegenUpgradeLevel() * ENERGY_REGEN_PER_UPGRADE;
    }

    /**
     * Обновляет статистику игрока после игры
     *
     * @param kills количество убитых врагов
     * @param wave  достигнутая волна
     */
    public static void updateStats(int kills, int wave) {
        // Обновляем общее количество убийств
        int totalKills = prefs.getInteger(PREF_KEY_TOTAL_KILLS, 0) + kills;
        prefs.putInteger(PREF_KEY_TOTAL_KILLS, totalKills);

        // Увеличиваем счетчик игр
        int gamesPlayed = prefs.getInteger(PREF_KEY_GAMES_PLAYED, 0) + 1;
        prefs.putInteger(PREF_KEY_GAMES_PLAYED, gamesPlayed);

        // Обновляем лучшую волну
        int bestWave = prefs.getInteger(PREF_KEY_BEST_WAVE, 0);
        if (wave > bestWave) {
            prefs.putInteger(PREF_KEY_BEST_WAVE, wave);
        }

        prefs.flush();
        LogHelper.log("PlayerData", "Updated stats: kills=" + kills + ", total=" + totalKills +
            ", games=" + gamesPlayed + ", wave=" + wave + ", best=" + Math.max(wave, bestWave));
    }

    /**
     * Получает общее количество убитых врагов
     */
    public static int getTotalKills() {
        return prefs.getInteger(PREF_KEY_TOTAL_KILLS, 0);
    }

    /**
     * Получает общее количество сыгранных игр
     */
    public static int getGamesPlayed() {
        return prefs.getInteger(PREF_KEY_GAMES_PLAYED, 0);
    }

    /**
     * Получает лучшую достигнутую волну
     */
    public static int getBestWave() {
        return prefs.getInteger(PREF_KEY_BEST_WAVE, 0);
    }

    /**
     * Проверяет, включен ли режим бесплатных способностей
     *
     * @return true если способности не тратят энергию
     */
    public static boolean isFreeAbilitiesEnabled() {
        return prefs.getBoolean(PREF_KEY_FREE_ABILITIES, false);
    }

    /**
     * Устанавливает настройку бесплатных способностей
     *
     * @param enabled true - способности не тратят энергию
     */
    public static void setFreeAbilitiesEnabled(boolean enabled) {
        prefs.putBoolean(PREF_KEY_FREE_ABILITIES, enabled);
        prefs.flush();
        LogHelper.log("PlayerData", "Free abilities " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Проверяет, установлено ли имя игрока
     *
     * @return true если имя уже установлено
     */
    public static boolean hasPlayerName() {
        return prefs.contains(PREF_KEY_PLAYER_NAME);
    }

    /**
     * Получает имя игрока
     *
     * @return имя игрока или пустую строку, если не установлено
     */
    public static String getPlayerName() {
        return prefs.getString(PREF_KEY_PLAYER_NAME, "");
    }

    /**
     * Устанавливает имя игрока
     *
     * @param name имя игрока
     */
    public static void setPlayerName(String name) {
        prefs.putString(PREF_KEY_PLAYER_NAME, name);
        prefs.flush();
        LogHelper.log("PlayerData", "Player name set to: " + name);
    }

    /**
     * Возвращает объект Preferences для доступа к сохраненным настройкам
     *
     * @return объект Preferences
     */
    public static Preferences getPreferences() {
        if (prefs == null) {
            initialize();
        }
        return prefs;
    }

    /**
     * Применяет все сохраненные улучшения к игроку
     *
     * @param player игрок, к которому применяются улучшения
     */
    public static void applyUpgradesToPlayer(PlayerActor player) {
        if (player == null) return;

        // Применяем улучшения здоровья
        int healthUpgradeLevel = getHealthUpgradeLevel();
        player.setMaxHealth(player.getMaxHealth() + (HEALTH_PER_UPGRADE * healthUpgradeLevel));
        player.setCurrentHealth(player.getMaxHealth());

        // Применяем улучшения энергии
        int energyUpgradeLevel = getEnergyUpgradeLevel();
        player.setMaxEnergy(player.getMaxEnergy() + (ENERGY_PER_UPGRADE * energyUpgradeLevel));
        player.setCurrentEnergy(player.getMaxEnergy());

        // Применяем улучшения скорости, но не превышаем максимальную скорость
        int speedUpgradeLevel = getSpeedUpgradeLevel();
        float newSpeed = player.getSpeed() + (SPEED_PER_UPGRADE * speedUpgradeLevel);
        player.setSpeed(Math.min(newSpeed, player.getMaxSpeed()));

        // Применяем улучшения регенерации здоровья
        int regenUpgradeLevel = getRegenUpgradeLevel();
        player.setHealthRegeneration(player.getHealthRegeneration() + (REGEN_PER_UPGRADE * regenUpgradeLevel));

        // Применяем улучшения регенерации энергии
        int energyRegenUpgradeLevel = getEnergyRegenUpgradeLevel();
        player.setEnergyRegeneration(player.getEnergyRegeneration() + (ENERGY_REGEN_PER_UPGRADE * energyRegenUpgradeLevel));
    }

    /**
     * Получает текущий режим управления игрока
     *
     * @return 0 - джойстик, 1 - касание на экран
     */
    public static int getControlMode() {
        return prefs.getInteger(PREF_KEY_CONTROL_MODE, 0);
    }

    /**
     * Устанавливает режим управления игрока
     *
     * @param mode 0 - джойстик, 1 - касание на экран
     */
    public static void setControlMode(int mode) {
        prefs.putInteger(PREF_KEY_CONTROL_MODE, mode);
        prefs.flush();
        LogHelper.log("PlayerData", "Control mode set to: " + mode);
    }

    /**
     * Получает уровень игрока
     *
     * @return текущий уровень
     */
    public static int getPlayerLevel() {
        return prefs.getInteger(PREF_KEY_PLAYER_LEVEL, 1);
    }

    /**
     * Сохраняет уровень игрока
     *
     * @param level новый уровень игрока
     */
    public static void savePlayerLevel(int level) {
        prefs.putInteger(PREF_KEY_PLAYER_LEVEL, level);
        prefs.flush();
        LogHelper.log("PlayerData", "Saved player level: " + level);
    }

    public enum DebuffType {
        SLOW,             // Замедление
        DAMAGE_OVER_TIME, // Урон со временем
        ARMOR_REDUCTION,  // Снижение защиты
        ATTACK_REDUCTION, // Снижение атаки
        STUN,             // Оглушение
    }
}
