/*
 * The MIT License (MIT)
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.despector.emitter.instruction;

import com.google.common.collect.Lists;
import org.spongepowered.despector.ast.members.insn.arg.Instruction;
import org.spongepowered.despector.ast.members.insn.arg.cst.StringConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.field.LocalArg;
import org.spongepowered.despector.ast.members.insn.function.InstanceMethodInvoke;
import org.spongepowered.despector.ast.members.insn.function.New;
import org.spongepowered.despector.ast.members.insn.function.StaticMethodInvoke;
import org.spongepowered.despector.emitter.EmitterContext;
import org.spongepowered.despector.emitter.InstructionEmitter;
import org.spongepowered.despector.util.TypeHelper;

import java.util.List;

public class InstanceMethodInvokeEmitter implements InstructionEmitter<InstanceMethodInvoke> {

    @Override
    public void emit(EmitterContext ctx, InstanceMethodInvoke arg, String type) {
        if (arg.getOwner().equals("Ljava/lang/StringBuilder;") && arg.getMethodName().equals("toString")) {
            // We detect and collapse string builder chains used to perform
            // string concatentation into simple "foo" + "bar" form
            boolean valid = true;
            Instruction callee = arg.getCallee();
            List<Instruction> constants = Lists.newArrayList();
            // We add all the constants to the front of this list as we have to
            // replay them in the reverse of the ordering that we will encounter
            // them in
            while (callee != null) {
                if (callee instanceof InstanceMethodInvoke) {
                    InstanceMethodInvoke call = (InstanceMethodInvoke) callee;
                    if (call.getParams().length == 1) {
                        constants.add(0, call.getParams()[0]);
                        callee = call.getCallee();
                        continue;
                    }
                } else if (callee instanceof New) {
                    New ref = (New) callee;
                    if ("Ljava/lang/StringBuilder;".equals(ref.getType())) {
                        if (ref.getParameters().length == 1) {
                            Instruction initial = ref.getParameters()[0];
                            if (initial instanceof StaticMethodInvoke) {
                                StaticMethodInvoke valueof = (StaticMethodInvoke) initial;
                                if (valueof.getMethodName().equals("valueOf") && valueof.getOwner().equals("Ljava/lang/String;")) {
                                    Instruction internal = valueof.getParams()[0];
                                    if (internal instanceof StringConstantArg) {
                                        initial = internal;
                                    } else if (internal instanceof LocalArg) {
                                        LocalArg local = (LocalArg) internal;
                                        if (local.getLocal().getType().equals("Ljava/lang/String;")) {
                                            initial = local;
                                        }
                                    }
                                }
                            }
                            constants.add(0, initial);
                        }
                        break;
                    }
                    valid = false;
                    break;
                }
                valid = false;
            }
            if (valid) {
                for (int i = 0; i < constants.size(); i++) {
                    ctx.emit(constants.get(i), "Ljava/lang/String;");
                    if (i < constants.size() - 1) {
                        ctx.printString(" + ");
                    }
                }
                return;
            }
        }
        if (arg.getMethodName().equals("<init>")) {
            if (ctx.getType() != null) {
                if (arg.getOwnerType().equals(ctx.getType().getName())) {
                    ctx.printString("this");
                } else {
                    ctx.printString("super");
                }
            } else {
                ctx.printString("super");
            }
        } else {
            if (arg.getCallee() instanceof LocalArg && ctx.getMethod() != null && !ctx.getMethod().isStatic()) {
                LocalArg local = (LocalArg) arg.getCallee();
                if (local.getLocal().getIndex() == 0) {
                    if (ctx.getType() != null && !arg.getOwnerType().equals(ctx.getType().getName())) {
                        ctx.printString("super.");
                    }
                } else {
                    ctx.emit(local, null);
                    ctx.printString(".");
                }
            } else {
                ctx.emit(arg.getCallee(), arg.getOwner());
                ctx.printString(".");
            }
            ctx.printString(arg.getMethodName());
        }
        ctx.printString("(");
        // TODO get param types if we have the ast
        List<String> param_types = TypeHelper.splitSig(arg.getMethodDescription());
        for (int i = 0; i < arg.getParams().length; i++) {
            Instruction param = arg.getParams()[i];
            ctx.emit(param, param_types.get(i));
            if (i < arg.getParams().length - 1) {
                ctx.printString(", ");
            }
        }
        ctx.printString(")");
    }

}