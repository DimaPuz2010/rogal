package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Класс для создания кнопок с пиксельной графикой
 */
public class ButtonCreator {

    /**
     * Создает стандартную кнопку с заданным текстом и шрифтом
     * @param text текст кнопки
     * @param font шрифт для текста
     * @return созданная кнопка
     */
    public static TextButton createButton(String text, int width, int height, BitmapFont font) {
        Texture upTexture = createButtonTexture(width, height, new Color(0.3f, 0.3f, 0.8f, 0.8f));
        Texture downTexture = createButtonTexture(width, height, new Color(0.2f, 0.2f, 0.6f, 0.8f));

        TextButton.TextButtonStyle style = createButtonStyle(font, upTexture, downTexture);

        return new TextButton(text, style);
    }
    public static TextButton createButton(String text, BitmapFont font) {
        return createButton(text, 200, 80, font);
    }

    /**
     * Создает текстуру для кнопки заданного размера и цвета
     * @param width ширина кнопки
     * @param height высота кнопки
     * @param color цвет кнопки
     * @return текстура кнопки
     */
    private static Texture createButtonTexture(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillRectangle(0, 0, width, height);

        // Создаем пиксельную рамку вокруг кнопки
        pixmap.setColor(Color.WHITE);
        pixmap.drawRectangle(0, 0, width, height);

        // Добавляем внутреннюю рамку для пиксельного эффекта
        pixmap.setColor(new Color(1, 1, 1, 0.5f));
        pixmap.drawRectangle(2, 2, width - 4, height - 4);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        return texture;
    }

    /**
     * Создает стиль кнопки с заданными параметрами
     * @param font шрифт текста
     * @param upTexture текстура в нормальном состоянии
     * @param downTexture текстура при нажатии
     * @return стиль кнопки
     */
    private static TextButton.TextButtonStyle createButtonStyle(BitmapFont font, Texture upTexture, Texture downTexture) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.up = new TextureRegionDrawable(new TextureRegion(upTexture));
        style.down = new TextureRegionDrawable(new TextureRegion(downTexture));
        style.over = new TextureRegionDrawable(new TextureRegion(upTexture));

        return style;
    }

    /**
     * Создает фон для таблиц и панелей указанного цвета
     *
     * @param color цвет фона
     * @return drawable объект для использования в качестве фона
     */
    public static Drawable createBackgroundDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        return new TextureRegionDrawable(texture);
    }

    /**
     * Создает фон для строк таблицы лидеров
     *
     * @param color цвет фона
     * @return drawable объект для использования в качестве фона
     */
    public static Drawable createBackground(Color color) {
        return createBackgroundDrawable(color);
    }
}
