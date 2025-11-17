package ru.myitschool.rogal;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

import ru.myitschool.rogal.CustomHelpers.utils.FontManager;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
import ru.myitschool.rogal.CustomHelpers.utils.PlayerData;
import ru.myitschool.rogal.Screens.StartScreen;
import ru.myitschool.rogal.networking.LeaderboardAPI;

public class Main extends Game {
    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 720;

    public static String VERSION = "1.1.1";

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        LogHelper.log("Main", "Игра запущена. Версия: " + VERSION);

        FontManager.initialize();
        PlayerData.initialize();
        LeaderboardAPI.initialize();

        setScreen(new StartScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();

        FontManager.dispose();
    }
}
