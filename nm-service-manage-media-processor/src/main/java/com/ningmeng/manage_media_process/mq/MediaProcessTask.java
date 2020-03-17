package com.ningmeng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.ningmeng.framework.domain.media.MediaFile;
import com.ningmeng.framework.domain.media.MediaFileProcess_m3u8;
import com.ningmeng.framework.exception.CustomExceptionCast;
import com.ningmeng.framework.model.response.CommonCode;
import com.ningmeng.framework.utils.HlsVideoUtil;
import com.ningmeng.framework.utils.Mp4VideoUtil;
import com.ningmeng.manage_media_process.dao.MediaFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaProcessTask.class);

    //ffmpeg绝对路径
    @Value("${nm-service-manage-media.ffmpeg-path}")
    String ffmpegPath;

    //上传文件根目录
    @Value("${nm-service-manage-media.video-location}")
    String videoPath;

    @Autowired
    MediaFileRepository mediaFileRepository;

    //监听的队列名称
    @RabbitListener(queues = "${nm-service-manage-media.mq.queue-media-video-processor}",containerFactory = "customContainerFactory")
    public void receiveMediaProcessTask(String msg) throws IOException {
        //{mediaId}转换成Json数据
        Map msgMap = JSON.parseObject(msg, Map.class);
        String mediaId = (String)msgMap.get("mediaId");
        //等到ID=fileMD5
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if(!optional.isPresent()){
            //没有查到对象
            CustomExceptionCast.cast(CommonCode.FAIL);
            LOGGER.error("视频处理对象不能为空");
            return;
        }
        MediaFile file = optional.get();
        //媒资文件类型
        String fileType = file.getFileType();
        //目前只处理avi文件
        if(fileType == null || !fileType.equals("avi")){
            file.setProcessStatus("303004");//处理状态为无需处理
            mediaFileRepository.save(file);
            return ;
        }else{
            file.setProcessStatus("303001");//处理状态为未处理
            mediaFileRepository.save(file);
        }
        //开始处理 状态为未处理并且只能是avi文件
        String mp4folderPath = "";
        String mp4Name="";
        if("303001".equals(file.getProcessStatus())){
            //先生成MP4
            //得到原文件（avi）路径
            String aviFilePath = videoPath+file.getFilePath()+file.getFileName();
            mp4folderPath = videoPath+file.getFilePath();
            mp4Name = file.getFileId()+".mp4";
            Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegPath,aviFilePath,mp4Name,mp4folderPath);
            String mp4Flag = mp4VideoUtil.generateMp4();
            if(mp4Flag == null || !mp4Flag.equals("success")){
                //操作失败写入处理日志
                file.setProcessStatus("303003");//处理状态为处理失败
                MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
                mediaFileProcess_m3u8.setErrormsg(mp4Flag);
                file.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
                mediaFileRepository.save(file);
                return ;
            }

        }
        //根据MP4生成m3u8
        //得到同文件名MP4文件
        String mp4FilePath = mp4folderPath+mp4Name;//此地址为mp4的地址
        String m3u8Name = file.getFileId()+".m3u8";
        String m3u8folderPath = videoPath+file.getFilePath()+"hls/";
        File files = new File(m3u8folderPath);
        if(!files.exists()){
            files.mkdirs();
        }
        //String ffmpeg_path, String video_path, String m3u8_name,String m3u8folder_path
        HlsVideoUtil hlsVideoUtil = new HlsVideoUtil(ffmpegPath,mp4FilePath,m3u8Name,m3u8folderPath);
        String hlsFlag = hlsVideoUtil.generateM3u8();
        if(hlsFlag == null || !hlsFlag.equals("success")){
            //操作失败写入处理日志
            file.setProcessStatus("303003");//处理状态为处理失败
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(hlsFlag);
            file.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(file);
            return ;
        }
        //分析mongDB中的数据信息
        file.setProcessStatus("");
        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        mediaFileProcess_m3u8.setTslist(ts_list);
        file.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        //m3u8文件url
        file.setFileUrl(file.getFilePath()+"hls/"+m3u8Name);
        mediaFileRepository.save(file);
    }
}
