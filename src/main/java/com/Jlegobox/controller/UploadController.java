package com.Jlegobox.controller;

import com.Jlegobox.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
