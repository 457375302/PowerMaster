package cn.iocoder.yudao.module.room.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.framework.common.entity.es.room.ele.RoomEleTotalRealtimeDo;
import cn.iocoder.yudao.framework.common.entity.es.room.ele.RoomEqTotalDayDo;
import cn.iocoder.yudao.framework.common.entity.es.room.ele.RoomEqTotalMonthDo;
import cn.iocoder.yudao.framework.common.entity.es.room.ele.RoomEqTotalWeekDo;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleBar;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleBox;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.bus.BoxIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.bus.BusIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetPdu;
import cn.iocoder.yudao.framework.common.entity.mysql.pdu.PduIndexDo;
import cn.iocoder.yudao.framework.common.entity.mysql.room.RoomIndex;
import cn.iocoder.yudao.framework.common.enums.AlarmTypeEnums;
import cn.iocoder.yudao.framework.common.enums.DelEnums;
import cn.iocoder.yudao.framework.common.enums.DisableEnums;
import cn.iocoder.yudao.framework.common.enums.PduBoxEnums;
import cn.iocoder.yudao.framework.common.mapper.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.room.dto.RoomPowDataDTO;
import cn.iocoder.yudao.module.room.dto.main.DevDataDTO;
import cn.iocoder.yudao.module.room.dto.main.EqDataDTO;
import cn.iocoder.yudao.module.room.dto.main.PowDataDTO;
import cn.iocoder.yudao.module.room.enums.CabinetLoadEnums;
import cn.iocoder.yudao.module.room.enums.DeviceAlarmStatusEnum;
import cn.iocoder.yudao.module.room.service.MainService;
import cn.iocoder.yudao.module.system.api.alarm.AlarmRecordApi;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.constant.FieldConstant.*;

/**
 * @author luowei
 * @version 1.0
 * @description: 机房操作
 * @date 2024/6/21 14:19
 */
@Slf4j
@Service
public class MainServiceImpl implements MainService {

    @Resource
    RoomIndexMapper roomIndexMapper;
    @Resource
    RedisTemplate redisTemplate;
    @Resource
    CabinetIndexMapper cabinetIndexMapper;

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private BusIndexDoMapper busIndexDoMapper;
    @Autowired
    private BoxIndexMapper boxIndexMapper;
    @Resource
    PduIndexDoMapper pduIndexDoMapper;

