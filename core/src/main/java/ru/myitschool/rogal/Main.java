package ru.myitschool.rogal;

import com.badlogic.gdx.Game;

import ru.myitschool.rogal.CustomHelpers.utils.FontManager;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
import ru.myitschool.rogal.CustomHelpers.utils.PlayerData;
import ru.myitschool.rogal.Screens.StartScreen;
import ru.myitschool.rogal.networking.LeaderboardAPI;

public class Main extends Game {
    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 720;

    // Сохраняем экземпляр для доступа из других классов
    private static Main instance;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void create() {
        instance = this;

        // Инициализация ресурсов
        FontManager.initialize();

        // Инициализация данных игрока
        PlayerData.initialize();

        // Инициализация API таблицы лидеров
        LeaderboardAPI.initialize();

        // Переход на начальный экран
        setScreen(new StartScreen(this));

        // Логируем информацию о запуске
        LogHelper.log("Main", "Game initialized");
    }

    @Override
    public void dispose() {
        super.dispose();

        // Освобождаем ресурсы
        FontManager.dispose();

        LogHelper.log("Main", "Game disposed");
    }
}
