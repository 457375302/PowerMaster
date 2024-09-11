package cn.iocoder.yudao.module.pdu.service.historydata;

import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetEnvSensor;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetPdu;
import cn.iocoder.yudao.framework.common.entity.mysql.room.RoomIndex;
import cn.iocoder.yudao.framework.common.mapper.*;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pdu.controller.admin.historydata.vo.EnvDataDetailsReqVO;
import cn.iocoder.yudao.module.pdu.controller.admin.historydata.vo.EnvDataPageReqVo;
import cn.iocoder.yudao.module.pdu.controller.admin.historydata.vo.HistoryDataDetailsReqVO;
import cn.iocoder.yudao.module.pdu.controller.admin.historydata.vo.HistoryDataPageReqVO;
import cn.iocoder.yudao.module.pdu.dal.mysql.pdudevice.PduIndex;
import cn.iocoder.yudao.module.pdu.dal.mysql.pdudevice.PduIndexMapper;
import cn.iocoder.yudao.module.pdu.service.energyconsumption.EnergyConsumptionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;


/**
 * @author linzhentian
 */
@Service
public class HistoryDataServiceImpl implements HistoryDataService {
    private static final int BATCH_SIZE = 200; // 每批处理的数据量
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PduIndexMapper pduIndexMapper;

    @Autowired
    private CabinetPduMapper cabinetPduMapper;

    @Autowired
    private CabinetIndexMapper cabinetIndexMapper;

    @Autowired
    private AisleIndexMapper aisleIndexMapper;

    @Autowired
    private RoomIndexMapper roomIndexMapper;

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private EnergyConsumptionService energyConsumptionService;

    @Autowired
    private CabinetEnvSensorMapper cabinetEnvSensorMapper;


    @Override
    public List<Object> getLocationsByPduIds(List<Map<String, Object>> mapList) {
        List<Object> resultList = new ArrayList<>(mapList.size());
        for (Map<String, Object> map : mapList){
            Object pduId = map.get("pdu_id");
            if (pduId instanceof Integer) {
                // 查询位置
                PduIndex pduIndex = pduIndexMapper.selectById( (int)pduId );
                if (pduIndex != null){
                    map.put("location", pduIndex.getDevKey());
                    map.put("address", getAddressByIpAddr(pduIndex.getDevKey()));
                }else{
                    map.put("location", null);
                    map.put("address", null);
                }
            }else{
                map.put("location", null);
                map.put("address", null);
            }
            resultList.add(map);
        }

        return resultList;
    }

    @Override
    public List<Object> getSensorLocationsByPduIds(List<Map<String, Object>> mapList) {
        List<Object> resultList = new ArrayList<>();
        for (Map<String, Object> map : mapList){
            Object pduId = map.get("pdu_id");
            Integer sensorId = (Integer) map.get("sensor_id");
            if (pduId instanceof Integer) {
                // 查询位置
                PduIndex pduIndex = pduIndexMapper.selectById( (int)pduId );
                if (pduIndex != null){
                    map.put("location", pduIndex.getDevKey());
                    map.put("address", getSensorAddressByIpAddr(pduIndex.getDevKey(), sensorId));
                }else{
                    map.put("location", null);
                    map.put("address", null);
                }
            }else{
                map.put("location", null);
                map.put("address", null);
            }
            resultList.add(map);
        }
        return resultList;
    }

