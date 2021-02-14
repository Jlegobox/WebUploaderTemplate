package com.Jlegobox.util;

import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * MD5计算。
 * @Author J
 * @Email jlc_game123@163.com
 * @Date 2021/2/14
 * @Time 22:16
 */
public class MD5Util {
    public static String calMD5(File file){
        if(file.exists()){
            try{
                FileInputStream inputStream = new FileInputStream(file);
                String md5 = DigestUtils.md5DigestAsHex(inputStream);
                return md5;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
