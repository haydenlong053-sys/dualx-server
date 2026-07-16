package com.app.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AppPageResp<T> {

//    @Schema("最后1条数据的id，用于请求下1页数据，若为0，则是最后1页")
//    protected Integer last;

    @Schema(description = "分页数据，空数组表示最后一页")
    protected List<T> list;

//    public abstract void fix();
}