    @Override
    public String getAddressByIpAddr(String location) {
//        String[] ipParts = location.split("-");
//        String address = null;
//        CabinetPdu cabinetPduA = cabinetPduMapper.selectOne(new LambdaQueryWrapperX<CabinetPdu>()
//                .eq(CabinetPdu::getPduIpA, ipParts[0])
//                .eq(CabinetPdu::getCasIdA, ipParts[1]));
//        CabinetPdu cabinetPduB = cabinetPduMapper.selectOne(new LambdaQueryWrapperX<CabinetPdu>()
//                .eq(CabinetPdu::getPduIpB, ipParts[0])
//                .eq(CabinetPdu::getCasIdB, ipParts[1]));
//        if(cabinetPduA != null){
//            int cabinetId = cabinetPduA.getCabinetId();
//            CabinetIndex cabinet = cabinetIndexMapper.selectById(cabinetId);
//            String cabinetName = cabinet.getName();
//            RoomIndex roomIndex = roomIndexMapper.selectById(cabinet.getRoomId());
//            String roomName = roomIndex.getName();
//            if(cabinet.getAisleId() != 0){
//                String aisleName = aisleIndexMapper.selectById(cabinet.getAisleId()).getName();
//                address = roomName + "-" + aisleName + "-" + cabinetName + "-" + "A路";
//            }else {
//                address = roomName + "-"  + cabinetName +  "-" + "A路";
//            }
//        }
//        if(cabinetPduB != null){
//            int cabinetId = cabinetPduB.getCabinetId();
//            CabinetIndex cabinet = cabinetIndexMapper.selectById(cabinetId);
//            String cabinetName = cabinet.getName();
//            RoomIndex roomIndex = roomIndexMapper.selectById(cabinet.getRoomId());
//            String roomName = roomIndex.getName();
//            if(cabinet.getAisleId() != 0){
//                String aisleName = aisleIndexMapper.selectById(cabinet.getAisleId()).getName();
//                address = roomName + "-" + aisleName + "-" + cabinetName + "-" + "B路";
//            }else {
//                address = roomName + "-"  + cabinetName +  "-" + "B路";
//            }
//        }
//        return address;
//    }
        // 分割字符串并检查长度
        String[] ipParts = location.split("-");
        if (ipParts.length < 2) {
//            // 如果分割后的数组长度小于 2，则返回 null 或抛出异常
             ipParts = location.split(":");
        }

        String address = null;

        // A 路
        CabinetPdu cabinetPduA = cabinetPduMapper.selectOne(
                new LambdaQueryWrapperX<CabinetPdu>()
                        .eq(CabinetPdu::getPduIpA, ipParts[0])
                        .eq(CabinetPdu::getCasIdA, ipParts[1])
        );

        if (cabinetPduA != null) {
            int cabinetId = cabinetPduA.getCabinetId();
            CabinetIndex cabinet = cabinetIndexMapper.selectById(cabinetId);
            String cabinetName = cabinet.getName();
            RoomIndex roomIndex = roomIndexMapper.selectById(cabinet.getRoomId());
            String roomName = roomIndex.getName();

            if (cabinet.getAisleId() != 0) {
                String aisleName = aisleIndexMapper.selectById(cabinet.getAisleId()).getName();
                address = roomName + "-" + aisleName + "-" + cabinetName + "-" + "A路";
            } else {
                address = roomName + "-" + cabinetName + "-" + "A路";
            }
        }

        // B 路
        CabinetPdu cabinetPduB = cabinetPduMapper.selectOne(
                new LambdaQueryWrapperX<CabinetPdu>()
                        .eq(CabinetPdu::getPduIpB, ipParts[0])
                        .eq(CabinetPdu::getCasIdB, ipParts[1])
        );

        if (cabinetPduB != null) {
            int cabinetId = cabinetPduB.getCabinetId();
            CabinetIndex cabinet = cabinetIndexMapper.selectById(cabinetId);
            String cabinetName = cabinet.getName();
            RoomIndex roomIndex = roomIndexMapper.selectById(cabinet.getRoomId());
            String roomName = roomIndex.getName();

            if (cabinet.getAisleId() != 0) {
                String aisleName = aisleIndexMapper.selectById(cabinet.getAisleId()).getName();
                address = roomName + "-" + aisleName + "-" + cabinetName + "-" + "B路";
            } else {
                address = roomName + "-" + cabinetName + "-" + "B路";
            }
        }

        return address;
    }

    @Override
    public Map<String, Object> getSensorAddressByIpAddr(String location, Integer sensorId) {
        Map<String, Object> map = new HashMap<>();
        String[] ipParts = location.split("-");
        String address = null;
        CabinetPdu cabinetPduA = cabinetPduMapper.selectOne(new LambdaQueryWrapperX<CabinetPdu>()
                .eq(CabinetPdu::getPduIpA, ipParts[0])
                .eq(CabinetPdu::getCasIdA, ipParts[1]));
        CabinetPdu cabinetPduB = cabinetPduMapper.selectOne(new LambdaQueryWrapperX<CabinetPdu>()
                .eq(CabinetPdu::getPduIpB, ipParts[0])
                .eq(CabinetPdu::getCasIdB, ipParts[1]));
        if(cabinetPduA != null){
            int cabinetId = cabinetPduA.getCabinetId();
            CabinetIndex cabinet = cabinetIndexMapper.selectById(cabinetId);
            String cabinetName = cabinet.getName();
            RoomIndex roomIndex = roomIndexMapper.selectById(cabinet.getRoomId());
            String roomName = roomIndex.getName();
            if(cabinet.getAisleId() != 0){
                String aisleName = aisleIndexMapper.selectById(cabinet.getAisleId()).getName();
//                address = roomName + "-" + aisleName + "-" + cabinetName + "-" + "A路";
                address = roomName + "-" + aisleName + "-" + cabinetName;
            }else {
//                address = roomName + "-"  + cabinetName +  "-" + "A路";
                address = roomName + "-"  + cabinetName ;
            }
            // 查传感器在机柜的前后门上中下位置
            CabinetEnvSensor cabinetEnvSensor = cabinetEnvSensorMapper.selectOne(new LambdaQueryWrapperX<CabinetEnvSensor>()
                    .eq(CabinetEnvSensor::getCabinetId, cabinetId)
                    .eq(CabinetEnvSensor::getPathPdu, 'A')
                    .eq(CabinetEnvSensor::getSensorId, sensorId));
            if (cabinetEnvSensor != null){
                map.put("channel", cabinetEnvSensor.getChannel());
                map.put("position", cabinetEnvSensor.getPosition());
            }else{
                map.put("channel", null);
                map.put("position", null);
            }
        }
        if(cabinetPduB != null){
            int cabinetId = cabinetPduB.getCabinetId();
            CabinetIndex cabinet = cabinetIndexMapper.selectById(cabinetId);
            String cabinetName = cabinet.getName();
            RoomIndex roomIndex = roomIndexMapper.selectById(cabinet.getRoomId());
            String roomName = roomIndex.getName();
            if(cabinet.getAisleId() != 0){
                String aisleName = aisleIndexMapper.selectById(cabinet.getAisleId()).getName();
//                address = roomName + "-" + aisleName + "-" + cabinetName + "-" + "B路";
                address = roomName + "-" + aisleName + "-" + cabinetName ;
            }else {
//                address = roomName + "-"  + cabinetName +  "-" + "B路";
                address = roomName + "-"  + cabinetName ;
            }
            // 查传感器在机柜的前后门上中下位置
            CabinetEnvSensor cabinetEnvSensor = cabinetEnvSensorMapper.selectOne(new LambdaQueryWrapperX<CabinetEnvSensor>()
                    .eq(CabinetEnvSensor::getCabinetId, cabinetId)
                    .eq(CabinetEnvSensor::getPathPdu, 'B')
                    .eq(CabinetEnvSensor::getSensorId, sensorId));
            if (cabinetEnvSensor != null){
                map.put("channel", cabinetEnvSensor.getChannel());
                map.put("position", cabinetEnvSensor.getPosition());
            }else{
                map.put("channel", null);
                map.put("position", null);
            }

        }
        map.put("address", address);
        return map;
    }

