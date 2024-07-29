/*
 * Copyright (c) 2024, Melxin <https://github.com/melxin>
 * All rights reserved.
 *
 * This code is licensed under GPL3, see the complete license in
 * the LICENSE file in the root directory of this submodule.
 */
package com.openosrs.injector.injectors.raw;

import java.util.List;
import net.runelite.asm.ClassFile;
import net.runelite.asm.Field;
import net.runelite.asm.Method;
import net.runelite.asm.attributes.Code;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.instructions.BALoad;
import net.runelite.asm.attributes.code.instructions.GetStatic;
import net.runelite.asm.attributes.code.instructions.ILoad;
import net.runelite.asm.attributes.code.instructions.IfEq;
import net.runelite.asm.attributes.code.instructions.IfNe;
import com.openosrs.injector.injection.InjectData;
import com.openosrs.injector.injectors.AbstractInjector;

public class CameraShakeDisabled extends AbstractInjector
{
	public CameraShakeDisabled(InjectData inject)
	{
		super(inject);
	}

	@Override
	public void inject()
	{
		final ClassFile clientVanilla = inject.toVanilla(
			inject.getDeobfuscated()
				.findClass("Client")
		);

		final Field cameraShakeDisabled = clientVanilla.findField("cameraShakeDisabled");
		final Method drawEntities = inject.toVanilla(inject.getDeobfuscated().findStaticMethod("drawEntities"));
		if (cameraShakeDisabled != null && drawEntities != null)
		{
			Code code = drawEntities.getCode();
			Instructions instructions = code.getInstructions();
			List<Instruction> ins = code.getInstructions().getInstructions();
			for (int i = 0; i < ins.size(); i++)
			{
				Instruction in = ins.get(i);
				if (in instanceof GetStatic && in.toString().contains("static [Z")
					&& i + 3 < ins.size()
					&& ins.get(i + 1) instanceof ILoad
					&& ins.get(i + 2) instanceof BALoad
					&& ins.get(i + 3) instanceof IfEq)
				{
					ins.add(i + 4, new GetStatic(instructions, cameraShakeDisabled.getPoolField()));
					ins.add(i + 5, new IfNe(instructions, ((IfEq) ins.get(i + 3)).getTo()));
				}
			}
		}
	}
}