package ru.myitschool.rogal.CustomHelpers.Vectors;

import com.badlogic.gdx.math.Polygon;

public class HitboxHelpers {

    public static Polygon createCustomPolygon(float[] verticesArray){
        Polygon hitbox = new Polygon(verticesArray);
        return hitbox;
    }
}
