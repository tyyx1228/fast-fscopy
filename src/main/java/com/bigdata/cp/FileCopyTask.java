package com.bigdata.cp;

import com.google.common.base.Stopwatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * @author: tongyu
 * @date: 2025/3/15 17:43
 * @email: tongyu@powerbeijing.com
 */
@Data
@Slf4j
public class FileCopyTask implements Runnable{

    private File file;
    private CopyFileMain cf;
    private ProgressStatistics statistics;

    public FileCopyTask(File file, CopyFileMain cf){
        this.file = file;
        this.cf = cf;
        this.statistics = cf.getProgressStatistics();
    }

    public File createTargetFile(){
        String inputDir = cf.getInputPath().toString();
        String fileDir = file.getParent();

        String subParent = new File(inputDir).isFile() ? "" : fileDir.substring(inputDir.length());
        String targetDir = cf.getOutputPath().toString();

        if(cf.getRuntime().isFlat()){
            File dir = new File(targetDir);
            if(!dir.exists()){
                boolean mkdirs = dir.mkdirs();
            }
            return new File(targetDir + File.separator + file.getName() + cf.getRuntime().getSuffixExt());
        }else {
            String targetFilePath = targetDir +
                    (targetDir.endsWith(File.separator) ? "": File.separator )
                    + subParent
                    + File.separator + file.getName()
                    + cf.getRuntime().getSuffixExt();

            File targetFile = new File(targetFilePath);
            if(!targetFile.getParentFile().exists()){
                boolean mkdirs = targetFile.getParentFile().mkdirs();
                log.info("目录：{}，已创建.", targetFile.getParentFile());
            }
            return targetFile;
        }
    }

    public void nioCopy(File source, File target){
        Stopwatch stopWatch = Stopwatch.createStarted();
        try(FileChannel fin = new FileInputStream(source).getChannel();
            FileChannel fou = new FileOutputStream(target).getChannel()){
            fin.transferTo(0, fin.size(), fou);
            // if(cf.getRuntime().isKeepOrg()){
            //     target.setLastModified(source.lastModified());
            //     target.setExecutable(source.canExecute());
            //     target.setReadable(source.canRead());
            //     target.setWritable(source.canWrite());
            // }
            CopyingMessage msg = new CopyingMessage(Thread.currentThread().getName(), "Completed copy: " + file,
                    CopyingMessage.Status.SUCCESS, CopyingMessage.IoType.NIO, this.file.length(), cf.getInputPath(), cf.getOutputPath(),
                    this.file.toPath(), target.toPath(), stopWatch.elapsed(TimeUnit.MICROSECONDS));
            statistics.push(msg);
        }catch (Exception e){
            CopyingMessage msg = new CopyingMessage(Thread.currentThread().getName(), "Failed copy: " + file,
                    CopyingMessage.Status.FAIL, CopyingMessage.IoType.NIO, this.file.length(), cf.getInputPath(), cf.getOutputPath(),
                    this.file.toPath(), target.toPath(), stopWatch.elapsed(TimeUnit.MICROSECONDS));
            statistics.push(msg);
        }
    }
    public void osCopy(File source, File target)  {
        Stopwatch stopWatch = Stopwatch.createStarted();
        try{
            CopyOption[] copyOptions = cf.getRuntime().isKeepOrg()
                    ? new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING}
                    : new CopyOption[]{StandardCopyOption.REPLACE_EXISTING};
            Files.copy(source.toPath(), target.toPath(), copyOptions);
            CopyingMessage msg = new CopyingMessage(Thread.currentThread().getName(), "Completed copy: " + file,
                    CopyingMessage.Status.SUCCESS, CopyingMessage.IoType.NATIVE, this.file.length(), cf.getInputPath(), cf.getOutputPath(),
                    this.file.toPath(), target.toPath(), stopWatch.elapsed(TimeUnit.MICROSECONDS));
            statistics.push(msg);
        }catch (Exception e){
            CopyingMessage msg = new CopyingMessage(Thread.currentThread().getName(), "Failed copy: " + file,
                    CopyingMessage.Status.FAIL, CopyingMessage.IoType.NATIVE, this.file.length(), cf.getInputPath(), cf.getOutputPath(),
                    this.file.toPath(), target.toPath(), stopWatch.elapsed(TimeUnit.MICROSECONDS));
            statistics.push(msg);
        }

    }
    public void bufferedCopy(File source, File target) {
        Stopwatch stopWatch = Stopwatch.createStarted();
        try(BufferedInputStream fin = new BufferedInputStream(new FileInputStream(source));
            BufferedOutputStream fou = new BufferedOutputStream(new FileOutputStream(target))){
            int len = 0;
            byte[] buf = new byte[4096];
            while ( (len = fin.read(buf)) != -1){
                fou.write(buf, 0, len);
            }

            CopyingMessage msg = new CopyingMessage(Thread.currentThread().getName(), "Completed copy: " + file,
                    CopyingMessage.Status.SUCCESS, CopyingMessage.IoType.BIO, this.file.length(), cf.getInputPath(), cf.getOutputPath(),
                    this.file.toPath(), target.toPath(), stopWatch.elapsed(TimeUnit.MICROSECONDS));
            statistics.push(msg);
        }catch (Exception e){
            CopyingMessage msg = new CopyingMessage(Thread.currentThread().getName(), "Failed copy: " + file,
                    CopyingMessage.Status.FAIL, CopyingMessage.IoType.BIO, this.file.length(), cf.getInputPath(), cf.getOutputPath(),
                    this.file.toPath(), target.toPath(), stopWatch.elapsed(TimeUnit.MICROSECONDS));
            statistics.push(msg);
        }
    }

    @Override
    public void run() {
        // Stopwatch stopWatch = Stopwatch.createStarted();
        boolean isMaxFile = this.file.length() > cf.getRuntime().getMaxFileSize();
        // boolean keepAttributes = cf.getRuntime().isKeepOrg();
        // long totalSize = this.file.getTotalSpace();
        File targetFile = createTargetFile();

        if(cf.getRuntime().isUseNative()){
            osCopy(this.file, targetFile);
            return;
        }
        if(isMaxFile){
            // nio to copy
            nioCopy(this.file, targetFile);
        } else {
            // bio to copy
            bufferedCopy(this.file, targetFile);
        }
        if(cf.getRuntime().isKeepOrg()){
            configOrgAttributes(this.file, targetFile);
        }

    }

    public void configOrgAttributes(File source, File target){
        target.setLastModified(source.lastModified());
        target.setExecutable(source.canExecute());
        target.setReadable(source.canRead());
        target.setWritable(source.canWrite());
    }

    public static void main(String[] args) throws IOException {
        File f = new File("E:\\FFOutput\\data\\output\\documents\\test.txt");
        // f.createNewFile();

        File f2 = new File("E:\\FFOutput\\data\\output\\documents\\test2.txt");
        // f2.createNewFile();

        f2.setLastModified(f.lastModified());
        f2.setExecutable(f.canExecute());
        f2.setReadable(f.canRead());
        f2.setWritable(f.canWrite());
    }
}
