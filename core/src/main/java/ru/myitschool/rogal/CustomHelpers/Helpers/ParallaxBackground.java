package ru.myitschool.rogal.CustomHelpers.Helpers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

public class ParallaxBackground {
    private Texture texture;
    private OrthographicCamera camera;
    private SpriteBatch batch;

    private float textureWidth;
    private float textureHeight;
    private float scale = 1.0f;

    public ParallaxBackground(String texturePath, OrthographicCamera camera) {
        this.camera = camera;

        // Загружаем текстуру фона
        texture = new Texture(texturePath);

        // Устанавливаем режим повторения текстуры
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        textureWidth = texture.getWidth() * scale;
        textureHeight = texture.getHeight() * scale;

        batch = new SpriteBatch();
    }

    public void render() {
        // Сохраняем позицию камеры
        Vector3 cameraPos = camera.position;

        // Определяем границы области рендеринга
        int tilesX = (int) Math.ceil(camera.viewportWidth / textureWidth) + 2;
        int tilesY = (int) Math.ceil(camera.viewportHeight / textureHeight) + 2;

        // Вычисляем начальные координаты
        float startX = cameraPos.x - camera.viewportWidth / 2;
        float startY = cameraPos.y - camera.viewportHeight / 2;

        // Корректируем начальные координаты для тайлинга
        float offsetX = startX % textureWidth;
        float offsetY = startY % textureHeight;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Рисуем необходимое количество тайлов для покрытия экрана
        for (int x = -1; x < tilesX; x++) {
            for (int y = -1; y < tilesY; y++) {
                batch.draw(
                    texture,
                    startX - offsetX + x * textureWidth,
                    startY - offsetY + y * textureHeight,
                    textureWidth,
                    textureHeight
                );
            }
        }

        batch.end();
    }

    public void setScale(float scale) {
        this.scale = scale;
        textureWidth = texture.getWidth() * scale;
        textureHeight = texture.getHeight() * scale;
    }

    public void dispose() {
        texture.dispose();
        batch.dispose();
    }
}
