package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Disposable;

/**
 * Утилита для упрощения создания UI в libGDX
 */
public class UIHelper implements Disposable {
    private final Skin skin;
    private Table rootTable;

    /**
     * Конструктор
     * @param stage сцена для отображения UI
     */
    public UIHelper(Stage stage) {
        this.skin = createSkin();
        this.rootTable = new Table();
        this.rootTable.setFillParent(true);
        stage.addActor(rootTable);
    }

    /**
     * Создает базовый скин для UI элементов
     */
    private Skin createSkin() {
        Skin uiSkin = new Skin();

        // Добавляем шрифт
        uiSkin.add("default", FontManager.getRegularFont(), com.badlogic.gdx.graphics.g2d.BitmapFont.class);

        // Создаем базовые текстуры
        createBaseTextures(uiSkin);

        // Создаем стили для UI элементов
        createStyles(uiSkin);

        return uiSkin;
    }

    /**
     * Создает базовые текстуры для UI
     */
    private void createBaseTextures(Skin skin) {
        // Создаем полупрозрачный фон для панелей
        Pixmap panelPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        panelPixmap.setColor(new Color(0.1f, 0.1f, 0.2f, 0.8f));
        panelPixmap.fill();
        skin.add("panel-background", new Texture(panelPixmap));
        panelPixmap.dispose();

        // Создаем базовую белую текстуру
        Pixmap whitePixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePixmap.setColor(Color.WHITE);
        whitePixmap.fill();
        skin.add("white", new Texture(whitePixmap));
        whitePixmap.dispose();
    }

    /**
     * Создает стили для UI элементов
     */
    private void createStyles(Skin skin) {
        // Стиль для меток
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // Стиль для кнопок
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = skin.getFont("default");
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.up = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.5f, 0.8f));
        buttonStyle.down = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.4f, 0.9f));
        buttonStyle.over = skin.newDrawable("white", new Color(0.4f, 0.4f, 0.6f, 0.8f));
        skin.add("default", buttonStyle);

        // Стиль для полос прогресса
        ProgressBar.ProgressBarStyle progressBarStyle = new ProgressBar.ProgressBarStyle();
        progressBarStyle.background = skin.newDrawable("white", new Color(0.2f, 0.1f, 0.1f, 0.8f));
        progressBarStyle.knob = skin.newDrawable("white", new Color(0.2f, 0.9f, 0.2f, 1f));
        progressBarStyle.knobBefore = progressBarStyle.knob;
        skin.add("default-horizontal", progressBarStyle);
    }

    /**
     * Создает панель с фоном
     * @param width ширина панели
     * @param height высота панели
     * @param color цвет фона
     * @return созданная панель
     */
    public Table createPanel(float width, float height, Color color) {
        Table panel = new Table();
        panel.setBackground(skin.newDrawable("white", color));
        panel.setSize(width, height);
        return panel;
    }

    /**
     * Создает метку
     * @param text текст метки
     * @param color цвет текста
     * @return созданная метка
     */
    public Label createLabel(String text, Color color) {
        Label.LabelStyle style = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
        style.fontColor = color;
        return new Label(text, style);
    }

    /**
     * Создает кнопку
     * @param text текст кнопки
     * @param color цвет кнопки
     * @return созданная кнопка
     */
    public TextButton createButton(String text, Color color) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(skin.get("default", TextButton.TextButtonStyle.class));
        style.up = skin.newDrawable("white", color);
        style.down = skin.newDrawable("white", color.cpy().mul(0.8f));
        style.over = skin.newDrawable("white", color.cpy().mul(1.2f));
        return new TextButton(text, style);
    }

    /**
     * Создает полосу прогресса
     * @param width ширина полосы
     * @param height высота полосы
     * @param min минимальное значение
     * @param max максимальное значение
     * @param color цвет полосы
     * @return созданная полоса прогресса
     */
    public ProgressBar createProgressBar(float width, float height, float min, float max, Color color) {
        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle(skin.get("default-horizontal", ProgressBar.ProgressBarStyle.class));
        style.knob = skin.newDrawable("white", color);
        style.knobBefore = style.knob;
        ProgressBar bar = new ProgressBar(min, max, 1, false, style);
        bar.setSize(width, height);
        return bar;
    }

    /**
     * Создает окно
     * @param title заголовок окна
     * @param width ширина окна
     * @param height высота окна
     * @return созданное окно
     */
    public Window createWindow(String title, float width, float height) {
        Window.WindowStyle style = new Window.WindowStyle();
        style.titleFont = skin.getFont("default");
        style.titleFontColor = Color.WHITE;
        style.background = skin.newDrawable("white", new Color(0.1f, 0.1f, 0.2f, 0.9f));
        
        Window window = new Window(title, style);
        window.setSize(width, height);
        window.setMovable(true);
        window.setResizable(false);
        return window;
    }

    /**
     * Создает диалог
     * @param title заголовок диалога
     * @param message сообщение
     * @return созданный диалог
     */
    public Dialog createDialog(String title, String message) {
        Dialog dialog = new Dialog(title, skin);
        dialog.text(message);
        dialog.button("OK");
        return dialog;
    }

    /**
     * Добавляет актора в корневую таблицу
     * @param actor актор для добавления
     */
    public void addToRoot(Actor actor) {
        rootTable.add(actor).row();
    }

    /**
     * Очищает корневую таблицу
     */
    public void clearRoot() {
        rootTable.clear();
    }

    /**
     * Освобождает ресурсы
     */
    @Override
    public void dispose() {
        skin.dispose();
    }
} 