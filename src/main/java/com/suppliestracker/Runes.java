/*
 * Copyright (c) 2017, Tyler <https://github.com/tylerthardy>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
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
package com.suppliestracker;


import com.google.common.collect.ImmutableMap;
import java.awt.image.BufferedImage;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import static net.runelite.api.gameval.ItemID.*;

public enum Runes
{
	AIR(1, AIRRUNE),
	WATER(2, WATERRUNE),
	EARTH(3, EARTHRUNE),
	FIRE(4, FIRERUNE),
	MIND(5, MINDRUNE),
	CHAOS(6, CHAOSRUNE),
	DEATH(7, DEATHRUNE),
	BLOOD(8, BLOODRUNE),
	COSMIC(9, COSMICRUNE),
	NATURE(10, NATURERUNE),
	LAW(11, LAWRUNE),
	BODY(12, BODYRUNE),
	SOUL(13, SOULRUNE),
	ASTRAL(14, ASTRALRUNE),
	MIST(15, MISTRUNE),
	MUD(16, MUDRUNE),
	DUST(17, DUSTRUNE),
	LAVA(18, LAVARUNE),
	STEAM(19, STEAMRUNE),
	SMOKE(20, SMOKERUNE),
	WRATH(21, WRATHRUNE),
	SUNFIRE(22, SUNFIRERUNE),
	AETHER(23, AETHERRUNE),
	;

	@Getter
	private final int id;
	@Getter
	private final int itemId;

	@Getter
	@Setter
	private BufferedImage image;

	private static final Map<Integer, Runes> runes;

	static
	{
		ImmutableMap.Builder<Integer, Runes> builder = new ImmutableMap.Builder<>();
		for (Runes rune : values())
		{
			builder.put(rune.getId(), rune);
		}
		runes = builder.build();
	}

	Runes(int id, int itemId)
	{
		this.id = id;
		this.itemId = itemId;
	}

	public static Runes getRune(int index)
	{
		return runes.get(index);
	}

}
