package cn.iocoder.yudao.module.bus.service.busenergyconsumption;

import cn.iocoder.yudao.framework.common.entity.mysql.bus.BoxIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.bus.BusIndex;
import cn.iocoder.yudao.framework.common.mapper.BoxIndexMapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bus.controller.admin.energyconsumption.VO.EnergyConsumptionPageReqVO;
import cn.iocoder.yudao.module.bus.dal.dataobject.boxindex.BoxIndexDO;
import cn.iocoder.yudao.module.bus.dal.dataobject.busindex.BusIndexDO;
import cn.iocoder.yudao.module.bus.dal.mysql.busindex.BusIndexMapper;
import cn.iocoder.yudao.module.bus.service.historydata.BusHistoryDataService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BusEnergyConsumptionServiceImpl implements BusEnergyConsumptionService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private BusHistoryDataService busHistoryDataService;

    @Autowired
    private BusIndexMapper busIndexMapper;

    @Autowired
    private BoxIndexMapper boxIndexMapper;

    private Integer[] getBusIdsByDevkeys(String[] devkeys){
        // 创建 QueryWrapper
        QueryWrapper<BusIndexDO> queryWrapper = new QueryWrapper<>();
        // 设置条件：dev_key 在数组 devkeys 中
        queryWrapper.in("dev_key", Arrays.asList(devkeys));
        // 查询指定字段 name
//        queryWrapper.select("id");
        // 执行查询
        List<BusIndexDO> entities = busIndexMapper.selectList(queryWrapper);
        // 提取 id 字段为 List<String>
        return entities.stream()
                .map(BusIndexDO::getId)
                .collect(Collectors.toList()).toArray(new Integer[0]);

    }

    private Integer[] getBoxIdsByDevkeys(String[] devkeys){
        // 创建 QueryWrapper
        QueryWrapper<BoxIndex> queryWrapper = new QueryWrapper<>();
        // 设置条件：dev_key 在数组 devkeys 中
        queryWrapper.in("dev_key", Arrays.asList(devkeys));
        // 查询指定字段 name
//        queryWrapper.select("id");
        // 执行查询
        List<BoxIndex> entities = boxIndexMapper.selectList(queryWrapper);
        // 提取 id 字段为 List<String>
        return entities.stream()
                .map(BoxIndex::getId)
                .collect(Collectors.toList()).toArray(new Integer[0]);

    }

    @Override
    public PageResult<Object> getEQDataPage(EnergyConsumptionPageReqVO pageReqVO) throws IOException {
        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 设置要排除的字段
        searchSourceBuilder.fetchSource(null, new String[]{"id", "bill_value", "bill_mode", "bill_period"});
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
        if (pageReqVO.getTimeRange() != null && pageReqVO.getTimeRange().length != 0) {
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(pageReqVO.getTimeRange()[0])
                    .to(pageReqVO.getTimeRange()[1]));
        }
        String[] devkeys = pageReqVO.getDevkeys();
        if (devkeys != null){
            Integer[] busIds = getBusIdsByDevkeys(devkeys);
            searchSourceBuilder.query(QueryBuilders.termsQuery("bus_id", busIds));
        }
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if ("day".equals(pageReqVO.getGranularity()) ){
            searchRequest.indices("bus_eq_total_day");
        }else if ("week".equals(pageReqVO.getGranularity()) ){
            searchRequest.indices("bus_eq_total_week");
        }else {
            searchRequest.indices("bus_eq_total_month");
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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(busHistoryDataService.getLocationsAndNameByBusIds(mapList))
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public PageResult<Object> getBillDataPage(EnergyConsumptionPageReqVO pageReqVO) throws IOException {
        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 设置要搜索的字段
//        searchSourceBuilder.fetchSource(new String[]{"bus_id", "start_time", "end_time", "eq_value",  "bill_value", }, null);
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
        if (pageReqVO.getTimeRange() != null && pageReqVO.getTimeRange().length != 0) {
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(pageReqVO.getTimeRange()[0])
                    .to(pageReqVO.getTimeRange()[1]));
        }
        String[] devkeys = pageReqVO.getDevkeys();
        if (devkeys != null){
            Integer[] busIds = getBusIdsByDevkeys(devkeys);
            searchSourceBuilder.query(QueryBuilders.termsQuery("bus_id", busIds));
        }
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if ("day".equals(pageReqVO.getGranularity()) ){
            searchRequest.indices("bus_eq_total_day");
        }else if ("week".equals(pageReqVO.getGranularity()) ){
            searchRequest.indices("bus_eq_total_week");
        }else {
            searchRequest.indices("bus_eq_total_month");
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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(busHistoryDataService.getLocationsAndNameByBusIds(mapList))
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public PageResult<Object> getEQDataDetails(EnergyConsumptionPageReqVO reqVO) throws IOException {
        // 根据导航栏传来的devkey 获取busIds 一定只有一个，如果是跳转到能耗排名界面的就不会传devkey，会直接传一个busId
        Integer busId;
        if (reqVO.getDevkey() != null){
            String[] devkeys = new String[]{reqVO.getDevkey()};
            Integer[] busIds = getBusIdsByDevkeys(devkeys);
            if (busIds.length == 0){
                PageResult<Object> pageResult=new PageResult<>();
                pageResult.setList(new ArrayList<>())
                        .setTotal(new Long(0));
                return pageResult;
            }
            busId = busIds[0];
        }else {
            PageResult<Object> pageResult=new PageResult<>();
            pageResult.setList(new ArrayList<>())
                    .setTotal(new Long(0));
            return pageResult;
//            busId = reqVO.getBusId();
        }

        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort("create_time.keyword", SortOrder.ASC);
        searchSourceBuilder.size(10000);
        searchSourceBuilder.trackTotalHits(true);
        if (reqVO.getTimeRange() != null && reqVO.getTimeRange().length != 0){
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(reqVO.getTimeRange()[0])
                    .to(reqVO.getTimeRange()[1]));
        }
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if ("day".equals(reqVO.getGranularity()) ){
            searchRequest.indices("bus_eq_total_day");
        }else if ("week".equals(reqVO.getGranularity()) ){
            searchRequest.indices("bus_eq_total_week");
        }else {
            searchRequest.indices("bus_eq_total_month");
        }

        searchSourceBuilder.query(QueryBuilders.termQuery("bus_id", busId));
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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(resultList)
                .setTotal(totalHits);
        return pageResult;
    }

    @Override
    public PageResult<Object> getRealtimeEQDataPage(EnergyConsumptionPageReqVO pageReqVO) throws IOException {
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
        SearchRequest searchRequest = new SearchRequest();
        if (pageReqVO.getTimeRange() != null && pageReqVO.getTimeRange().length != 0) {
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(pageReqVO.getTimeRange()[0])
                    .to(pageReqVO.getTimeRange()[1]));
        }
        String[] devkeys = pageReqVO.getDevkeys();
        if (devkeys != null){
            Integer[] busIds = getBusIdsByDevkeys(devkeys);
            searchSourceBuilder.query(QueryBuilders.termsQuery("bus_id", busIds));
        }
        searchRequest.indices("bus_ele_total_realtime");
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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(busHistoryDataService.getLocationsAndNameByBusIds(mapList))
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public Map<String, Object> getSumData(String[] indices, String[] name, LocalDateTime[] timeAgo) throws IOException {
        Map<String, Object> resultItem = new HashMap<>();
        // 添加范围查询
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < (name.length==3?3:1); i++) {
            SearchRequest searchRequest;
            if(indices.length==2){
                 searchRequest = new SearchRequest(indices[0],indices[1]);
            }else{
                searchRequest = new SearchRequest(indices[0]);
            }
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(timeAgo[i].format(formatter))
                    .to(now.format(formatter)));
            // 添加计数聚合
            if(indices[0]=="bus_eq_total_day"||indices[0]=="bus_ele_total_realtime"){
                searchSourceBuilder.aggregation(
                    AggregationBuilders.count("total_insertions").field("bus_id")
            );
            }
            else{
                searchSourceBuilder.aggregation(
                        AggregationBuilders.count("total_insertions").field("box_id")
                );
            }
            searchRequest.source(searchSourceBuilder);
            // 执行搜索请求
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            // 从聚合结果中获取文档数量
            ValueCount totalInsertionsAggregation = searchResponse.getAggregations().get("total_insertions");
            long totalInsertions = totalInsertionsAggregation.getValue();
            resultItem.put(name[i], totalInsertions);
        }
        return resultItem;
    }

    @Override
    public Map<String, Object> getNewData() throws IOException {
        String[] indices = new String[]{"bus_eq_total_day"};
        String[] name = new String[]{"day", "week", "month"};
        LocalDateTime[] timeAgo = new LocalDateTime[]{LocalDateTime.now().minusDays(1), LocalDateTime.now().minusWeeks(1), LocalDateTime.now().minusMonths(1)};
        Map<String, Object> map = getSumData(indices, name, timeAgo);
        return map;
    }

    @Override
    public Map<String, Object> getOneDaySumData() throws IOException {
        String[] indices = new String[]{"bus_ele_total_realtime"};
        String[] name = new String[]{"total"};
        LocalDateTime[] timeAgo = new LocalDateTime[]{LocalDateTime.now().minusDays(1)};
        Map<String, Object> map = getSumData(indices, name, timeAgo);
        return map;
    }

    @Override
    public PageResult<Object> getBusSubBillDetails(EnergyConsumptionPageReqVO reqVO) throws IOException {
        Integer busId = reqVO.getBusId();
        if (Objects.equals(busId, null)){
            return null;
        }
        // 把传来的开始时间(例如"2024-07-25 10:00:00") 的年月日加一天变成 '2024-07-26 00:00:00'和'2024-07-27 00:00:00'
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 解析原始时间字符串为LocalDateTime对象
        LocalDateTime originalTime = LocalDateTime.parse(reqVO.getStartTime(), formatter);
        // 获取当天的开始时间
        LocalDateTime startTime = originalTime.withHour(0).withMinute(0).withSecond(0).withNano(0).plusDays(1);
        // 获取下一天的开始时间（即当天的结束时间的下一天）
        LocalDateTime endTime = startTime.plusDays(1);
        // 将LocalDateTime对象转换回字符串
        String startTimeString = startTime.format(formatter);
        String endTimeString = endTime.format(formatter);
        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort("start_time.keyword", SortOrder.ASC);
        searchSourceBuilder.size(10000);
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                .from(startTimeString)
                .to(endTimeString));
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("bus_eq_sub_total_day");
        searchSourceBuilder.query(QueryBuilders.termQuery("bus_id", busId));
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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(resultList)
                .setTotal(totalHits);
        return pageResult;
    }


    //--------------------------------------------------------插接箱
    @Override
    public PageResult<Object> getBoxEQDataPage(EnergyConsumptionPageReqVO pageReqVO) throws IOException {
        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 设置要排除的字段
        searchSourceBuilder.fetchSource(null, new String[]{"id", "bill_value", "bill_mode", "bill_period"});
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
        if (pageReqVO.getTimeRange() != null && pageReqVO.getTimeRange().length != 0) {
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(pageReqVO.getTimeRange()[0])
                    .to(pageReqVO.getTimeRange()[1]));
        }
        String[] devkeys = pageReqVO.getDevkeys();
        if (devkeys != null){
            Integer[] boxIds = getBoxIdsByDevkeys(devkeys);
            System.out.println();
            searchSourceBuilder.query(QueryBuilders.termsQuery("box_id", boxIds));
        }
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if ("day".equals(pageReqVO.getGranularity()) ){
            searchRequest.indices("box_eq_total_day");
        }else if ("week".equals(pageReqVO.getGranularity()) ){
            searchRequest.indices("box_eq_total_week");
        }else {
            searchRequest.indices("box_eq_total_month");
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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(busHistoryDataService.getLocationsAndNameByBoxIds(mapList))
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public PageResult<Object> getBoxBillDataPage(EnergyConsumptionPageReqVO pageReqVO) throws IOException {
        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 设置要搜索的字段
//        searchSourceBuilder.fetchSource(new String[]{"box_id", "start_time", "end_time", "eq_value",  "bill_value", }, null);
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
        if (pageReqVO.getTimeRange() != null && pageReqVO.getTimeRange().length != 0) {
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(pageReqVO.getTimeRange()[0])
                    .to(pageReqVO.getTimeRange()[1]));
        }
        String[] devkeys = pageReqVO.getDevkeys();
        if (devkeys != null){
            Integer[] boxIds = getBoxIdsByDevkeys(devkeys);
            searchSourceBuilder.query(QueryBuilders.termsQuery("box_id", boxIds));
        }
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if ("day".equals(pageReqVO.getGranularity()) ){
            searchRequest.indices("box_eq_total_day");
        }else if ("week".equals(pageReqVO.getGranularity()) ){
            searchRequest.indices("box_eq_total_week");
        }else {
            searchRequest.indices("box_eq_total_month");
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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(busHistoryDataService.getLocationsAndNameByBoxIds(mapList))
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public PageResult<Object> getBoxEQDataDetails(EnergyConsumptionPageReqVO reqVO) throws IOException {
        // 根据导航栏传来的devkey 获取boxIds 一定只有一个，如果是跳转到能耗排名界面的就不会传devkey，会直接传一个boxId
        Integer boxId;
        if (reqVO.getDevkey() != null){
            String[] devkeys = new String[]{reqVO.getDevkey()};
            Integer[] boxIds = getBoxIdsByDevkeys(devkeys);
            if (boxIds.length == 0){
                PageResult<Object>pageResult=new PageResult<>();
                pageResult.setList(new ArrayList<>())
                        .setTotal(new Long(0));
                return pageResult;

            }
            boxId = boxIds[0];
        }else {
            PageResult<Object>pageResult=new PageResult<>();
            pageResult.setList(new ArrayList<>())
                    .setTotal(new Long(0));
            return pageResult;
//            boxId = reqVO.getBoxId();
        }
        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort("create_time.keyword", SortOrder.ASC);
        searchSourceBuilder.size(10000);
        searchSourceBuilder.trackTotalHits(true);
        if (reqVO.getTimeRange() != null && reqVO.getTimeRange().length != 0){
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(reqVO.getTimeRange()[0])
                    .to(reqVO.getTimeRange()[1]));
        }
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if ("day".equals(reqVO.getGranularity()) ){
            searchRequest.indices("box_eq_total_day");
        }else if ("week".equals(reqVO.getGranularity()) ){
            searchRequest.indices("box_eq_total_week");
        }else {
            searchRequest.indices("box_eq_total_month");
        }

        searchSourceBuilder.query(QueryBuilders.termQuery("box_id", boxId));

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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(resultList)
                .setTotal(totalHits);
        return pageResult;
    }

    @Override
    public PageResult<Object> getBoxRealtimeEQDataPage(EnergyConsumptionPageReqVO pageReqVO) throws IOException {
        if (pageReqVO==null){
            PageResult<Object> pageResult = new PageResult<>();
            pageResult.setTotal(new Long(0));
            pageResult.setList(new ArrayList<>());
        }

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
        SearchRequest searchRequest = new SearchRequest();
        if (pageReqVO.getTimeRange() != null && pageReqVO.getTimeRange().length != 0) {
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(pageReqVO.getTimeRange()[0])
                    .to(pageReqVO.getTimeRange()[1]));
        }
        String[] devkeys = pageReqVO.getDevkeys();
        if (devkeys != null){
            Integer[] boxIds = getBoxIdsByDevkeys(devkeys);
            searchSourceBuilder.query(QueryBuilders.termsQuery("box_id", boxIds));
        }
        searchRequest.indices("box_ele_total_realtime");
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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(busHistoryDataService.getLocationsAndNameByBoxIds(mapList))
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public Map<String, Object> getBoxNewData() throws IOException {
        String[] indices = new String[]{"box_eq_outlet_day","box_eq_total_day"};
        String[] name = new String[]{"day", "week", "month"};
        LocalDateTime[] timeAgo = new LocalDateTime[]{LocalDateTime.now().minusDays(1), LocalDateTime.now().minusWeeks(1), LocalDateTime.now().minusMonths(1)};
        Map<String, Object> map = getSumData(indices, name, timeAgo);
        return map;
    }

    @Override
    public Map<String, Object> getBoxOneDaySumData() throws IOException {
        String[] indices = new String[]{"box_ele_total_realtime"};
        String[] name = new String[]{"total"};
        LocalDateTime[] timeAgo = new LocalDateTime[]{LocalDateTime.now().minusDays(1)};
        Map<String, Object> map = getSumData(indices, name, timeAgo);
        return map;
    }

    @Override
    public PageResult<Object> getBoxSubBillDetails(EnergyConsumptionPageReqVO reqVO) throws IOException {
        Integer boxId = reqVO.getBoxId();
        if (Objects.equals(boxId, null)){
            return null;
        }
        // 把传来的开始时间(例如"2024-07-25 10:00:00") 的年月日加一天变成 '2024-07-26 00:00:00'和'2024-07-27 00:00:00'
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 解析原始时间字符串为LocalDateTime对象
        LocalDateTime originalTime = LocalDateTime.parse(reqVO.getStartTime(), formatter);
        // 获取当天的开始时间
        LocalDateTime startTime = originalTime.withHour(0).withMinute(0).withSecond(0).withNano(0).plusDays(1);
        // 获取下一天的开始时间（即当天的结束时间的下一天）
        LocalDateTime endTime = startTime.plusDays(1);
        // 将LocalDateTime对象转换回字符串
        String startTimeString = startTime.format(formatter);
        String endTimeString = endTime.format(formatter);
        // 搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort("start_time.keyword", SortOrder.ASC);
        searchSourceBuilder.size(10000);
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                .from(startTimeString)
                .to(endTimeString));
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("box_eq_sub_total_day");
        searchSourceBuilder.query(QueryBuilders.termQuery("box_id", boxId));
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
        PageResult<Object> pageResult = new PageResult<>();
        pageResult.setList(resultList)
                .setTotal(totalHits);
        return pageResult;

    }

    @Override
    public List<Object> getNewlList(List<Object> list) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof Map && ((Map<?, ?>) obj).keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                mapList.add(map);
            }
        }
        for(int i=0;i<mapList.size();i++){
            mapList.get(i).put("create_time",mapList.get(i).get("create_time").toString().substring(0,10));
            mapList.get(i).put("start_time",mapList.get(i).get("start_time").toString().substring(0,10));
            mapList.get(i).put("end_time",mapList.get(i).get("end_time").toString().substring(0,10));
        }
        return list;
    }

    @Override
    public List<Object> getNewDetailList(List<Object> list) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof Map && ((Map<?, ?>) obj).keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                mapList.add(map);
            }
        }
        for(int i=0;i<mapList.size();i++){
            mapList.get(i).put("create_time",mapList.get(i).get("create_time").toString().substring(0,16));
            mapList.get(i).put("start_time",mapList.get(i).get("start_time").toString().substring(0,16));
            mapList.get(i).put("end_time",mapList.get(i).get("end_time").toString().substring(0,16));
        }
        return list;
    }

    @Override
    public List<Object> getNewBillList(List<Object> list) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof Map && ((Map<?, ?>) obj).keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                mapList.add(map);
            }
        }
        for(int i=0;i<mapList.size();i++){
            mapList.get(i).put("start_time",mapList.get(i).get("start_time").toString().substring(0,16));
            mapList.get(i).put("end_time",mapList.get(i).get("end_time").toString().substring(0,16));
        }
        return list;
    }

    @Override
    public List<Object> getNewRealtimeList(List<Object> list) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof Map && ((Map<?, ?>) obj).keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                mapList.add(map);
            }
        }
        for(int i=0;i<mapList.size();i++){
            mapList.get(i).put("create_time",mapList.get(i).get("create_time").toString().substring(0,16));
        }
        return list;
    }

    @Override
    public List<Object> getNewList(List<Object> list) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof Map && ((Map<?, ?>) obj).keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                mapList.add(map);
            }
        }
        for(int i=0;i<mapList.size();i++){
            mapList.get(i).put("create_time",mapList.get(i).get("create_time").toString().substring(0,10));
            mapList.get(i).put("start_time",mapList.get(i).get("start_time").toString().substring(0,10));
            mapList.get(i).put("end_time",mapList.get(i).get("end_time").toString().substring(0,10));

        }
        return list;
    }


}
