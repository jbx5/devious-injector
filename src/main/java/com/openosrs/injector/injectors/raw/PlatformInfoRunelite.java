package com.openosrs.injector.injectors.raw;

import com.openosrs.injector.Injection;
import com.openosrs.injector.injection.InjectData;
import com.openosrs.injector.injectors.AbstractInjector;
import java.util.List;
import java.util.ListIterator;
import net.runelite.asm.ClassFile;
import net.runelite.asm.Method;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.instructions.ILoad;
import net.runelite.asm.attributes.code.instructions.LDC;

public class PlatformInfoRunelite extends AbstractInjector
{
	private static final int DEV = 8;
	private static final int RELEASE = 24;

	public PlatformInfoRunelite(InjectData inject)
	{
		super(inject);
	}

	@Override
	public String getCompletionMsg()
	{
		return null;
	}

	public void inject()
	{
		final ClassFile PlatformInfoVanilla = inject.toVanilla(
			inject.getDeobfuscated()
				.findClass("PlatformInfo")
		);

		Method platformInfoInit = PlatformInfoVanilla.findMethod("<init>");

		final Instructions instructions = platformInfoInit.getCode().getInstructions();
		final List<Instruction> ins = instructions.getInstructions();

		for (ListIterator<Instruction> iterator = ins.listIterator(); iterator.hasNext(); )
		{
			Instruction i = iterator.next();

			if (i instanceof ILoad)
			{
				ILoad i1 = (ILoad) i;

				if (i1.getVariableIndex() == 12)
				{
					iterator.set(new LDC(instructions, Injection.development ? DEV : RELEASE));
					return;
				}
			}
		}
	}
}
