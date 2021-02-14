package com.Jlegobox.controller;

import com.Jlegobox.pojo.FileInfo;
import com.Jlegobox.service.UploadService;
import com.Jlegobox.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;


/**
 * @Author J
 * @Email jlc_game123@163.com
 * @Date 2021/2/9
 * @Time 12:04
 */
@RestController
public class UploadController {
    @Autowired
    private UploadService uploadService;

    // 上传文件
    @RequestMapping("doUploadFile.ajax")
    public String doUploadFile(final MultipartHttpServletRequest request){
        // 根据传入的信息，维护两个文件。一个是文件主体，一个是文件信息
        // 每个上传的文件切面，会直接合并到文件主体中。使用MD5值来验证文件合并后的正确性
        // 文件信息保存文件总体的MD5，当前MD5以及文件的其他各种信息，用于校验和完成暂停等功能。
        uploadService.doUploadFile(request);

        return "success";
    }

    // 整个文件的MD5验证
    @RequestMapping("checkUploadFile.ajax")
    public String checkUploadFile(@RequestParam("uploadFileMD5") String uploadFileMD5){
        boolean existFlag =  uploadService.checkUploadFile(uploadFileMD5);
        if(existFlag){
            return "exists";
        }
        return "permit";
    }

    // 文件切片的的MD5验证
    @RequestMapping("checkUploadFileSlice.ajax")
    public String checkUploadFileSlice(@RequestParam("uploadFileMD5") String uploadFileMD5){
        boolean existFlag =  uploadService.checkUploadFileSlice(uploadFileMD5);
        if(existFlag){
            return "exists";
        }
        return "permit";
    }

    @RequestMapping("doMergeFile.ajax")
    public String doMergeFile(HttpServletRequest request){
       return uploadService.doMergeFile(request);
    }
}
