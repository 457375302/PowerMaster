package cn.iocoder.yudao.module.statis.dao;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.framework.common.entity.es.pdu.ele.outlet.PduEleOutletDo;
import cn.iocoder.yudao.framework.common.entity.es.pdu.ele.outlet.PduEqOutletBaseDo;
import cn.iocoder.yudao.framework.common.entity.es.pdu.ele.outlet.PduEqOutletDayDo;
import cn.iocoder.yudao.framework.common.entity.es.pdu.ele.total.PduEqTotalDayDo;
import cn.iocoder.yudao.framework.common.enums.EsIndexEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.statis.util.TimeUtil;
import cn.iocoder.yudao.module.statis.vo.EqBillConfigVo;
import lombok.extern.slf4j.Slf4j;
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
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.constant.FieldConstant.*;
import static cn.iocoder.yudao.module.statis.constant.Constants.*;

/**
 * @author Legrand
 * @version 1.0
 * @description: 输出位电量计算
 * @date 2024/4/10 13:40
 */
@Slf4j
@Component
public class PduEleOutletDao {

    @Autowired
    RestHighLevelClient client;



    /**
     *  电费计算 (按天统计)
     * @param billConfigVoList 电费计算配置
     */
    public List<PduEqOutletBaseDo> statisEleDay(List<EqBillConfigVo> billConfigVoList){
        List<PduEqOutletBaseDo> list = new ArrayList<>();

        try {
            billConfigVoList.forEach(billConfigVo -> {
                if (billConfigVo.getBillMode() == 1){
                    //固定计费
                    DateTime end = DateTime.now();
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.HOUR_OF_DAY, -24);
                    DateTime start = DateTime.of(calendar.getTime());

                    String startTime = DateUtil.formatDateTime(start) ;
                    String endTime = DateUtil.formatDateTime(end) ;

                    billConfigVo.setStartTime(startTime);
                    billConfigVo.setEndTime(endTime);
                }

                //获取时间段内第一条和最后一条数据
                Map<Integer, Map<Integer, PduEleOutletDo>> endMap = getEleData(billConfigVo,
                        SortOrder.DESC,
                        EsIndexEnum.PDU_ELE_OUTLET.getIndex());
                Map<Integer, Map<Integer,PduEleOutletDo>> startMap = getEleData(billConfigVo,
                        SortOrder.ASC,
                        EsIndexEnum.PDU_ELE_OUTLET.getIndex());
                endMap.keySet().forEach(pduId -> endMap.get(pduId).keySet().forEach(outletId ->{

                    PduEqOutletBaseDo dayDo = new PduEqOutletBaseDo();
                    //统计时间段
                    dayDo.setStartTime(DateUtil.parseDateTime(billConfigVo.getStartTime()));
                    dayDo.setEndTime(DateUtil.parseDateTime(billConfigVo.getEndTime()));
                    dayDo.setPduId(pduId);
                    dayDo.setOutletId(outletId);

                    PduEleOutletDo endRealtimeDo = endMap.get(pduId).get(outletId);
                    PduEleOutletDo startRealtimeDo = startMap.get(pduId).get(outletId);
                    //结束时间电量
                    double endEle = endRealtimeDo.getEle();
                    dayDo.setEndEle(endEle);
                    //开始时间电量
                    double startEle = startRealtimeDo.getEle();
                    dayDo.setStartEle(startEle);
                    //判断使用电量  开始电量大于结束电量，电量有清零操作，以结束电量为准
                    double eq ;
                    if (startEle>endEle){
                        eq = endEle;
                    }else {
                        eq = endEle - startEle;
                    }
                    dayDo.setEq(eq);
                    //电费计算 电量*该时间段计费
                    double bill = billConfigVo.getBill() * eq;
                    dayDo.setBill(bill);
                    dayDo.setCreateTime(DateTime.now());
                    dayDo.setBillPeriod(billConfigVo.getBillPeriod());
                    dayDo.setBillMode(billConfigVo.getBillMode());
                    list.add(dayDo);
                }));

            });

            return list;
        }catch (Exception e){
            log.error("计算异常：",e);
        }
        return list;
    }


    /**
     * @description:  获取ES中数据
     * @param configVo 时间段配置
     * @param sortOrder 排序
     * @param index  索引名称
     * @return Map<Integer, Map<Integer,PduEleOutletDo>>
     * @author luowei
     * @date: 2024/4/10 10:46
     */
    private Map<Integer, Map<Integer,PduEleOutletDo>> getEleData(EqBillConfigVo configVo,SortOrder sortOrder,String index){
        Map<Integer, Map<Integer,PduEleOutletDo>> dataMap = new HashMap<>();
        try {
            // 创建SearchRequest对象, 设置查询索引名
            SearchRequest searchRequest = new SearchRequest(index);
            // 通过QueryBuilders构建ES查询条件，
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //获取需要处理的数据
            builder.query(QueryBuilders.rangeQuery(CREATE_TIME + KEYWORD)
                    .gte(configVo.getStartTime())
                    .lt(configVo.getEndTime()));

            // 创建terms桶聚合，聚合名字=by_pdu, 字段=pdu_id，根据pdu_id分组
            TermsAggregationBuilder pduAggregationBuilder = AggregationBuilders.terms(BY_PDU)
                    .field(PDU_ID);
            // 设置Avg指标聚合，按outlet_id分组
            TermsAggregationBuilder outletAggregationBuilder = AggregationBuilders.terms(BY_OUTLET).field(OUTLET_ID);

            // 嵌套聚合
            // 设置聚合查询
            String top = "top";
            AggregationBuilder topAgg = AggregationBuilders.topHits(top)
                    .size(1).sort(CREATE_TIME +KEYWORD, sortOrder);

            builder.aggregation(pduAggregationBuilder.subAggregation(outletAggregationBuilder.subAggregation(topAgg)));

            // 设置搜索条件
            searchRequest.source(builder);

            // 执行ES请求
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            // 处理聚合查询结果
            Aggregations aggregations = searchResponse.getAggregations();
            // 根据by_pdu名字查询terms聚合结果
            Terms byPduAggregation = aggregations.get(BY_PDU);

            for (Terms.Bucket bucket : byPduAggregation.getBuckets()){
                Map<Integer,PduEleOutletDo> outletDoMap = new HashMap<>();
                Terms byOutletAggregation = bucket.getAggregations().get(BY_OUTLET);
                //获取按outlet_Id分组
                for (Terms.Bucket baseBucket : byOutletAggregation.getBuckets()) {
                    TopHits tophits = baseBucket.getAggregations().get(top);
                    SearchHits tophitsHits = tophits.getHits();
                    SearchHit hit = tophitsHits.getHits()[0];

                    PduEleOutletDo realtimeDo = JsonUtils.parseObject(hit.getSourceAsString(), PduEleOutletDo.class);
                    outletDoMap.put(Integer.parseInt(String.valueOf(baseBucket.getKey())),realtimeDo);
                }
                dataMap.put(Integer.parseInt(String.valueOf(bucket.getKey())),outletDoMap);

            }
            return dataMap;
        }catch (Exception e){
            log.error("获取数据异常：",e);
        }
        return dataMap;
    }



    /**
     *  电费计算 (按周统计)
     * @param billConfigVoList 电费计算配置
     */
    public  List<PduEqOutletBaseDo> statisEleWeek(List<EqBillConfigVo> billConfigVoList){
        List<PduEqOutletBaseDo> list = new ArrayList<>();

        try {
            billConfigVoList.forEach(billConfigVo -> {
                if (billConfigVo.getBillMode() == 1){
                    //固定计费
                    DateTime end = DateTime.now();
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_MONTH, -7);
                    DateTime start = DateTime.of(calendar.getTime());

                    String startTime = DateUtil.formatDateTime(start) ;
                    String endTime = DateUtil.formatDateTime(end) ;

                    billConfigVo.setStartTime(startTime);
                    billConfigVo.setEndTime(endTime);
                }

                //计算总量
                Map<Integer,Map<Integer,Map<String,Double>>> dataMap = getEleDataByDay(billConfigVo,
                        EsIndexEnum.PDU_EQ_OUTLET_DAY.getIndex());

                dataMap.keySet().forEach(pduId -> dataMap.get(pduId).keySet().forEach(outletId -> {
                    PduEqOutletBaseDo baseDo = new PduEqOutletBaseDo();
                    //统计时间段
                    baseDo.setStartTime(DateUtil.parseDateTime(billConfigVo.getStartTime()));
                    baseDo.setEndTime(DateUtil.parseDateTime(billConfigVo.getEndTime()));
                    baseDo.setPduId(pduId);
                    baseDo.setOutletId(outletId);

                    //结束时间电量
                    double endEle = dataMap.get(pduId).get(outletId).get(END_ELE);
                    baseDo.setEndEle(endEle);
                    //开始时间电量
                    double startEle = dataMap.get(pduId).get(outletId).get(START_ELE);
                    baseDo.setStartEle(startEle);
                    //电量集合
                    baseDo.setEq(dataMap.get(pduId).get(outletId).get(EQ_VALUE));
                    //电费集合
                    baseDo.setBill(dataMap.get(pduId).get(outletId).get(BILL_VALUE));
                    baseDo.setCreateTime(DateTime.now());
                    baseDo.setBillPeriod(billConfigVo.getBillPeriod());
                    baseDo.setBillMode(billConfigVo.getBillMode());
                    list.add(baseDo);
                }));

            });

            return list;
        }catch (Exception e){
            log.error("计算异常：",e);
        }
        return list;
    }



    /**
     *  电费计算 (按月统计)
     * @param billConfigVoList 电费计算配置
     */
    public  List<PduEqOutletBaseDo> statisEleMonth(List<EqBillConfigVo> billConfigVoList){
        List<PduEqOutletBaseDo> list = new ArrayList<>();

        try {
            billConfigVoList.forEach(billConfigVo -> {
                if (billConfigVo.getBillMode() == 1){
                    //固定计费
                    DateTime end = DateTime.now();
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MONTH, -1);
                    DateTime start = DateTime.of(calendar.getTime());

                    String startTime = DateUtil.formatDateTime(start) ;
                    String endTime = DateUtil.formatDateTime(end) ;

                    billConfigVo.setStartTime(startTime);
                    billConfigVo.setEndTime(endTime);
                }
                //计算总量
                Map<Integer,Map<Integer,Map<String,Double>>> dataMap = getEleDataByDay(billConfigVo,
                        EsIndexEnum.PDU_EQ_OUTLET_DAY.getIndex());

                dataMap.keySet().forEach(pduId -> dataMap.get(pduId).keySet().forEach(outletId -> {
                    PduEqOutletBaseDo baseDo = new PduEqOutletBaseDo();
                    //统计时间段
                    baseDo.setStartTime(DateUtil.parseDateTime(billConfigVo.getStartTime()));
                    baseDo.setEndTime(DateUtil.parseDateTime(billConfigVo.getEndTime()));
                    baseDo.setPduId(pduId);
                    baseDo.setOutletId(outletId);

                    //结束时间电量
                    double endEle = dataMap.get(pduId).get(outletId).get(END_ELE);
                    baseDo.setEndEle(endEle);
                    //开始时间电量
                    double startEle = dataMap.get(pduId).get(outletId).get(START_ELE);
                    baseDo.setStartEle(startEle);
                    //电量集合
                    baseDo.setEq(dataMap.get(pduId).get(outletId).get(EQ_VALUE));
                    //电费集合
                    baseDo.setBill(dataMap.get(pduId).get(outletId).get(BILL_VALUE));
                    baseDo.setCreateTime(DateTime.now());
                    baseDo.setBillPeriod(billConfigVo.getBillPeriod());
                    baseDo.setBillMode(billConfigVo.getBillMode());
                    list.add(baseDo);
                }));

            });

            return list;
        }catch (Exception e){
            log.error("计算异常：",e);
        }
        return list;
    }


    /**
     * @description:  获取ES中数据
     * @param configVo 时间段配置
     * @param index  索引名称
     * @return Map<Integer,PduEleTotalRealtimeDo>
     * @author luowei
     * @date: 2024/4/10 10:46
     */
    private Map<Integer,Map<Integer,Map<String,Double>>>  getEleDataByDay(EqBillConfigVo configVo, String index){
        Map<Integer,Map<Integer,Map<String,Double>>> dataMap = new HashMap<>();
        try {
            // 创建SearchRequest对象, 设置查询索引名
            SearchRequest searchRequest = new SearchRequest(index);
            // 通过QueryBuilders构建ES查询条件，
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //获取需要处理的数据
            builder.query(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery(CREATE_TIME + KEYWORD)
                            .gte(configVo.getStartTime())
                            .lt(configVo.getEndTime()))
                    .must(QueryBuilders.termQuery(BILL_MODE ,configVo.getBillMode()))
                    .must(QueryBuilders.termQuery(BILL_PERIOD + KEYWORD,configVo.getBillPeriod())));

            // 创建terms桶聚合，聚合名字=by_pdu, 字段=pdu_id，根据pdu_id分组
            TermsAggregationBuilder pduAggregationBuilder = AggregationBuilders.terms(BY_PDU)
                    .field(PDU_ID);
            // 嵌套聚合
            TermsAggregationBuilder outletAggregationBuilder = AggregationBuilders.terms(BY_OUTLET).field(OUTLET_ID);
            // 设置聚合查询
            builder.aggregation(pduAggregationBuilder.subAggregation(outletAggregationBuilder
                    .subAggregation(AggregationBuilders.sum(BILL_VALUE).field(BILL_VALUE))
                    .subAggregation(AggregationBuilders.sum(EQ_VALUE).field(EQ_VALUE)))
            );

            // 设置搜索条件
            searchRequest.source(builder);


            // 执行ES请求
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);


            SearchHits hits = searchResponse.getHits();
            LinkedList<PduEqOutletDayDo> resList = new LinkedList<>();

            for (SearchHit hit : hits.getHits()) {
                String str = hit.getSourceAsString();
                PduEqOutletDayDo dayDo = JsonUtils.parseObject(str, PduEqOutletDayDo.class);
                resList.add(dayDo);
            }

            Map<Integer,List<PduEqOutletDayDo>>  dayMap = resList.stream().collect(Collectors.groupingBy(PduEqOutletDayDo::getPduId));

            dayMap.keySet().forEach(pduId -> {
                Map<Integer,List<PduEqOutletDayDo>> outletMap = dayMap.get(pduId).stream().collect(Collectors.groupingBy(PduEqOutletDayDo::getOutletId));
                Map<Integer,Map<String,Double>> outMap = new HashMap<>();
                outletMap.keySet().forEach(outletId -> {
                    List<PduEqOutletDayDo> outletDayDos = outletMap.get(outletId).stream().sorted(Comparator.comparing(PduEqOutletDayDo::getCreateTime)).collect(Collectors.toList());
                    Map<String,Double> map = new HashMap<>();
                    map.put(START_ELE,outletDayDos.get(0).getStartEle());
                    map.put(END_ELE,outletDayDos.get(outletDayDos.size()-1).getEndEle());
                    outMap.put(outletId,map);
                });
                dataMap.put(pduId,outMap);
            });

            Map<Integer,Map<Integer,String>> startEleMap = getData(configVo,SortOrder.ASC,index);

            Map<Integer,Map<Integer,String>> endEleMap = getData(configVo,SortOrder.DESC,index);

            // 处理聚合查询结果
            Aggregations aggregations = searchResponse.getAggregations();
            // 根据by_pdu名字查询terms聚合结果
            Terms byPduAggregation = aggregations.get(BY_PDU);

            for (Terms.Bucket bucket : byPduAggregation.getBuckets()){
                Integer pudId = Integer.parseInt(String.valueOf(bucket.getKey()));
                Map<Integer,Map<String,Double>>  pduMap = new HashMap<>();
                Terms byOutletAggregation = bucket.getAggregations().get(BY_OUTLET);
                for (Terms.Bucket outBucket : byOutletAggregation.getBuckets()){
                    Integer outletId = Integer.parseInt(String.valueOf(outBucket.getKey()));
                    Sum bills  = outBucket.getAggregations().get(BILL_VALUE);
                    Sum  eqs = outBucket.getAggregations().get(EQ_VALUE);

                    PduEqOutletDayDo startDo = JsonUtils.parseObject(startEleMap.get(pudId).get(outletId),PduEqOutletDayDo.class);

                    PduEqOutletDayDo endDo = JsonUtils.parseObject(startEleMap.get(pudId).get(outletId),PduEqOutletDayDo.class);

                    Map<String,Double> map = new HashMap<>();
                    map.put(BILL_VALUE,bills.getValue());
                    map.put(EQ_VALUE,eqs.getValue());
                    map.put(START_ELE,startDo.getStartEle());
                    map.put(END_ELE,endDo.getEndEle());
                    pduMap.put(outletId,map);
                }
                dataMap.put(pudId,pduMap);

            }
            return dataMap;
        }catch (Exception e){
            log.error("获取数据异常：",e);
        }
        return dataMap;
    }


    /**
     * 获取最大/最小数据
     * @param sortOrder 升序或降序
     */
    private  Map<Integer,Map<Integer,String>>  getData(EqBillConfigVo configVo, SortOrder sortOrder, String index) throws IOException {
        Map<Integer,Map<Integer,String>>  dataMap = new HashMap<>();
        // 创建SearchRequest对象, 设置查询索引名
        SearchRequest searchRequest = new SearchRequest(index);
        // 通过QueryBuilders构建ES查询条件，
        SearchSourceBuilder builder = new SearchSourceBuilder();

        //获取需要处理的数据
        builder.query(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery(CREATE_TIME + KEYWORD)
                        .gte(configVo.getStartTime())
                        .lt(configVo.getEndTime()))
                .must(QueryBuilders.termQuery(BILL_MODE ,configVo.getBillMode()))
                .must(QueryBuilders.termQuery(BILL_PERIOD + KEYWORD,configVo.getBillPeriod())));

        // 创建terms桶聚合，聚合名字=by_pdu, 字段=pdu_id，根据pdu_id分组
        TermsAggregationBuilder pduAggregationBuilder = AggregationBuilders.terms(BY_PDU)
                .field(PDU_ID);
        // 嵌套聚合
        TermsAggregationBuilder outletAggregationBuilder = AggregationBuilders.terms(BY_OUTLET).field(OUTLET_ID);
        // 设置聚合查询
        String top = "top";
        AggregationBuilder topAgg = AggregationBuilders.topHits(top)
                .size(1).sort(CREATE_TIME + KEYWORD, sortOrder);

        builder.aggregation(pduAggregationBuilder.subAggregation(outletAggregationBuilder.subAggregation(topAgg)));

        // 设置搜索条件
        searchRequest.source(builder);

        // 执行ES请求
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        // 处理聚合查询结果
        Aggregations aggregations = searchResponse.getAggregations();
        // 根据by_pdu名字查询terms聚合结果
        Terms byPduAggregation = aggregations.get(BY_PDU);

        for (Terms.Bucket bucket : byPduAggregation.getBuckets()){
            Integer pduId = Integer.parseInt(String.valueOf(bucket.getKey()));

            Terms byOutletAggregation = bucket.getAggregations().get(BY_OUTLET);
            Map<Integer,String> outletDoMap = new HashMap<>();
            //获取按outlet_Id分组
            for (Terms.Bucket baseBucket : byOutletAggregation.getBuckets()) {
                TopHits tophits = bucket.getAggregations().get(top);
                SearchHits sophistsHits = tophits.getHits();
                SearchHit hit = sophistsHits.getHits()[0];

                outletDoMap.put(Integer.parseInt(String.valueOf(baseBucket.getKey())),hit.getSourceAsString());
            }
            dataMap.put(pduId,outletDoMap);
        }
        return dataMap;

    }

}