package com.hmall.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.service.ISearchService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements ISearchService {

    private final ItemClient itemClient;
    private RestHighLevelClient client;

    private final String INDEX_NAME = "items";
    {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.150.101:9200")
        ));
    }

    @Override
    public void saveItemById(Long itemId) {
        // 根据id查询商品
        ItemDTO itemDTO = itemClient.queryItemById(itemId);

        try {
            // 1.准备Request对象
            IndexRequest request = new IndexRequest(INDEX_NAME).id(itemId.toString());
            // 2.准备请求参数
            request.source(JSONUtil.toJsonStr(BeanUtil.copyProperties(itemDTO, ItemDoc.class)), XContentType.JSON);
            // 3.发送请求
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("更新商品失败，商品id：" + itemId, e);
        }
    }

    @Override
    public void deleteItemById(Long itemId) {
        try {
            // 1.准备Request对象
            DeleteRequest request = new DeleteRequest(INDEX_NAME, itemId.toString());
            // 3.发送请求
            client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("删除商品失败，商品id：" + itemId, e);
        }
    }
}
