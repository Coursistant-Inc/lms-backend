package com.coursistant.lms.service.system;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.VersionInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;

import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.RemoteIterator;


import java.util.ArrayList;
import java.util.List;

@Service
public class HadoopService {

    @Value("${hadoop.fs.defaultFS:hdfs://dev.xlearnedu.com:9666}")
    private String defaultFs;

    @Value("${hadoop.fs.user:admin}")
    private String hadoopUser;

    /**
     * 打开 HDFS 上的 PDF 文件并渲染第一页为 Base64 编码的图片
     * Open a PDF file on HDFS and render the first page as a Base64-encoded image
     */
    public String openFile(String path, String fileName) throws Exception {
        // 拼接完整路径 // Construct the full path
        String filePath = "/coursistant/database/" + path + "/" + fileName;
        System.out.println("Hadoop Version: " + VersionInfo.getVersion());

        // 配置 HDFS 连接 // Configure HDFS connection
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", defaultFs);

        // 连接到 HDFS // Connect to HDFS
        FileSystem fileSystem = FileSystem.get(new URI(defaultFs), conf, hadoopUser);
        Path hdfsPath = new Path(filePath);

        if (!fileSystem.exists(hdfsPath)) {
            throw new Exception("File does not exist: " + filePath);
        }

        // 打开文件流并处理 PDF // Open file stream and process PDF
        try (InputStream inputStream = fileSystem.open(hdfsPath);
             PDDocument document = PDDocument.load(inputStream)) {

            // 检查 PDF 是否有页面 // Check if the PDF has pages
            if (document.getNumberOfPages() < 1) {
                throw new Exception("The PDF file is empty.");
            }

            // 使用 PDFRenderer 渲染第一页 // Use PDFRenderer to render the first page
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 300); // 页码从0开始，DPI为300 // Page index starts from 0, DPI is set to 300

            // 将图片转换为 Base64 编码 // Convert image to Base64 encoding
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } finally {
            // 关闭文件系统 // Close file system
            fileSystem.close();
        }
    }

    /**
     * 列出 HDFS 目录中的所有文件
     * List all files in an HDFS directory
     */
    public List<String> listFiles(String path) throws Exception {
        List<String> fileList = new ArrayList<>();

        // 配置 HDFS 连接 // Configure HDFS connection
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", defaultFs);

        // 连接到 HDFS // Connect to HDFS
        FileSystem fileSystem = FileSystem.get(new URI(defaultFs), conf, hadoopUser);
        Path hdfsPath = new Path(path);

        if (!fileSystem.exists(hdfsPath)) {
            throw new Exception("Path does not exist: " + path);
        }

        // 遍历路径下的所有文件 // Iterate through all files in the directory
        RemoteIterator<LocatedFileStatus> fileStatusIterator = fileSystem.listFiles(hdfsPath, true);
        while (fileStatusIterator.hasNext()) {
            LocatedFileStatus fileStatus = fileStatusIterator.next();
            fileList.add(fileStatus.getPath().toString());
        }

        // 关闭文件系统连接 // Close HDFS connection
        fileSystem.close();

        return fileList;
    }
}
