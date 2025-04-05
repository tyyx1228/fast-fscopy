package com.bigdata.cp;

/**
 * @author: tongyu
 * @date: 2025/4/4 14:24
 * @email: tongyu@powerbeijing.com
 */

import java.io.File;

public class Pair{
    private File srcDir;
    private File tagDir;

    public Pair(File srcDir, File tagDir){
        this.srcDir = srcDir;
        this.tagDir = tagDir;
    }

    public void resetAttributes(){
        tagDir.setLastModified(srcDir.lastModified());
        tagDir.setExecutable(srcDir.canExecute());
        tagDir.setReadable(srcDir.canRead());
        tagDir.setWritable(srcDir.canWrite());
    }
}
