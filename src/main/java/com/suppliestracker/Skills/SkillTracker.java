package com.suppliestracker.Skills;

import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class SkillTracker {
    public final Map<Skill, Integer> skillXp = new HashMap<>();

    public void loadAll(Client client) {
        for (Skill skill : Skill.values()) {
            skillXp.put(skill, client.getSkillExperience(skill));
        }
    }
}
