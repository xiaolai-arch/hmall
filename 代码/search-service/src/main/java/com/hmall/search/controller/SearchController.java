package com.hmall.search.controller;


import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.PageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageVO<ItemDoc> search(ItemPageQuery query) {
        // ... 搜索
        return PageVO.empty(0L, 0L);
    }
}
