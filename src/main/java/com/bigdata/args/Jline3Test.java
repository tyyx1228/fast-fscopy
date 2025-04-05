package com.bigdata.args;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: tongyu
 * @date: 2025/3/29 15:03
 * @email: tongyu@powerbeijing.com
 */
public class Jline3Test {
    public static void main(String[] args) throws IOException {

        Terminal terminal = TerminalBuilder.terminal();
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        List<String> signalList = Arrays.asList("yes", "no");
        while (true){
            String line = lineReader.readLine("Start copy tasks?" + "(yes/no, default: yes)");
            String signal = line == null || "".equals(line.trim()) ? "yes" : line.trim();
            if(!signalList.contains(signal)){
               continue;
            }
            if("no".equals(signal)){
                System.out.println("Cancel copy tasks. ");
                System.exit(0);
            }

            System.out.println("处理业务");
            break;
        }

    }
}
