package ru.myitschool.rogal.CustomHelpers.Controls;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class TouchpadHelpers{
    /**
     *  Функции создания Джостика
     * @param backgroundTexture текстура заднего фона джостика
     * @param touchpadKnobTexture текстура движущиеся части
     * @param deadzoneRadius чувствительность(По умолчанию 0)
     * @P.S. Кардинаты необходимо менять самостоятельно(По умолчанию x = 0; y = 0)
     */
    public static Touchpad createTouchpad(Texture backgroundTexture, Texture touchpadKnobTexture, float deadzoneRadius){
        Drawable background = new TextureRegionDrawable(backgroundTexture);
        Drawable touchpadKnob  = new TextureRegionDrawable(touchpadKnobTexture);
        //
        Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle(background, touchpadKnob);
        Touchpad touchpad = new Touchpad(deadzoneRadius, style);
        return touchpad;
    }
    public static Touchpad createTouchpad(String backgroundTexture, String touchpadKnobTexture, float deadzoneRadius){
        Drawable background = new TextureRegionDrawable(new Texture(backgroundTexture));
        Drawable touchpadKnob  = new TextureRegionDrawable(new Texture(touchpadKnobTexture));
        //
        Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle(background, touchpadKnob);
        Touchpad touchpad = new Touchpad(deadzoneRadius, style);
        return touchpad;
    }
    public static Touchpad createTouchpad(Texture backgroundTexture, Texture touchpadKnobTexture){
        return createTouchpad(backgroundTexture, touchpadKnobTexture, 0);
    }
    public static Touchpad createTouchpad(String backgroundTexture, String touchpadKnobTexture){
        return  createTouchpad(backgroundTexture, touchpadKnobTexture, 0);
    }

    /**
     * Создает джойстик с учетом масштаба UI
     *
     * @param backgroundTexture   текстура заднего фона джойстика
     * @param touchpadKnobTexture текстура движущейся части
     * @param deadzoneRadius      чувствительность (по умолчанию 0)
     * @param uiScale             масштаб интерфейса
     * @return созданный джойстик с примененным масштабом
     */
    public static Touchpad createScaledTouchpad(String backgroundTexture, String touchpadKnobTexture, float deadzoneRadius, float uiScale) {
        Touchpad touchpad = createTouchpad(backgroundTexture, touchpadKnobTexture, deadzoneRadius);
        // Применяем масштаб к размеру джойстика
        touchpad.setSize(touchpad.getWidth() * uiScale, touchpad.getHeight() * uiScale);
        return touchpad;
    }

    /**
     * Создает джойстик с учетом масштаба UI (без указания чувствительности)
     *
     * @param backgroundTexture   текстура заднего фона джойстика
     * @param touchpadKnobTexture текстура движущейся части
     * @param uiScale             масштаб интерфейса
     * @return созданный джойстик с примененным масштабом
     */
    public static Touchpad createScaledTouchpad(String backgroundTexture, String touchpadKnobTexture, float uiScale) {
        return createScaledTouchpad(backgroundTexture, touchpadKnobTexture, 0, uiScale);
    }
}
