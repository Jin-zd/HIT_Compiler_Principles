package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {
    private SymbolTable symbolTable;
    private final List<Instruction> ir = new ArrayList<>();
    private final Stack<Symbol> symbolStack = new Stack<>();


    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        var text = currentToken.getText();
        var symbol = new Symbol(currentToken);
        if (isNumeric(text)) {
            symbol.value = IRImmediate.of(Integer.parseInt(text));
        } else {
            symbol.value = IRVariable.named(text);
        }
        symbolStack.push(symbol);
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        int index = production.index();
        var head = new Symbol(production.head());
        switch (index) {
            case 6 -> {
                // S -> id = E
                var E = symbolStack.pop();
                symbolStack.pop();
                var id = symbolStack.pop();
                ir.add(Instruction.createMov((IRVariable) id.value, E.value));
                symbolStack.push(head);
            }
            case 7 -> {
                // S -> return E
                var E = symbolStack.pop();
                symbolStack.pop();
                ir.add(Instruction.createRet(E.value));
                symbolStack.push(head);
            }
            case 8 -> {
                // E -> E + A
                var A = symbolStack.pop();
                symbolStack.pop();
                var E = symbolStack.pop();
                var temp = IRVariable.temp();
                ir.add(Instruction.createAdd(temp, E.value, A.value));
                head.value = temp;
                symbolStack.push(head);
            }
            case 9 -> {
                // E -> E - A
                var A = symbolStack.pop();
                symbolStack.pop();
                var E = symbolStack.pop();
                var temp = IRVariable.temp();
                ir.add(Instruction.createSub(temp, E.value, A.value));
                head.value = temp;
                symbolStack.push(head);
            }
            case 10, 12, 14, 15 -> {
                // E -> A, A -> B, B -> id, B -> IntConst
                head.value = symbolStack.pop().value;
                symbolStack.push(head);
            }
            case 11 -> {
                // A -> A * B
                var B = symbolStack.pop();
                symbolStack.pop();
                var A = symbolStack.pop();
                var temp = IRVariable.temp();
                ir.add(Instruction.createMul(temp, A.value, B.value));
                head.value = temp;
                symbolStack.push(head);
            }
            case 13 -> {
                // B -> ( E )
                symbolStack.pop();
                head.value = symbolStack.pop().value;
                symbolStack.pop();
                symbolStack.push(head);
            }
            default -> {
                for (var i = 0; i < production.body().size(); i++) {
                    symbolStack.pop();
                }
                symbolStack.push(head);
            }
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return ir;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

