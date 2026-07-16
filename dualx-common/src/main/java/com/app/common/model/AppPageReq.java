package com.app.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AppPageReq {

    @Schema(description = "上次最后一条数据的id，若没有传-1，默认-1")
    private Integer last = -1;

    @Schema(description = "分页大小,默认100，最大500")
    private Integer size = 100;

    public Integer getLast() {
        return last == null || last == -1 ? Integer.MAX_VALUE : last;
    }

    public Integer getSize() {
        return size == null || size > 500 ? 500 : size;
    }
}
