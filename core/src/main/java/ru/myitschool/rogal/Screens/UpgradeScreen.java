package ru.myitschool.rogal.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import ru.myitschool.rogal.CustomHelpers.utils.ButtonCreator;
import ru.myitschool.rogal.CustomHelpers.utils.FontManager;
import ru.myitschool.rogal.CustomHelpers.utils.LogHelper;
import ru.myitschool.rogal.CustomHelpers.utils.PlayerData;
import ru.myitschool.rogal.Main;

/**
 * Экран улучшения характеристик игрока за валюту
 */
public class UpgradeScreen implements Screen {
    private final Main game;
    private final Stage stage;
    private final Texture backgroundTexture;

    // Метки информации
    private Label currencyLabel;
    private Label healthUpgradeInfoLabel;
    private Label energyUpgradeInfoLabel;
    private Label speedUpgradeInfoLabel;
    private Label healthRegenUpgradeInfoLabel;
    private Label energyRegenUpgradeInfoLabel;

    // Кнопки улучшений
    private TextButton healthUpgradeButton;
    private TextButton energyUpgradeButton;
    private TextButton speedUpgradeButton;
    private TextButton healthRegenUpgradeButton;
    private TextButton energyRegenUpgradeButton;

    public UpgradeScreen(final Main game) {
        this.game = game;
        stage = new Stage(new FitViewport(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        backgroundTexture = new Texture("BG.png");

        createUI();
    }

    private void createUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);

        // Стили текста
        Label.LabelStyle titleStyle = new Label.LabelStyle(FontManager.getTitleFont(), Color.WHITE);
        Label.LabelStyle headerStyle = new Label.LabelStyle(FontManager.getRegularFont(), Color.YELLOW);
        headerStyle.fontColor = Color.YELLOW;
        Label.LabelStyle textStyle = new Label.LabelStyle(FontManager.getRegularFont(), Color.WHITE);

        // Заголовок экрана
        Label titleLabel = new Label("МАГАЗИН УЛУЧШЕНИЙ", titleStyle);
        titleLabel.setAlignment(Align.center);

        // Верхняя панель с валютой и кнопкой выхода
        Table topPanel = new Table();
        topPanel.setBackground(ButtonCreator.createBackgroundDrawable(new Color(0.1f, 0.1f, 0.3f, 0.8f)));
        topPanel.pad(10);

        // Показываем текущее количество валюты
        currencyLabel = new Label("КРЕДИТЫ: " + PlayerData.getCurrency(), headerStyle);
        currencyLabel.setFontScale(1.2f);

        // Кнопка "Назад" в верхней панели
        TextButton backButton = ButtonCreator.createButton("ВЫХОД", FontManager.getRegularFont());
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new StartScreen(game));
                dispose();
            }
        });

        topPanel.add(currencyLabel).left().expandX();
        topPanel.add(backButton).width(100).height(40).right();

        // Таблица для улучшений
        Table upgradesTable = new Table();
        upgradesTable.setBackground(ButtonCreator.createBackgroundDrawable(new Color(0.2f, 0.2f, 0.4f, 0.7f)));
        upgradesTable.pad(20);

        // Создание прокручиваемой панели для улучшений
        ScrollPane scrollPane = new ScrollPane(upgradesTable);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);

        // Секция улучшения здоровья
        Label healthHeader = new Label("ЗДОРОВЬЕ", headerStyle);
        healthUpgradeInfoLabel = new Label(getHealthUpgradeInfo(), textStyle);
        healthUpgradeButton = createUpgradeButton("УЛУЧШИТЬ", PlayerData.getNextHealthUpgradeCost(),
            new UpgradeHandler() {
                @Override
                public void onUpgrade() {
                    if (PlayerData.upgradeHealth()) {
                        updateUpgradeInfo();
                    }
                }
            });

        // Секция улучшения энергии
        Label energyHeader = new Label("ЭНЕРГИЯ", headerStyle);
        energyUpgradeInfoLabel = new Label(getEnergyUpgradeInfo(), textStyle);
        energyUpgradeButton = createUpgradeButton("УЛУЧШИТЬ", PlayerData.getNextEnergyUpgradeCost(),
            new UpgradeHandler() {
                @Override
                public void onUpgrade() {
                    if (PlayerData.upgradeEnergy()) {
                        updateUpgradeInfo();
                    }
                }
            });

        // Секция улучшения скорости
        Label speedHeader = new Label("СКОРОСТЬ", headerStyle);
        speedUpgradeInfoLabel = new Label(getSpeedUpgradeInfo(), textStyle);
        speedUpgradeButton = createUpgradeButton("УЛУЧШИТЬ", PlayerData.getNextSpeedUpgradeCost(),
            new UpgradeHandler() {
                @Override
                public void onUpgrade() {
                    if (PlayerData.upgradeSpeed()) {
                        updateUpgradeInfo();
                    }
                }
            });

        // Секция улучшения регенерации здоровья
        Label healthRegenHeader = new Label("РЕГЕНЕРАЦИЯ ЗДОРОВЬЯ", headerStyle);
        healthRegenUpgradeInfoLabel = new Label(getHealthRegenUpgradeInfo(), textStyle);
        healthRegenUpgradeButton = createUpgradeButton("УЛУЧШИТЬ", PlayerData.getNextRegenUpgradeCost(),
            new UpgradeHandler() {
                @Override
                public void onUpgrade() {
                    if (PlayerData.upgradeRegen()) {
                        updateUpgradeInfo();
                    }
                }
            });

        // Секция улучшения регенерации энергии
        Label energyRegenHeader = new Label("РЕГЕНЕРАЦИЯ ЭНЕРГИИ", headerStyle);
        energyRegenUpgradeInfoLabel = new Label(getEnergyRegenUpgradeInfo(), textStyle);
        energyRegenUpgradeButton = createUpgradeButton("УЛУЧШИТЬ", PlayerData.getNextEnergyRegenUpgradeCost(),
            new UpgradeHandler() {
                @Override
                public void onUpgrade() {
                    if (PlayerData.upgradeEnergyRegen()) {
                        updateUpgradeInfo();
                    }
                }
            });

        // Добавляем все секции в таблицу улучшений с небольшими отступами
        upgradesTable.add(healthHeader).colspan(2).align(Align.left).padBottom(10).row();
        upgradesTable.add(healthUpgradeInfoLabel).align(Align.left).padRight(20);
        upgradesTable.add(healthUpgradeButton).width(180).padBottom(20).row();

        upgradesTable.add(energyHeader).colspan(2).align(Align.left).padBottom(10).row();
        upgradesTable.add(energyUpgradeInfoLabel).align(Align.left).padRight(20);
        upgradesTable.add(energyUpgradeButton).width(180).padBottom(20).row();

        upgradesTable.add(speedHeader).colspan(2).align(Align.left).padBottom(10).row();
        upgradesTable.add(speedUpgradeInfoLabel).align(Align.left).padRight(20);
        upgradesTable.add(speedUpgradeButton).width(180).padBottom(20).row();

        upgradesTable.add(healthRegenHeader).colspan(2).align(Align.left).padBottom(10).row();
        upgradesTable.add(healthRegenUpgradeInfoLabel).align(Align.left).padRight(20);
        upgradesTable.add(healthRegenUpgradeButton).width(180).padBottom(20).row();

        upgradesTable.add(energyRegenHeader).colspan(2).align(Align.left).padBottom(10).row();
        upgradesTable.add(energyRegenUpgradeInfoLabel).align(Align.left).padRight(20);
        upgradesTable.add(energyRegenUpgradeButton).width(180).padBottom(20).row();

        // Добавляем всё в главную таблицу
        mainTable.add(titleLabel).padBottom(20).row();
        mainTable.add(topPanel).fillX().padBottom(10).row();
        mainTable.add(scrollPane).width(700).height(450).padBottom(20).row();

        stage.addActor(mainTable);
    }

    /**
     * Создает кнопку улучшения с ценой
     */
    private TextButton createUpgradeButton(String text, int cost, final UpgradeHandler handler) {
        String buttonText = text;
        if (cost > 0) {
            buttonText += " (" + cost + ")";
        } else {
            buttonText = "МАКС. УРОВЕНЬ";
        }

        TextButton button = ButtonCreator.createButton(buttonText, FontManager.getRegularFont());

        // Если достигнут максимальный уровень, делаем кнопку неактивной
        if (cost <= 0) {
            button.setDisabled(true);
            button.getStyle().fontColor = Color.GRAY;
        } else {
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    handler.onUpgrade();
                }
            });
        }

        return button;
    }

    /**
     * Обновляет информацию об улучшениях после покупки
     */
    private void updateUpgradeInfo() {
        // Обновляем количество валюты
        currencyLabel.setText("КРЕДИТЫ: " + PlayerData.getCurrency());

        // Обновляем информацию об улучшениях
        healthUpgradeInfoLabel.setText(getHealthUpgradeInfo());
        energyUpgradeInfoLabel.setText(getEnergyUpgradeInfo());
        speedUpgradeInfoLabel.setText(getSpeedUpgradeInfo());
        healthRegenUpgradeInfoLabel.setText(getHealthRegenUpgradeInfo());
        energyRegenUpgradeInfoLabel.setText(getEnergyRegenUpgradeInfo());

        // Обновляем кнопки с новыми ценами
        int cost = PlayerData.getNextHealthUpgradeCost();
        updateButtonText(healthUpgradeButton, "УЛУЧШИТЬ", cost);

        cost = PlayerData.getNextEnergyUpgradeCost();
        updateButtonText(energyUpgradeButton, "УЛУЧШИТЬ", cost);

        cost = PlayerData.getNextSpeedUpgradeCost();
        updateButtonText(speedUpgradeButton, "УЛУЧШИТЬ", cost);

        cost = PlayerData.getNextRegenUpgradeCost();
        updateButtonText(healthRegenUpgradeButton, "УЛУЧШИТЬ", cost);

        cost = PlayerData.getNextEnergyRegenUpgradeCost();
        updateButtonText(energyRegenUpgradeButton, "УЛУЧШИТЬ", cost);

        LogHelper.log("UpgradeScreen", "Updated upgrade information");
    }

    /**
     * Обновляет текст на кнопке улучшения
     */
    private void updateButtonText(TextButton button, String text, int cost) {
        if (cost > 0) {
            button.setText(text + " (" + cost + ")");
            button.setDisabled(false);
            button.getStyle().fontColor = Color.WHITE;
        } else {
            button.setText("МАКС. УРОВЕНЬ");
            button.setDisabled(true);
            button.getStyle().fontColor = Color.GRAY;
        }
    }

    /**
     * Получает информацию об улучшении здоровья
     */
    private String getHealthUpgradeInfo() {
        int level = PlayerData.getHealthUpgradeLevel();
        int bonus = PlayerData.getHealthBonus();
        return "Уровень: " + level + "/10\nБонус здоровья: +" + bonus;
    }

    /**
     * Получает информацию об улучшении энергии
     */
    private String getEnergyUpgradeInfo() {
        int level = PlayerData.getEnergyUpgradeLevel();
        int bonus = PlayerData.getEnergyBonus();
        return "Уровень: " + level + "/10\nБонус энергии: +" + bonus;
    }

    /**
     * Получает информацию об улучшении скорости
     */
    private String getSpeedUpgradeInfo() {
        int level = PlayerData.getSpeedUpgradeLevel();
        float bonus = PlayerData.getSpeedBonus();
        return "Уровень: " + level + "/10\nБонус скорости: +" + String.format("%.1f", bonus);
    }

    /**
     * Получает информацию об улучшении регенерации здоровья
     */
    private String getHealthRegenUpgradeInfo() {
        int level = PlayerData.getRegenUpgradeLevel();
        float bonus = PlayerData.getRegenBonus();
        return "Уровень: " + level + "/10\nБонус регенерации: +" + String.format("%.1f", bonus) + "/сек";
    }

    /**
     * Получает информацию об улучшении регенерации энергии
     */
    private String getEnergyRegenUpgradeInfo() {
        int level = PlayerData.getEnergyRegenUpgradeLevel();
        float bonus = PlayerData.getEnergyRegenBonus();
        return "Уровень: " + level + "/10\nБонус регенерации: +" + String.format("%.1f", bonus) + "/сек";
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        // Рисуем фон
        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0, 0, Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT);
        stage.getBatch().end();

        // Рисуем интерфейс
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
    }

    /**
     * Интерфейс для обработки нажатия на кнопку улучшения
     */
    private interface UpgradeHandler {
        void onUpgrade();
    }
}
