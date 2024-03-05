/*
 * Copyright (c) 2024, Melxin <https://github.com/melxin>
 * All rights reserved.
 *
 * This code is licensed under GPL3, see the complete license in
 * the LICENSE file in the root directory of this submodule.
 */
package com.openosrs.injector.injectors.raw;

import java.util.HashMap;
import java.util.Map;
import net.runelite.asm.ClassFile;
import net.runelite.asm.ClassGroup;
import net.runelite.asm.Field;
import net.runelite.asm.Method;
import net.runelite.asm.Type;
import net.runelite.asm.attributes.Code;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.instructions.GetStatic;
import net.runelite.asm.attributes.code.instructions.InvokeStatic;
import net.runelite.asm.attributes.code.instructions.LDC;
import net.runelite.asm.attributes.code.instructions.VReturn;
import net.runelite.asm.signature.Signature;
import net.runelite.deob.DeobAnnotations;
import com.openosrs.injector.injection.InjectData;
import com.openosrs.injector.injectors.AbstractInjector;

public class CheckResize extends AbstractInjector
{
	public CheckResize(InjectData inject)
	{
		super(inject);
	}

	@Override
	public void inject()
	{
		final ClassGroup deobGroup = inject.getDeobfuscated();
		final ClassGroup vanillaGroup = inject.getVanilla();

		final ClassFile clientVanilla = inject.toVanilla(
			inject.getDeobfuscated()
				.findClass("Client")
		);

		final ClassFile evictingDualNodeHashTableVanilla = inject.toVanilla(
			inject.getDeobfuscated()
				.findClass("EvictingDualNodeHashTable")
		);

		final Type evictingDualNodeHashTableType = new Type("L" + evictingDualNodeHashTableVanilla.getName() + ";");

		/**
		 * Map all static EvictingDualNodeHashTable caches to de-obfuscated field names
		 */

		final Map<Field, String> staticEvictingDualNodeHashTablefields = new HashMap<>();
		for (ClassFile cf : deobGroup)
		{
			cf.getFields().stream()
				.filter(deobField -> deobField.isStatic() && deobField.getObfuscatedType().equals(evictingDualNodeHashTableType))
				.forEach(deobField ->
				{
					Field vanillaField = vanillaGroup
						.findClass(deobField.getClassFile().getAnnotations().get(DeobAnnotations.OBFUSCATED_NAME).getValueString())
						.findField(deobField.getAnnotations().get(DeobAnnotations.OBFUSCATED_NAME).getValueString());

					staticEvictingDualNodeHashTablefields.put(vanillaField, deobField.getName());
				});
		}

		/**
		 * Create checkResize method to invoke check method which is constructed in EvictingDualNodeHashTableMixin
		 */

		final Method check = evictingDualNodeHashTableVanilla.findStaticMethod("check");
		final Method checkResize = new Method(evictingDualNodeHashTableVanilla, "checkResize", new Signature("()V"));
		Code code = new Code(checkResize);
		Instructions ins = code.getInstructions();
		for (net.runelite.asm.Field f : staticEvictingDualNodeHashTablefields.keySet())
		{
			ins.addInstruction(new LDC(ins, staticEvictingDualNodeHashTablefields.get(f)));
			ins.addInstruction(new GetStatic(ins, f.getPoolField()));
			ins.addInstruction(new InvokeStatic(ins, check));
			//System.out.println("field: " + f + " name: " + staticEvictingDualNodeHashTablefields.get(f));
		}
		ins.addInstruction(new VReturn(ins));
		checkResize.setCode(code);
		checkResize.setPublic();
		checkResize.setStatic(true);
		evictingDualNodeHashTableVanilla.addMethod(checkResize);

		/**
		 * Create method to invoke checkResize from rl api
		 */

		final Method clientCheckResize = new Method(clientVanilla, "checkResize", new Signature("()V"));
		code = new Code(clientCheckResize);
		ins = code.getInstructions();
		ins.addInstruction(new InvokeStatic(ins, checkResize.getPoolMethod()));
		ins.addInstruction(new VReturn(ins));
		clientCheckResize.setCode(code);
		clientCheckResize.setPublic();
		clientVanilla.addMethod(clientCheckResize);
	}
}