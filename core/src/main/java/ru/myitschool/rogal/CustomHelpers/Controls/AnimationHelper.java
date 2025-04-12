package ru.myitschool.rogal.CustomHelpers.Controls;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import ru.myitschool.rogal.CustomHelpers.Helpers.TexturesHelper;

public class AnimationHelper {
    public static Animation<TextureRegion> createAnimation(TextureRegion[] array){
        Animation.PlayMode playMode = Animation.PlayMode.LOOP;
        float frameDuration = 1 / 6F;
        Array<TextureRegion> a = new Array<>();
        for (int i = 0; i < array.length; i++) {
            a.add(array[i]);
        }
        return new Animation<>(frameDuration, a, playMode);
    }
    public static Animation<TextureRegion> createAnimation(String[] textures){
        return createAnimation(TexturesHelper.textureReionsArray(textures));
    }
    public static Animation<TextureRegion> createAnimation(Texture[] textures){
        return createAnimation(TexturesHelper.textureReionsArray(textures));
    }

}
