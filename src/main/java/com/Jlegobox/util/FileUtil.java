package com.Jlegobox.util;

import com.Jlegobox.pojo.FileInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * 文件工具类，负责文件的处理
 *
 * @Author J
 * @Email jlc_game123@163.com
 * @Date 2021/2/9
 * @Time 12:02
 */
@Component
public class FileUtil {
    private static String FILE_REP = "C:\\Users\\J\\GitHub\\WebUploaderTemplate\\src\\main\\resources\\AppData";

    private static void setFileRep(String fileRep) {
        FILE_REP = fileRep;
    }

    /**
     * 判断文件夹路径path是否存在，不存在则创建空文件夹。创建失败返回false
     *
     * @param path
     * @return
     */
    private synchronized static boolean checkDirect(String path) {
        File file = new File(path);
        if (!file.exists() && !file.isDirectory()) {
            System.out.println("不存在文件路径:" + path);
            return file.mkdir();
        } else {
            System.out.println("目录存在");
            return true;
        }
    }

    public static FileInfo collectFileInfo(final ServletRequest request) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(request.getParameter("id"));
        fileInfo.setOriginFileName(request.getParameter("name"));
        fileInfo.setFileSize(request.getParameter("size"));
        fileInfo.setLastModifiedDate(request.getParameter("lastModifiedDate"));
        String chunks = request.getParameter("chunks");
        String chunk = request.getParameter("chunk");
        fileInfo.setChunks(Integer.parseInt(chunks == null ? "1" : chunks)); // 至少一个分片
        fileInfo.setCurrentChunk(Integer.parseInt(chunk == null ? "0" : chunk));
        fileInfo.setMD5(request.getParameter("uploadFileMD5"));
        fileInfo.setCurrentChunkMD5(request.getParameter("uploadFileSliceMD5"));

