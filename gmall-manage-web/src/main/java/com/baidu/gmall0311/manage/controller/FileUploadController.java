package com.baidu.gmall0311.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author Alei
 * @create 2019-08-04 23:13
 */
@RestController
@CrossOrigin
public class FileUploadController {

    //软编码将服务器的 ip 地址的配置到 application.properties 中
    //@Value 注解使用的前提条件是 当前类必须注入到 spring 容器中 否则不能使用
    @Value("${fileServer.url}")
    private String fileUrl;

    //文件上传
    //http://localhost:8082/fileUpload 文件上传路径
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {

        //声明一个图片全路径
        String imgUrl = fileUrl;

        if (file != null) {
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            StorageClient storageClient=new StorageClient(trackerServer,null);
//            String orginalFilename="E:\\常用资料\\壁纸/ccfc05.jpg";   此处上传使用的是全路径

            //获取文件名称   只是获取到文件的名称  没有获取到全路径
            String originalFilename = file.getOriginalFilename();

            //获取文件后缀名
            String extName = StringUtils.substringAfterLast(originalFilename, ".");

            //originalFilename 只是一个文件名称 并不是文件全路径 不是文件全路径应该是文件的字节数组
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                System.out.println("path = " + path);
//            s = group1
//            s = M00/00/00/wKjHhl1G8_GAB6MuAAgw6QZ0-ns791.jpg
                imgUrl += "/" + path;
            }
        }

        //最后的图片全路径地址
        //http://192.168.199.134/group1/M00/00/00/wKjHhl1G8_GAB6MuAAgw6QZ0-ns791.jpg
        return imgUrl;
    }
}
