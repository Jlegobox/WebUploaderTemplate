package com.Jlegobox.service;

import org.springframework.stereotype.Service;

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


}