    @Override
    public Integer getPduIdByAddr(String ipAddr, String cascadeAddr) {
        String devKey = ipAddr+"-"+cascadeAddr;
        QueryWrapper<PduIndex> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dev_key", devKey); // 指定查询条件：name 字段等于给定的 name 值
        PduIndex pduIndex = pduIndexMapper.selectOne(queryWrapper); // 执行查询，返回匹配的实体对象
        if (pduIndex != null){
            return Math.toIntExact(pduIndex.getId());
        }
        return null;
    }

    @Override
    public List<String> getPduIdsByIps(String[] ips){
        QueryWrapper<PduIndex> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id");
        queryWrapper.in("dev_key", ips);
        return pduIndexMapper.selectObjs(queryWrapper);

    }

    @Override
    public Map getHistoryDataTypeMaxValue() throws IOException {
        HashMap resultMap = new HashMap<>();
        String[] indexArr = new String[]{"pdu_hda_line_realtime", "pdu_hda_loop_realtime", "pdu_hda_outlet_realtime"};
        String[] fieldNameArr = new String[]{"line_id", "loop_id", "outlet_id"};
        for (int i = 0; i < indexArr.length; i++) {
            SearchRequest searchRequest = new SearchRequest(indexArr[i]);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            // 添加最大值聚合
            searchSourceBuilder.aggregation(
                    AggregationBuilders.max("max_value").field(fieldNameArr[i])
            );
            searchRequest.source(searchSourceBuilder);
            // 执行搜索请求
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            // 从聚合结果中获取最大值
            Max maxAggregation = searchResponse.getAggregations().get("max_value");
            Integer maxValue = (int) maxAggregation.getValue();
            resultMap.put(fieldNameArr[i]+"_max_value", maxValue);
        }

        return resultMap;
    }

    @Override
    public Map getSensorIdMaxValue() throws IOException {
        HashMap resultMap = new HashMap<>();
        SearchRequest searchRequest = new SearchRequest("pdu_env_realtime");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 添加最大值聚合
        searchSourceBuilder.aggregation(
                AggregationBuilders.max("max_value").field("sensor_id")
        );
        searchRequest.source(searchSourceBuilder);
        // 执行搜索请求
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        // 从聚合结果中获取最大值
        Max maxAggregation = searchResponse.getAggregations().get("max_value");
        Integer maxValue = (int) maxAggregation.getValue();
        resultMap.put("sensor_id_max_value", maxValue);
        return resultMap;
    }

