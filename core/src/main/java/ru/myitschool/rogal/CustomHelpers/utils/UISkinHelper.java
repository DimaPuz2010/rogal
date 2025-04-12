package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.util.HashMap;
import java.util.Map;

/**
 * Вспомогательный класс для создания скина UI элементов
 */
public class UISkinHelper {
    private static Map<String, Texture> textures = new HashMap<>();
    
    /**
     * Создает и инициализирует скин для UI-элементов
     * @return созданный скин
     */
    public static Skin createSkin() {
        Skin skin = new Skin();
        
        generateTextures();
        
        skin.add("default", FontManager.getRegularFont(), BitmapFont.class);
    
        for (Map.Entry<String, Texture> entry : textures.entrySet()) {
            skin.add(entry.getKey(), entry.getValue());
        }
        
        createScrollPaneStyle(skin);
        createListStyle(skin);
        createSelectBoxStyle(skin);
        createCheckBoxStyle(skin);
        createSliderStyle(skin);
        createWindowStyle(skin);
        createTextButtonStyle(skin);
        createLabelStyle(skin);
        
        return skin;
    }
    
    /**
     * Генерирует все необходимые текстуры для UI-элементов
     */
    private static void generateTextures() {
        // Создаем белую текстуру
        Pixmap whitePixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePixmap.setColor(Color.WHITE);
        whitePixmap.fill();
        textures.put("white", new Texture(whitePixmap));
        whitePixmap.dispose();

        // Чекбокс включенный
        Pixmap checkboxOnPixmap = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        checkboxOnPixmap.setColor(Color.WHITE);
        checkboxOnPixmap.drawRectangle(0, 0, 20, 20);
        checkboxOnPixmap.fillRectangle(4, 4, 12, 12);
        textures.put("checkbox-on", new Texture(checkboxOnPixmap));
        checkboxOnPixmap.dispose();
        
        // Чекбокс выключенный
        Pixmap checkboxOffPixmap = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        checkboxOffPixmap.setColor(Color.WHITE);
        checkboxOffPixmap.drawRectangle(0, 0, 20, 20);
        textures.put("checkbox-off", new Texture(checkboxOffPixmap));
        checkboxOffPixmap.dispose();
        
        // SelectBox фон
        Pixmap selectBoxPixmap = new Pixmap(100, 30, Pixmap.Format.RGBA8888);
        selectBoxPixmap.setColor(new Color(0.2f, 0.2f, 0.4f, 1));
        selectBoxPixmap.fill();
        selectBoxPixmap.setColor(Color.WHITE);
        selectBoxPixmap.drawRectangle(0, 0, 100, 30);
        textures.put("selectbox", new Texture(selectBoxPixmap));
        selectBoxPixmap.dispose();
        
        // Выделение элемента списка
        Pixmap selectionPixmap = new Pixmap(100, 30, Pixmap.Format.RGBA8888);
        selectionPixmap.setColor(new Color(0.4f, 0.4f, 0.6f, 1));
        selectionPixmap.fill();
        textures.put("selection", new Texture(selectionPixmap));
        selectionPixmap.dispose();
        
        // Фон для ScrollPane
        Pixmap scrollPanePixmap = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
        scrollPanePixmap.setColor(new Color(0.2f, 0.2f, 0.4f, 0.8f));
        scrollPanePixmap.fill();
        textures.put("scrollpane", new Texture(scrollPanePixmap));
        scrollPanePixmap.dispose();
        
        // Горизонтальный скроллбар
        Pixmap scrollBarHPixmap = new Pixmap(100, 10, Pixmap.Format.RGBA8888);
        scrollBarHPixmap.setColor(new Color(0.3f, 0.3f, 0.5f, 1));
        scrollBarHPixmap.fill();
        textures.put("scrollbar-h", new Texture(scrollBarHPixmap));
        scrollBarHPixmap.dispose();
        
        // Вертикальный скроллбар
        Pixmap scrollBarVPixmap = new Pixmap(10, 100, Pixmap.Format.RGBA8888);
        scrollBarVPixmap.setColor(new Color(0.3f, 0.3f, 0.5f, 1));
        scrollBarVPixmap.fill();
        textures.put("scrollbar-v", new Texture(scrollBarVPixmap));
        scrollBarVPixmap.dispose();
        
        // Ползунок горизонтального скроллбара
        Pixmap scrollBarHKnobPixmap = new Pixmap(30, 10, Pixmap.Format.RGBA8888);
        scrollBarHKnobPixmap.setColor(Color.WHITE);
        scrollBarHKnobPixmap.fill();
        textures.put("scrollbar-knob-h", new Texture(scrollBarHKnobPixmap));
        scrollBarHKnobPixmap.dispose();
        
        // Ползунок вертикального скроллбара
        Pixmap scrollBarVKnobPixmap = new Pixmap(10, 30, Pixmap.Format.RGBA8888);
        scrollBarVKnobPixmap.setColor(Color.WHITE);
        scrollBarVKnobPixmap.fill();
        textures.put("scrollbar-knob-v", new Texture(scrollBarVKnobPixmap));
        scrollBarVKnobPixmap.dispose();
        
        // Фон для слайдера
        Pixmap sliderBgPixmap = new Pixmap(200, 10, Pixmap.Format.RGBA8888);
        sliderBgPixmap.setColor(new Color(0.2f, 0.2f, 0.4f, 1));
        sliderBgPixmap.fill();
        sliderBgPixmap.setColor(Color.WHITE);
        sliderBgPixmap.drawRectangle(0, 0, 200, 10);
        textures.put("slider-bg", new Texture(sliderBgPixmap));
        sliderBgPixmap.dispose();
        
        // Ползунок для слайдера
        Pixmap sliderKnobPixmap = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        sliderKnobPixmap.setColor(new Color(0.4f, 0.4f, 0.7f, 1));
        sliderKnobPixmap.fillCircle(10, 10, 10);
        sliderKnobPixmap.setColor(Color.WHITE);
        sliderKnobPixmap.drawCircle(10, 10, 10);
        textures.put("slider-knob", new Texture(sliderKnobPixmap));
        sliderKnobPixmap.dispose();

        // Фон для окна
        Pixmap windowBgPixmap = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
        windowBgPixmap.setColor(new Color(0.15f, 0.15f, 0.2f, 0.9f));
        windowBgPixmap.fill();
        textures.put("window-bg", new Texture(windowBgPixmap));
        windowBgPixmap.dispose();

        // Кнопка в обычном состоянии
        Pixmap buttonUpPixmap = new Pixmap(100, 50, Pixmap.Format.RGBA8888);
        buttonUpPixmap.setColor(new Color(0.3f, 0.3f, 0.5f, 0.8f));
        buttonUpPixmap.fill();
        textures.put("button-up", new Texture(buttonUpPixmap));
        buttonUpPixmap.dispose();

        // Кнопка при нажатии
        Pixmap buttonDownPixmap = new Pixmap(100, 50, Pixmap.Format.RGBA8888);
        buttonDownPixmap.setColor(new Color(0.2f, 0.2f, 0.4f, 0.8f));
        buttonDownPixmap.fill();
        textures.put("button-down", new Texture(buttonDownPixmap));
        buttonDownPixmap.dispose();

        // Кнопка при наведении
        Pixmap buttonOverPixmap = new Pixmap(100, 50, Pixmap.Format.RGBA8888);
        buttonOverPixmap.setColor(new Color(0.4f, 0.4f, 0.6f, 0.8f));
        buttonOverPixmap.fill();
        textures.put("button-over", new Texture(buttonOverPixmap));
        buttonOverPixmap.dispose();
    }
    
