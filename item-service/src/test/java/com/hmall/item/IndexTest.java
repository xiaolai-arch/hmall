package com.hmall.item;

import cn.hutool.core.bean.BeanUtil;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.utils.CollUtils;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("local")//指定环境
@Slf4j
public class IndexTest {
    private RestHighLevelClient client;
    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://localhost:9200")
        ));
    }
    @Test
    void testConnect() {
        System.out.println(client);
    }
    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    @Test
    @SneakyThrows
    void testCreateIndex() {
        CreateIndexRequest request = new CreateIndexRequest("item");
        String json = "{\n" +
                "  \"mappings\": {\n" +
                "    \"properties\": {\n" +
                "      \"id\": {\n" +
                "        \"type\": \"keyword\"\n" +
                "      },\n" +
                "      \"name\":{\n" +
                "        \"type\": \"text\",\n" +
                "        \"analyzer\": \"ik_max_word\"\n" +
                "      },\n" +
                "      \"price\":{\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"stock\":{\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"image\":{\n" +
                "        \"type\": \"keyword\",\n" +
                "        \"index\": false\n" +
                "      },\n" +
                "      \"category\":{\n" +
                "        \"type\": \"keyword\"\n" +
                "      },\n" +
                "      \"brand\":{\n" +
                "        \"type\": \"keyword\"\n" +
                "      },\n" +
                "      \"sold\":{\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"commentCount\":{\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"isAD\":{\n" +
                "        \"type\": \"boolean\"\n" +
                "      },\n" +
                "      \"updateTime\":{\n" +
                "        \"type\": \"date\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        request.source(json, XContentType.JSON);
        client.indices().create(request, RequestOptions.DEFAULT);

    }

    @Test
    @SneakyThrows
    void testDeleteIndex() {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("item");
        client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
    }

    /**
     * 判断索引是否存在
     * */
    @Test
    @SneakyThrows
    void testGetIndex() {

        GetIndexRequest getIndexRequest = new GetIndexRequest("item");
        GetIndexResponse response = client.indices().get(getIndexRequest, RequestOptions.DEFAULT);
        System.out.println("response" + response);
    }

    @Autowired
    private IItemService itemService;

    @Test
    @SneakyThrows
    public void testAdd() {
        Item item = itemService.getById(625465);
        System.out.println(item);
        ItemDTO dto = BeanUtil.toBean(item, ItemDTO.class);
        IndexRequest request = new IndexRequest("item");
        request.id(item.getId().toString());
        request.source(JSONUtil.toJsonStr(dto), XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    @SneakyThrows
    public void testGetId() {
        GetRequest request = new GetRequest("item", "625465");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String json = response.getSourceAsString();
        ItemDTO dto = JSONUtil.toBean(json, ItemDTO.class);
        System.out.println(dto);
    }

    @Test
    @SneakyThrows
    public void testDelete() {
        DeleteRequest request = new DeleteRequest("item", "625465");
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    @SneakyThrows
    public void testUpdate() {
        UpdateRequest request = new UpdateRequest("item", "625465");
        Map<String, Object> map = new HashMap<>();
        map.put("sold", 99);
        map.put("brand", "营养快线");
        request.doc(map);
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    @SneakyThrows
    public void testBulk() {
        int pageNO = 1;
        int pageSize = 10000;

        while(true) {
            log.info("开始处理第{}页数据", pageNO);
            Page<Item> page = itemService.lambdaQuery().page(new Page<>(pageNO, pageSize));
            List<Item> records = page.getRecords();
            if (CollUtil.isEmpty(records)) {
                log.info("数据处理完成，页面：{}", pageNO);
                return;
            }
            // 代码执行到这说明有数据
            //
            BulkRequest request = new BulkRequest();
            for (Item record : records) {
                ItemDTO dto = BeanUtil.toBean(record, ItemDTO.class);
                request.add(new IndexRequest("item")
                        .id(record.getId()
                                .toString()).source(JSONUtil.toJsonStr(dto), XContentType.JSON));
            }
            client.bulk(request, RequestOptions.DEFAULT);
            pageNO++;
        }
    }

    /**
     * match_all 查询：查询索引中的所有文档，不做任何过滤
     * <p>
     * 等价 DSL：
     * <pre>{@code
     * GET /item/_search
     * {
     *   "query": {
     *     "match_all": {}
     *   }
     * }
     * }</pre>
     */
    @Test
    @SneakyThrows
    void testMatchAll() {
        // 1. 创建搜索请求，指定索引名为 "item"
        SearchRequest request = new SearchRequest("item");
        // 2. 构建查询条件：match_all 查询，匹配索引中的所有文档
        //    相当于不带任何条件的全量查询，默认返回前 10 条
        request.source()
                .query(QueryBuilders.matchAllQuery());
        // 3. 通过 High Level REST Client 发送请求，获取搜索结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4. 调用封装好的工具方法解析并打印响应结果
        handleResponse(response);
    }

    /**
     * 解析并打印 Elasticsearch 搜索响应
     * <p>
     * 处理流程：SearchResponse → SearchHits → SearchHit[] → _source JSON → ItemDTO
     *
     * @param response ES 搜索响应对象
     */
    private void handleResponse(SearchResponse response) {
        // 1. 获取命中结果集合（SearchHits），包含总数和命中记录数组
        SearchHits searchHits = response.getHits();
        // 2. 获取命中总条数（即使只返回 10 条，total 也是实际匹配的总数）
        long total = searchHits.getTotalHits().value;
        System.out.println("总条数：" + total);
        // 3. 获取命中记录数组（SearchHit[]），遍历每一条
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // 4. 从 hit 中提取 _source 字段，即文档的原始 JSON 字符串
            String source = hit.getSourceAsString();
            // 5. 将 JSON 反序列化为 ItemDTO 对象，方便在 Java 代码中使用
            ItemDTO itemDTO = JSONUtil.toBean(source, ItemDTO.class);
            System.out.println(itemDTO);
        }
    }

    /**
     * match 查询：全文检索，先对搜索词分词，再用分词结果去匹配
     * <p>
     * 特点：name 字段为 text 类型，配合 IK 分词器，"脱脂牛奶" 会被分词为 ["脱脂", "牛奶"]，
     * 只要文档 name 中包含任一个词即可命中（OR 逻辑）。
     * <p>
     * 等价 DSL：
     * <pre>{@code
     * GET /item/_search
     * {
     *   "query": {
     *     "match": {
     *       "name": "脱脂牛奶"
     *     }
     *   }
     * }
     * }</pre>
     */
    @Test
    @SneakyThrows
    public void testMatch() {
        // 1. 创建搜索请求，指定索引 "item"
        SearchRequest searchRequest = new SearchRequest("item");
        // 2. 构建 match 查询 → 分词后再匹配，适合 text 字段的模糊搜索
        searchRequest.source().query(QueryBuilders.matchQuery("name", "脱脂牛奶"));
        // 3. 发送请求
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        // 4. 解析并打印响应结果
        handleResponse(response);
    }

    /**
     * term 查询：精确匹配，不对搜索词分词，直接拿整个值去倒排索引中查找
     * <p>
     * 特点：brand 字段为 keyword 类型，"华为" 作为一个不可分割的完整词条存入索引，
     * 只有完全相等时才能命中。不会对搜索词做任何分词处理。
     * <p>
     * 与 match 的核心区别：term 不分词（精确匹配），match 先分词再匹配（全文检索）
     * <p>
     * 等价 DSL：
     * <pre>{@code
     * GET /item/_search
     * {
     *   "query": {
     *     "term": {
     *       "brand": "华为"
     *     }
     *   }
     * }
     * }</pre>
     */
    @Test
    @SneakyThrows
    public void testTerm() {
        // 1. 创建搜索请求，指定索引 "item"
        SearchRequest searchRequest = new SearchRequest("item");
        // 2. 构建 term 查询 → 不分词，精确匹配，适合 keyword / 数字 / 日期 / 布尔字段
        searchRequest.source().query(QueryBuilders.termQuery("brand", "华为"));
        // 3. 发送请求
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        // 4. 解析并打印响应结果
        handleResponse(response);
    }

    /**
     * range 查询：范围查询
     * */
    @Test
    @SneakyThrows
    public void testRange() {
        // 1. 创建request
        SearchRequest request = new SearchRequest("item");
        // 2. 构建range查询
        request.source().query(QueryBuilders.rangeQuery("price").from(1).to(100));
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4. 解析并打印响应结果
        handleResponse(response);
    }

    /**
     * 复合查询
     * */
    @Test
    @SneakyThrows
    public void testBool() {
        // 1. 创建request
        SearchRequest request = new SearchRequest("item");
        // 2. 组织请求参数
        // 构建bool查询
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        // 2.2 关键字搜索
        bool.must(QueryBuilders.matchQuery("name", "手机"));
        // 2.3 筛选品牌
        bool.filter(QueryBuilders.termQuery("brand", "华为"));
        // 2.4 筛选价格
        bool.filter(QueryBuilders.rangeQuery("price").lte(3000));
        request.source().query(bool);
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4. 解析并打印响应结果
        handleResponse(response);
    }

    /**
     * 排序和分页
     * */
    @Test
    @SneakyThrows
    public void testSortAndPage() {
        int pageNo = 1;
        int pageSize = 5;
        // 1. 创建request
        SearchRequest request = new SearchRequest("item");
        // 2. 组织请求条件
        // 2.1 搜索条件参数
        request.source().query(QueryBuilders.matchQuery("name", "脱脂牛奶"));
        // 2.2 排序条件
        request.source().sort("price", SortOrder.ASC);
        // 2.3 分页参数
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4. 解析请求
        handleResponse(response);
    }

    /**
     * 高亮查询：将搜索结果中匹配的关键词用标签包裹，便于前端做高亮展示
     * <p>
     * 如搜索"脱脂牛奶"，返回的 name 字段中"脱脂"和"牛奶"会被 &lt;em&gt; 标签包裹，
     * 前端渲染时即可高亮显示匹配词。
     * <p>
     * 等价 DSL：
     * <pre>{@code
     * GET /item/_search
     * {
     *   "query": { "match": { "name": "脱脂牛奶" } },
     *   "highlight": {
     *     "fields": {
     *       "name": {
     *         "pre_tags": ["<em>"],
     *         "post_tags": ["</em>"]
     *       }
     *     }
     *   }
     * }
     * }</pre>
     */
    @Test
    @SneakyThrows
    public void testHighlight() {
        // 1. 创建搜索请求，指定索引 "item"
        SearchRequest request = new SearchRequest("item");
        // 2. 组织请求参数
        // 2.1 查询条件：match 全文检索 name 中包含"脱脂牛奶"的文档
        request.source().query(QueryBuilders.matchQuery("name", "脱脂牛奶"));
        // 2.2 高亮条件：name 字段的高亮结果用 <em></em> 包裹匹配词
        request.source().highlighter(
                SearchSourceBuilder.highlight()
                        .field("name")
                        .preTags("<em>")    // 高亮前缀，搜索结果中匹配词前插入的标签
                        .postTags("</em>")  // 高亮后缀，搜索结果中匹配词后插入的标签
        );
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4. 使用专门解析高亮结果的方法打印响应
        handleHighLightResponse(response);
    }

    /**
     * 解析并打印 ES 高亮查询的响应结果
     * <p>
     * 处理流程：SearchResponse → SearchHits → SearchHit[] → _source → ItemDTO
     * → 提取高亮字段 → 用高亮值替换原始字段值
     *
     * @param response ES 搜索响应对象（需包含 highlight 配置）
     */
    private void handleHighLightResponse(SearchResponse response) {
        // 1. 获取命中结果集合（SearchHits）
        SearchHits searchHits = response.getHits();
        // 2. 获取命中总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("总条数：" + total);
        // 3. 获取命中记录数组（SearchHit[]），遍历每一条
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // 4. 从 _source 中提取原始 JSON 文档，反序列化为 ItemDTO
            String source = hit.getSourceAsString();
            ItemDTO item = JSONUtil.toBean(source, ItemDTO.class);
            // 5. 获取高亮字段 Map，key 为字段名，value 为该字段的高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            // HighlightField 中包含 fragments[] 数组，每个 fragment 是一段带高亮标签的文本
            if (!CollUtils.isEmpty(highlightFields)) {
                // 5.1 取出 name 字段的高亮结果
                HighlightField highlightField = highlightFields.get("name");
                if (highlightField != null) {
                    // 5.2 从 fragments 数组中取第一段高亮文本，替换原始 name
                    String name = highlightFields.get("name").fragments()[0].string();
                    item.setName(name);
                }
            }
            // 6. 打印最终结果（name 已被替换为带 <em> 标签的高亮版本）
            System.out.println(item);
        }
    }

    /**
     * 数据聚合：先将数据分组（类似 GROUP BY），再对每组做统计（COUNT / AVG 等）
     * <p>
     * 聚合只对 query 命中的文档生效——先筛选，后分组统计。
     * size(0) 表示不返回文档本身，只返回聚合结果，节省带宽。
     * <p>
     * 本示例：筛选 category=手机 且 price>=30000 的商品，按 brand 分组统计数量
     * <p>
     * 等价 DSL：
     * <pre>{@code
     * GET /item/_search
     * {
     *   "size": 0,
     *   "query": {
     *     "bool": {
     *       "filter": [
     *         { "term": { "category": "手机" } },
     *         { "range": { "price": { "gte": 30000 } } }
     *       ]
     *     }
     *   },
     *   "aggs": {
     *     "brand_agg": {
     *       "terms": { "field": "brand", "size": 5 }
     *     }
     *   }
     * }
     * }</pre>
     */
    @Test
    @SneakyThrows
    public void testAggregation() {
        // 1. 创建搜索请求，指定索引 "item"
        SearchRequest request = new SearchRequest("item");
        // 2. 构建查询条件：bool 的 filter 组合，不参与打分，性能更好
        //    - category 精确等于 "手机"（keyword 字段，用 term）
        //    - price 大于等于 30000（分）
        BoolQueryBuilder bool = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("category", "手机"))
                .filter(QueryBuilders.rangeQuery("price").gte(30000));
        // size(0)：只返回聚合结果，不返回命中的文档列表
        request.source().query(bool).size(0);
        // 3. 构建聚合条件：terms 聚合 = 按字段值分组（类似 GROUP BY）
        //    "brand_agg" 是自定义聚合名称，后续解析时用这个名字获取结果
        //    size(5) 返回文档数 TOP 5 的分组
        request.source().aggregation(
                AggregationBuilders.terms("brand_agg").field("brand").size(5)
        );
        // 4. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 5. 解析聚合结果：聚合结果和文档结果分离，通过 getAggregations() 获取
        Aggregations aggregations = response.getAggregations();
        // 5.1 按聚合名称取出 terms 聚合结果，强转为 Terms 类型
        Terms brandAgg = aggregations.get("brand_agg");
        // 5.2 获取所有桶（bucket），每个桶代表一个分组
        List<? extends Terms.Bucket> buckets = brandAgg.getBuckets();
        // 5.3 遍历每个桶，提取分组 key（品牌名）和 doc_count（文档数量）
        for (Terms.Bucket bucket : buckets) {
            // 5.4 getKeyAsString() 获取分组键（品牌名）
            String brand = bucket.getKeyAsString();
            System.out.println("brand = " + brand);
            // getDocCount() 获取该分组下的文档数量
            long count = bucket.getDocCount();
            System.out.println("count = " + count);
        }
    }

}