package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.ir.InstructionKind;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    List<Instruction> instructions = new ArrayList<>();
    HashMap<IRVariable, String> variableRegMap = new HashMap<>();
    HashMap<String, IRVariable> regVariableMap = new HashMap<>(){
        {
            put("t0", null);
            put("t1", null);
            put("t2", null);
            put("t3", null);
            put("t4", null);
            put("t5", null);
            put("t6", null);
        }
    };
    List<String> instructionsResult = new ArrayList<>();
    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        for (Instruction instruction : originInstructions) {
            if (instruction.getKind() == InstructionKind.ADD) {
                var lhs = instruction.getLHS();
                var rhs = instruction.getRHS();
                if (lhs.isImmediate() && rhs.isIRVariable()) {
                    instruction = Instruction.createAdd(instruction.getResult(), rhs, lhs);
                }
            } else if (instruction.getKind() == InstructionKind.SUB) {
                var lhs = instruction.getLHS();
                var rhs = instruction.getRHS();
                if (lhs.isImmediate() && rhs.isIRVariable()) {
                    var newMov = Instruction.createMov(IRVariable.temp(), lhs);
                    instructions.add(newMov);
                    instruction = Instruction.createSub(instruction.getResult(), newMov.getResult(), rhs);
                }
            }
            instructions.add(instruction);
        }
    }


    private String getReg(IRVariable variable) {
        if (variableRegMap.containsKey(variable)) {
            return variableRegMap.get(variable);
        }
        for (String reg : regVariableMap.keySet()) {
            if (regVariableMap.get(reg) == null) {
                regVariableMap.put(reg, variable);
                variableRegMap.put(variable, reg);
                return reg;
            }
        }
        for (String reg : regVariableMap.keySet()) {

            if (regVariableMap.get(reg).isTemp()) {
                regVariableMap.put(reg, variable);
                variableRegMap.put(variable, reg);
                return reg;
            }
        }
        return null;
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        for (Instruction instruction : instructions) {
            switch (instruction.getKind()) {
                case MOV -> {
                    var result = instruction.getResult();
                    var from = instruction.getFrom();
                    if (from.isImmediate()) {
                        var resultReg = getReg(result);
                        instructionsResult.add("li" + " " + resultReg + ", " + from);
                    } else if (from.isIRVariable()) {
                        var fromReg = getReg((IRVariable) from);
                        var resultReg = getReg(result);
                        instructionsResult.add("mv" + " " + resultReg + ", " + fromReg);
                    }
                }
                case ADD -> {
                    var result = instruction.getResult();
                    var lhs = instruction.getLHS();
                    var rhs = instruction.getRHS();
                    if (rhs.isImmediate()) {
                        var resultReg = getReg(result);
                        var lhsReg = getReg((IRVariable) lhs);
                        instructionsResult.add("addi" + " " + resultReg + ", " + lhsReg + ", " + rhs);
                    } else if (rhs.isIRVariable()) {
                        var resultReg = getReg(result);
                        var lhsReg = getReg((IRVariable) lhs);
                        var rhsReg = getReg((IRVariable) rhs);
                        instructionsResult.add("add" + " " + resultReg + ", " + lhsReg + ", " + rhsReg);
                    }
                 }
                case SUB, MUL-> {
                    var result = instruction.getResult();
                    var lhs = instruction.getLHS();
                    var rhs = instruction.getRHS();
                    var resultReg = getReg(result);
                    var lhsReg = getReg((IRVariable) lhs);
                    var rhsReg = getReg((IRVariable) rhs);
                    if (instruction.getKind() == InstructionKind.MUL)
                        instructionsResult.add("mul" + " " + resultReg + ", " + lhsReg + ", " + rhsReg);
                    else {
                        instructionsResult.add("sub" + " " + resultReg + ", " + lhsReg + ", " + rhsReg);
                    }
                }
                case RET -> {
                    var returnValue = instruction.getReturnValue();
                    var resultReg = getReg((IRVariable) returnValue);
                    instructionsResult.add("mv" + " " + "a0" + ", " + resultReg);
                }
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        File file = new File(path);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(".text\n");
            for (String instruction : instructionsResult) {
                fileWriter.write("\t" + instruction + "\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

