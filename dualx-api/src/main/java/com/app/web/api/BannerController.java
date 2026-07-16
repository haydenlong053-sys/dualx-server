package com.app.web.api;

import com.app.common.annotation.Login;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import com.app.web.service.IBannerService;
import com.app.db.entity.Banner;
 
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Isolation;
 
/**
 * <p>
 * Banner 前端控制器
 * </p>
 *
 * @author Auto
 * @since 2026-05-26
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/banner")
@Tag(name = "Banner")
@Transactional(isolation = Isolation.READ_COMMITTED)
public class BannerController {

    @Resource
    private IBannerService bannerService;

    /**
     * 查询所有数据
     * @return
     */
    @Login
    @GetMapping
    @Operation(summary = "查询Banner列表")
    public List<Banner> findAll() {
        return bannerService.list();
    }
 
    /**
     * 新增或者更新
     * @return
     */
    @Login
    @PostMapping
    @Operation(summary = "添加或者更新Banner")
    public Map<String,Boolean> saveOrUpdate(@RequestBody Banner banner) {
        Map<String,Boolean> map = new HashMap<>();
        map.put("result",true);
        bannerService.saveOrUpdate(banner);
        return map;
    }
 

    /**
     * 根据id查询一条数据
     * @return
     */
    @Login
    @GetMapping(value = "/{id}")
    @Operation(summary = "根据ID查询Banner")
    public Banner findOne(@PathVariable Integer id) {
        return bannerService.getById(id);
    }
 
    /**
     * 分页查询
     * @return
     */
    @Login
    @GetMapping(value = "/page")
    @Operation(summary = "分页查询Banner")
    public Page findPage(@RequestParam Integer pageNum,
                                    @RequestParam Integer pageSize) {
        QueryWrapper<Banner> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        Page page = bannerService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return page;
    }
 
}
 
