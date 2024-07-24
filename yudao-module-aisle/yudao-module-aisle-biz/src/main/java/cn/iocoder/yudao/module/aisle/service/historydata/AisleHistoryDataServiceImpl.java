package cn.iocoder.yudao.module.aisle.service.historydata;

import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleIndex;
import cn.iocoder.yudao.framework.common.mapper.AisleIndexMapper;
import cn.iocoder.yudao.framework.common.mapper.RoomIndexMapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aisle.controller.admin.historydata.vo.AisleHistoryDataDetailsReqVO;
import cn.iocoder.yudao.module.aisle.controller.admin.historydata.vo.AisleHistoryDataPageReqVO;
import cn.iocoder.yudao.module.aisle.dal.dataobject.aisleindex.AisleIndexDO;
import cn.iocoder.yudao.module.aisle.service.energyconsumption.AisleEnergyConsumptionService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * @author linzhentian
 */
@Service
public class AisleHistoryDataServiceImpl implements AisleHistoryDataService {

    @Autowired
    private AisleEnergyConsumptionService aisleEnergyConsumptionService;
    @Resource
    private AisleIndexMapper aisleIndexMapper;

    @Autowired
    private RoomIndexMapper roomIndexMapper;

    @Autowired
    private RestHighLevelClient client;

    @Override
    public List<Object> getLocationsByAisleIds(List<Map<String, Object>> mapList) {
        List<Object> resultList = new ArrayList<>();
            for (Map<String, Object> map : mapList){
                try{
                    Object aisleId = map.get("aisle_id");
                    if (aisleId instanceof Integer) {
                        AisleIndex aisleIndex = aisleIndexMapper.selectById((Serializable) aisleId);
                        String roomName = roomIndexMapper.selectById(aisleIndex.getRoomId()).getName();
                        String localtion = roomName + "-"  + aisleIndex.getName() ;

                        map.put("location", localtion);
                    }else{
                        map.put("location", null);
                    }
                } catch (Exception  e) {
                    map.put("location", null);
                }
                resultList.add(map);
            }
        return resultList;
    }

    @Override
    public PageResult<Object> getHistoryDataPage(AisleHistoryDataPageReqVO pageReqVO) throws IOException {
        PageResult<Object> pageResult = null;
        // 创建BoolQueryBuilder对象
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        int pageNo = pageReqVO.getPageNo();
        int pageSize = pageReqVO.getPageSize();
        int index = (pageNo - 1) * pageSize;
        searchSourceBuilder.from(index);
        // 最后一页请求超过一万，pageSize设置成请求刚好一万条
        if (index + pageSize > 10000){
            searchSourceBuilder.size(10000 - index);
        }else{
            searchSourceBuilder.size(pageSize);
        }
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.sort("create_time.keyword", SortOrder.DESC);
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();

        String[] aisleIds = pageReqVO.getAisleIds();
        if (aisleIds != null){
            searchSourceBuilder.query(QueryBuilders.termsQuery("aisle_id", aisleIds));
        }
        if ("realtime".equals(pageReqVO.getGranularity())) {
            searchRequest.indices("aisle_hda_pow_realtime");
        } else if ("hour".equals(pageReqVO.getGranularity())) {
            searchRequest.indices("aisle_hda_pow_hour");
        } else {
            searchRequest.indices("aisle_hda_pow_day");
        }
        if (pageReqVO.getTimeRange() != null && pageReqVO.getTimeRange().length != 0) {
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(pageReqVO.getTimeRange()[0])
                    .to(pageReqVO.getTimeRange()[1]));
        }
        searchRequest.source(searchSourceBuilder);
        // 执行搜索,向ES发起http请求
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        // 搜索结果
        List<Map<String, Object>> mapList = new ArrayList<>();
        SearchHits hits = searchResponse.getHits();
        hits.forEach(searchHit -> mapList.add(searchHit.getSourceAsMap()));
        // 匹配到的总记录数
        Long totalHits = hits.getTotalHits().value;
        // 返回的结果
        pageResult = new PageResult<>();
        pageResult.setList(getLocationsByAisleIds(mapList))
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public PageResult<Object> getHistoryDataDetails(AisleHistoryDataDetailsReqVO reqVO) throws IOException{
        Integer aisleId = reqVO.getAisleId();
        if (Objects.equals(aisleId, null)){
            return null;
        }
        // 创建BoolQueryBuilder对象
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort("create_time.keyword", SortOrder.ASC);
        searchSourceBuilder.size(10000);
        searchSourceBuilder.trackTotalHits(true);
        PageResult<Object> pageResult = null;
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if ("realtime".equals(reqVO.getGranularity()) ){
            searchRequest.indices("aisle_hda_pow_realtime");
        }else if ("hour".equals(reqVO.getGranularity()) ){
            searchRequest.indices("aisle_hda_pow_hour");
        }else {
            searchRequest.indices("aisle_hda_pow_day");
        }
        if (reqVO.getTimeRange() != null && reqVO.getTimeRange().length != 0){
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(reqVO.getTimeRange()[0])
                    .to(reqVO.getTimeRange()[1]));
        }
        searchSourceBuilder.query(QueryBuilders.termQuery("aisle_id", aisleId));
        searchRequest.source(searchSourceBuilder);
        // 执行搜索,向ES发起http请求
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        // 搜索结果
        List<Object> resultList = new ArrayList<>();
        SearchHits hits = searchResponse.getHits();
        hits.forEach(searchHit -> resultList.add(searchHit.getSourceAsMap()));
        // 匹配到的总记录数
        Long totalHits = hits.getTotalHits().value;
        // 返回的结果
        pageResult = new PageResult<>();
        pageResult.setList(resultList)
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public Map<String, Object> getNavNewData(String granularity) throws IOException {
        String[] indices = new String[0];
        String[] key = new String[]{"total"};
        LocalDateTime[] timeAgo = new LocalDateTime[0];
        Map<String, Object> map;
        switch (granularity){
            case "realtime":
                indices = new String[]{"aisle_hda_pow_realtime"};
                timeAgo = new LocalDateTime[]{LocalDateTime.now().minusMinutes(1)};
                break;
            case "hour":
                indices = new String[]{"aisle_hda_pow_hour"};
                timeAgo = new LocalDateTime[]{LocalDateTime.now().minusHours(1)};
                break;
            case "day":
                indices = new String[]{"aisle_hda_pow_day"};
                timeAgo = new LocalDateTime[]{LocalDateTime.now().minusDays(1)};
                break;
            default:
        }
        map = aisleEnergyConsumptionService.getSumData(indices, key, timeAgo);
        return map;
    }

}