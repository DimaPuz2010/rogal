package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * Класс для управления шрифтами в игре
 */
public class FontManager {

    private static BitmapFont titleFont;
    private static BitmapFont buttonFont;
    private static BitmapFont regularFont;
    private static BitmapFont smallFont;

    /**
     * Инициализирует все шрифты для игры
     */
    public static void initialize() {
        String path = "fonts/NeuePixelSans.ttf";
        // Создаем шрифт для заголовков
        titleFont = generateFont(path, 48, Color.WHITE);

        // Создаем шрифт для кнопок
        buttonFont = generateFont(path, 32, Color.WHITE);

        // Создаем шрифт для обычного текста
        regularFont = generateFont(path, 20, Color.WHITE);

        // Создаем малый шрифт для таблиц
        smallFont = generateFont(path, 16, Color.WHITE);
    }

    /**
     * Генерирует шрифт из TTF файла
     *
     * @param fontPath путь к файлу шрифта
     * @param size размер шрифта
     * @param color цвет шрифта
     * @return сгенерированный шрифт
     */
    public static BitmapFont generateFont(String fontPath, int size, Color color) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.color = color;
        parameter.borderWidth = 1;
        parameter.borderColor = Color.BLACK;
        parameter.shadowOffsetX = 1;
        parameter.shadowOffsetY = 1;
        parameter.shadowColor = new Color(0, 0, 0, 0.5f);
        // Улучшаем сглаживание шрифта
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.genMipMaps = true;
        // Настройки хинтинга для улучшения отображения
        parameter.hinting = FreeTypeFontGenerator.Hinting.AutoMedium;
        // Добавляем поддержку кириллицы
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";

        BitmapFont font = generator.generateFont(parameter);
        font.getRegion().getTexture().setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        generator.dispose();

        return font;
    }

    public static BitmapFont getTitleFont() {
        return titleFont;
    }

    public static BitmapFont getButtonFont() {
        return buttonFont;
    }

    public static BitmapFont getRegularFont() {
        return regularFont;
    }

    public static BitmapFont getSmallFont() {
        return smallFont;
    }

    public static void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (regularFont != null) regularFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
