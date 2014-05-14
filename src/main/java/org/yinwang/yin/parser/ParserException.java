package org.yinwang.yin.parser;


public class ParserException extends Exception {
    public int line;
    public int col;
    public int start;


    public ParserException(String message, int line, int col, int start) {
        super(message);
        this.line = line;
        this.col = col;
        this.start = start;
    }


    @Override
    public String toString() {
        return (line + 1) + ":" + (col + 1) + " parsing error " + getMessage();
    }
}
