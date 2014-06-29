package org.yinwang.yin;

import org.yinwang.yin.ast.Node;
import org.yinwang.yin.parser.Parser;
import org.yinwang.yin.parser.ParserException;
import org.yinwang.yin.parser.PreParser;

import java.io.*;
/**
 * Created by C.Zhang on 6/28/14.
 */
public class InteractiveInterpreter {
    Scope persistentScope = Scope.buildInitScope();

    Node parseString(StringBuffer buffer) throws ParserException {
        PreParser preparser = new PreParser(buffer);
        Node prenode = preparser.parse();
        return Parser.parseNode(prenode);
    }

    boolean isTolerable(ParserException e) {
        if (e.getMessage().equals("runaway string")) {
            return true;
        } else if (e.getMessage().startsWith("unclosed delimeter till end of file")) {
            return true;
        }
        return false;
    }
    void runREPLLoop() throws IOException {
        StringBuffer buffer = new StringBuffer();

        int c ;
        int state = 0; // Ready

        // the Run-Eval-Print Loop
        while(true) {
            if (state < 0) {
                break;
            }
            switch (state) {
                case 0:
                    System.out.print("(seq ");
                    buffer.setLength(0);
                    buffer.append("(seq ");
                    break;
                case 1:
                    System.out.print(".... ");
                    break;

            }

            // placeholder
            state = -1;
            // stop when ctrl-d is pressed
            while ((c = System.in.read()) != -1) {

                char character = (char)c;
                buffer.append(character);
                // newline
                if (c == '\n') {
                    Node program;
                    String result = "";
                    try {
                        program = parseString(buffer);

                        try {
                            result = program.interp(persistentScope).toString();
                        } catch (Exception e){
                            result = e.getMessage();
                        } finally {
                            System.out.println(result);
                            state = 0;
                        }

                    } catch (ParserException pe) {
                        if (isTolerable(pe)) {
                            state = 1;
                        } else {
                            System.out.println(pe.getMessage());
                            state = 0;
                        }
                    }
                    break;
                }
            }
        }
    }
    public static void main(String[] args) throws IOException {
        InteractiveInterpreter interp = new InteractiveInterpreter();
        interp.runREPLLoop();
    }
}
