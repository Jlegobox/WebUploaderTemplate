package com.Jlegobox.service;

import com.Jlegobox.pojo.FileInfo;
import com.Jlegobox.util.FileUtil;
import com.Jlegobox.util.MD5Util;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

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
        File fileByMD5 = FileUtil.getFileByMD5(uploadFileMD5);
        if(fileByMD5 != null){
            try {
                String md5 = MD5Util.calMD5(fileByMD5);
                if(uploadFileMD5.equals(md5))
                    return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // 验证上传分片的MD5
    public boolean checkUploadFileSlice(String uploadFileMD5,String uploadFileSliceMD5,int chunk) {
        File fileSliceByMD5 = FileUtil.getFileSliceByMD5(uploadFileMD5);
        String fileName = chunk + ".slice";
        if(fileSliceByMD5!=null){
            File[] files = fileSliceByMD5.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.equals(fileName);
                }
            });
            if(files!=null && files.length>0){
                for (File file : files) {
                    try {
                        String md5 = MD5Util.calMD5(file);
                        return uploadFileSliceMD5.equals(md5);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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
