package ru.myitschool.rogal;

import com.badlogic.gdx.Game;

import ru.myitschool.rogal.Screens.StartScreen;
import ru.myitschool.rogal.CustomHelpers.utils.FontManager;

public class Main extends Game {
    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 720;

    @Override
    public void create() {
        FontManager.initialize();

        setScreen(new StartScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();

        FontManager.dispose();
    }
}