    @Override
    public PageResult<Object> getHistoryDataPage(HistoryDataPageReqVO pageReqVO) throws IOException {
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
        Integer pduId = null;
        if (!Objects.equals(pageReqVO.getIpAddr(), "") && !Objects.equals(pageReqVO.getIpAddr(), null)){
            pduId = getPduIdByAddr(pageReqVO.getIpAddr(), pageReqVO.getCascadeAddr());
            if(pduId != null){
                searchSourceBuilder.query(QueryBuilders.termQuery("pdu_id", pduId));
            }else{
                // 查不到pdu 直接返回空数据
                pageResult = new PageResult<>();
                pageResult.setList(null)
                        .setTotal(0L);
                return pageResult;
            }
        }else{
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        }
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if (pageReqVO.getTimeRange() != null && pageReqVO.getTimeRange().length != 0) {
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(pageReqVO.getTimeRange()[0])
                    .to(pageReqVO.getTimeRange()[1]));
        }
        List<String> pduIds = null;
        String[] ipArray = pageReqVO.getIpArray();
        if (ipArray != null){
            pduIds = getPduIdsByIps(ipArray);
            searchSourceBuilder.query(QueryBuilders.termsQuery("pdu_id", pduIds));
        }
        switch (pageReqVO.getType()) {
            case "total":
                if ("realtime".equals(pageReqVO.getGranularity())) {
                    searchRequest.indices("pdu_hda_total_realtime");
                } else if ("hour".equals(pageReqVO.getGranularity())) {
                    searchRequest.indices("pdu_hda_total_hour");
                } else {
                    searchRequest.indices("pdu_hda_total_day");
                }
                break;
            case "line":
                if ("realtime".equals(pageReqVO.getGranularity())) {
                    searchRequest.indices("pdu_hda_line_realtime");
                } else if ("hour".equals(pageReqVO.getGranularity())) {
                    searchRequest.indices("pdu_hda_line_hour");
                } else {
                    searchRequest.indices("pdu_hda_line_day");
                }
                if( pageReqVO.getLineId() != null){
                    Integer lineId = pageReqVO.getLineId();
                    // 创建匹配查询
                    QueryBuilder termQuery = QueryBuilders.termQuery ("line_id", lineId);
                    if (pduId != null) {
                        QueryBuilder termQuery1 = QueryBuilders.termQuery("pdu_id", pduId);
                        boolQuery.must(termQuery1);
                    }
                    boolQuery.must(termQuery);
                    // 将布尔查询设置到SearchSourceBuilder中
                    searchSourceBuilder.query(boolQuery);
                }
                break;

            case "loop":
                if ("realtime".equals(pageReqVO.getGranularity())) {
                    searchRequest.indices("pdu_hda_loop_realtime");
                } else if ("hour".equals(pageReqVO.getGranularity())) {
                    searchRequest.indices("pdu_hda_loop_hour");
                } else {
                    searchRequest.indices("pdu_hda_loop_day");
                }
                if (pageReqVO.getTimeRange() != null && pageReqVO.getTimeRange().length != 0) {
                    searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                            .from(pageReqVO.getTimeRange()[0])
                            .to(pageReqVO.getTimeRange()[1]));
                }
                if( pageReqVO.getLoopId() != null){
                    Integer loopId = pageReqVO.getLoopId();
                    // 创建匹配查询
                    QueryBuilder termQuery = QueryBuilders.termQuery("loop_id", loopId);
                    boolQuery.must(termQuery);
                    if (pduId != null) {
                        QueryBuilder termQuery1 = QueryBuilders.termQuery("pdu_id", pduId);
                        boolQuery.must(termQuery1);
                    }
                    // 将布尔查询设置到SearchSourceBuilder中
                    searchSourceBuilder.query(boolQuery);
                }
                break;

