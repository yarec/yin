package org.yinwang.yin.parser;


import org.jetbrains.annotations.Nullable;
import org.yinwang.yin.Constants;
import org.yinwang.yin._;
import org.yinwang.yin.ast.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Lexer
 * split text stream into tokens, nubmers, delimeters etc
 */
public class Lexer {

    public String file;
    public String text;

    // current offset indicators
    public int offset;
    public int line;
    public int col;


    public Lexer(String file) {
        this.file = _.unifyPath(file);
        this.text = _.readFile(file);
        this.offset = 0;
        this.line = 0;
        this.col = 0;

        if (text == null) {
            _.abort("failed to read file: " + file);
        }

        Delimeter.addDelimiterPair(Constants.PAREN_BEGIN, Constants.PAREN_END);
        Delimeter.addDelimiterPair(Constants.CURLY_BEGIN, Constants.CURLY_END);
        Delimeter.addDelimiterPair(Constants.SQUARE_BEGIN, Constants.ARRAY_END);

        Delimeter.addDelimiter(Constants.ATTRIBUTE_ACCESS);
    }


    public void forward() {
        if (text.charAt(offset) == '\n') {
            line++;
            col = 0;
            offset++;
        } else {
            col++;
            offset++;
        }
    }


    public boolean skipSpaces() {
        boolean found = false;

        while (offset < text.length() &&
                Character.isWhitespace(text.charAt(offset)))
        {
            found = true;
            forward();
        }
        return found;
    }


    public boolean skipComments() {
        boolean found = false;

        if (text.startsWith(Constants.LINE_COMMENT, offset)) {
            found = true;

            // skip to line end
            while (offset < text.length() && text.charAt(offset) != '\n') {
                forward();
            }
            if (offset < text.length()) {
                forward();
            }
        }
        return found;
    }


    public void skipSpacesAndComments() {
        while (skipSpaces() || skipComments());
    }


    public boolean atStringStart() {
        return text.charAt(offset) == '"' &&
                (offset == 0 || text.charAt(offset - 1) != '\\');
    }


    public Node scanString() {
        int start = offset;
        int startLine = line;
        int startCol = col;
        forward();   // skip "

        while (offset < text.length() && !atStringStart()) {
            if (text.charAt(offset) == '\n') {
                _.abort(file + ":" + startLine + ":" + startCol + ": runaway string");
                return null;
            }
            forward();
        }

        if (offset >= text.length()) {
            _.abort(file + ":" + startLine + ":" + startCol + ": runaway string");
            return null;
        }

        forward(); // skip "
        int end = offset;

        String content = text.substring(start + 1, end - 1);
        return new Str(content, file, start, end, startLine, startCol);
    }


    public Node scanNumber() {
        char cur = text.charAt(offset);
        int start = offset;
        int startLine = line;
        int startCol = col;

        while (offset < text.length() &&
                !Character.isWhitespace(cur) &&
                !(Delimeter.isDelimiter(cur) && cur != '.'))
        {
            forward();
            if (offset < text.length()) {
                cur = text.charAt(offset);
            }
        }

        String content = text.substring(start, offset);

        IntNum intNum = IntNum.parse(content, file, start, offset, startLine, startCol);
        if (intNum != null) {
            return intNum;
        } else {
            FloatNum floatNum = FloatNum.parse(content, file, start, offset, startLine, startCol);
            if (floatNum != null) {
                return floatNum;
            } else {
                _.abort(file + ":" + startLine + ":" + startCol + " : incorrect number format: " + content);
                return null;
            }
        }
    }


    public Node scanNameOrKeyword() {
        char cur = text.charAt(offset);
        int start = offset;
        int startLine = line;
        int startCol = col;

        while (offset < text.length() &&
                !Character.isWhitespace(cur) &&
                !Delimeter.isDelimiter(cur))
        {
            forward();
            if (offset < text.length()) {
                cur = text.charAt(offset);
            }
        }

        String content = text.substring(start, offset);
        if (content.matches(":\\w.*")) {
            return new Keyword(content.substring(1), file, start, offset, startLine, startCol);
        } else {
            return new Name(content, file, start, offset, startLine, startCol);
        }
    }


    /**
     * Lexer
     *
     * @return a token or null if file ends
     */
    @Nullable
    public Node nextToken() {

        skipSpacesAndComments();

        // end of file
        if (offset >= text.length()) {
            return null;
        }

        {
            // case 1. delimiters
            char cur = text.charAt(offset);
            if (Delimeter.isDelimiter(cur)) {
                Node ret = new Delimeter(Character.toString(cur), file, offset, offset + 1, line, col);
                forward();
                return ret;
            }
        }

        // case 2. string
        if (atStringStart()) {
            return scanString();
        }

        // case 3. number
        if (Character.isDigit(text.charAt(offset)) ||
                ((text.charAt(offset) == '+' || text.charAt(offset) == '-')
                        && Character.isDigit(text.charAt(offset + 1))))
        {
            return scanNumber();
        }

        // case 4. name or keyword
        return scanNameOrKeyword();
    }


    public static void main(String[] args) {
        Lexer lex = new Lexer(args[0]);

        List<Node> tokens = new ArrayList<>();
        Node n = lex.nextToken();
        while (n != null) {
            tokens.add(n);
            n = lex.nextToken();
        }
        _.msg("lexer result: ");
        for (Node node : tokens) {
            _.msg(node.toString());
        }
    }
}
