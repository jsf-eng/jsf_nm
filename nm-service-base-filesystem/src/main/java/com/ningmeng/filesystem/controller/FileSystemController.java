package com.ningmeng.filesystem.controller;

import com.ningmeng.api.filesystemapi.FileSyetemControllerApi;
import com.ningmeng.filesystem.service.FileSystemService;
import com.ningmeng.framework.domain.filesystem.response.UploadFileResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/filesystem")
public class FileSystemController implements FileSyetemControllerApi {

    @Autowired
    FileSystemService fileSystemService;

    @Override
    @PostMapping("/upload")
    public UploadFileResult upload(@RequestParam(value = "file",required = true) MultipartFile multipartFile, @RequestParam(value = "filetag", required = true)String filetag,@RequestParam(value = "businesskey", required = false) String businesskey,@RequestParam(value = "metedata", required = false) String metadata) {
        return fileSystemService.upload(multipartFile,filetag,businesskey,metadata);
    }
}
