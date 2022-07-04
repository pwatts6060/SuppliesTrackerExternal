/*
 * Copyright (c) 2022, Patrick <https://github.com/pwatts6060>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *	list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	this list of conditions and the following disclaimer in the documentation
 *	and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
