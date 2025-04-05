package com.bigdata;

import com.bigdata.cp.Pair;
import com.google.common.collect.Lists;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: tongyu
 * @date: 2025/4/4 1:05
 * @email: tongyu@powerbeijing.com
 */
public class FileSuffixRename {
    private Path inputPath;
    private String suffix;
    private String toSuffix;
    private List<Pair> configDirs = new ArrayList<>();


    public FileSuffixRename(Path inputPath, String suffix, String toSuffix){
        this.inputPath = inputPath;
        this.suffix = suffix;
        this.toSuffix = toSuffix;
    }

    public void doRename(File file) throws IOException {
        if(file.isFile()){
            String fileName = file.getName();
            if(!fileName.endsWith(suffix)) {
                return;
            }
            String newFileName = fileName.substring(0, fileName.lastIndexOf(suffix)) + toSuffix;
            File fileWithNewName = new File(file.getParent() + File.separator + newFileName);
            file.renameTo(fileWithNewName);

        }else if(file.isDirectory()) {
            // configDirs.add(new Pair(file, file));
            List<Path> files = Files.list(file.toPath()).collect(Collectors.toList());
            for (Path path : files) {
                doRename(path.toFile());
            }
        }
    }

    public void renameAll() throws IOException {
        doRename(inputPath.toFile());
    }

    public static void main(String[] args) throws IOException {
        // args = new String[]{"E:\\FFOutput\\data\\output\\documents", ".ext"};

        List<String> paramList = new ArrayList<>();
        for (String arg : args) {
            paramList.add(arg);
        }
        if(paramList.size()==2){
            paramList.add("");
        }
        String[] consoleParams = args.length==0 ? new String[]{"-h"} : paramList.toArray(new String[]{});

        ArgumentParser parser = ArgumentParsers.newFor("resext").build()
                .defaultHelp(true)
                .description("Rename the files extension");
        parser.addArgument("file/dir").nargs(1)
                .required(true)
                .help("File or files in directory to rename.");
        parser.addArgument("oldExtension").nargs(1)
                .required(true)
                .help("Specify old extension.");
        parser.addArgument("newExtension").nargs(1)
                .required(false)
                .help("Specify new extension.");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(consoleParams);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        String path = String.valueOf(ns.getList("file/dir").get(0));
        String suffix = String.valueOf(ns.getList("oldExtension").get(0));
        String nsuffix = String.valueOf(ns.getList("newExtension").get(0));

        System.out.println(path);
        System.out.println(suffix);
        System.out.println(nsuffix);

        FileSuffixRename fsr = new FileSuffixRename(Paths.get(path),
                suffix.startsWith(".") ? suffix : "."+suffix,
                nsuffix.startsWith(".") || "".equals(nsuffix) ? nsuffix : "."+nsuffix);

        fsr.renameAll();

    }
}
