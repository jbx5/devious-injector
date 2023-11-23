/*
 * Copyright (c) 2023, Melxin <https://github.com/melxin>
 * All rights reserved.
 *
 * This code is licensed under GPL3, see the complete license in
 * the LICENSE file in the root directory of this submodule.
 */
package com.openosrs.injector.injectors.raw;

import java.util.List;
import net.runelite.asm.ClassFile;
import net.runelite.asm.Method;
import net.runelite.asm.attributes.Code;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.InstructionType;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.instructions.ALoad;
import net.runelite.asm.attributes.code.instructions.GetStatic;
import net.runelite.asm.attributes.code.instructions.InvokeInterface;
import net.runelite.asm.attributes.code.instructions.InvokeStatic;
import net.runelite.asm.attributes.code.instructions.LDC;
import net.runelite.asm.attributes.code.instructions.PutStatic;
import net.runelite.asm.attributes.code.instructions.Return;
import net.runelite.asm.pool.Class;
import net.runelite.asm.signature.Signature;
import com.openosrs.injector.injection.InjectData;
import com.openosrs.injector.injectors.AbstractInjector;

public class JagexLauncherCredentials extends AbstractInjector
{
	public JagexLauncherCredentials(InjectData inject)
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

		final Method init = clientVanilla.findMethod("init");
		final Method getStaticCredentialsProperty = new Method(clientVanilla, "getStaticCredentialsProperty", new Signature("(Ljava/lang/String;)Ljava/lang/String;"));

		final Instructions instructions = init.getCode().getInstructions();
		final List<Instruction> ins = instructions.getInstructions();

		boolean insert = false;
		for (int i = 0; i < ins.size(); i++)
		{
			Instruction in = ins.get(i);

			if (in instanceof PutStatic && ((PutStatic) in).getMyField().getObfuscatedType().toString().startsWith("Lclient;"))
			{
				getStaticCredentialsProperty.setPublic();
				getStaticCredentialsProperty.setStatic(true);
				Code code = new Code(getStaticCredentialsProperty);
				code.setMaxStack(2);
				getStaticCredentialsProperty.setCode(code);
				List<Instruction> getStaticCredentialsPropertyIns = code.getInstructions().getInstructions();
				getStaticCredentialsPropertyIns.add(new GetStatic(instructions, ((PutStatic) in).getField()));
				getStaticCredentialsPropertyIns.add(new ALoad(instructions, 0));
				getStaticCredentialsPropertyIns.add(new InvokeInterface(instructions, new net.runelite.asm.pool.Method(new Class("net/runelite/rs/api/RSClient"), "getCredentialsProperty", new Signature("(Ljava/lang/String;)Ljava/lang/String;"))));
				getStaticCredentialsPropertyIns.add(new Return(instructions, InstructionType.ARETURN));
				clientVanilla.addMethod(getStaticCredentialsProperty);
			}

			if (in instanceof LDC && ((LDC) in).getConstant() instanceof String && ((String) ((LDC) in).getConstant()).startsWith("JX_"))
			{
				//System.out.println("LDC: " + ((LDC) in).getConstant());
				insert = true;
			}

			if (insert && in instanceof InvokeStatic && ((InvokeStatic) in).getMethod().getName().equals("getenv"))
			{
				ins.set(i, new InvokeStatic(instructions, getStaticCredentialsProperty));
				insert = false;
				//System.out.println("Replace method: " + ((InvokeStatic) in).getMethod().getName() + " -> " + getStaticCredentialsProperty.getName());
			}
		}

	}
}