    @Override
    public EqDataDTO getMainEq() {

        EqDataDTO result = new EqDataDTO();
        try {
            //获取所有机房
            List<RoomIndex> roomIndexList = roomIndexMapper.selectList(new LambdaQueryWrapper<RoomIndex>()
                    .eq(RoomIndex::getIsDelete,DelEnums.NO_DEL.getStatus()));

            List<EqDataDTO.RoomEq> roomEqList = new ArrayList<>();

            AtomicReference<Double> todayTotal = new AtomicReference<>((double) 0);
            AtomicReference<Double> yesTotal = new AtomicReference<>((double) 0);
            AtomicReference<Double> weekTotal = new AtomicReference<>((double) 0);
            AtomicReference<Double> monthTotal = new AtomicReference<>((double) 0);

            if (!CollectionUtils.isEmpty(roomIndexList)) {
                List<Integer> ids = roomIndexList.stream().map(RoomIndex::getId).collect(Collectors.toList());

                //今日
                String startTime = DateUtil.formatDateTime(DateUtil.beginOfDay(DateTime.now()));
                String endTime = DateUtil.formatDateTime(DateTime.now());

                Map<Integer,Double> todayEq = getDayEq(startTime, endTime, ids);
                //昨日
                List<String> yesList = getData(startTime, endTime, ids, ROOM_EQ_TOTAL_DAY);
                Map<Integer, Double> yesEqMap = new HashMap<>();

                yesList.forEach(str -> {
                    RoomEqTotalDayDo dayDo = JsonUtils.parseObject(str, RoomEqTotalDayDo.class);
                    yesEqMap.put(dayDo.getRoomId(), dayDo.getEqValue());
                });
                //上周
                startTime = DateUtil.formatDateTime(DateUtil.beginOfWeek(DateTime.now()));

                List<String> weekList = getData(startTime, endTime, ids, ROOM_EQ_TOTAL_WEEK);

                Map<Integer, Double> weekEqMap = new HashMap<>();

                weekList.forEach(str -> {
                    RoomEqTotalWeekDo weekDo = JsonUtils.parseObject(str, RoomEqTotalWeekDo.class);
                    weekEqMap.put(weekDo.getRoomId(), weekDo.getEqValue());

                });
                //上月
                startTime = DateUtil.formatDateTime(DateUtil.beginOfMonth(DateTime.now()));
                List<String> list = getData(startTime, endTime, ids, AISLE_EQ_TOTAL_MONTH);

                Map<Integer, Double> monthEqMap = new HashMap<>();

                list.forEach(str -> {
                    RoomEqTotalMonthDo monthDo = JsonUtils.parseObject(str, RoomEqTotalMonthDo.class);
                    monthEqMap.put(monthDo.getRoomId(), monthDo.getEqValue());

                });

                roomIndexList.forEach(roomIndex -> {
                    EqDataDTO eqDataDTO = new EqDataDTO();
                    EqDataDTO.RoomEq roomEq = eqDataDTO.new RoomEq();
                    roomEq.setId(roomIndex.getId());
                    roomEq.setName(roomIndex.getName());
                    roomEq.setTodayEq(todayEq.getOrDefault(roomIndex.getId(),0.0));
                    roomEq.setLastWeekEq(weekEqMap.getOrDefault(roomIndex.getId(),0.0));
                    roomEq.setLastMonthEq(monthEqMap.getOrDefault(roomIndex.getId(),0.0));
                    roomEq.setYesterdayEq(yesEqMap.getOrDefault(roomIndex.getId(),0.0));
                    roomEqList.add(roomEq);

                    yesTotal.updateAndGet(v -> new Double((double) (v + yesEqMap.getOrDefault(roomIndex.getId(),0.0))));
                    todayTotal.updateAndGet(v -> new Double((double) (v + todayEq.getOrDefault(roomIndex.getId(),0.0))));
                    weekTotal.updateAndGet(v -> new Double((double) (v + weekEqMap.getOrDefault(roomIndex.getId(),0.0))));
                    monthTotal.updateAndGet(v -> new Double((double) (v + monthEqMap.getOrDefault(roomIndex.getId(),0.0))));
                });
            }
            result.setRoomEqList(roomEqList);
            result.setLastWeekEqTotal(weekTotal.get());
            result.setLastMonthEqTotal(monthTotal.get());
            result.setTodayEqTotal(todayTotal.get());
            result.setYesterdayEqTotal(yesTotal.get());

        } catch (Exception e) {
            log.error("获取数据异常：", e);
        }
        return result;
    }

