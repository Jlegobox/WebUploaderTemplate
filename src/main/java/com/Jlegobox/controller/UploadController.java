package com.Jlegobox.controller;

import com.Jlegobox.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;


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

    @RequestMapping("doUploadFile.ajax")
    public String doUploadFile(final MultipartHttpServletRequest request){
        return "success";
    }

    @RequestMapping("checkUploadFile.ajax")
    public String checkUploadFile(@RequestParam("uploadFileMD5") String uploadFileMD5){
        boolean existFlag =  uploadService.checkUploadFile(uploadFileMD5);
        if(existFlag){
            return "exist";
        }
        return "success";
    }
}
