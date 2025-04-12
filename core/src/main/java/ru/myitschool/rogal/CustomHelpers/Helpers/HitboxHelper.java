package ru.myitschool.rogal.CustomHelpers.Helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.Array;

/**
 * Вспомогательный класс для создания хитбоксов по изображению
 */
public class HitboxHelper {

    private static ShapeRenderer shapeRenderer;

    /**
     * Создает оптимизированный хитбокс для изображения
     * @param texturePath путь к файлу текстуры
     * @param scale масштаб для хитбокса
     * @param approximation количество точек для аппроксимации (меньше = меньше точек, быстрее работает)
     * @return полигон для использования в качестве хитбокса
     */
    public static Polygon createHitboxFromTexture(String texturePath, float scale, int approximation) {
        // Загружаем текстуру и преобразуем в Pixmap для доступа к пикселям
        Texture texture = new Texture(Gdx.files.internal(texturePath));
        texture.getTextureData().prepare();
        Pixmap pixmap = texture.getTextureData().consumePixmap();

        // Создаем хитбокс
        Polygon hitbox = createHitboxFromPixmap(pixmap, approximation, scale);

        // Освобождаем ресурсы
        pixmap.dispose();

        return hitbox;
    }

    /**
     * Создает оптимизированный хитбокс для текстуры
     * @param texture готовая текстура
     * @param scale масштаб для хитбокса
     * @param approximation количество точек для аппроксимации
     * @return полигон для использования в качестве хитбокса
     */
    public static Polygon createHitboxFromTexture(Texture texture, float scale, int approximation) {
        texture.getTextureData().prepare();
        Pixmap pixmap = texture.getTextureData().consumePixmap();

        Polygon hitbox = createHitboxFromPixmap(pixmap, approximation, scale);

        pixmap.dispose();

        return hitbox;
    }

    /**
     * Создает хитбокс на основе TextureRegion
     * @param region регион текстуры
     * @param scale масштаб для хитбокса
     * @param approximation количество точек для аппроксимации
     * @return полигон для использования в качестве хитбокса
     */
    public static Polygon createHitboxFromTextureRegion(TextureRegion region, float scale, int approximation) {
        return createHitboxFromTexture(region.getTexture(), scale, approximation);
    }

    /**
     * Создает простой прямоугольный хитбокс
     * @param width ширина
     * @param height высота
     * @param originX центр по X
     * @param originY центр по Y
     * @return прямоугольный полигон
     */
    public static Polygon createRectangleHitbox(float width, float height, float originX, float originY) {
        float[] vertices = new float[] {
            0, 0,
            width, 0,
            width, height,
            0, height
        };

        Polygon hitbox = new Polygon(vertices);
        hitbox.setOrigin(originX, originY);
        return hitbox;
    }

    /**
     * Создает круговой хитбокс
     * @param radius радиус круга
     * @param segments количество сегментов для аппроксимации круга
     * @return полигон, аппроксимирующий круг
     */
    public static Polygon createCircleHitbox(float radius, int segments) {
        float[] vertices = new float[segments * 2];

        for (int i = 0; i < segments; i++) {
            float angle = i * 360f / segments * MathUtils.degreesToRadians;
            vertices[i * 2] = radius + MathUtils.cos(angle) * radius;
            vertices[i * 2 + 1] = radius + MathUtils.sin(angle) * radius;
        }

        Polygon hitbox = new Polygon(vertices);
        hitbox.setOrigin(radius, radius);
        return hitbox;
    }

    /**
     * Создает треугольный хитбокс
     * @param width ширина
     * @param height высота
     * @return треугольный полигон
     */
    public static Polygon createTriangleHitbox(float width, float height) {
        float[] vertices = new float[] {
            width / 2, height,  // вершина
            0, 0,              // левый нижний угол
            width, 0           // правый нижний угол
        };

        Polygon hitbox = new Polygon(vertices);
        hitbox.setOrigin(width / 2, height / 2);
        return hitbox;
    }

    /**
     * Создает хитбокс на основе Pixmap с определенным уровнем аппроксимации
     * @param pixmap изображение для анализа
     * @param approximation уровень аппроксимации (выше значение = меньше точек)
     * @param scale масштаб для хитбокса
     * @return полигон для использования в качестве хитбокса
     */
    private static Polygon createHitboxFromPixmap(Pixmap pixmap, int approximation, float scale) {
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();

        // Определяем границы непрозрачных пикселей
        Array<Float> points = new Array<>();

        // Шаг аппроксимации - определяет, сколько пикселей пропускаем
        int step = Math.max(1, Math.min(width, height) / approximation);

        // Находим контурные точки по периметру в 8 направлениях
        findEdgePoints(pixmap, points, width, height, step);

        // Конвертируем в массив float[] для создания полигона
        float[] vertices = new float[points.size];
        for (int i = 0; i < points.size; i++) {
            vertices[i] = points.get(i) * scale;
        }

        // Создаем полигон и устанавливаем начало координат в центр
        Polygon hitbox = new Polygon(vertices);
        hitbox.setOrigin(width * scale / 2, height * scale / 2);

        return hitbox;
    }

