package com.app.web.api;

import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.exception.DcException;
import com.app.common.util.AssertUtil;
import com.app.common.util.ImgUtil;
import com.app.web.api.req.FileReq;
import com.app.common.annotation.Login;
import com.app.common.model.BaseResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/api/file")
@CrossOrigin(origins = "*")
@Tag(name = "文件管理")
@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
public class FileController {

    @Value(value = "${com.app.upload.filePath}")
    private String fileUrl;

    @Value(value = "${com.app.upload.getHttpImg}")
    private String getHttpImg;

    @Login
    @PostMapping(value = "uploads")
    @Operation(summary = "多文件上传")
    public List<FileReq> uploadFiles(@Parameter(description = "files") @Valid @NotNull(message = "文件不能为空") List<MultipartFile> files) throws IOException {
        if(files == null){
            throw new DcException(BaseResultCodeEnum.FILE_PARAMETERS_ERROR);
        }
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        // 格式化日期
        String formattedDate = currentDate.format(formatter);
        String fileUrlPas = fileUrl + "/" + formattedDate;
        String aogoUrlPas = getHttpImg + "/" + formattedDate;
        List<FileReq> respList = new ArrayList<>(files.size());
        FileReq fileReq = new FileReq();
        for (MultipartFile file : files) {
            if(!ImgUtil.isValidImage(file)){
                throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
            }
            fileReq = new FileReq();
            // 首先判断上传的文件是否为空
            AssertUtil.isTrue(file.isEmpty(),BaseResultCodeEnum.FILE_CANNOT_BE_EMPTY);
            try {
                String suffix = ".png"; // 初始文件后缀
                String name = file.getOriginalFilename(); // 获取上传的文件名
                fileReq.setOriginFileName(name);
                // 获取文件的后缀
                if (name != null && name.indexOf(".") > 0) {
                    suffix = name.substring(name.lastIndexOf(".")); // 使用 lastIndexOf 防止文件名中间有多个点的情况
                }
                String destFileName = UUID.randomUUID() + suffix; // 生成保存的文件名
                fileReq.setFileName(destFileName);
                fileReq.setFilePath(fileUrlPas);
                // 检查目录是否存在，若不存在则创建
                File destDir = new File(fileUrlPas);
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                // 保存文件到指定位置
                File destFile = new File(fileUrlPas + "/" + destFileName);
                file.transferTo(destFile);
                fileReq.setFullUrl(aogoUrlPas + "/" + destFileName); // 设置完整URL
                respList.add(fileReq);
            } catch (IOException e) {
                log.error("多文件上传失败, fileName={}", file.getOriginalFilename(), e);
                throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
            }
        }
        return respList;
    }

    @Login
    @PostMapping(value = "upload")
    @Operation(summary = "单上传文件")
    public BaseResult<FileReq> upload(@Parameter(description = "file") @Valid @NotNull(message = "文件不能为空") MultipartFile file) throws IOException {
        FileReq fileReq = new FileReq();
        // 首先判断上传的文件是否为空
        AssertUtil.isTrue(!file.isEmpty(), BaseResultCodeEnum.FILE_CANNOT_BE_EMPTY);
        String formattedDate = "userimg";
        String fileUrlPas = fileUrl + "/" + formattedDate;
        String aogoUrlPas = getHttpImg + formattedDate;
        if(!ImgUtil.isValidImage(file)){
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        try {
            String suffix = ".png"; // 初始文件后缀
            String name = file.getOriginalFilename(); // 获取上传的文件名
            fileReq.setOriginFileName(name);
            // 获取文件的后缀
            if (name != null && name.indexOf(".") > 0) {
                suffix = name.substring(name.lastIndexOf(".")); // 使用 lastIndexOf 防止文件名中间有多个点的情况
            }
            String destFileName = UUID.randomUUID() + suffix; // 生成保存的文件名
            fileReq.setFileName(destFileName);
            fileReq.setFilePath(fileUrlPas);
            // 检查目录是否存在，若不存在则创建
            File destDir = new File(fileUrlPas);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            // 保存文件到指定位置
            File destFile = new File(fileUrlPas + "/" + destFileName);
            file.transferTo(destFile);
            fileReq.setFullUrl(aogoUrlPas + "/" + destFileName); // 设置完整URL
            //http://127.0.0.1:8098//userimg/1d9a04d7-8e42-4c04-86b4-85d2cc9a25b0.png
            return new BaseResult<>(fileReq);
        } catch (IOException e) {
            log.error("单文件上传失败, fileName={}", file.getOriginalFilename(), e);
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
    }
}
