package ru.myitschool.rogal.Abilities.Abilitis;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;

import ru.myitschool.rogal.Abilities.Ability;
import ru.myitschool.rogal.Abilities.AbilityType;
import ru.myitschool.rogal.Actors.EnemyActor;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;

/**
 * Способность "Молниевая Цепь" - наносит урон ближайшему врагу и перескакивает
 * на других врагов рядом с ним. Количество перескоков увеличивается с уровнем способности.
 */
public class LightningChainAbility extends Ability {

    private float damageAmount = 25f;           // Снижен базовый урон для баланса
    private float chainRange = 280f;            // Увеличена дальность перескока
    private int maxJumps = 3;                   // Увеличено начальное количество перескоков
    private float damageFalloff = 0.7f;         // Увеличен коэффициент уменьшения урона
    private final String lightningTexturePath = "abilities/lightning.png";

    /**
     * Конструктор способности Молниевая Цепь
     */
    public LightningChainAbility() {
        super("Lightning Chain",
            "Поражает ближайшего врага молнией, которая перескакивает на соседних врагов.\n" +
                "Уровень 2: Увеличивает урон и снижает перезарядку.\n" +
                "Уровень 3: Молния перескакивает ещё на одну цель.\n" +
                "Уровень 4: Значительно увеличивает урон и дальность цепи.\n" +
                "Уровень 5: Максимальное количество перескоков увеличивается до 5.",
              "abilities/lightning.png",
            5.0f,
            400f);

        this.abilityType = AbilityType.ATTACK;
        this.energyCost = 20f;

        if (Gdx.files.internal("abilities/lightning.png").exists()) {
            this.icon = new Texture(Gdx.files.internal("abilities/lightning.png"));
        }
    }

    @Override
    protected boolean use(Vector2 position) {
        if (owner == null || owner.getStage() == null) {
            return false;
        }

        Vector2 playerPos = new Vector2(owner.getX() + owner.getOriginX(),
                                        owner.getY() + owner.getOriginY());

        EnemyActor firstTarget = findNearestEnemy(playerPos, range);

        if (firstTarget == null) {
            return false;
        }

        Array<EnemyActor> hitEnemies = new Array<>();

        Vector2 targetPos = new Vector2(firstTarget.getX() + firstTarget.getOriginX(),
                                        firstTarget.getY() + firstTarget.getOriginY());

        LightningEffect effect = new LightningEffect(playerPos, targetPos);
        owner.getStage().addActor(effect);

        firstTarget.takeDamage(Math.round(damageAmount));
        hitEnemies.add(firstTarget);


        chainLightning(firstTarget, hitEnemies, 1, damageAmount * damageFalloff);

        return true;
    }

    /**
     * Рекурсивно продолжает цепь молнии на следующие цели
     * @param currentTarget текущая цель, от которой перескакивает молния
     * @param hitEnemies список уже пораженных врагов
     * @param currentJump текущий номер перескока
     * @param currentDamage текущий урон молнии
     */
    private void chainLightning(EnemyActor currentTarget, Array<EnemyActor> hitEnemies,
                                int currentJump, float currentDamage) {
        if (currentJump >= maxJumps || owner == null || owner.getStage() == null) {
            return;
        }

        Vector2 currentPos = new Vector2(currentTarget.getX() + currentTarget.getOriginX(),
                                        currentTarget.getY() + currentTarget.getOriginY());

        EnemyActor nextTarget = findNearestEnemyExcluding(currentPos, chainRange, hitEnemies);

        if (nextTarget == null) {
            return;
        }

        Vector2 nextPos = new Vector2(nextTarget.getX() + nextTarget.getOriginX(),
                                    nextTarget.getY() + nextTarget.getOriginY());

        LightningEffect effect = new LightningEffect(currentPos, nextPos);
        owner.getStage().addActor(effect);

        int damage = Math.round(currentDamage);
        nextTarget.takeDamage(damage);
        hitEnemies.add(nextTarget);


        // Продолжаем цепь к следующей цели
        chainLightning(nextTarget, hitEnemies, currentJump + 1, currentDamage * damageFalloff);
    }

