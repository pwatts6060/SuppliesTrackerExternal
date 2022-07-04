package com.suppliestracker.Skills;

import lombok.AllArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class XpDropTracker {

    @Inject
    private Client client;

    private static final Map<Skill, XpDrop> skillToLastXpDrop = new HashMap<>();

    public XpDrop lastXpDrop(Skill skill) {
        return skillToLastXpDrop.getOrDefault(skill, XpDrop.NONE);
    }

    public void update(Skill skill, int xpDrop) {
        skillToLastXpDrop.put(skill, new XpDrop(client.getTickCount(), xpDrop));
    }

    public boolean hadXpThisTick(Skill skill) {
        return lastXpDrop(skill).tick == client.getTickCount();
    }

    @AllArgsConstructor
    static class XpDrop {

        public final int tick;
        public final int xpDrop;

        public static final XpDrop NONE = new XpDrop(-1, 0);
    }
}
