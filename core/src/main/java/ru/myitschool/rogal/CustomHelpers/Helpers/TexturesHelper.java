package ru.myitschool.rogal.CustomHelpers.Helpers;


import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class TexturesHelper {
    /**
     * <p>Масивы текстур.
     * <p>В основном для анимаций
     *
     * @param textures могут получать масивы Texture и String(сылки на текстуры)
     * */
    public static TextureRegion[] textureReionsArray(Texture[] textures){
        TextureRegion[] t = new TextureRegion[textures.length];
        for (int i = 0; i < textures.length; i++) {
            t[i] = new TextureRegion(textures[i]);
        }
        return t;
    }
    public static TextureRegion[] textureReionsArray(String[] textures){
        TextureRegion[] t = new TextureRegion[textures.length];
        for (int i = 0; i < textures.length; i++) {
            t[i] = new TextureRegion(new Texture(textures[i]));
        }
        return t;
    }

    public static Texture[] textureArray(String[] textures){
        Texture[] t = new Texture[textures.length];
        for (int i = 0; i < textures.length; i++) {
            t[i] = new Texture(textures[i]);
        }
        return t;
    }
}