    /**
     * Создает стиль для чекбокса
     * @param skin скин для добавления стиля
     */
    private static void createCheckBoxStyle(Skin skin) {
        CheckBoxStyle checkBoxStyle = new CheckBoxStyle();
        checkBoxStyle.checkboxOn = new TextureRegionDrawable(new TextureRegion(skin.get("checkbox-on", Texture.class)));
        checkBoxStyle.checkboxOff = new TextureRegionDrawable(new TextureRegion(skin.get("checkbox-off", Texture.class)));
        checkBoxStyle.font = skin.getFont("default");
        checkBoxStyle.fontColor = Color.WHITE;
        skin.add("default", checkBoxStyle);
    }
    
    /**
     * Создает стиль для выпадающего списка
     * @param skin скин для добавления стиля
     */
    private static void createSelectBoxStyle(Skin skin) {
        SelectBoxStyle selectBoxStyle = new SelectBoxStyle();
        selectBoxStyle.font = skin.getFont("default");
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = new TextureRegionDrawable(new TextureRegion(skin.get("selectbox", Texture.class)));
        selectBoxStyle.scrollStyle = skin.get(ScrollPaneStyle.class);
        selectBoxStyle.listStyle = skin.get(ListStyle.class);
        skin.add("default", selectBoxStyle);
    }
    
