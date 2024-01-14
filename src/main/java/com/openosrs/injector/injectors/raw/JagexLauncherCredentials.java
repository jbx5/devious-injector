/*
 * Copyright (c) 2023 - 2024, Melxin <https://github.com/melxin>
 * All rights reserved.
 *
 * This code is licensed under GPL3, see the complete license in
 * the LICENSE file in the root directory of this submodule.
 */
package com.openosrs.injector.injectors.raw;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.asm.ClassFile;
import net.runelite.asm.ClassGroup;
import net.runelite.asm.Method;
import net.runelite.asm.Type;
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
import net.runelite.asm.pool.Field;
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
		final List<String> JX_PROPERTY_KEYS = List.of("JX_ACCESS_TOKEN", "JX_REFRESH_TOKEN", "JX_SESSION_ID", "JX_CHARACTER_ID", "JX_DISPLAY_NAME");

		final ClassGroup vanillaGroup = inject.getVanilla();

		final ClassFile clientVanilla = inject.toVanilla(
			inject.getDeobfuscated()
				.findClass("Client")
		);

		final Method getStaticCredentialsProperty = new Method(clientVanilla, "getStaticCredentialsProperty", new Signature("(Ljava/lang/String;)Ljava/lang/String;"));

		final Type clientType = new Type("Lclient;");

		final AtomicBoolean createdGetStaticCredentialsProperty = new AtomicBoolean(false);
		final AtomicInteger replacedEnvCount = new AtomicInteger(0);

		Field staticClient = clientVanilla.findMethod("init").getCode().getInstructions().getInstructions().stream()
			.filter(in -> in instanceof PutStatic && ((PutStatic) in).getMyField().getObfuscatedType().equals(clientType))
			.map(in -> ((PutStatic) in).getField())
			.findFirst()
			.orElse(null);

		for (ClassFile cf : vanillaGroup)
		{
			if (!createdGetStaticCredentialsProperty.get())
			{
				if (staticClient == null)
				{
					staticClient = cf.getFields().stream()
						.filter(f -> f.isStatic() && f.getObfuscatedType().equals(clientType))
						.map(f -> f.getPoolField())
						.findFirst()
						.orElse(null);
				}

				if (staticClient != null)
				{
					//System.out.println("Found static client: " + staticClient);
					getStaticCredentialsProperty.setPublic();
					getStaticCredentialsProperty.setStatic(true);
					Code code = new Code(getStaticCredentialsProperty);
					code.setMaxStack(2);
					getStaticCredentialsProperty.setCode(code);
					Instructions instructions = code.getInstructions();
					List<Instruction> ins = instructions.getInstructions();
					ins.add(new GetStatic(instructions, staticClient));
					ins.add(new ALoad(instructions, 0));
					ins.add(new InvokeInterface(instructions, new net.runelite.asm.pool.Method(new Class("net/runelite/rs/api/RSClient"), "getCredentialsProperty", new Signature("(Ljava/lang/String;)Ljava/lang/String;"))));
					ins.add(new Return(instructions, InstructionType.ARETURN));
					clientVanilla.addMethod(getStaticCredentialsProperty);

					createdGetStaticCredentialsProperty.set(true);
				}
			}

			if (replacedEnvCount.get() != JX_PROPERTY_KEYS.size())
			{
				cf.getMethods().stream()
					.filter(m -> m.getCode() != null && m.getCode().getInstructions().getInstructions().stream()
						.filter(in -> in instanceof LDC && ((LDC) in).getConstant() instanceof String && JX_PROPERTY_KEYS.contains(((LDC) in).getConstant()))
						.count() == JX_PROPERTY_KEYS.size())
					.findFirst()
					.ifPresent(targetMethod ->
					{
						final Code code = targetMethod.getCode();
						final Instructions instructions = code.getInstructions();
						final List<Instruction> ins = instructions.getInstructions();

						for (int i = 0; i < ins.size(); i++)
						{
							Instruction in = ins.get(i);

							if (in instanceof LDC && ((LDC) in).getConstant() instanceof String && JX_PROPERTY_KEYS.contains(((LDC) in).getConstant()))
							{
								//System.out.println("LDC: " + ((LDC) in).getConstant());
								Instruction shouldBeInvokeStatic = i + 1 < ins.size() ? ins.get(i + 1) : null;
								if (shouldBeInvokeStatic != null && shouldBeInvokeStatic instanceof InvokeStatic && ((InvokeStatic) shouldBeInvokeStatic).getMethod().getName().equals("getenv"))
								{
									ins.set(i + 1, new InvokeStatic(instructions, getStaticCredentialsProperty));
									replacedEnvCount.getAndIncrement();
									//System.out.println("Replace: " + " cf: " + cf + " method: " + ((InvokeStatic) in).getMethod() + " -> " + getStaticCredentialsProperty);
								}
							}
						}
					});
			}

			if (createdGetStaticCredentialsProperty.get() && replacedEnvCount.get() == JX_PROPERTY_KEYS.size())
			{
				System.out.println("JagexLauncherCredentials injection was successfully," + " replaced " + JX_PROPERTY_KEYS + " with " + getStaticCredentialsProperty);
				break;
			}
		}

		if (!createdGetStaticCredentialsProperty.get() || replacedEnvCount.get() != JX_PROPERTY_KEYS.size())
		{
			System.err.println("JagexLauncherCredentials injection was unsuccessfully, manual fix required!");
		}
	}
}