/*
 * The MIT License (MIT)
 *
 * Copyright (c) Despector <https://despector.voxelgenesis.com>
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
package org.spongepowered.despector.decompiler.kotlin.method.graph.data;

import static com.google.common.base.Preconditions.checkState;

import org.spongepowered.despector.ast.Locals;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.kotlin.Elvis;
import org.spongepowered.despector.ast.stmt.StatementBlock;
import org.spongepowered.despector.decompiler.method.StatementBuilder;
import org.spongepowered.despector.decompiler.method.graph.data.block.BlockSection;
import org.spongepowered.despector.decompiler.method.graph.data.opcode.OpcodeBlock;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A block section represents a kotlin elvis (?:) statement.
 */
public class ElvisBlockSection extends BlockSection {

    private final OpcodeBlock block;

    public ElvisBlockSection(OpcodeBlock block) {
        this.block = block;
    }

    /**
     * Gets the body of the else clause.
     */
    public OpcodeBlock getBlock() {
        return this.block;
    }

    @Override
    public void appendTo(StatementBlock block, Locals locals, Deque<Instruction> stack) {
        // The blocksection before this one should have been our processed elvis
        // and will have left the value to be null checked on the stack
        StatementBlock dummy = new StatementBlock(StatementBlock.Type.IF);
        Deque<Instruction> dummy_stack = new ArrayDeque<>();
        StatementBuilder.appendBlock(this.block, dummy, locals, dummy_stack);
        checkState(dummy_stack.size() == 1);
        Elvis elvis = new Elvis(stack.pop(), dummy_stack.pop());
        stack.push(elvis);
    }
}