    @Override
    public DevDataDTO getMainDevData() {

        DevDataDTO devDataDTO = new DevDataDTO();

        //pdu数量
        List<PduIndexDo> pduIndexDos = pduIndexDoMapper.selectList(new LambdaQueryWrapper<PduIndexDo>()
                .eq(PduIndexDo::getIsDeleted,DelEnums.NO_DEL.getStatus()));
        if (!CollectionUtils.isEmpty(pduIndexDos)){
            devDataDTO.setPduNum(pduIndexDos.size());
            int offLine = (int) pduIndexDos.stream()
                    .filter(pduIndexDo -> pduIndexDo.getRunStatus() == DeviceAlarmStatusEnum.OFF_LINE.getStatus())
                    .distinct().count();
            devDataDTO.setPduOnLine(pduIndexDos.size()-offLine);
            devDataDTO.setPduOffLine(offLine);
        }
        //母线数量
        List<BusIndex> busIndexList = busIndexDoMapper.selectList(new LambdaQueryWrapper<BusIndex>()
                .eq(BusIndex::getIsDeleted,DelEnums.NO_DEL.getStatus()));
        if (!CollectionUtils.isEmpty(busIndexList)){
            devDataDTO.setBarNum(busIndexList.size());
            devDataDTO.setBusNum(busIndexList.size());

            int offLine = (int) busIndexList.stream()
                    .filter(busIndex -> busIndex.getRunStatus() == DeviceAlarmStatusEnum.OFF_LINE.getStatus())
                    .distinct().count();
            devDataDTO.setBusOnLine(busIndexList.size()-offLine);
            devDataDTO.setBusOffLine(offLine);
        }
        List<BoxIndex> boxIndexList = boxIndexMapper.selectList(new LambdaQueryWrapper<BoxIndex>()
                .eq(BoxIndex::getIsDeleted,DelEnums.NO_DEL.getStatus()));
        if (!CollectionUtils.isEmpty(boxIndexList)){
            devDataDTO.setBoxNum(boxIndexList.size());

            int offLine = (int) boxIndexList.stream()
                    .filter(boxIndex -> boxIndex.getRunStatus() == DeviceAlarmStatusEnum.OFF_LINE.getStatus())
                    .distinct().count();
            devDataDTO.setBoxOnLine(boxIndexList.size()-offLine);
            devDataDTO.setBoxOffLine(offLine);
        }

        //机柜
        List<CabinetIndex> cabinetIndexList = cabinetIndexMapper.selectList(new LambdaQueryWrapper<CabinetIndex>()
                .eq(CabinetIndex::getIsDeleted, DelEnums.NO_DEL.getStatus()));
        if (!CollectionUtils.isEmpty(cabinetIndexList)){
            devDataDTO.setCabNum(cabinetIndexList.size());

            //空载
            int grey = (int) cabinetIndexList.stream()
                    .filter(cabinetIndex -> cabinetIndex.getLoadStatus() == CabinetLoadEnums.GREY.getStatus())
                    .distinct().count();
            devDataDTO.setCabUnused(grey);
            devDataDTO.setCabUse(cabinetIndexList.size()-grey);

        }

        //计算系统运行时长
        List<PduIndexDo> pduIndexDoList = pduIndexDoMapper.selectList(new LambdaQueryWrapper<PduIndexDo>()
                .orderByAsc(PduIndexDo::getCreateTime));
        List<BusIndex> busIndices = busIndexDoMapper.selectList(new LambdaQueryWrapper<BusIndex>()
                .orderByAsc(BusIndex::getCreateTime));
        LocalDateTime pduTime = LocalDateTime.now();
        if (!CollectionUtils.isEmpty(pduIndexDoList)){
            pduTime = pduIndexDoList.get(0).getCreateTime();
        }
        LocalDateTime busTime = LocalDateTime.now();
        if (!CollectionUtils.isEmpty(busIndices)){
            busTime = busIndices.get(0).getCreateTime();
        }
        if (pduTime.isBefore(busTime)){
            long daysBetween = ChronoUnit.DAYS.between( pduTime,LocalDateTime.now());
            devDataDTO.setDays(daysBetween);
        }else {
            long daysBetween = ChronoUnit.DAYS.between( busTime,LocalDateTime.now());
            devDataDTO.setDays(daysBetween);
        }

        return devDataDTO;
    }

