package com.Jlegobox.service;

import com.Jlegobox.pojo.FileInfo;
import com.Jlegobox.util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author J
 * @Email jlc_game123@163.com
 * @Date 2021/2/9
 * @Time 12:06
 */
@Service
public class UploadService {
    // 验证完整文件的MD5
    public boolean checkUploadFile(String uploadFileMD5) {
        // 默认不存在
        return false;
    }

    // 验证上传分片的MD5
    public boolean checkUploadFileSlice(String uploadFileMD5) {
        // 默认不存在
        return false;
    }


    public String doUploadFile(MultipartHttpServletRequest request) {
        //获取上传的文件信息
        FileInfo fileInfo = FileUtil.collectFileInfo(request);
        MultipartFile file = request.getMultiFileMap().getFirst("file");
        //储存文件信息
        FileUtil.saveSlice(file,fileInfo);
        return "success";
    }

    public String doMergeFile(HttpServletRequest request) {
        FileInfo fileInfo = FileUtil.collectFileInfo(request);
        String s = FileUtil.mergeFileSlice(fileInfo);

        return s;
    }
}