        return fileInfo;
    }

    public static String saveSlice(MultipartFile uploadFile, FileInfo fileInfo) {
        String md5 = fileInfo.getMD5();
        String DirPath = FILE_REP + File.separator + md5;
        if (!checkDirect(DirPath)) { // 创建文件夹失败，服务器错误
            return "sever error";
        }
        // 保存并验证fileInfo(目前无验证逻辑)
        try {
            boolean localFileInfoStatus = checkFileInfo(fileInfo);
            if (!localFileInfoStatus)
                return "sever error";
        } catch (Exception e) {
            e.printStackTrace();
            return "sever error";
        }
        // 要储存的位置
        File file = new File(DirPath + File.separator + fileInfo.getCurrentChunk() + ".slice");
        try {
            file.createNewFile();
            uploadFile.transferTo(file);
            String savedMD5 = MD5Util.calMD5(file);
            if (savedMD5 == null || !savedMD5.equals(fileInfo.getCurrentChunkMD5())) {
                // 保存的分片有误，报错
                file.delete();
                return "fail to save";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "fail to save";
        }
        return "success";
    }

    public static String mergeFileSlice(FileInfo fileInfo) {
        File file = new File(FILE_REP + File.separator + fileInfo.getMD5() + ".block");
        try {
            file.createNewFile();
            String slicePath = FILE_REP + File.separator + fileInfo.getMD5();
            File sliceDir = new File(slicePath);
            if (sliceDir.exists() && sliceDir.isDirectory()) {
                fileInfo = getFileInfo(new File(slicePath + File.separator + fileInfo.getMD5() + ".INFO"));
                merge(file, slicePath, fileInfo);
                deleteFolder(sliceDir);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "success";
    }

    private synchronized static boolean  deleteFolder(File sliceDir) {
        if(sliceDir.exists()){
            if(sliceDir.isDirectory()){
                File[] files = sliceDir.listFiles();
                if(files != null && files.length>0){
                    for (File file : files) {
                        if(!deleteFolder(file)){ // 没删完，返回错误
                            return false;
                        }
                    }
                }
                // 空文件夹，删除
                if(!sliceDir.delete())
                    return false;
            }
            //是文件 删除
            return sliceDir.delete();
        }
        //本就不存在，也算删除
        return true;
    }

    /**
     * 对于大文件来说，这是一个非常耗时的工作，可以考虑异步进行
     * 注意流的关闭
     *
     * @param file
     * @param slicePath
     * @param fileInfo
     * @return
     * @throws IOException
     */
    private static String merge(File file, String slicePath, FileInfo fileInfo) throws IOException {
        FileChannel fileOutputStream = null;
        FileChannel fileInputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file).getChannel(); // 如果file存在，会导致file被清空（覆盖重写）。想要追加，可以使用追加的方式
            slicePath = slicePath + File.separator;
            int chunks = fileInfo.getChunks();
            for (int i = 0; i < chunks; i++) {
                File fileSlice = new File(slicePath + i + ".slice");
                //MD5验证
                String md5 = MD5Util.calMD5(fileSlice);
                if (md5 == null || !fileInfo.getChunkMD5().get(i).equals(md5)) {
                    return "sever error";
                }
                fileInputStream = new FileInputStream(fileSlice).getChannel();
                fileOutputStream.transferFrom(fileInputStream, fileOutputStream.size(), fileInputStream.size());
                // 先关闭流再删除,不然新new出来的fileOutputStream的流无法关闭，垃圾回收也回收不掉
                fileInputStream.close();
//                fileSlice.delete();

            }
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }

        //考虑对整个文件再进行一次md5验证
        return "success";
    }

    // 需要排序的方案
    private static String merge_deprecate(File file, String slicePath, FileInfo fileInfo) throws IOException {
        FileChannel fileOutputStream = new FileOutputStream(file).getChannel();
        File sliceDir = new File(slicePath);
        // 不需要排序，因为都是按照数字命名的文件

        File[] files = sliceDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".slice");
            }
        });
        if (files != null && fileInfo.getChunks() != files.length) {
            Arrays.asList(files).sort(new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    String[] split1 = o1.getName().split("//.");
                    String[] split2 = o2.getName().split("//.");
                    int chunk1 = Integer.getInteger(split1[split1.length - 2]);
                    int chunk2 = Integer.getInteger(split2[split2.length - 2]);
                    return chunk1 < chunk2 ? 1 : -1;
                }
            });
            for (File fileSlice : files) {
                FileChannel fileSliceInput = new FileInputStream(fileSlice).getChannel();
                fileOutputStream.transferFrom(fileSliceInput, fileOutputStream.size(), fileSliceInput.size());
                fileSlice.delete();
            }
            if (fileInfo.getMD5().equals(MD5Util.calMD5(file))) {
                file.delete();
                return "sever error";
            }
            return "success";
        }

        return "server error";
    }

    /**
     * （目前验证逻辑）
     * 保存上传的文件信息，分片上传时用于储存分片文件的md5值。
     * 合并时用于查找信息
     *
     * @param fileInfo
     * @return
     */
    private synchronized static boolean checkFileInfo(FileInfo fileInfo) throws Exception {
        File file = new File(FILE_REP + File.separator + fileInfo.getMD5() + File.separator + fileInfo.getMD5() + ".INFO");
        if (!file.exists()) { // 不存在，则储存
            createFileInfo(file, fileInfo);
        }
        FileInfo localFileInfo = getFileInfo(file);
        if (localFileInfo == null)
            return false;
        // 验证逻辑

        // 储存分片的MD5值
        Map<Integer, String> chunkMD5 = localFileInfo.getChunkMD5();
        chunkMD5.put(fileInfo.getCurrentChunk(), fileInfo.getCurrentChunkMD5());
        createFileInfo(file, localFileInfo);
        return true;
    }

    private synchronized static boolean createFileInfo(File file, FileInfo fileInfo) throws IOException {
        if (!file.createNewFile()) {
            file.delete();
        }
        OutputStream fileOutput = null;
        ObjectOutput objectOutput = null;
        try {
            fileOutput = new FileOutputStream(file);
            objectOutput = new ObjectOutputStream(fileOutput);
            objectOutput.writeObject(fileInfo);
        } finally {
            if (fileOutput != null) {
                fileOutput.close();
            }
            if (objectOutput != null) {
                objectOutput.close();
            }
        }

        return true;
    }

    private synchronized static FileInfo getFileInfo(File file) throws Exception {
        FileInputStream fileInput = null;
        ObjectInputStream objectInput = null;
        FileInfo fileInfo = null;
        try {
            fileInput = new FileInputStream(file);
            objectInput = new ObjectInputStream(fileInput);
            fileInfo = (FileInfo) objectInput.readObject();
        } finally {
            if (objectInput != null) {
                objectInput.close();
            }
            if (fileInput != null) {
                fileInput.close();
            }
        }
        return fileInfo;
    }


    /**
     * 返回文件系统中，传入MD5代表的文件
     * @param uploadFileMD5
     * @return
     */
    public static File getFileByMD5(String uploadFileMD5) {
        File file = new File(FILE_REP + File.separator + uploadFileMD5 + ".block");
        if(file.exists())
            return file;
        return null;
    }

    /**
     * 返回的是分片所在文件夹
     * @param uploadFileMD5
     * @return
     */
    public static File getFileSliceByMD5(String uploadFileMD5) {
        File file = new File(FILE_REP + File.separator + uploadFileMD5);
        if(file.exists() && file.isDirectory())
            return file;
        return null;
    }
}