    @Override
    public PowDataDTO getMainPowData() {
        PowDataDTO powDataDTO = new PowDataDTO();

        ValueOperations ops = redisTemplate.opsForValue();

        List<RoomPowDataDTO> result = new ArrayList<>();

        AtomicReference<Float> totalPowActive = new AtomicReference<>((float) 0);
        AtomicReference<Float> totalPowApparent = new AtomicReference<>((float) 0);
        AtomicReference<Float> totalPowReactive = new AtomicReference<>((float) 0);

        //获取所有机房
        List<RoomIndex> roomIndexList = roomIndexMapper.selectList(new LambdaQueryWrapper<RoomIndex>()
                .eq(RoomIndex::getIsDelete,DelEnums.NO_DEL.getStatus()));
        if (!CollectionUtils.isEmpty(roomIndexList)){
            List<String> keys = new ArrayList<>();
            Map<Integer,RoomPowDataDTO> dtoMap = new HashMap<>();
            roomIndexList.forEach(roomIndex -> {
                keys.add(REDIS_KEY_ROOM + roomIndex.getId());
                RoomPowDataDTO roomMainDataDTO = new RoomPowDataDTO();
                roomMainDataDTO.setId(roomIndex.getId());
                roomMainDataDTO.setName(roomIndex.getName());
                dtoMap.put(roomIndex.getId(),roomMainDataDTO);

            });
            List<Object> objects = ops.multiGet(keys);

            if (!CollectionUtils.isEmpty(objects)){
                objects.forEach(object -> {
                    //功率数据
                    if (Objects.nonNull(object)){
                        JSONObject data = JSON.parseObject(JSON.toJSONString(object));
                        JSONObject roomData = data.containsKey(ROOM_POWER) ? data.getJSONObject(ROOM_POWER) : new JSONObject();
                        Integer id = data.getInteger(ROOM_KEY);
                        RoomPowDataDTO roomMainDataDTO = dtoMap.get(id);

                        if (roomData.containsKey(TOTAL_DATA)){
                            JSONObject totalData = roomData.getJSONObject(TOTAL_DATA);
                            //有功功率
                            if (totalData.containsKey(POW_ACTIVE)){
                                roomMainDataDTO.setPowActive(totalData.getFloatValue(POW_ACTIVE));
                                totalPowActive.updateAndGet(v -> new Float((float) (v + totalData.getFloatValue(POW_ACTIVE))));
                            }
                            //视在功率
                            if (totalData.containsKey(POW_APPARENT)){
                                roomMainDataDTO.setPowApparent(totalData.getFloatValue(POW_APPARENT));
                                totalPowApparent.updateAndGet(v -> new Float((float) (v + totalData.getFloatValue(POW_APPARENT))));
                            }
                            //无功功率
                            if (totalData.containsKey(POW_REACTIVE)){
                                roomMainDataDTO.setPowReactive(totalData.getFloatValue(POW_REACTIVE));
                                totalPowReactive.updateAndGet(v -> new Float((float) (v + totalData.getFloatValue(POW_REACTIVE))));
                            }
                            //功率因素
                            if (totalData.containsKey(POWER_FACTOR)){
                                roomMainDataDTO.setPowerFactor(totalData.getFloatValue(POWER_FACTOR));
                            }
                        }
                        result.add(roomMainDataDTO);
                    }
                });
            }

        }
        powDataDTO.setRoomDataList(result);
        powDataDTO.setTotalPowActive(totalPowActive.get());
        powDataDTO.setTotalPowApparent(totalPowApparent.get());
        powDataDTO.setTotalPowReactive(totalPowReactive.get());
        powDataDTO.setTotalPowerFactor(totalPowApparent.get()>0? totalPowActive.get()/totalPowApparent.get() : 0);
        return powDataDTO;
    }


    /**
     * 获取日用电量
     *
     * @param startTime
     * @param endTime
     * @param ids
     * @return
     */
    private Map<Integer,Double> getDayEq(String startTime, String endTime, List<Integer> ids) {
        log.info("startTime : " + startTime + "endTime：" + endTime);
        Map<Integer,Double> eqMap = new HashMap<>();
        //获取时间段内第一条和最后一条数据
        Map<Integer,RoomEleTotalRealtimeDo> endRealtimeDo = getEleData(startTime, endTime,
                SortOrder.DESC,
                ROOM_ELE_TOTAL_REALTIME, ids);
        Map<Integer,RoomEleTotalRealtimeDo> startRealtimeDo = getEleData(startTime, endTime,
                SortOrder.ASC,
                ROOM_ELE_TOTAL_REALTIME, ids);
        ids.forEach(id ->{
            //结束时间电量
            double endEle = endRealtimeDo.get(id).getEleTotal();

            //开始时间电量
            double startEle = startRealtimeDo.get(id).getEleTotal();

            //判断使用电量  开始电量大于结束电量，记为0
            double eq;
            if (startEle > endEle) {
                eq = 0;
            } else {
                eq = endEle - startEle;
            }
            eqMap.put(id,eq);

        });

        return eqMap;
    }