    /**
     * Создает стиль для списка
     * @param skin скин для добавления стиля
     */
    private static void createListStyle(Skin skin) {
        ListStyle listStyle = new ListStyle();
        listStyle.font = skin.getFont("default");
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = Color.LIGHT_GRAY;
        listStyle.selection = new TextureRegionDrawable(new TextureRegion(skin.get("selection", Texture.class)));
        skin.add("default", listStyle);
    }
    
    /**
     * Создает стиль для прокручиваемой панели
     * @param skin скин для добавления стиля
     */
    private static void createScrollPaneStyle(Skin skin) {
        ScrollPaneStyle scrollPaneStyle = new ScrollPaneStyle();
        scrollPaneStyle.background = new TextureRegionDrawable(new TextureRegion(skin.get("scrollpane", Texture.class)));
        scrollPaneStyle.hScroll = new TextureRegionDrawable(new TextureRegion(skin.get("scrollbar-h", Texture.class)));
        scrollPaneStyle.vScroll = new TextureRegionDrawable(new TextureRegion(skin.get("scrollbar-v", Texture.class)));
        scrollPaneStyle.hScrollKnob = new TextureRegionDrawable(new TextureRegion(skin.get("scrollbar-knob-h", Texture.class)));
        scrollPaneStyle.vScrollKnob = new TextureRegionDrawable(new TextureRegion(skin.get("scrollbar-knob-v", Texture.class)));
        skin.add("default", scrollPaneStyle);
    }
    
    /**
     * Создает стиль для слайдера
     * @param skin скин для добавления стиля
     */
    private static void createSliderStyle(Skin skin) {
        SliderStyle sliderStyle = new SliderStyle();
        sliderStyle.background = new TextureRegionDrawable(new TextureRegion(skin.get("slider-bg", Texture.class)));
        sliderStyle.knob = new TextureRegionDrawable(new TextureRegion(skin.get("slider-knob", Texture.class)));
        
        // Регистрируем стиль под именем 'default-horizontal', которое запрашивается в коде
        skin.add("default-horizontal", sliderStyle);
    }

    private static void createWindowStyle(Skin skin) {
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = skin.getFont("default");
        windowStyle.background = new TextureRegionDrawable(new TextureRegion(skin.get("window-bg", Texture.class)));
        windowStyle.titleFontColor = Color.WHITE;
        skin.add("default", windowStyle);
    }

    private static void createTextButtonStyle(Skin skin) {
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = skin.getFont("default");
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.up = new TextureRegionDrawable(new TextureRegion(skin.get("button-up", Texture.class)));
        buttonStyle.down = new TextureRegionDrawable(new TextureRegion(skin.get("button-down", Texture.class)));
        buttonStyle.over = new TextureRegionDrawable(new TextureRegion(skin.get("button-over", Texture.class)));
        skin.add("default", buttonStyle);
    }

    private static void createLabelStyle(Skin skin) {
        // Базовый стиль для Label
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // Стиль для заголовков
        Label.LabelStyle titleStyle = new Label.LabelStyle(labelStyle);
        titleStyle.fontColor = new Color(1f, 0.8f, 0.2f, 1f);
        skin.add("title", titleStyle);
    }
    
    /**
     * Освобождает все ресурсы
     */
    public static void dispose() {
        for (Texture texture : textures.values()) {
            if (texture != null) {
                texture.dispose();
            }
        }
        textures.clear();
    }
} 