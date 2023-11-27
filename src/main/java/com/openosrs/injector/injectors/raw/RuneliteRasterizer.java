/*
 * Copyright (c) 2023, Melxin <https://github.com/melxin>
 * All rights reserved.
 *
 * This code is licensed under GPL3, see the complete license in
 * the LICENSE file in the root directory of this submodule.
 */
package com.openosrs.injector.injectors.raw;

import com.openosrs.injector.injection.InjectData;
import com.openosrs.injector.injectors.AbstractInjector;
import org.objectweb.asm.Opcodes;
import java.util.List;
import net.runelite.asm.ClassFile;
import net.runelite.asm.Field;
import net.runelite.asm.Method;
import net.runelite.asm.Type;
import net.runelite.asm.attributes.Code;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.InstructionType;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.instructions.Dup;
import net.runelite.asm.attributes.code.instructions.GetStatic;
import net.runelite.asm.attributes.code.instructions.InvokeSpecial;
import net.runelite.asm.attributes.code.instructions.New;
import net.runelite.asm.attributes.code.instructions.PutStatic;
import net.runelite.asm.attributes.code.instructions.Return;
import net.runelite.asm.attributes.code.instructions.VReturn;
import net.runelite.asm.signature.Signature;

public class RuneliteRasterizer extends AbstractInjector
{
	private static final String RUNELITE_RASTERIZER = "RuneLiteRasterizer";

	public RuneliteRasterizer(InjectData inject)
	{
		super(inject);
	}

	public void inject()
	{
		final ClassFile runeliteRasterizerVanilla = inject.getVanilla().findClass(RUNELITE_RASTERIZER);

		final ClassFile abstractRasterizerVanilla = inject.toVanilla(
			inject.getDeobfuscated()
				.findClass("AbstractRasterizer")
		);

		final ClassFile clientVanilla = inject.toVanilla(
			inject.getDeobfuscated()
				.findClass("Client")
		);

		final Field rasterizer = new Field(abstractRasterizerVanilla, "rasterizer", new Type("Lnet/runelite/api/Rasterizer;"));
		rasterizer.setAccessFlags(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
		abstractRasterizerVanilla.addField(rasterizer);

		final Method clinit = new Method(abstractRasterizerVanilla, "<clinit>", new Signature("()V"));
		clinit.setPublic();
		clinit.setStatic(true);

		Code code = new Code(clinit);
		code.setMaxStack(2);
		clinit.setCode(code);
		abstractRasterizerVanilla.addMethod(clinit);

		Instructions instructions = code.getInstructions();
		List<Instruction> ins = instructions.getInstructions();
		ins.add(new New(instructions, runeliteRasterizerVanilla.getPoolClass()));
		ins.add(new Dup(instructions));
		ins.add(new InvokeSpecial(instructions, new net.runelite.asm.pool.Method(runeliteRasterizerVanilla.getPoolClass(), "<init>", new Signature("()V"))));
		ins.add(new PutStatic(instructions, rasterizer));
		ins.add(new VReturn(instructions));

		final Method getRasterizer = new Method(clientVanilla, "getRasterizer", new Signature("()Lnet/runelite/api/Rasterizer;"));
		getRasterizer.setPublic();

		code = new Code(getRasterizer);
		code.setMaxStack(2);
		getRasterizer.setCode(code);
		clientVanilla.addMethod(getRasterizer);

		instructions = code.getInstructions();
		ins = instructions.getInstructions();
		ins.add(new GetStatic(instructions, rasterizer.getPoolField()));
		ins.add(new Return(instructions, InstructionType.ARETURN));
	}
}
