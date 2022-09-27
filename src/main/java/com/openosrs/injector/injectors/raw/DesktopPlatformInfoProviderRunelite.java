package com.openosrs.injector.injectors.raw;

import com.openosrs.injector.injection.InjectData;
import com.openosrs.injector.injectors.AbstractInjector;
import java.util.List;
import java.util.ListIterator;
import net.runelite.asm.ClassFile;
import net.runelite.asm.Method;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.instructions.IfICmpEq;
import net.runelite.asm.attributes.code.instructions.IfICmpNe;
import net.runelite.asm.attributes.code.instructions.LDC;

public class DesktopPlatformInfoProviderRunelite extends AbstractInjector
{
	public DesktopPlatformInfoProviderRunelite(InjectData inject)
	{
		super(inject);
	}

	@Override
	public String getCompletionMsg()
	{
		return null;
	}

	@Override
	public void inject()
	{
		ClassFile clazz = inject.toVanilla(inject.getDeobfuscated().findClass("DesktopPlatformInfoProvider"));

		for (Method m : clazz.getMethods())
		{

			invertMicrosoftPlatformDetection(m);
		}
	}

	/**
	 * RuneLite's patched jar inverts the microsoft platform check. Vanilla does not do this. We previously did not do this.
	 * <p>
	 * } else if (var5.toLowerCase().indexOf("microsoft") != -1) {
	 * should become
	 * } else if (var5.toLowerCase().indexOf("microsoft") == -1) {
	 */
	private void invertMicrosoftPlatformDetection(Method target)
	{
		// L68 {
		//   f_new (Locals[9]: og, 1, 1, java/lang/String, java/lang/String, java/lang/String, java/lang/String, 1, 1) (Stack[0]: null)
		//   aload5
		//   invokevirtual java/lang/String.toLowerCase()Ljava/lang/String;
		//   ldc "microsoft" (java.lang.String)
		//   invokevirtual java/lang/String.indexOf(Ljava/lang/String;)I
		//   iconst_m1
		//   if_icmpeq L71
		//   iload1
		//   ldc 51821248 (java.lang.Integer)
		//   if_icmpeq L72
		//   new java/lang/IllegalStateException
		//   dup
		//   invokespecial java/lang/IllegalStateException.<init>()V
		//   athrow
		// }

		Instructions instructions = target.getCode().getInstructions();
		List<Instruction> ins = instructions.getInstructions();
		ListIterator<Instruction> iterator = ins.listIterator();
		var seenMicrosoftString = false;

		while (iterator.hasNext())
		{
			Instruction i = iterator.next();

			if (i instanceof LDC && ((LDC) i).getConstant().equals("microsoft"))
			{
				seenMicrosoftString = true;
			}

			// we are looking for the first if_icmpeq after the "microsoft" constant string is seen
			if (i instanceof IfICmpEq && seenMicrosoftString)
			{
				var to = ((IfICmpEq) i).getJumps().get(0);
				iterator.set(new IfICmpNe(instructions, to));
				return;
			}
		}
	}
}
