package com.ningmeng.manage_media;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class testChunk {

    @Test
    public void testChunk() throws Exception{
        //原文件地址
        File sourceFile = new File("E:\\cms\\ffmpeg\\video\\lucene.mp4");
        String chunkPath = "E:\\cms\\ffmpeg\\video\\chunk\\";
        File ChunkFile = new File(chunkPath);
        if(!ChunkFile.exists()){
            //不存在创建目录
            ChunkFile.mkdirs();
        }
        //设置分块文件的大小 以kb为单位
        long chunkSize = 1024*1024*1;
        //应该采用向上转型
        long chunkNum = (long)Math.ceil(sourceFile.length()*1.0/chunkSize);
        if(chunkNum<=0){
            chunkNum=1;
        }
        //读取sourceFile文件 循环写入块文件
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile,"r");
        //定义缓存区
        byte[] b = new byte[1024];
        for(int i=0;i<chunkNum;i++){
            //raf_read读出来 之后 写入文件
            ///制定一个文件名
            File file = new File(chunkPath+i);
            boolean newFile = file.createNewFile();
            if(newFile){
                RandomAccessFile raf_write = new RandomAccessFile(file,"rw");

                //read 读到-1 读完了
                int len=-1;
                while((len = raf_read.read(b))>-1){
                    raf_write.write(b,0,len);
                    if(file.length()>chunkSize){
                        break;
                    }
                }
            }
        }
        raf_read.close();
    }

    @Test
    public void testMerge() throws  Exception{
        //先得到分块数据List
        //然后进行排序 0-n
        //依次写入一个合并文件中
        //块文件目录
        //分块文件地址
        String chunkPage="E:\\cms\\ffmpeg\\video\\chunk\\";
        File ChunkFile = new File(chunkPage);
        //合并文件
        File mergeFile = new File("E:\\cms\\ffmpeg\\video\\lucene1.mp4");
        if(mergeFile.exists()){
            mergeFile.delete();
        }
        //如果不存在 开始业务
        mergeFile.createNewFile();

        //先得到分块列表   采用升序排序
        File[] fileArray = ChunkFile.listFiles();
        System.out.println(fileArray);
        Arrays.sort(fileArray, new Comparator<File>() {
            //自定义排序规则   采用升序
            @Override
            public int compare(File o1, File o2) {

                if(Integer.parseInt(o1.getName()) > Integer.parseInt(o2.getName())){
                    return 1;
                }
                return -1;
            }
        });
        System.out.println(fileArray);
        RandomAccessFile raf_write = new RandomAccessFile(mergeFile,"rw");
        byte[] b = new byte[1024];
        for(File chunkFile:fileArray){
            RandomAccessFile raf_read = new RandomAccessFile(chunkFile,"r");
            int len = -1;
            while ((len=raf_read.read(b))!=-1){
                raf_write.write(b,0,len);
            }
            raf_read.close();
        }
        raf_write.close();
    }
}
