package com.app.web.api.req;

import lombok.Data;

@Data
public class FileReq {
    //源文件名
    private String originFileName;

    //新文件名
    private String fileName;

    //文件相对路径
    private String filePath;

    //文件完整url
    private String fullUrl;
}