            case "outlet":
                if ("realtime".equals(pageReqVO.getGranularity())) {
                    searchRequest.indices("pdu_hda_outlet_realtime");
                } else if ("hour".equals(pageReqVO.getGranularity())) {
                    searchRequest.indices("pdu_hda_outlet_hour");
                } else {
                    searchRequest.indices("pdu_hda_outlet_day");
                }
                if( pageReqVO.getOutletId() != null){
                    Integer outletId = pageReqVO.getOutletId();
                    // 创建匹配查询
                    QueryBuilder termQuery = QueryBuilders.termQuery("outlet_id", outletId);
                    boolQuery.must(termQuery);
                    if (pduId != null) {
                        QueryBuilder termQuery1 = QueryBuilders.termQuery("pdu_id", pduId);
                        boolQuery.must(termQuery1);
                    }
                    // 将布尔查询设置到SearchSourceBuilder中
                    searchSourceBuilder.query(boolQuery);
                }
                break;
            default:
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
        pageResult.setList(getLocationsByPduIds(mapList))
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public PageResult<Object> getHistoryDataDetails(HistoryDataDetailsReqVO reqVO) throws IOException{
        Integer pduId = reqVO.getPduId();
        if (Objects.equals(pduId, null)){
            pduId = getPduIdByAddr(reqVO.getIpAddr(), reqVO.getCascadeAddr());
            if (Objects.equals(pduId, null)){
                return null;
            }
        }
        // 创建BoolQueryBuilder对象
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
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
        PageResult<Object> pageResult = null;
        switch (reqVO.getType()) {
            case "total":
                if ("realtime".equals(reqVO.getGranularity()) ){
                    searchRequest.indices("pdu_hda_total_realtime");
                }else if ("hour".equals(reqVO.getGranularity()) ){
                    searchRequest.indices("pdu_hda_total_hour");
                }else {
                    searchRequest.indices("pdu_hda_total_day");
                }
                searchSourceBuilder.query(QueryBuilders.termQuery("pdu_id", pduId));
                break;

            case "line":
                if ("realtime".equals(reqVO.getGranularity()) ){
                    searchRequest.indices("pdu_hda_line_realtime");
                }else if ("hour".equals(reqVO.getGranularity()) ){
                    searchRequest.indices("pdu_hda_line_hour");
                }else {
                    searchRequest.indices("pdu_hda_line_day");
                }
                Integer lineId = reqVO.getLineId();
                // 创建范围查询
                QueryBuilder termQuery = QueryBuilders.termQuery("pdu_id", pduId);
                // 创建匹配查询
                QueryBuilder termQuery1 = QueryBuilders.termQuery("line_id", lineId);
                // 将范围查询和匹配查询添加到布尔查询中
                boolQuery.must(termQuery);
                boolQuery.must(termQuery1);
                // 将布尔查询设置到SearchSourceBuilder中
                searchSourceBuilder.query(boolQuery);
                break;

            case "loop":
                if ("realtime".equals(reqVO.getGranularity()) ){
                    searchRequest.indices("pdu_hda_loop_realtime");
                }else if ("hour".equals(reqVO.getGranularity()) ){
                    searchRequest.indices("pdu_hda_loop_hour");
                }else {
                    searchRequest.indices("pdu_hda_loop_day");
                }
                Integer loopId = reqVO.getLoopId();
                // 创建范围查询
                termQuery = QueryBuilders.termQuery("pdu_id", pduId);
                // 创建匹配查询
                termQuery1 = QueryBuilders.termQuery("loop_id", loopId);
                // 将范围查询和匹配查询添加到布尔查询中
                boolQuery.must(termQuery);
                boolQuery.must(termQuery1);
                // 将布尔查询设置到SearchSourceBuilder中
                searchSourceBuilder.query(boolQuery);
                break;

            case "outlet":
                if ("realtime".equals(reqVO.getGranularity()) ){
                    searchRequest.indices("pdu_hda_outlet_realtime");
                }else if ("hour".equals(reqVO.getGranularity()) ){
                    searchRequest.indices("pdu_hda_outlet_hour");
                }else {
                    searchRequest.indices("pdu_hda_outlet_day");
                }
                Integer outletId = reqVO.getOutletId();
                // 创建范围查询
                termQuery = QueryBuilders.termQuery("pdu_id", pduId);
                // 创建匹配查询
                termQuery1 = QueryBuilders.termQuery("outlet_id", outletId);
                // 将范围查询和匹配查询添加到布尔查询中
                boolQuery.must(termQuery);
                boolQuery.must(termQuery1);
                // 将布尔查询设置到SearchSourceBuilder中
                searchSourceBuilder.query(boolQuery);
                break;
            default:
        }
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
    public PageResult<Object> getEnvDataPage(EnvDataPageReqVo pageReqVO) throws IOException {
        PageResult<Object> pageResult;
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

//        if (!Objects.equals(pageReqVO.getIpAddr(), "") && !Objects.equals(pageReqVO.getIpAddr(), null)){
//            Integer pduId = getPduIdByAddr(pageReqVO.getIpAddr(), pageReqVO.getCascadeAddr());
//            if(pduId != null){
//                if (!Objects.equals(sensorId, 0)){
//                    // 创建范围查询
//                    QueryBuilder termQuery = QueryBuilders.termQuery("pdu_id", pduId);
//                    // 创建匹配查询
//                    QueryBuilder termQuery1 = QueryBuilders.termQuery("sensor_id", sensorId);
//                    // 创建BoolQueryBuilder对象
//                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
//                    // 将范围查询和匹配查询添加到布尔查询中
//                    boolQuery.must(termQuery);
//                    boolQuery.must(termQuery1);
//                    // 将布尔查询设置到SearchSourceBuilder中
//                    searchSourceBuilder.query(boolQuery);
//                }else{
//                    searchSourceBuilder.query(QueryBuilders.termQuery("pdu_id", pduId));
//                }
//            }else{
//                // 查不到pdu 直接返回空数据
//                pageResult = new PageResult<>();
//                pageResult.setList(null)
//                        .setTotal(0L);
//                return pageResult;
//            }
//        }else{
//            if (!Objects.equals(sensorId, 0)){
//                QueryBuilder termQuery = QueryBuilders.termQuery("sensor_id", sensorId);
//                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
//                boolQuery.must(termQuery);
//                searchSourceBuilder.query(boolQuery);
//            }else{
//                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
//            }
//        }

        // 接收机柜id数组 查出ip数组
        List<String> pduIds = new ArrayList<>();
        String[] cabinetIds = pageReqVO.getCabinetIds();
        Integer channel = pageReqVO.getChannel();
        Integer position = pageReqVO.getPosition();

        // 前端筛选机柜但没筛选探测点触发
        if (cabinetIds != null && channel == null){
            QueryWrapper<CabinetPdu> cabinetPduQueryWrapper = new QueryWrapper<>();
            cabinetPduQueryWrapper.in("cabinet_id", cabinetIds);
            List<CabinetPdu> cabinetPduList = cabinetPduMapper.selectList(cabinetPduQueryWrapper);
            for (CabinetPdu cabinetPdu1 : cabinetPduList){
                Integer ipA = getPduIdByAddr(cabinetPdu1.getPduIpA(), String.valueOf(cabinetPdu1.getCasIdA()));
                Integer ipB = getPduIdByAddr(cabinetPdu1.getPduIpB(), String.valueOf(cabinetPdu1.getCasIdB()));
                if (ipA != null) {
                    pduIds.add(String.valueOf(ipA));
                }
                if (ipB != null) {
                    pduIds.add(String.valueOf(ipB));
                }
            }
            if (!pduIds.isEmpty()) {
                searchSourceBuilder.query(QueryBuilders.termsQuery("pdu_id", pduIds));
            }else{
                // 查不到pdu 直接返回空数据
                pageResult = new PageResult<>();
                pageResult.setList(null)
                        .setTotal(0L);
                return pageResult;
            }
        }else{
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        // 前端筛选某个机柜和探测点后触发
        if (cabinetIds!= null && channel != null && position != null){
            QueryWrapper<CabinetEnvSensor> cabinetEnvSensorQueryWrapper = new QueryWrapper<>();
            cabinetEnvSensorQueryWrapper.eq("cabinet_id", cabinetIds[0])
                    .eq("channel", channel)
                    .eq("position", position);
            CabinetEnvSensor cabinetEnvSensor = cabinetEnvSensorMapper.selectOne(cabinetEnvSensorQueryWrapper);
            // 表示此机柜此位置没有传感器 直接返回
            if ( cabinetEnvSensor == null ){
                pageResult = new PageResult<>();
                pageResult.setList(null)
                        .setTotal(0L);
                return pageResult;
            }
            QueryWrapper<CabinetPdu> cabinetPduQueryWrapper = new QueryWrapper<>();
            cabinetPduQueryWrapper.eq("cabinet_id", cabinetIds[0]);
            CabinetPdu cabinetPdu = cabinetPduMapper.selectOne(cabinetPduQueryWrapper);
            Integer pduId = null;
            if (cabinetEnvSensor.getPathPdu() == 'A'){
                pduId = getPduIdByAddr(cabinetPdu.getPduIpA(), String.valueOf(cabinetPdu.getCasIdA()));
            }
            if (cabinetEnvSensor.getPathPdu() == 'B'){
                pduId = getPduIdByAddr(cabinetPdu.getPduIpB(), String.valueOf(cabinetPdu.getCasIdB()));
            }
            // 创建范围查询
            if (pduId != null) {
                QueryBuilder termQuery = QueryBuilders.termQuery("pdu_id", pduId);
                // 创建匹配查询
                QueryBuilder termQuery1 = QueryBuilders.termQuery("sensor_id", cabinetEnvSensor.getSensorId());
                // 创建BoolQueryBuilder对象
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                // 将范围查询和匹配查询添加到布尔查询中
                boolQuery.must(termQuery);
                boolQuery.must(termQuery1);
                // 将布尔查询设置到SearchSourceBuilder中
                searchSourceBuilder.query(boolQuery);
            }

        }

        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if ("realtime".equals(pageReqVO.getGranularity())) {
            searchRequest.indices("pdu_env_realtime");
        } else if ("hour".equals(pageReqVO.getGranularity())) {
            searchRequest.indices("pdu_env_hour");
        } else {
            searchRequest.indices("pdu_env_day");
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
        pageResult.setList(getSensorLocationsByPduIds(mapList))
                .setTotal(totalHits);

        return pageResult;
    }

    @Override
    public Map<String, Object> getEnvDataDetails(EnvDataDetailsReqVO reqVO) throws IOException {
        // 创建BoolQueryBuilder对象
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort("create_time.keyword", SortOrder.ASC);
        searchSourceBuilder.size(10000);
        searchSourceBuilder.trackTotalHits(true);
        // 搜索请求对象
        SearchRequest searchRequest = new SearchRequest();
        if ("realtime".equals(reqVO.getGranularity()) ){
            searchRequest.indices("pdu_env_realtime");
        }else if ("hour".equals(reqVO.getGranularity()) ){
            searchRequest.indices("pdu_env_hour");
        }else {
            searchRequest.indices("pdu_env_day");
        }
        if (reqVO.getTimeRange() != null && reqVO.getTimeRange().length != 0){
            searchSourceBuilder.postFilter(QueryBuilders.rangeQuery("create_time.keyword")
                    .from(reqVO.getTimeRange()[0])
                    .to(reqVO.getTimeRange()[1]));
        }
        Map<String, Object> map = new HashMap<>();
        Integer sensorId = reqVO.getSensorId();
        Integer pduId = reqVO.getPduId();
        Integer cabinetId = reqVO.getCabinetId();
        Integer channel = reqVO.getChannel();
        Integer position = reqVO.getPosition();
        String ipAddr = null;
        /**
         * 不是跳转到环境分析的情况 此时没有pduId和sensorId 要用cabinetId、channel和position来查
         */
        if (pduId == null && sensorId == null){
            QueryWrapper<CabinetEnvSensor> cabinetEnvSensorQueryWrapper = new QueryWrapper<>();
            cabinetEnvSensorQueryWrapper.eq("cabinet_id", cabinetId)
                    .eq("channel", channel)
                    .eq("position", position);
            CabinetEnvSensor cabinetEnvSensor = cabinetEnvSensorMapper.selectOne(cabinetEnvSensorQueryWrapper);
            // 表示此机柜此位置没有传感器 直接返回
            if ( cabinetEnvSensor == null ){
                System.out.println("机柜此位置没有传感器");
                map.put("list", null);
                map.put("total", 0);
                map.put("ipAddr", null);
                return map;
            }
            sensorId = cabinetEnvSensor.getSensorId();
            QueryWrapper<CabinetPdu> cabinetPduQueryWrapper = new QueryWrapper<>();
            cabinetPduQueryWrapper.eq("cabinet_id", cabinetId);
            CabinetPdu cabinetPdu = cabinetPduMapper.selectOne(cabinetPduQueryWrapper);
            if (cabinetEnvSensor.getPathPdu() == 'A'){
                pduId = getPduIdByAddr(cabinetPdu.getPduIpA(), String.valueOf(cabinetPdu.getCasIdA()));
                ipAddr = cabinetPdu.getPduIpA()+'-'+ cabinetPdu.getCasIdA();
            } else if (cabinetEnvSensor.getPathPdu() == 'B') {
                pduId = getPduIdByAddr(cabinetPdu.getPduIpB(), String.valueOf(cabinetPdu.getCasIdB()));
                ipAddr = cabinetPdu.getPduIpB()+'-'+ cabinetPdu.getCasIdB();
            }
        }
        if (pduId == null){
            return null;
        }

        // 创建匹配查询
        QueryBuilder termQuery = QueryBuilders.termQuery("pdu_id", pduId);
        QueryBuilder termQuery1 = QueryBuilders.termQuery("sensor_id", sensorId);
        // 将匹配查询添加到布尔查询中
        boolQuery.must(termQuery);
        boolQuery.must(termQuery1);
        // 将布尔查询设置到SearchSourceBuilder中
        searchSourceBuilder.query(boolQuery);
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
        map.put("list", resultList);
        map.put("total", totalHits);
        map.put("ipAddr", ipAddr);

        return map;
    }

    @Override
    public Map<String, Object> getNavNewData(String granularity) throws IOException {
        String[] indices = new String[0];
        String[] key = new String[]{"total", "line", "loop", "outlet"};
        LocalDateTime[] timeAgo = new LocalDateTime[0];
        Map<String, Object> map;
        switch (granularity){
            case "realtime":
                indices = new String[]{"pdu_hda_total_realtime", "pdu_hda_line_realtime", "pdu_hda_loop_realtime", "pdu_hda_outlet_realtime"};
                timeAgo = new LocalDateTime[]{LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusMinutes(1)};
                break;
            case "hour":
                indices = new String[]{"pdu_hda_total_hour", "pdu_hda_line_hour", "pdu_hda_loop_hour", "pdu_hda_outlet_hour"};
                timeAgo = new LocalDateTime[]{LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1)};
                break;
            case "day":
                indices = new String[]{"pdu_hda_total_day", "pdu_hda_line_day", "pdu_hda_loop_day", "pdu_hda_outlet_day"};
                timeAgo = new LocalDateTime[]{LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1)};
                break;
            default:
        }
        map = energyConsumptionService.getSumData(indices, key, timeAgo);
        return map;
    }

    @Override
    public Map<String, Object> getEnvNavNewData() throws IOException {
        String[] indices = new String[]{"pdu_env_realtime", "pdu_env_hour", "pdu_env_day"};
        String[] name = new String[]{"hour", "day", "week"};
        LocalDateTime[] timeAgo = new LocalDateTime[]{LocalDateTime.now().minusHours(1), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusWeeks(1)};
        Map<String, Object> map = energyConsumptionService.getSumData(indices, name, timeAgo);
        return map;
    }
    @Override
    public List<Object> getEnExcelList(List<Object> list){
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof Map && ((Map<?, ?>) obj).keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                mapList.add(map);
            }
        }
        for(int i=0;i<mapList.size();i++){
            Map mp=(Map)mapList.get(i).get("address");
            mapList.get(i).put("address",mp.get("address"));
            mapList.get(i).put("create_time",mapList.get(i).get("create_time").toString().substring(0,16));
            if(mapList.get(i).containsKey("tem_max_time")&&mapList.get(i).containsKey("tem_min_time")){
                mapList.get(i).put("tem_max_time",mapList.get(i).get("tem_max_time").toString().substring(0,16));
                mapList.get(i).put("tem_min_time",mapList.get(i).get("tem_min_time").toString().substring(0,16));
            }
        }
        return list;
    }

    @Override
    public List<Object> getNewHistoryDataDetails(List<Object> list,String ob) {
        List<Map<String, Object>> mapList = new ArrayList<>();

        for (Object obj : list) {
            if (obj instanceof Map && ((Map<?, ?>) obj).keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                mapList.add(map);
            }
        }
        if(ob.equals("realtime")){
            for (int i = 0; i < mapList.size(); i++) {
                mapList.get(i).put("create_time", mapList.get(i).get("create_time").toString().substring(0, 16));
            }
        }
        else if(ob.equals("not_realtime")){
            for (int i = 0; i < mapList.size(); i++) {
                mapList.get(i).put("create_time", mapList.get(i).get("create_time").toString().substring(0, 16));
                mapList.get(i).put("pow_apparent_max_time", mapList.get(i).get("pow_apparent_max_time").toString().substring(0, 16));
                mapList.get(i).put("pow_apparent_min_time", mapList.get(i).get("pow_apparent_min_time").toString().substring(0, 16));
            }
        }

        return list;
    }

    @Override
    public List<Object> getNewExcelList(List<Object> list,String ob) {
        List<Map<String, Object>> mapList = new ArrayList<>();

        for (Object obj : list) {
            if (obj instanceof Map && ((Map<?, ?>) obj).keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                mapList.add(map);
            }
        }
        if(ob.equals("1")){
            for (int i = 0; i < mapList.size(); i++) {
                mapList.get(i).put("create_time", mapList.get(i).get("create_time").toString().substring(0, 16));
            }
        }
        else if (ob.equals("3")){
            for (int i = 0; i < mapList.size(); i++) {
                mapList.get(i).put("create_time", mapList.get(i).get("create_time").toString().substring(0, 16));
                mapList.get(i).put("pow_active_max_time", mapList.get(i).get("pow_active_max_time").toString().substring(0, 16));
                mapList.get(i).put("pow_active_min_time", mapList.get(i).get("pow_active_min_time").toString().substring(0, 16));
                mapList.get(i).put("pow_apparent_max_time", mapList.get(i).get("pow_apparent_max_time").toString().substring(0, 16));
                mapList.get(i).put("pow_apparent_min_time", mapList.get(i).get("pow_apparent_min_time").toString().substring(0, 16));
                mapList.get(i).put("cur_max_time", mapList.get(i).get("cur_max_time").toString().substring(0, 16));
                mapList.get(i).put("cur_min_time", mapList.get(i).get("cur_min_time").toString().substring(0, 16));
                mapList.get(i).put("vol_max_time", mapList.get(i).get("vol_max_time").toString().substring(0, 16));
                mapList.get(i).put("vol_min_time", mapList.get(i).get("vol_min_time").toString().substring(0, 16));
            }
        }
        else if (ob.equals("4")){
            for (int i = 0; i < mapList.size(); i++) {
                mapList.get(i).put("create_time", mapList.get(i).get("create_time").toString().substring(0, 16));
                mapList.get(i).put("pow_active_max_time", mapList.get(i).get("pow_active_max_time").toString().substring(0, 16));
                mapList.get(i).put("pow_active_min_time", mapList.get(i).get("pow_active_min_time").toString().substring(0, 16));
                mapList.get(i).put("pow_apparent_max_time", mapList.get(i).get("pow_apparent_max_time").toString().substring(0, 16));
                mapList.get(i).put("pow_apparent_min_time", mapList.get(i).get("pow_apparent_min_time").toString().substring(0, 16));
                mapList.get(i).put("cur_max_time", mapList.get(i).get("cur_max_time").toString().substring(0, 16));
                mapList.get(i).put("cur_min_time", mapList.get(i).get("cur_min_time").toString().substring(0, 16));
            }
        }
        else {
            for (int i = 0; i < mapList.size(); i++) {
                mapList.get(i).put("create_time", mapList.get(i).get("create_time").toString().substring(0, 16));
                mapList.get(i).put("pow_active_max_time", mapList.get(i).get("pow_active_max_time").toString().substring(0, 16));
                mapList.get(i).put("pow_active_min_time", mapList.get(i).get("pow_active_min_time").toString().substring(0, 16));
                mapList.get(i).put("pow_apparent_max_time", mapList.get(i).get("pow_apparent_max_time").toString().substring(0, 16));
                mapList.get(i).put("pow_apparent_min_time", mapList.get(i).get("pow_apparent_min_time").toString().substring(0, 16));
            }
        }

        return list;
    }


}