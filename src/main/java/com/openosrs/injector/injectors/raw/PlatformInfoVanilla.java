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
import net.runelite.asm.attributes.code.instructions.BiPush;
import net.runelite.asm.attributes.code.instructions.ILoad;
import net.runelite.asm.attributes.code.instructions.LDC;

public class PlatformInfoVanilla extends AbstractInjector
{
	private static final int OS_VALUE = 1;
	private static final int OS_VERSION = 9;

	private static final int JAVA_VENDOR = 5;
	private static final int JAVA_MAJOR = 7;
	private static final int JAVA_MINOR = 0;
	private static final int JAVA_PATCH = 0;

	private static final int MAX_MEMORY = 372;

	public PlatformInfoVanilla(InjectData inject)
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

				if (i1.getVariableIndex() == 1)
				{
					iterator.set(new LDC(instructions, OS_VALUE));
				}
				else if (i1.getVariableIndex() == 3)
				{
					iterator.set(new LDC(instructions, OS_VERSION));
				}
				else if (i1.getVariableIndex() == 4)
				{
					iterator.set(new LDC(instructions, JAVA_VENDOR));
				}
				else if (i1.getVariableIndex() == 5)
				{
					iterator.set(new LDC(instructions, JAVA_MAJOR));
				}
				else if (i1.getVariableIndex() == 6)
				{
					iterator.set(new LDC(instructions, JAVA_MINOR));
				}
				else if (i1.getVariableIndex() == 7)
				{
					iterator.set(new LDC(instructions, JAVA_PATCH));
				}
				else if (i1.getVariableIndex() == 9)
				{
					iterator.set(new LDC(instructions, MAX_MEMORY));
				}
			}
		}
	}
}