    /**
     * Находит точки на краях объекта
     * @param pixmap пиксельное представление изображения
     * @param points список для сохранения найденных точек (x1, y1, x2, y2, ...)
     * @param width ширина изображения
     * @param height высота изображения
     * @param step шаг проверки для оптимизации
     */
    private static void findEdgePoints(Pixmap pixmap, Array<Float> points, int width, int height, int step) {
        // Ищем только по контуру для оптимизации

        // Максимальное количество точек для конкретного объекта
        int maxPoints = 100; // Увеличьте для более сложных форм
        int pointCount = 0;

        // Обходим периметр объекта
        // Верхняя граница
        for (int x = 0; x < width; x += step) {
            for (int y = 0; y < height; y += step) {
                if (isPixelOpaque(pixmap, x, y)) {
                    addPoint(points, x, y);
                    pointCount++;
                    break;
                }
            }
            if (pointCount >= maxPoints) break;
        }

        // Правая граница
        for (int y = 0; y < height; y += step) {
            for (int x = width - 1; x >= 0; x -= step) {
                if (isPixelOpaque(pixmap, x, y)) {
                    addPoint(points, x, y);
                    pointCount++;
                    break;
                }
            }
            if (pointCount >= maxPoints) break;
        }

        // Нижняя граница
        for (int x = width - 1; x >= 0; x -= step) {
            for (int y = height - 1; y >= 0; y -= step) {
                if (isPixelOpaque(pixmap, x, y)) {
                    addPoint(points, x, y);
                    pointCount++;
                    break;
                }
            }
            if (pointCount >= maxPoints) break;
        }

        // Левая граница
        for (int y = height - 1; y >= 0; y -= step) {
            for (int x = 0; x < width; x += step) {
                if (isPixelOpaque(pixmap, x, y)) {
                    addPoint(points, x, y);
                    pointCount++;
                    break;
                }
            }
            if (pointCount >= maxPoints) break;
        }

        // Если не нашли точки, создаем простой прямоугольник
        if (points.size == 0) {
            addPoint(points, 0, 0);
            addPoint(points, width, 0);
            addPoint(points, width, height);
            addPoint(points, 0, height);
        }
    }

    /**
     * Добавляет точку в список, только если это новая уникальная точка
     * @param points список точек
     * @param x координата x
     * @param y координата y
     */
    private static void addPoint(Array<Float> points, float x, float y) {
        // Проверяем, не дублирует ли новая точка уже существующую
        for (int i = 0; i < points.size; i += 2) {
            if (Math.abs(points.get(i) - x) < 1 && Math.abs(points.get(i + 1) - y) < 1) {
                return; // Пропускаем близкие точки для уменьшения сложности полигона
            }
        }
        points.add(x);
        points.add(y);
    }

    /**
     * Проверяет, является ли пиксель непрозрачным
     * @param pixmap изображение
     * @param x координата x
     * @param y координата y
     * @return true если пиксель непрозрачный (alpha > 0)
     */
    private static boolean isPixelOpaque(Pixmap pixmap, int x, int y) {
        if (x < 0 || y < 0 || x >= pixmap.getWidth() || y >= pixmap.getHeight()) {
            return false;
        }

        int pixel = pixmap.getPixel(x, y);
        return ((pixel & 0x000000ff)) > 10; // Проверяем только alpha канал
    }

    /**
     * Рисует хитбокс для отладки
     * @param polygon полигон хитбокса
     * @param color цвет линии хитбокса
     * @param camera камера игрового мира
     */
    public static void renderHitbox(Polygon polygon, Color color, OrthographicCamera camera) {
        if (shapeRenderer == null) {
            shapeRenderer = new ShapeRenderer();
        }
        
        // Получаем вершины полигона
        float[] vertices = polygon.getTransformedVertices();
        
        // Устанавливаем матрицу проекции камеры для правильного отображения
        shapeRenderer.setProjectionMatrix(camera.combined);
        
        // Включаем смешивание для прозрачности
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        // Рисуем контур полигона
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(color);
        
        for (int i = 0; i < vertices.length; i += 2) {
            int next = (i + 2) % vertices.length;
            shapeRenderer.line(vertices[i], vertices[i + 1], vertices[next], vertices[next + 1]);
        }
        
        shapeRenderer.end();
        
        // Отключаем смешивание
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Освобождает ресурсы рендерера хитбоксов
     */
    public static void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
    }
}
