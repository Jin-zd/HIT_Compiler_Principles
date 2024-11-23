package cn.edu.hitsz.compiler.parser.table;

import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

public class Symbol {
    public final Token token;
    public final NonTerminal nonTerminal;
    public SourceCodeType type;
    public IRValue value;

    public Symbol(Token token) {
        this.token = token;
        this.nonTerminal = null;
        this.type = null;
        this.value = null;
    }

    public Symbol(NonTerminal nonTerminal) {
        this.token = null;
        this.nonTerminal = nonTerminal;
        this.type = null;
        this.value = null;
    }
}
