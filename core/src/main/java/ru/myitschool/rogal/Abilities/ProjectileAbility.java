package ru.myitschool.rogal.Abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

/**
 * Класс для способностей, создающих летящие снаряды
 */
public abstract class ProjectileAbility extends Ability {

    // Параметры снаряда
    protected String projectileTexturePath;  // Путь к текстуре снаряда
    protected float projectileSpeed;         // Скорость снаряда
    protected float projectileSize;          // Размер снаряда
    protected int projectileCount = 1;       // Количество снарядов
    protected float projectileDamage;        // Базовый урон снаряда
    protected float projectileLifespan;      // Время жизни снаряда (в секундах)

    /**
     * Конструктор для снарядной способности
     * @param name название способности
     * @param description описание способности
     * @param iconPath путь к файлу иконки
     * @param cooldown время перезарядки в секундах
     * @param range радиус действия способности
     * @param projectileTexturePath путь к текстуре снаряда
     * @param projectileSpeed скорость снаряда
     * @param projectileSize размер снаряда
     * @param projectileDamage базовый урон от снаряда
     * @param projectileLifespan время жизни снаряда
     */
    public ProjectileAbility(String name, String description, String iconPath,
                             float cooldown, float range,
                           String projectileTexturePath, float projectileSpeed,
                           float projectileSize, float projectileDamage, float projectileLifespan) {
        super(name, description, iconPath, cooldown, range);

        this.projectileTexturePath = projectileTexturePath;
        this.projectileSpeed = projectileSpeed;
        this.projectileSize = projectileSize;
        this.projectileDamage = projectileDamage;
        this.projectileLifespan = projectileLifespan;

        // Загрузка иконки
        try {
            this.icon = new Texture(Gdx.files.internal(iconPath));
        } catch (Exception e) {
            Gdx.app.error("ProjectileAbility", "Не удалось загрузить иконку: " + iconPath);
        }
    }

    @Override
    protected boolean use(Vector2 position) {
        if (owner == null || owner.getStage() == null) {
            return false;
        }

        Stage stage = owner.getStage();
        boolean success = false;

        // Получаем направление от игрока к целевой позиции
        Vector2 playerPos = new Vector2(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2);
        Vector2 direction = new Vector2(position).sub(playerPos).nor();

        // Создаем снаряды
        for (int i = 0; i < projectileCount; i++) {
            Actor projectile = createProjectile(playerPos, direction);
            if (projectile != null) {
                stage.addActor(projectile);
                success = true;
            }
        }

        return success;
    }

    @Override
    protected void updateActive(float delta) {
        // В базовой реализации снаряды управляются своей собственной логикой
        // Поэтому здесь только проверяем, не закончилась ли активная фаза
        if (activeTime >= 0.1f) { // Активная фаза короткая, так как снаряды существуют независимо
            isActive = false;
        }
    }

    @Override
    protected void onLevelUp() {
        projectileDamage *= 1.2f;

        if (level == 3) {
            projectileCount++;
        }
        if (level == 5) {
            cooldown *= 0.8f;
        }
    }

    /**
     * Создает новый снаряд с заданными параметрами
     * @param startPosition начальная позиция снаряда
     * @param direction направление движения снаряда
     * @return созданный актор снаряда
     */
    protected abstract Actor createProjectile(Vector2 startPosition, Vector2 direction);

    /**
     * Возвращает текущее количество снарядов
     * @return количество создаваемых снарядов
     */
    public int getProjectileCount() {
        return projectileCount;
    }

    /**
     * Возвращает текущий урон снаряда с учетом уровня способности
     * @return значение урона
     */
    public float getProjectileDamage() {
        return projectileDamage;
    }
}
