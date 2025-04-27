package ru.myitschool.rogal.Abilities;

import java.util.ArrayList;
import java.util.List;

import ru.myitschool.rogal.Abilities.Abilitis.EnergyBullet;
import ru.myitschool.rogal.Abilities.Abilitis.FrostAuraAbility;
import ru.myitschool.rogal.Abilities.Abilitis.HealingAuraAbility;
import ru.myitschool.rogal.Abilities.Abilitis.LightningChainAbility;
import ru.myitschool.rogal.Abilities.Abilitis.OrbitingBladeAbility;
import ru.myitschool.rogal.Abilities.Abilitis.Relsatron;

/**
 * Класс, предоставляющий доступ к списку доступных способностей
 */
public class AvailableAbilities {

    /**
     * Возвращает список начальных способностей, доступных игроку
     * @return список способностей
     */
    public static List<Ability> getInitialAbilities() {
        List<Ability> abilities = new ArrayList<>();
        abilities.add(new EnergyBullet());
        abilities.add(new HealingAuraAbility());
        abilities.add(new OrbitingBladeAbility());
        abilities.add(new LightningChainAbility());
        abilities.add(new FrostAuraAbility());
        abilities.add(new Relsatron());
        return abilities;
    }
}