    /**
     * Находит ближайшего врага в радиусе от позиции
     * @param position центр поиска
     * @param searchRadius радиус поиска
     * @return ближайший враг или null, если врагов нет
     */
    private EnemyActor findNearestEnemy(Vector2 position, float searchRadius) {
        if (owner == null || owner.getStage() == null) return null;

        Stage stage = owner.getStage();
        EnemyActor nearestEnemy = null;
        float minDistance = Float.MAX_VALUE;

        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;
                Vector2 enemyPos = new Vector2(enemy.getX() + enemy.getOriginX(),
                                             enemy.getY() + enemy.getOriginY());
                float distance = position.dst(enemyPos);

                if (distance <= searchRadius && distance < minDistance) {
                    minDistance = distance;
                    nearestEnemy = enemy;
                }
            }
        }

        return nearestEnemy;
    }

    /**
     * Находит ближайшего врага, исключая уже пораженных
     * @param position центр поиска
     * @param searchRadius радиус поиска
     * @param excludeEnemies список врагов для исключения
     * @return ближайший подходящий враг или null
     */
    private EnemyActor findNearestEnemyExcluding(Vector2 position, float searchRadius,
                                               Array<EnemyActor> excludeEnemies) {
        if (owner == null || owner.getStage() == null) return null;

        Stage stage = owner.getStage();
        EnemyActor nearestEnemy = null;
        float minDistance = Float.MAX_VALUE;

        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;
                // Пропускаем уже пораженных врагов
                if (excludeEnemies.contains(enemy, true)) {
                    continue;
                }

                Vector2 enemyPos = new Vector2(enemy.getX() + enemy.getOriginX(),
                                             enemy.getY() + enemy.getOriginY());
                float distance = position.dst(enemyPos);

                if (distance <= searchRadius && distance < minDistance) {
                    minDistance = distance;
                    nearestEnemy = enemy;
                }
            }
        }

        return nearestEnemy;
    }

    @Override
    protected void updateActive(float delta) {
        isActive = false;
    }

    /**
     * Обновляет параметры способности при повышении уровня
     */
    @Override
    protected void onLevelUp() {
        switch (level) {
            case 2:
                damageAmount += 10f;
                cooldown -= 0.5f;
                break;
            case 3:
                maxJumps += 1;
                chainRange += 40f;
                break;
            case 4:
                damageAmount += 15f;
                chainRange += 60f;
                cooldown -= 0.5f;
                break;
            case 5:
                maxJumps = 5;
                damageAmount += 10f;
                damageFalloff = 0.8f;
                break;
        }
    }

    @Override
    public String getDescription() {
        if (level == 1){
            return description;
        } else if (level+1 == 3){
            return "Молния перескакивает ещё на одну цель.";
        } else if (level+1 == 4) {
            return "Значительно увеличивает урон и дальность цепи.";
        } else if (level+1 == 5) {
            return "Максимальное количество перескоков увеличивается до 5.";
        }
        return description;
    }

    protected void tryAutoActivate() {
        if (!autoUse || currentCooldown > 0) return;
        if (owner == null || owner.getStage() == null) return;

        boolean enemiesExist = false;
        for (Actor actor : owner.getStage().getActors()) {
            if (actor instanceof EnemyActor) {
                enemiesExist = true;
                break;
            }
        }

        if (!enemiesExist) {
            return;
        }

        if (energyCost > 0 && owner.getCurrentEnergy() < energyCost) {
            return;
        }

        Vector2 playerPos = new Vector2(owner.getX() + owner.getOriginX(),
                                        owner.getY() + owner.getOriginY());

        activate(playerPos);
    }

    /**
     * Публичный метод для создания мини-молнии из других способностей
     *
     * @param sourceAbility  способность-источник
     * @param startEnemy     враг, от которого начинается цепь молнии
     * @param damage         базовый урон мини-молнии
     * @param miniChainRange дальность прыжка мини-молнии
     * @param miniJumps      максимальное количество перескоков
     * @param miniFalloff    коэффициент уменьшения урона
     */
    public static void createMiniLightning(Ability sourceAbility, EnemyActor startEnemy,
                                           float damage, float miniChainRange,
                                           int miniJumps, float miniFalloff) {
        if (sourceAbility == null || sourceAbility.owner == null ||
            sourceAbility.owner.getStage() == null || startEnemy == null) {
            return;
        }

        // Создаем массив уже пораженных врагов
        Array<EnemyActor> hitEnemies = new Array<>();
        hitEnemies.add(startEnemy);

        // Получаем позицию стартового врага
        Vector2 enemyPos = new Vector2(startEnemy.getX() + startEnemy.getOriginX(),
            startEnemy.getY() + startEnemy.getOriginY());

        // Находим следующую цель для молнии
        EnemyActor nextTarget = findNearestEnemyExcluding(sourceAbility.owner.getStage(),
            enemyPos, miniChainRange, hitEnemies);

        if (nextTarget != null) {
            // Координаты следующей цели
            Vector2 nextPos = new Vector2(nextTarget.getX() + nextTarget.getOriginX(),
                nextTarget.getY() + nextTarget.getOriginY());

            // Создаем эффект мини-молнии
            MiniLightningEffect effect = new MiniLightningEffect(enemyPos, nextPos);
            sourceAbility.owner.getStage().addActor(effect);

            // Наносим урон
            int lightningDamage = Math.round(damage);
            nextTarget.takeDamage(lightningDamage);
            hitEnemies.add(nextTarget);

            LogHelper.log(sourceAbility.getClass().getSimpleName(), "Mini lightning hit for " + lightningDamage + " damage");

            // Продолжаем цепь молнии
            chainMiniLightning(sourceAbility, nextTarget, hitEnemies, 1, damage * miniFalloff,
                miniChainRange, miniJumps, miniFalloff);
        }
    }

    /**
     * Публичный метод для рекурсивного продолжения цепи мини-молнии
     */
    private static void chainMiniLightning(Ability sourceAbility, EnemyActor currentTarget,
                                           Array<EnemyActor> hitEnemies, int currentJump,
                                           float currentDamage, float chainRange,
                                           int maxJumps, float damageFalloff) {
        if (currentJump >= maxJumps || sourceAbility.owner == null ||
            sourceAbility.owner.getStage() == null) {
            return;
        }

        Vector2 currentPos = new Vector2(currentTarget.getX() + currentTarget.getOriginX(),
            currentTarget.getY() + currentTarget.getOriginY());

        EnemyActor nextTarget = findNearestEnemyExcluding(sourceAbility.owner.getStage(),
            currentPos, chainRange, hitEnemies);

        if (nextTarget == null) {
            return;
        }

        Vector2 nextPos = new Vector2(nextTarget.getX() + nextTarget.getOriginX(),
            nextTarget.getY() + nextTarget.getOriginY());

        MiniLightningEffect effect = new MiniLightningEffect(currentPos, nextPos);
        sourceAbility.owner.getStage().addActor(effect);

        int damage = Math.round(currentDamage);
        nextTarget.takeDamage(damage);
        hitEnemies.add(nextTarget);

        LogHelper.log(sourceAbility.getClass().getSimpleName(), "Mini lightning jump #" + currentJump +
            " hit target for " + damage + " damage");

        // Продолжаем цепь к следующей цели
        chainMiniLightning(sourceAbility, nextTarget, hitEnemies, currentJump + 1,
            currentDamage * damageFalloff, chainRange, maxJumps, damageFalloff);
    }

    /**
     * Публичный метод для поиска ближайшего врага, исключая уже пораженных
     */
    public static EnemyActor findNearestEnemyExcluding(Stage stage, Vector2 position,
                                                       float searchRadius, Array<EnemyActor> excludeEnemies) {
        if (stage == null) return null;

        EnemyActor nearestEnemy = null;
        float minDistance = Float.MAX_VALUE;

        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyActor) {
                EnemyActor enemy = (EnemyActor) actor;
                // Пропускаем уже пораженных врагов
                if (excludeEnemies.contains(enemy, true)) {
                    continue;
                }

                Vector2 enemyPos = new Vector2(enemy.getX() + enemy.getOriginX(),
                    enemy.getY() + enemy.getOriginY());
                float distance = position.dst(enemyPos);

                if (distance <= searchRadius && distance < minDistance) {
                    minDistance = distance;
                    nearestEnemy = enemy;
                }
            }
        }

        return nearestEnemy;
    }

    /**
     * Публичный класс для визуального эффекта мини-молнии
     * Модифицированная версия обычного эффекта молнии
     */
    public static class MiniLightningEffect extends Actor {
        private final TextureRegion texture;
        private final Vector2 start = new Vector2();
        private final Vector2 end = new Vector2();
        private final float duration = 0.3f;
        private final int segments = 3;
        private final Vector2[] points;
        private final float jitterAmount = 5f;
        private final float jitterFrequency = 0.05f;
        private float jitterTimer = 0f;

        /**
         * Создает визуальный эффект мини-молнии между двумя точками
         */
        public MiniLightningEffect(Vector2 start, Vector2 end) {
            this.start.set(start);
            this.end.set(end);

            try {
                this.texture = new TextureRegion(new Texture(Gdx.files.internal("abilities/lightning.png")));
            } catch (Exception e) {
                LogHelper.error("MiniLightningEffect", "Failed to load lightning texture", e);
                throw new RuntimeException("Failed to load lightning texture", e);
            }

            points = new Vector2[segments + 1];
            for (int i = 0; i <= segments; i++) {
                points[i] = new Vector2();
            }

            generateLightningPoints();

            float width = end.dst(start);
            float height = 15f;

            setWidth(width);
            setHeight(height);
            setPosition(start.x, start.y - height / 2);
            setColor(0.5f, 0.7f, 1f, 0.8f);

            addAction(Actions.sequence(
                Actions.delay(duration * 0.7f),
                Actions.fadeOut(duration * 0.3f),
                Actions.removeActor()
            ));
        }

        /**
         * Генерирует точки для сегментов молнии с случайными отклонениями
         */
        private void generateLightningPoints() {
            points[0].set(start);
            points[segments].set(end);

            for (int i = 1; i < segments; i++) {
                float t = (float) i / segments;

                float x = start.x + (end.x - start.x) * t;
                float y = start.y + (end.y - start.y) * t;

                float dx = end.x - start.x;
                float dy = end.y - start.y;
                float length = (float) Math.sqrt(dx * dx + dy * dy);

                if (length < 0.01f) continue;

                float nx = -dy / length;
                float ny = dx / length;

                float offset = MathUtils.random(-jitterAmount, jitterAmount);
                x += nx * offset;
                y += ny * offset;

                points[i].set(x, y);
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color color = getColor();
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

            for (int i = 0; i < segments; i++) {
                Vector2 p1 = points[i];
                Vector2 p2 = points[i + 1];

                float dx = p2.x - p1.x;
                float dy = p2.y - p1.y;
                float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                float length = Vector2.dst(p1.x, p1.y, p2.x, p2.y);

                batch.draw(texture,
                    p1.x, p1.y - getHeight() / 2,
                    0, getHeight() / 2,
                    getHeight()/2, length,
                    1, 1,
                    angle - 90);
            }

            batch.setColor(1, 1, 1, 1);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            jitterTimer += delta;
            if (jitterTimer >= jitterFrequency) {
                jitterTimer = 0;
                generateLightningPoints();
            }
        }
    }

    /**
     * Внутренний класс для визуального эффекта молнии
     */
    public class LightningEffect extends Actor {
        private final TextureRegion texture;
        private final Vector2 start = new Vector2();
        private final Vector2 end = new Vector2();
        private final float duration = 0.5f;
        private final int segments = 5;
        private final Vector2[] points;
        private final float jitterAmount = 10f;
        private float jitterTimer = 0f;
        private final float jitterFrequency = 0.05f;

        /**
         * Создает визуальный эффект молнии между двумя точками
         * @param start начальная точка
         * @param end конечная точка
         */
        public LightningEffect(Vector2 start, Vector2 end) {
            this.start.set(start);
            this.end.set(end);

            try {
                this.texture = new TextureRegion(new Texture(Gdx.files.internal(lightningTexturePath)));
            } catch (Exception e) {
                LogHelper.error("LightningEffect", "Failed to load lightning texture", e);
                throw new RuntimeException("Failed to load lightning texture", e);
            }

            points = new Vector2[segments + 1];
            for (int i = 0; i <= segments; i++) {
                points[i] = new Vector2();
            }

            generateLightningPoints();

            float width = end.dst(start);
            float height = 20f;

            setWidth(width);
            setHeight(height);
            setPosition(start.x, start.y - height/2);

            addAction(Actions.sequence(
                Actions.delay(duration * 0.8f),
                Actions.fadeOut(duration * 0.2f),
                Actions.removeActor()
            ));
        }

        /**
         * Генерирует точки для сегментов молнии с случайными отклонениями
         */
        protected void generateLightningPoints() {
            points[0].set(start);
            points[segments].set(end);

            for (int i = 1; i < segments; i++) {
                float t = (float) i / segments;

                float x = start.x + (end.x - start.x) * t;
                float y = start.y + (end.y - start.y) * t;

                float dx = end.x - start.x;
                float dy = end.y - start.y;
                float length = (float) Math.sqrt(dx * dx + dy * dy);

                if (length < 0.01f) continue;

                float nx = -dy / length;
                float ny = dx / length;

                float offset = MathUtils.random(-jitterAmount, jitterAmount);
                x += nx * offset;
                y += ny * offset;

                points[i].set(x, y);
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color color = getColor();
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

            for (int i = 0; i < segments; i++) {
                Vector2 p1 = points[i];
                Vector2 p2 = points[i + 1];

                float dx = p2.x - p1.x;
                float dy = p2.y - p1.y;
                float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                float length = Vector2.dst(p1.x, p1.y, p2.x, p2.y);

                batch.draw(texture,
                    p1.x, p1.y - getHeight() / 2,
                    0, getHeight() / 2,
                    getHeight()/2, length,
                    1, 1,
                    angle - 90);
            }

            batch.setColor(1, 1, 1, 1);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            jitterTimer += delta;
            if (jitterTimer >= jitterFrequency) {
                jitterTimer = 0;
                generateLightningPoints();
            }
        }
    }
}
