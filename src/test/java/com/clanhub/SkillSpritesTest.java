package com.clanhub;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link SkillSprites}: the sprite of every skill, ignoring case, and -1 for a name that is none. */
public class SkillSpritesTest
{
	@Test
	public void knowsEverySkill()
	{
		assertEquals(197, SkillSprites.spriteOf("Attack"));
		assertEquals(203, SkillSprites.spriteOf("Hitpoints"));
		assertEquals(215, SkillSprites.spriteOf("Runecraft"));
		assertEquals(228, SkillSprites.spriteOf("Sailing"));
		assertEquals(168, SkillSprites.spriteOf("combat"));
		assertEquals(222, SkillSprites.spriteOf("Total"));
		assertEquals(197, SkillSprites.spriteOf("attack"));
		assertEquals(-1, SkillSprites.spriteOf("Nonsense"));
	}
}
