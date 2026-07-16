package com.app.common.model;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;

@Data
public class BasePageReq {

    @Schema(description = "分页页码，从1开始")
    private Integer page = 1;

    @Schema(description = "分页大小，默认20，最大500")
    private Integer size = 20;

    @Schema(description = "1活动 2公告")
    private Integer type;

    //获取page对象，用于mybatis-plus，返回分页大小
    @JsonIgnore
    public IPage<?> getIPage() {
        if (page == null) {
            page = 1;
        }
        if (size == null) {
            size = 20;
        }
        return new Page<>(page, size).setSearchCount(true);
    }

    @JsonIgnore
    public IPage<?> getIPageForExport() {
        return new Page<>(1, Integer.MAX_VALUE).setSearchCount(false);
    }

    //获取offset，用于手动处理sql
    @JsonIgnore
    public int getOffset() {
        if (page == null) {
            page = 1;
        }
        if (size == null) {
            size = 20;
        }
        return page > 1 ? page * size - size : 0;
    }

    public int getSize() {
        if (size == null || size <= 0 || size > 500) {
            return 20;
        }
        return size;
    }
}
