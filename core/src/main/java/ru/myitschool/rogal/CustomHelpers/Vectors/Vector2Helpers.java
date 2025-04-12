package ru.myitschool.rogal.CustomHelpers.Vectors;

import com.badlogic.gdx.math.Vector2;

public class Vector2Helpers {
    /**
    * <p>Получаем угол поворота с помощью кординат
     * <p>Есле надо чтобы угол поворота при 0 кардинатах не изменялся можно сделать проверку</p>
     * <pre>float angle = Vector2Helpers.getRotationByVector(nextX, nextY);
     *if(angle != 0){
     *  setRotation(angle);
     *}</pre>
    * */
    public static float getRotationByVector(float x, float y){
        Vector2 vector = new Vector2(x, y);
        return vector.angleDeg();
    }
    public static float getRotationByVector(Vector2 vector){
        return vector.angleDeg();
    }
}
