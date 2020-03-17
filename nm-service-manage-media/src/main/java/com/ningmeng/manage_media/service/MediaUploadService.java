package com.ningmeng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.ningmeng.framework.domain.media.MediaFile;
import com.ningmeng.framework.domain.media.response.CheckChunkResult;
import com.ningmeng.framework.domain.media.response.MediaCode;
import com.ningmeng.framework.exception.CustomExceptionCast;
import com.ningmeng.framework.model.response.CommonCode;
import com.ningmeng.framework.model.response.ResponseResult;
import com.ningmeng.manage_media.config.RabbitMQConfig;
import com.ningmeng.manage_media.controller.MediaUploadController;
import com.ningmeng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {
    private final static Logger LOGGER = LoggerFactory.getLogger(MediaUploadController.class);
    @Autowired
    private MediaFileRepository mediaFileRepository;

    //nm-service-manage-media.upload-location
    @Value("${nm-service-manage-media.upload-location}")
    String uploadPath;
    @Value("${nm-service-manage-media.mq.routingkey-media-video}")
    private String routingkey_media_video;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //获取文件根目录
    /**
     * 根据文件md5得到文件路径
     * 规则：
     * 一级目录：md5的第一个字符
     * 二级目录：md5的第二个字符
     * 三级目录：md5
     * 文件名：md5+文件扩展名
     * @param fileMd5 文件md5值
     * @param fileExt 文件扩展名
     * @return 文件路径
     */
    public String getFilePath(String fileMd5,String fileExt){
        return uploadPath+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+"."+fileExt;
    }

    //得到文件目录相对路径，路径中去掉根目录
    private String getFileFolderRelativePath(String fileMd5){
        return fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/";
    }

    //得到文件所在目录
    private String getFileFolderPath(String fileMd5){
        return uploadPath+ fileMd5.substring(0, 1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" ;
    }

    //创建文件目录
    private boolean createFileFold(String fileMd5){
        //创建上传文件目录
        String fileFolderPath = getFileFolderPath(fileMd5);
        File fileFolder = new File(fileFolderPath);
        if (!fileFolder.exists()) {
            //创建文件夹
            boolean mkdirs = fileFolder.mkdirs();
            return mkdirs;
        }
        return true;
    }

    //得到块文件所在目录
    private String getChunkFileFolderPath(String fileMd5){
        String fileChunkFolderPath = getFileFolderPath(fileMd5) + "chunks" + "/";
        return fileChunkFolderPath;
    }

    /**
     * 创建分块文件目录
     * @param fileMd5
     * @return
     */
    private boolean createChunkFileFolder(String fileMd5){
        String fileChunkFolderPath = getChunkFileFolderPath(fileMd5);
        File file = new File(fileChunkFolderPath);
        if(!file.exists()){
            return file.mkdirs();
        }else{
            return true;
        }
    }

    /**
     * 上传准备工作
     * @param fileMd5
     * @param fileName
     * @param fileSize
     * @param mimetype
     * @param fileExt
     * @return
     */
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {

        //检查文件是否上传过
        //1、得到文件的路径
        String fileFolderPath = this.getFileFolderPath(fileMd5);
        File fileFolder = new File(fileFolderPath);
        //2、查询数据库中文件是否存在
        Optional<MediaFile> optionl = mediaFileRepository.findById(fileMd5);
        if(fileFolder.exists() && optionl.isPresent()){
            CustomExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        //根据MD5创建文件目录
        boolean fileFoldFloag = createFileFold(fileMd5);
        if(!fileFoldFloag){
            CustomExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_FAIL);
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 检查分块是否存在
     * @param fileMd5
     * @param chunk     下标 0 1 2 3 4 5
     * @param chunkSize
     * @return
     */
    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {

        String fileChunkFolderPath = getChunkFileFolderPath(fileMd5);
        File chunkFile = new File(fileChunkFolderPath+chunk);

        if(chunkFile.exists()){
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK,true);
        }else{
            return new CheckChunkResult(null,false);
        }
    }

    /**
     * 分块上传方法
     * @param file
     * @param chunk
     * @param fileMd5
     * @return
     */
    public ResponseResult uploadchunk(MultipartFile file, Integer chunk, String fileMd5) {
        if(file == null){
            CustomExceptionCast.cast(MediaCode.UPLOAD_CHUNKFILE_IS_NOLL);
        }
        //创建块文件目录
        boolean fileFolder = createChunkFileFolder(fileMd5);
        if(!fileFolder){
            CustomExceptionCast.cast(CommonCode.FAIL);
        }
        //块文件
        File chunkfile = new File(getChunkFileFolderPath(fileMd5)+chunk);
        //上传的块文件
        InputStream inputStream= null;
        FileOutputStream outputStream = null;
        try {
            inputStream = file.getInputStream();
            //向那个文件中写内容
            outputStream = new FileOutputStream(chunkfile);
            IOUtils.copy(inputStream,outputStream);
        }catch (Exception e){
            return new ResponseResult(CommonCode.FAIL);
        }finally {
            try {
                if(inputStream!=null){
                    inputStream.close();
                }
                if(outputStream!=null){
                    outputStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 合并分块
     * @param fileMd5
     * @param fileName
     * @param fileSize
     * @param mimetype
     * @param fileExt
     * @return
     */
    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //获取块文件的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        File chunkfileFolder = new File(chunkFileFolderPath);
        if(!chunkfileFolder.exists()){
            chunkfileFolder.mkdirs();
        }
        //合并文件路径
        String mergeFilePath = getFilePath(fileMd5, fileExt);
        File mergeFile = new File(mergeFilePath);
        if(mergeFile.exists()){
            //如果存在先删除再创建
            mergeFile.mkdirs();
        }
        boolean newFile=false;
        try {
            newFile=mergeFile.createNewFile();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(!newFile){
            CustomExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //获取块文件，此列表是已经排好序的列表
        File[] chunkFiles = this.getChunkFiles(chunkfileFolder);
        //合并文件
        File file = this.mergeFile(mergeFile, chunkFiles);
        //校验文件(md5值)    为了保证合并的成功性
        boolean bool = this.checkFileMd5(mergeFile,fileMd5);
        if(!bool){
            CustomExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //将文件信息保存到数据库
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5+"."+fileExt); mediaFile.setFileOriginalName(fileName);
        //文件路径保存相对路径
        mediaFile.setFilePath(getFileFolderRelativePath(fileMd5));
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        //状态为上传成功
        mediaFile.setFileStatus("301002");
        mediaFileRepository.save(mediaFile);
        String mediaId = mediaFile.getFileId();
        //向MQ发送视频处理消息
        sendProcessVideoMsg(mediaId);
        return new ResponseResult(CommonCode.SUCCESS);
    }
    //向MQ发送视频处理消息
    private ResponseResult sendProcessVideoMsg(String mediaId) {
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if(!optional.isPresent()){
            return new ResponseResult(CommonCode.FAIL);
        }
        MediaFile mediaFile = optional.get();
        //发送视频处理消息
        Map<String,String> msgMap = new HashMap<>();
        msgMap.put("mediaId",mediaId);
        //发送的消息
        String msg = JSON.toJSONString(msgMap);
        try {
            this.rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK,routingkey_media_video,msg);

            LOGGER.info("send media process task msg:{}",msg);
        }catch (Exception e){
            e.printStackTrace();
            LOGGER.info("send media process task error,msg is:{},error:{}",msg,e.getMessage());
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //获取所有块文件
    private File[] getChunkFiles(File chunkfileFolder){
        //获取列表
        File[] chunkFiles = chunkfileFolder.listFiles();
        //排序
        Arrays.sort(chunkFiles, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if(Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                    return 1;
                }
                return -1;
            }
        });
        return chunkFiles;
    }

    /**
     * 合并文件
     * @param mergeFile 合并文件空文件
     * @param chunkFiles  分块文件列表
     * @return
     */
    private File mergeFile(File mergeFile,File[] chunkFiles){
        try {
            RandomAccessFile raf_write = new RandomAccessFile(mergeFile,"rw");
            byte[] b = new byte[1024];
            for(File chunkFile:chunkFiles){
                RandomAccessFile raf_read = new RandomAccessFile(chunkFile,"r");
                int len = -1;
                while ((len=raf_read.read(b))!=-1){
                    raf_write.write(b,0,len);
                }
                raf_read.close();
            }
            raf_write.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return mergeFile;
    }

    /**
     * 检查文件md5值
     * @param mergeFile
     * @param md5
     * @return
     */
    private boolean checkFileMd5(File mergeFile,String md5){
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(mergeFile);
            //先计算
            String mergeFileMd5 = DigestUtils.md5Hex(inputStream);
            if(mergeFileMd5.equalsIgnoreCase(md5)){
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(inputStream!=null){
                    inputStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }


}
