package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private final ArrayList<String> srcLines;
    private final ArrayList<Token> tokens;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.srcLines = new ArrayList<>();
        this.tokens = new ArrayList<>();
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        srcLines.addAll(FileUtils.readLines(path));
    }


    private Boolean isDigital(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }


    private void fillSymbolTable(String text) {
        if (!symbolTable.has(text)) {
            symbolTable.add(text);
        }
    }


    private void tokenGenerator(String buffer) {
        if (TokenKind.isAllowed(buffer)) {
            tokens.add(Token.simple(buffer));
        } else {
            if (isDigital(buffer)) {
                tokens.add(Token.normal("IntConst", buffer));
            } else {
                tokens.add(Token.normal("id", buffer));
                fillSymbolTable(buffer);
            }
        }
    }

    enum State {
        INITIAL,
        IN_TOKEN
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        StringBuilder buffer = new StringBuilder();
        State currentState = State.INITIAL;

        for (final var line : srcLines) {
            for (final var ch : line.toCharArray()) {
                switch (currentState) {
                    case INITIAL:
                        if (Character.isWhitespace(ch)) {
                            continue;
                        } else if (ch == ';') {
                            tokenGenerator("Semicolon");
                        } else {
                            buffer.append(ch);
                            currentState = State.IN_TOKEN;
                        }
                        break;
                    case IN_TOKEN:
                        if (Character.isWhitespace(ch)) {
                            tokenGenerator(buffer.toString());
                            buffer.setLength(0);
                            currentState = State.INITIAL;
                        } else if (ch == ';') {
                            tokenGenerator(buffer.toString());
                            buffer.setLength(0);
                            tokenGenerator("Semicolon");
                            currentState = State.INITIAL;
                        } else {
                            buffer.append(ch);
                        }
                        break;
                }
            }
        }

        if (!buffer.isEmpty()) {
            tokenGenerator(buffer.toString());
        }
        tokenGenerator("$");
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
