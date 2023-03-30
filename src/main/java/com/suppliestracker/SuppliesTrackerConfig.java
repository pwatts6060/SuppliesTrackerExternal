/*
 * Copyright (c) 2018, Daddy Dozer <https://github.com/Dyldozer>
 * Copyright (c) 2018, Davis Cook <daviscook447@gmail.com>
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
package com.suppliestracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import static com.suppliestracker.SuppliesTrackerConfig.GROUP_NAME;

@ConfigGroup(GROUP_NAME)
public interface SuppliesTrackerConfig extends Config
{
	String GROUP_NAME = "suppliestracker";

	@ConfigItem(
			keyName = "chargesBox",
			name = "Show weapons charges used box?",
			description = "Separates items with charges to show how many of those charges you used."
	)
	default boolean chargesBox()
	{
		return false;
	}

	@ConfigItem(
		keyName = "vorkathsHead",
		name = "Attached Vorkath's head?",
		description = "Whether or not you attached a Vorkath's head to your Ranging cape for Assembler effect."
	)
	default boolean vorkathsHead()
	{
		return false;
	}

	@ConfigItem(
		keyName = "curSession",
		name = "Current Session by Default",
		description = "Use current session as default option rather than all time."
	)
	default boolean curSessionDefault()
	{
		return false;
	}

	@ConfigItem(
		keyName = "jsonEnabled",
		name = "Save data as json",
		description = "Save data in a json format."
	)
	default boolean jsonEnabled()
	{
		return false;
	}
}
