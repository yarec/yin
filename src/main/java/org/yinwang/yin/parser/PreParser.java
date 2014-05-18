package org.yinwang.yin.parser;

import org.yinwang.yin.Constants;
import org.yinwang.yin._;
import org.yinwang.yin.ast.Delimeter;
import org.yinwang.yin.ast.Name;
import org.yinwang.yin.ast.Node;
import org.yinwang.yin.ast.Tuple;

import java.util.ArrayList;
import java.util.List;


/**
 * parse text into a meanlingless but more structured format
 * similar to S-expressions but with less syntax
 * (just matched (..) [..] and {..})
 */
public class PreParser {

    String file;
    String text;
    Lexer lexer;


    public PreParser(String file) {
        this.file = _.unifyPath(file);
        this.text = _.readFile(file);
        this.lexer = new Lexer(file);
    }


    /**
     * Get next node from token stream
     */
    public Node nextNode() throws ParserException {
        return nextNode1(0);
    }


    /**
     * Helper for nextNode, which does the real work
     *
     * @return a Node or null if file ends
     */
    public Node nextNode1(int depth) throws ParserException {
        Node first = lexer.nextToken();

        // end of file
        if (first == null) {
            return null;
        }

        if (Delimeter.isOpen(first)) {   // try to get matched (...)
            List<Node> elements = new ArrayList<>();
            Node next = nextNode1(depth + 1);

            while (!Delimeter.matchDelimeter(first, next)) {
                if (next == null) {
                    throw new ParserException("unclosed delimeter: " + first, first);
                } else if (Delimeter.isClose(next)) {
                    throw new ParserException("unmatched closing delimeter: " + next +
                            " does not close: " + first, next);
                } else {
                    elements.add(next);
                    next = nextNode1(depth + 1);
                }
            }
            return new Tuple(elements, first, next, first.file, first.start, next.end, first.line, first.col);
        } else if (depth == 0 && Delimeter.isClose(first)) {
            throw new ParserException("unmatched closing delimeter: " + first +
                    " does not close any open delimeter", first);
        } else {
            return first;
        }
    }


    /**
     * Parse file into a Node
     *
     * @return a Tuple containing the file's parse tree
     */
    public Node parse() throws ParserException {
        List<Node> elements = new ArrayList<>();
        elements.add(Name.genName(Constants.SEQ_KEYWORD));      // synthetic block keyword

        Node s = nextNode();
        while (s != null) {
            elements.add(s);
            s = nextNode();
        }

        return new Tuple(
                elements,
                Name.genName(Constants.PAREN_BEGIN),
                Name.genName(Constants.PAREN_END),
                file, 0, text.length(), 0, 0
        );
    }


    public static void main(String[] args) throws ParserException {
        PreParser p = new PreParser(args[0]);
        _.msg("preparser result: " + p.parse());
    }

}
