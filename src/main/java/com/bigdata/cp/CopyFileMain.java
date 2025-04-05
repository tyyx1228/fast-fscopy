package com.bigdata.cp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author: tongyu
 * @date: 2025/3/15 17:18
 * @email: tongyu@powerbeijing.com
 */
@Data
@Slf4j
public class CopyFileMain {

    private Path inputPath;
    private Path outputPath;
    private ExecutorService pool;

    private List<File> fileList;
    private ProgressStatistics progressStatistics;

    private CopyingRuntime runtime;

    private long filesCnt = 0;
    private long dirsCnt = 0;
    private List<Pair> configDirs = new ArrayList();



    public CopyFileMain(CopyingRuntime runtime) throws IOException{
        this.runtime = runtime;
        this.inputPath = Paths.get(runtime.getInput());
        this.outputPath = Paths.get(runtime.getOutput());
        File tmp = new File(runtime.getInput());
        File file = copyDir(tmp);
        this.outputPath = file.toPath();

        this.pool = new ThreadPoolExecutor(Math.min(runtime.getNumThreads(), 128), Math.min(runtime.getNumThreads()*100, 2048),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        // this.fileList = Collections.unmodifiableList(tmp.isFile() ? Arrays.asList(tmp) : pathsRecursion(this.inputPath));
        this.progressStatistics = new ProgressStatistics(Long.MAX_VALUE);
    }

    public CopyFileMain(String inputPath, String outputPath, int nThread) throws IOException {
        this.inputPath = Paths.get(inputPath);
        this.outputPath = Paths.get(outputPath);
        File tmp = new File(inputPath);
        File file = copyDir(tmp);
        this.outputPath = file.toPath();
        // this.fileList = Collections.unmodifiableList(tmp.isFile() ? Arrays.asList(tmp) : pathsRecursion(this.inputPath));
        // this.pool = new ThreadPoolExecutor(Math.min(nThread, 128), Math.min(nThread*100, 2048),
        //         0L, TimeUnit.MILLISECONDS,
        //         new LinkedBlockingQueue<Runnable>());
        // this.progressStatistics = new ProgressStatistics(fileList.size());

        this.pool = new ThreadPoolExecutor(Math.min(nThread, 128), Math.min(nThread*100, 2048),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        // this.fileList = Collections.unmodifiableList(tmp.isFile() ? Arrays.asList(tmp) : pathsRecursion(this.inputPath));
        this.progressStatistics = new ProgressStatistics(Long.MAX_VALUE);
    }

    public List<File> pathsRecursion(Path path) throws IOException {
        ArrayList<File> totalFiles = new ArrayList<>();
        List<Path> list = Files.list(path).collect(Collectors.toList());
        for (Path p : list) {
            File file = new File(p.toUri());
            if(file.isFile()){
                totalFiles.add(file);
            }else if(file.isDirectory()){
                List<File> files = pathsRecursion(p);
                totalFiles.addAll(files);
            }
        }
        return totalFiles;
    }

    public void submit(File file){
        FileCopyTask task = new FileCopyTask(file, this);
        boolean notSubmitted = true;
        while (notSubmitted){
            try {
                pool.execute(task);
                notSubmitted = false;
                TimeUnit.NANOSECONDS.sleep(10);
            }catch (RejectedExecutionException e){
                log.warn("重新提交 {}", file);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public File copyDir(File file){
        if(!file.isDirectory()){
            return null;
        }
        String dirname = file.toPath().toString()
                .substring(inputPath.toString().length());

        dirname = "".equals(dirname) && !runtime.isFlat() ? file.toPath().getFileName().toString(): dirname;

        File targetDir = new File(outputPath.toString() + File.separator + dirname);

        if(targetDir.exists()){
            return targetDir;
        }
        boolean mkdirs = targetDir.mkdirs();
        if(runtime.isKeepOrg() && targetDir!=null){
            configDirs.add(new Pair(file, targetDir));
        }
        return targetDir;
    }

    public void recursionSubmit(Path path) throws IOException {
        File checkFile = path.toFile();
        if(checkFile.isFile()){
            submit(checkFile);
            return ;
        }
        List<Path> list = Files.list(path).collect(Collectors.toList());
        for (Path p : list) {
            File file = new File(p.toUri());
            if(file.isFile()){
                submit(file);
                filesCnt += 1;
            }else if(file.isDirectory()){
                if(!runtime.isFlat()){
                    copyDir(file);
                    dirsCnt += 1;
                }
                recursionSubmit(file.toPath());
            }
        }
        progressStatistics.getTotalNumFiles().set(filesCnt);
    }

    public void doCopy() throws Exception {
        this.progressStatistics.setStartTime(new Date());
        recursionSubmit(inputPath);
        // fileList.forEach(s->submit(s));
    }

    public void waitToComplete() throws InterruptedException, IOException {
        Thread thread = new Thread(progressStatistics);
        thread.start();
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
        this.progressStatistics.setEndTime(new Date());
        thread.interrupt();
        thread.join();

        for (Pair p : configDirs) {
            p.resetAttributes();
        }

    }

    public static void main(String[] args) throws Exception {
        // args = new String[]{
        //         "-i", "E:\\FFOutput\\data\\input",
        //         "-o", "E:\\FFOutput\\data\\output\\documents",
        //         "-s", "ext",
        //         "-b", "1",
        //         "-k", "true",
        //         "-n", "true",
        //         "-f", "false"
        // } ;

        // args = new String[]{
        //         "-i", "D:\\win11app\\game\\CSGO",
        //         "-o", "E:\\FFOutput\\data\\output\\CSGO",
        //         "-b", "5",
        //         "-k", "true",
        //         "-t", "2"
        // };

        String[] consoleArgs = args.length==0 ? new String[]{"-h"} : args;
        CopyingRuntime copyingRuntime = new CopyingRuntime(consoleArgs);

        Terminal terminal = TerminalBuilder.terminal();
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        List<String> signalList = Arrays.asList("yes", "no");
        String signal = null;
        while (true){
            String line = lineReader.readLine("Start copy tasks?" + "(yes/no, default: yes)");
            signal = line == null || "".equals(line.trim()) ? "yes" : line.trim();
            if(!signalList.contains(signal)){
                continue;
            }
            if("no".equals(signal)){
                System.out.println("Cancel copy tasks. ");
                System.exit(0);
            }
            break;
        }
        System.out.println();
        CopyFileMain copyFile = new CopyFileMain(copyingRuntime);
        copyFile.doCopy();
        copyFile.waitToComplete();


    }

}
