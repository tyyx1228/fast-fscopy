package com.bigdata.cp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.TreeSet;

/**
 * @author: tongyu
 * @date: 2025/3/30 21:02
 * @email: tongyu@powerbeijing.com
 */
@Setter
@Getter
@NoArgsConstructor
public class CopyingRuntime {
    private String input;
    private String output;
    private int numThreads = 1 ;
    private boolean flat;
    private boolean keepOrg;
    private long maxFileSize;
    private boolean md5Verify;
    private String suffixExt;
    private boolean useNative;

    public CopyingRuntime(String args[]) {
        ArgumentParser parser = ArgumentParsers.newFor("fastcp").build()
                .defaultHelp(true)
                .description("Copy files or directory fastly.");

        parser.addArgument("-t", "--threads")
                .type(Integer.class)
                .setDefault(Runtime.getRuntime().availableProcessors())
                .help("Specify number of thread tasks.");
        parser.addArgument("-f", "--flat")
                .type(Boolean.class)
                .choices(true, false)
                .setDefault(false)
                .help("Copy everything to the same target directory.");
        parser.addArgument("-k", "--keep")
                .type(Boolean.class)
                .choices(true, false)
                .setDefault(false)
                .help("Keep the original file timestamp.");
        parser.addArgument("-b", "--bigfile")
                .type(Long.class)
                .setDefault(8L)
                .help("Big file size of unit M.");
        // parser.addArgument("-c", "--check")
        //         .type(Boolean.class)
        //         .choices(true, false)
        //         .setDefault(false)
        //         .help("Verify file after copy (Use MD5).");
        parser.addArgument("-s", "--suffixext")
                .setDefault("")
                .help("Specify file name suffix to extend.");
        parser.addArgument("-n", "--native")
                .type(Boolean.class)
                .choices(true, false)
                .setDefault(false)
                .help("Use os native copy method.");
        // parser.addArgument("-s", "--suffix-clear")
        //         .setDefault("")
        //         .help("Specify file name suffix to clear.");
        parser.addArgument("-i", "--input")
                // .required(true)
                .help("Specify source file or directory");
        parser.addArgument("-o", "--output")
                // .required(true)
                .help("Specify target directory.");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        this.input = ns.getString("input");
        this.output = ns.getString("output");
        this.numThreads = ns.getInt("threads");
        this.flat = ns.getBoolean("flat");
        this.keepOrg = ns.getBoolean("keep");
        this.maxFileSize = ns.getLong("bigfile") * 1024 * 1024; // MB转为字节数
        // this.md5Verify = ns.getBoolean("check");
        String suffixExt = ns.getString("suffixext");
        this.suffixExt = suffixExt == null ? "" : suffixExt.startsWith(".") ? suffixExt : "." + suffixExt;
        this.useNative = ns.getBoolean("native");
        for (String key : new TreeSet<>(ns.getAttrs().keySet())) {
            System.out.println(key + ": " + ns.getAttrs().get(key));
        }
    }

    @Override
    public String toString() {
        return "CopyingRuntime{" +
                "input='" + input + '\'' +
                ", output='" + output + '\'' +
                ", numThreads=" + numThreads +
                ", flat=" + flat +
                ", keepOrg=" + keepOrg +
                ", maxFileSize=" + maxFileSize +
                ", md5Verify=" + md5Verify +
                ", suffixExt='" + suffixExt + '\'' +
                '}';
    }

    public static void main(String[] args) {
        // String[] consoleArgs = args.length==0 ? new String[]{"-h"} : args;
        String[] consoleArgs = args.length==0 ? new String[]{"-f", "false", "-b", "256"} : args;
        CopyingRuntime copyingRuntime = new CopyingRuntime(consoleArgs);
        System.out.println(copyingRuntime);

    }
}
