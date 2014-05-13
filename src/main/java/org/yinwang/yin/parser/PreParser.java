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
        Node begin = lexer.nextToken();

        // end of file
        if (begin == null) {
            return null;
        }

        if (depth == 0 && Delimeter.isClose(begin)) {
            _.abort(begin, "unmatched closing delimeter: " + begin);
            return null;
        } else if (Delimeter.isOpen(begin)) {   // try to get matched (...)
            List<Node> elements = new ArrayList<>();
            Node iter = nextNode1(depth + 1);

            while (!Delimeter.matchDelimeter(begin, iter)) {
                if (iter == null) {
                    _.abort(begin, "unclosed delimeter: " + begin);
                    return null;
                } else if (Delimeter.isClose(iter)) {
                    _.abort(iter, "unmatched closing delimeter: " + iter);
                    return null;
                } else {
                    elements.add(iter);
                    iter = nextNode1(depth + 1);
                }
            }
            return new Tuple(elements, begin, iter, begin.file, begin.start, iter.end, begin.line, begin.col);
        } else {
            return begin;
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
