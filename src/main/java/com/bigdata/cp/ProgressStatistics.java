package com.bigdata.cp;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统计总文件数、成功完成复制的数量
 * 统计文件复制总耗时，
 * @author: tongyu
 * @date: 2025/3/16 10:31
 * @email: tongyu@powerbeijing.com
 */
@Data
@Slf4j
public class ProgressStatistics implements Runnable {
    private AtomicLong totalNumFiles;
    // private AtomicLong currNumFiles = new AtomicLong(0);
    // private AtomicLong totalTimes = new AtomicLong(0);
    // private AtomicLong totalFileSize = new AtomicLong(0);
    private long currNumFiles = 0;
    private long totalTimes = 0;
    private long totalFileSize = 0;
    private Date startTime;
    private Date endTime;
    private LinkedBlockingDeque<CopyingMessage> queue;
    private Map<String, Long> sizeMapper = new TreeMap<String, Long>(){
        {
            put("B", 1L);
            put("KB", (long) Math.pow(1024, 1));
            put("MB", (long) Math.pow(1024, 2));
            put("GB", (long) Math.pow(1024, 3));
            put("TB", (long) Math.pow(1024, 4));
            put("PB", (long) Math.pow(1024, 5));
            put("EB", (long) Math.pow(1024, 6));
        }
    };

    public ProgressStatistics(long tatolNumFiles){
        this.totalNumFiles = new AtomicLong(tatolNumFiles);
        this.queue= new LinkedBlockingDeque<>();
    }

    public void push(CopyingMessage msg){
        queue.push(msg);
    }

    public String fileSizeFormat(long size){
        return fileSizeFormat(size, null);
    }

    public String fileSizeFormat(long size, String unit){
        if(unit!=null && sizeMapper.containsKey(unit.toUpperCase())){
            long l = size / sizeMapper.get(unit);
            return String.format("%.2f", (double)size / sizeMapper.get(unit)) + unit.toUpperCase();
        }

        for (String key : sizeMapper.keySet()) {
            long l = size / sizeMapper.get(key);
            if(l > 0 && l < 1024){
                return String.format("%.2f", (double)size / sizeMapper.get(key)) + key;
            }
        }
        return size+"";
    }

    public void doStatistics(CopyingMessage msg){
        doStatistics(msg, true);
    }

    public void doStatistics(CopyingMessage msg, boolean printFile){
        // long completeFileCnts = currNumFiles.incrementAndGet();
        // long completeFileTimeCost = totalTimes.addAndGet(msg.getTimeLength());
        // long completeFileSize = totalFileSize.addAndGet(msg.getFileSize());

        long completeFileCnts = ++currNumFiles;
        long completeFileTimeCost = (totalTimes+=msg.getTimeLength());
        long completeFileSize = (totalFileSize+=msg.getFileSize());

        long timecost = System.currentTimeMillis() - startTime.getTime();
        String prtMsg = String.format("总进度：%s%%(%s/%s), 总大小：%s, 平均速率：%s/s, 并行耗时/串行耗时：%sms/%sms",
                String.format("%.2f", completeFileCnts * 100f / totalNumFiles.get()),
                completeFileCnts,
                totalNumFiles.get()==Long.MAX_VALUE ? "-" : totalNumFiles,
                fileSizeFormat(completeFileSize),
                fileSizeFormat(completeFileSize / timecost * 1000, "MB"),
                timecost,
                completeFileTimeCost/1000
        );
        // if(printFile){
        //     System.out.print(prtMsg + " - " + msg.getMsg() + "\r");
        // }else {
        //
        // }
        System.out.print(prtMsg + "\r");


        // log.info("总进度：{}%({}/{}), 总大小：{}, 并行耗时/串行耗时：{}ms/{}ms, 复制速率：{}/s, {}",
        //         String.format("%.2f", completeFileCnts*100f/ totalNumFiles),
        //         completeFileCnts,
        //         totalNumFiles,
        //         fileSizeFormat(completeFileSize),
        //         completeFileSize,
        //         completeFileTimeCost,
        //         fileSizeFormat(completeFileSize / timecost *1000, "MB"),
        //         msg.toString());
    }

    public void listen() {
        try{
            do{
                CopyingMessage msg = queue.take();
                doStatistics(msg);
                // Thread.sleep(100);
            } while (!Thread.currentThread().isInterrupted());
            throw new InterruptedException("Complete and exit.");
        }catch (InterruptedException e){
            CopyingMessage msg = null;
            while ( (msg = queue.poll()) != null ){
                // log.info(msg.getMsg());
                doStatistics(msg, false);
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        System.out.println(String.format("%.2f", 90*100f/220));
        System.out.println(String.format("%.2f", 90*100f/220));
        System.out.println(String.format("%.2f", 90*100f/220));
        System.out.print(String.format("%.2f", 90*100f/220) +"\n");
        // System.out.print("\b");

        // System.out.print("\033[3A\033[2K");
        System.out.print("\r");
        System.out.print("-----");
        System.out.print("\r\b\r\b");
        System.out.println("***");
        char a = 8;
        System.out.write(a);
        System.out.write(a);

        System.out.print("##");

    }

    @Override
    public void run() {
        listen();
    }
}