    /**
     * @param startTime
     * @param endTime
     * @param sortOrder
     * @param index
     * @param ids
     * @return
     */
    private Map<Integer,RoomEleTotalRealtimeDo> getEleData(String startTime, String endTime,
                                                           SortOrder sortOrder, String index, List<Integer> ids) {
        Map<Integer,RoomEleTotalRealtimeDo> realtimeDo = new HashMap<>();
        try {
            // 创建SearchRequest对象, 设置查询索引名
            SearchRequest searchRequest = new SearchRequest(index);
            // 通过QueryBuilders构建ES查询条件，
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //获取需要处理的数据
            builder.query(QueryBuilders.constantScoreQuery(QueryBuilders.boolQuery()
                    .must(QueryBuilders.rangeQuery(CREATE_TIME + KEYWORD).gte(startTime).lt(endTime))
                    .must(QueryBuilders.termsQuery(ROOM_ID, ids))));

            // 嵌套聚合
            TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(BY_ROOM)
                    .field(ROOM_ID).size(BUCKET_SIZE);
            // 设置聚合查询
            String top = "top";
            AggregationBuilder topAgg = AggregationBuilders.topHits(top)
                    .size(1).sort(CREATE_TIME + KEYWORD, sortOrder);

            builder.aggregation(aggregationBuilder.subAggregation(topAgg));

            // 设置搜索条件
            searchRequest.source(builder);

            // 执行ES请求
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            // 处理聚合查询结果
            Aggregations aggregations = searchResponse.getAggregations();
            // 查询terms聚合结果
            Terms aggregation = aggregations.get(BY_ROOM);


            // 遍历terms聚合结果
            for (Terms.Bucket bucket : aggregation.getBuckets()) {
                // 获取分组
                int roomId = Integer.parseInt(String.valueOf(bucket.getKey()));
                TopHits tophits = bucket.getAggregations().get(top);
                SearchHits sophistsHits = tophits.getHits();
                if (null != sophistsHits.getHits() && sophistsHits.getHits().length>0){
                    SearchHit hit = sophistsHits.getHits()[0];
                    realtimeDo.put(roomId,JsonUtils.parseObject(hit.getSourceAsString(), RoomEleTotalRealtimeDo.class)) ;
                }
            }
            return realtimeDo;
        } catch (Exception e) {
            log.error("获取数据异常：", e);
        }
        return realtimeDo;
    }

    /**
     * 获取es数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param index     索引表
     * @return
     * @throws IOException
     */
    private List<String> getData(String startTime, String endTime, List<Integer> ids, String index) throws IOException {
        // 创建SearchRequest对象, 设置查询索引名
        SearchRequest searchRequest = new SearchRequest(index);
        // 通过QueryBuilders构建ES查询条件，
        SearchSourceBuilder builder = new SearchSourceBuilder();

        //获取需要处理的数据
        builder.query(QueryBuilders.constantScoreQuery(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery(CREATE_TIME + KEYWORD).gte(startTime).lt(endTime))
                .must(QueryBuilders.termsQuery(ROOM_ID, ids))));
        builder.sort(CREATE_TIME + KEYWORD, SortOrder.ASC);
        // 设置搜索条件
        searchRequest.source(builder);
        builder.size(10000);

        List<String> list = new ArrayList<>();
        // 执行ES请求
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        if (searchResponse != null) {
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                String str = hit.getSourceAsString();
                list.add(str);
            }
        }
        return list;

    }

}
