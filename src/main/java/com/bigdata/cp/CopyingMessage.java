package com.bigdata.cp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

/**
 * @author: tongyu
 * @date: 2025/3/16 10:33
 * @email: tongyu@powerbeijing.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopyingMessage {
    static enum Status{
        SUCCESS, FAIL
    }
    static enum IoType{
        BIO, NIO, NATIVE
    }

    private String threadName; // 线程名称
    private String msg;  //
    private Status status;  // 状态
    private IoType ioType; // nio、io、native
    private long fileSize;  // 文件大小
    private Path baseCopyDir; // 源目录
    private Path baseTagetDir; // 目录目录
    private Path sourceFilePath; // 源文件地址
    private Path targetFilePath; // 目标文件地址
    private long timeLength; // 复制耗时


}
