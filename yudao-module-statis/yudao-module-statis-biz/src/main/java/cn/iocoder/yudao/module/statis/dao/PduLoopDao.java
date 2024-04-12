package cn.iocoder.yudao.module.statis.dao;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.framework.common.entity.es.pdu.loop.PduHdaLoopBaseDo;
import cn.iocoder.yudao.framework.common.entity.es.pdu.loop.PduHdaLoopHourDo;
import cn.iocoder.yudao.framework.common.entity.es.pdu.loop.PduHdaLoopRealtimeDo;
import cn.iocoder.yudao.framework.common.enums.EsIndexEnum;
import cn.iocoder.yudao.framework.common.enums.EsStatisFieldEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.Min;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.constant.FieldConstant.*;

/**
 * @Author: chenwany
 * @Date: 2024/4/3 09:35
 * @Description: 回路历史数据统计
 */
@Slf4j
@Component
public class PduLoopDao {

    @Autowired
    RestHighLevelClient client;


    /**
     * 回路历史数据统计(按小时)
     *
     * @param startTime 统计开始时间
     * @param endTime   统计结束时间
     * @return
     */
    public Map<Object, Map<Object, PduHdaLoopBaseDo>> statisLoopHour(String startTime, String endTime) {
        Map<Object, Map<Object, PduHdaLoopBaseDo>> result = new HashMap<>();
        try {
            // 创建SearchRequest对象, 设置查询索引名
            SearchRequest searchRequest = new SearchRequest(EsIndexEnum.PDU_HDA_LOOP_REALTIME.getIndex());
            // 通过QueryBuilders构建ES查询条件，
            SearchSourceBuilder builder = new SearchSourceBuilder();

            //获取需要处理的数据
            builder.query(QueryBuilders.rangeQuery(CREATE_TIME + ".keyword").gte(startTime).lt(endTime));

//            builder.query(QueryBuilders.matchAllQuery());
            // 创建terms桶聚合，聚合名字=by_pdu, 字段=pdu_id，根据pdu_id分组
            TermsAggregationBuilder pduAggregationBuilder = AggregationBuilders.terms("by_pdu")
                    .field("pdu_id");

            // 设置Avg指标聚合，按loop_id分组
            TermsAggregationBuilder loopAggregationBuilder = AggregationBuilders.terms("by_loop").field(LOOP_ID);
            // 嵌套聚合
            // 设置聚合查询
            builder.aggregation(pduAggregationBuilder.subAggregation(loopAggregationBuilder
                    //统计平均电压
                    .subAggregation(AggregationBuilders.avg(VOL_AVG_VALUE).field(VOL))
                    //最大电压
                    .subAggregation(AggregationBuilders.max(VOL_MAX_VALUE).field(VOL))
                    //最小电压
                    .subAggregation(AggregationBuilders.min(VOL_MIN_VALUE).field(VOL))
                    //统计平均电流
                    .subAggregation(AggregationBuilders.avg(CUR_AVG_VALUE).field(CUR))
                    //最大电流
                    .subAggregation(AggregationBuilders.max(CUR_MAX_VALUE).field(CUR))
                    //最小电流
                    .subAggregation(AggregationBuilders.min(CUR_MIN_VALUE).field(CUR))
                    //平均有功功率
                    .subAggregation(AggregationBuilders.avg(ACTIVE_POW_AVG_VALUE).field(ACTIVE_POW))
                    //最大有功功率
                    .subAggregation(AggregationBuilders.max(ACTIVE_POW_MAX_VALUE).field(ACTIVE_POW))
                    // 最小有功功率
                    .subAggregation(AggregationBuilders.min(ACTIVE_POW_MIN_VALUE).field(ACTIVE_POW))
                    //平均视在功率
                    .subAggregation(AggregationBuilders.avg(APPARENT_POW_AVG_VALUE).field(APPARENT_POW))
                    //最大视在功率
                    .subAggregation(AggregationBuilders.max(APPARENT_POW_MAX_VALUE).field(APPARENT_POW))
                    //最小视在功率
                    .subAggregation(AggregationBuilders.min(APPARENT_POW_MIN_VALUE).field(APPARENT_POW))));

            // 设置搜索条件
            searchRequest.source(builder);
            // 如果只想返回聚合统计结果，不想返回查询结果可以将分页大小设置为0
//            builder.size(0);

            // 执行ES请求
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            //查询结果
            SearchHits hits = searchResponse.getHits();
            LinkedList<PduHdaLoopRealtimeDo> resList = new LinkedList<>();

            for (SearchHit hit : hits.getHits()) {
                String str = hit.getSourceAsString();
                PduHdaLoopRealtimeDo loopRealtimeDo = JsonUtils.parseObject(str, PduHdaLoopRealtimeDo.class);
                resList.add(loopRealtimeDo);
            }


            // 处理聚合查询结果
            Aggregations aggregations = searchResponse.getAggregations();
            // 根据by_pdu名字查询terms聚合结果
            Terms byPduAggregation = aggregations.get("by_pdu");


            // 遍历terms聚合结果
            for (Terms.Bucket bucket : byPduAggregation.getBuckets()) {
                // 获取按pduId分组
                Map<Object, PduHdaLoopBaseDo> dataMap = new HashMap<>();
                Terms byLoopAggregation = bucket.getAggregations().get("by_loop");
                //获取按loopId分组
                for (Terms.Bucket loopBucket : byLoopAggregation.getBuckets()) {
                    PduHdaLoopBaseDo loopBaseDo = new PduHdaLoopBaseDo();
                    loopBaseDo.setLoopId(Integer.parseInt(String.valueOf(loopBucket.getKey())));
                    loopBaseDo.setPduId(Integer.parseInt(String.valueOf(bucket.getKey())));
                    EsStatisFieldEnum.fields().forEach(field -> {
                        if (field.equals(VOL_AVG_VALUE)) {
                            Avg avg = loopBucket.getAggregations().get(field);
                            loopBaseDo.setVolAvgValue(((Double) avg.getValue()).floatValue());
                        }
                        if (field.equals(VOL_MAX_VALUE)) {
                            Max max = loopBucket.getAggregations().get(field);
                            loopBaseDo.setVolMaxValue(((Double) max.getValue()).floatValue());
                        }

                        if (field.equals(VOL_MIN_VALUE)) {
                            Min min = loopBucket.getAggregations().get(field);
                            loopBaseDo.setVolMinValue(((Double) min.getValue()).floatValue());
                        }
                        if (field.equals(CUR_AVG_VALUE)) {
                            Avg avg = loopBucket.getAggregations().get(field);
                            loopBaseDo.setCurAvgValue(((Double) avg.getValue()).floatValue());
                        }
                        if (field.equals(CUR_MAX_VALUE)) {
                            Max max = loopBucket.getAggregations().get(field);
                            loopBaseDo.setCurMaxValue(((Double) max.getValue()).floatValue());
                        }

                        if (field.equals(CUR_MIN_VALUE)) {
                            Min min = loopBucket.getAggregations().get(field);
                            loopBaseDo.setCurMinValue(((Double) min.getValue()).floatValue());
                        }
                        if (field.equals(ACTIVE_POW_AVG_VALUE)) {
                            Avg avg = loopBucket.getAggregations().get(field);
                            loopBaseDo.setActivePowAvgValue(((Double) avg.getValue()).floatValue());
                        }
                        if (field.equals(ACTIVE_POW_MAX_VALUE)) {
                            Max max = loopBucket.getAggregations().get(field);
                            loopBaseDo.setActivePowMaxValue(((Double) max.getValue()).floatValue());
                        }

                        if (field.equals(ACTIVE_POW_MIN_VALUE)) {
                            Min min = loopBucket.getAggregations().get(field);
                            loopBaseDo.setActivePowMinValue(((Double) min.getValue()).floatValue());
                        }
                        if (field.equals(APPARENT_POW_AVG_VALUE)) {
                            Avg avg = loopBucket.getAggregations().get(field);
                            loopBaseDo.setApparentPowAvgValue(((Double) avg.getValue()).floatValue());
                        }
                        if (field.equals(APPARENT_POW_MAX_VALUE)) {
                            Max max = loopBucket.getAggregations().get(field);
                            loopBaseDo.setApparentPowMaxValue(((Double) max.getValue()).floatValue());
                        }

                        if (field.equals(APPARENT_POW_MIN_VALUE)) {
                            Min min = loopBucket.getAggregations().get(field);
                            loopBaseDo.setApparentPowMinValue(((Double) min.getValue()).floatValue());
                        }
                    });
                    loopBaseDo.setCreateTime(DateTime.now());
                    dataMap.put(loopBucket.getKey(), loopBaseDo);
                }
                result.put(bucket.getKey(), dataMap);
            }

            //获取时间
            Map<Integer, List<PduHdaLoopRealtimeDo>> loopRealtimeDoMap = resList.stream().collect(Collectors.groupingBy(PduHdaLoopRealtimeDo::getPduId));
            loopRealtimeDoMap.keySet().forEach(pduId -> {
                List<PduHdaLoopRealtimeDo> loopRealtimeDos = loopRealtimeDoMap.get(pduId);
                Map<Integer, List<PduHdaLoopRealtimeDo>> loopMap = loopRealtimeDos.stream().collect(Collectors.groupingBy(PduHdaLoopRealtimeDo::getLoopId));
                loopMap.keySet().forEach(loopId -> {
                    List<PduHdaLoopRealtimeDo> list = loopMap.get(loopId);

                    PduHdaLoopBaseDo fieldMap = result.get(Long.parseLong(String.valueOf(pduId))).get(Long.parseLong(String.valueOf(loopId)));

                    Map<Float, DateTime> volMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopRealtimeDo::getVol,PduHdaLoopRealtimeDo::getCreateTime,(v1, v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));
                    Map<Float, DateTime> curMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopRealtimeDo::getCur,PduHdaLoopRealtimeDo::getCreateTime,(v1,v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));

                    Map<Float, DateTime> activePowMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopRealtimeDo::getActivePow,PduHdaLoopRealtimeDo::getCreateTime,(v1,v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));

                    Map<Float, DateTime> apparentPowMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopRealtimeDo::getApparentPow,PduHdaLoopRealtimeDo::getCreateTime,(v1,v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));
                    fieldMap.setCurMinTime(curMap.get(fieldMap.getCurMinValue()));
                    fieldMap.setCurMaxTime(curMap.get(fieldMap.getCurMaxValue()));
                    fieldMap.setVolMaxTime(volMap.get(fieldMap.getVolMaxValue()));
                    fieldMap.setVolMinTime(volMap.get(fieldMap.getVolMinValue()));
                    fieldMap.setActivePowMaxTime(activePowMap.get(fieldMap.getActivePowMaxValue()));
                    fieldMap.setActivePowMinTime(activePowMap.get(fieldMap.getActivePowMinValue()));
                    fieldMap.setApparentPowMaxTime(apparentPowMap.get(fieldMap.getApparentPowMaxValue()));
                    fieldMap.setApparentPowMinTime(apparentPowMap.get(fieldMap.getActivePowMinValue()));

                    result.get(Long.parseLong(String.valueOf(pduId))).put(loopId, fieldMap);
                });
            });
            log.info("--------------------" + result);

            return result;

        } catch (Exception e) {
            log.error("获取数据失败：", e);
        }
        return result;
    }

    /**
     * 回路历史数据统计(按天)
     *
     * @param startTime 统计开始时间
     * @param endTime   统计结束时间
     * @return
     */
    public Map<Object, Map<Object, PduHdaLoopBaseDo>> statisLoopDay(String startTime, String endTime) {
        Map<Object, Map<Object, PduHdaLoopBaseDo>> result = new HashMap<>();
        try {
            // 创建SearchRequest对象, 设置查询索引名
            SearchRequest searchRequest = new SearchRequest(EsIndexEnum.PDU_HDA_LOOP_HOUR.getIndex());
            // 通过QueryBuilders构建ES查询条件，
            SearchSourceBuilder builder = new SearchSourceBuilder();

            //获取需要处理的数据
            builder.query(QueryBuilders.rangeQuery(CREATE_TIME + ".keyword").gte(startTime).lt(endTime));

//            builder.query(QueryBuilders.matchAllQuery());
            // 创建terms桶聚合，聚合名字=by_pdu, 字段=pdu_id，根据pdu_id分组
            TermsAggregationBuilder pduAggregationBuilder = AggregationBuilders.terms("by_pdu")
                    .field("pdu_id");

            // 设置Avg指标聚合，按loop_id分组
            TermsAggregationBuilder loopAggregationBuilder = AggregationBuilders.terms("by_loop").field(LOOP_ID);
            // 嵌套聚合
            // 设置聚合查询
            builder.aggregation(pduAggregationBuilder.subAggregation(loopAggregationBuilder
                    //统计平均电压
                    .subAggregation(AggregationBuilders.avg(VOL_AVG_VALUE).field(VOL_AVG_VALUE))
                    //最大电压
                    .subAggregation(AggregationBuilders.max(VOL_MAX_VALUE).field(VOL_MAX_VALUE))
                    //最小电压
                    .subAggregation(AggregationBuilders.min(VOL_MIN_VALUE).field(VOL_MIN_VALUE))
                    //统计平均电流
                    .subAggregation(AggregationBuilders.avg(CUR_AVG_VALUE).field(CUR_AVG_VALUE))
                    //最大电流
                    .subAggregation(AggregationBuilders.max(CUR_MAX_VALUE).field(CUR_MAX_VALUE))
                    //最小电流
                    .subAggregation(AggregationBuilders.min(CUR_MIN_VALUE).field(CUR_MIN_VALUE))
                    //平均有功功率
                    .subAggregation(AggregationBuilders.avg(ACTIVE_POW_AVG_VALUE).field(ACTIVE_POW_AVG_VALUE))
                    //最大有功功率
                    .subAggregation(AggregationBuilders.max(ACTIVE_POW_MAX_VALUE).field(ACTIVE_POW_MAX_VALUE))
                    // 最小有功功率
                    .subAggregation(AggregationBuilders.min(ACTIVE_POW_MIN_VALUE).field(ACTIVE_POW_MIN_VALUE))
                    //平均视在功率
                    .subAggregation(AggregationBuilders.avg(APPARENT_POW_AVG_VALUE).field(APPARENT_POW_AVG_VALUE))
                    //最大视在功率
                    .subAggregation(AggregationBuilders.max(APPARENT_POW_MAX_VALUE).field(APPARENT_POW_MAX_VALUE))
                    //最小视在功率
                    .subAggregation(AggregationBuilders.min(APPARENT_POW_MIN_VALUE).field(APPARENT_POW_MIN_VALUE))));

            // 设置搜索条件
            searchRequest.source(builder);
            // 如果只想返回聚合统计结果，不想返回查询结果可以将分页大小设置为0
//            builder.size(0);

            // 执行ES请求
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            //查询结果
            SearchHits hits = searchResponse.getHits();
            LinkedList<PduHdaLoopHourDo> resList = new LinkedList<>();

            for (SearchHit hit : hits.getHits()) {
                String str = hit.getSourceAsString();
                PduHdaLoopHourDo loopRealtimeDo = JsonUtils.parseObject(str, PduHdaLoopHourDo.class);
                resList.add(loopRealtimeDo);
            }


            // 处理聚合查询结果
            Aggregations aggregations = searchResponse.getAggregations();
            // 根据by_pdu名字查询terms聚合结果
            Terms byPduAggregation = aggregations.get("by_pdu");


            // 遍历terms聚合结果
            for (Terms.Bucket bucket : byPduAggregation.getBuckets()) {
                // 获取按pduId分组
                Map<Object, PduHdaLoopBaseDo> dataMap = new HashMap<>();
                Terms byLoopAggregation = bucket.getAggregations().get("by_loop");
                //获取按loopId分组
                for (Terms.Bucket loopBucket : byLoopAggregation.getBuckets()) {
                    PduHdaLoopBaseDo loopBaseDo = new PduHdaLoopBaseDo();
                    loopBaseDo.setLoopId(Integer.parseInt(String.valueOf(loopBucket.getKey())));
                    loopBaseDo.setPduId(Integer.parseInt(String.valueOf(bucket.getKey())));
                    EsStatisFieldEnum.fields().forEach(field -> {
                        if (field.equals(VOL_AVG_VALUE)) {
                            Avg avg = loopBucket.getAggregations().get(field);
                            loopBaseDo.setVolAvgValue(((Double) avg.getValue()).floatValue());
                        }
                        if (field.equals(VOL_MAX_VALUE)) {
                            Max max = loopBucket.getAggregations().get(field);
                            loopBaseDo.setVolMaxValue(((Double) max.getValue()).floatValue());
                        }

                        if (field.equals(VOL_MIN_VALUE)) {
                            Min min = loopBucket.getAggregations().get(field);
                            loopBaseDo.setVolMinValue(((Double) min.getValue()).floatValue());
                        }
                        if (field.equals(CUR_AVG_VALUE)) {
                            Avg avg = loopBucket.getAggregations().get(field);
                            loopBaseDo.setCurAvgValue(((Double) avg.getValue()).floatValue());
                        }
                        if (field.equals(CUR_MAX_VALUE)) {
                            Max max = loopBucket.getAggregations().get(field);
                            loopBaseDo.setCurMaxValue(((Double) max.getValue()).floatValue());
                        }

                        if (field.equals(CUR_MIN_VALUE)) {
                            Min min = loopBucket.getAggregations().get(field);
                            loopBaseDo.setCurMinValue(((Double) min.getValue()).floatValue());
                        }
                        if (field.equals(ACTIVE_POW_AVG_VALUE)) {
                            Avg avg = loopBucket.getAggregations().get(field);
                            loopBaseDo.setActivePowAvgValue(((Double) avg.getValue()).floatValue());
                        }
                        if (field.equals(ACTIVE_POW_MAX_VALUE)) {
                            Max max = loopBucket.getAggregations().get(field);
                            loopBaseDo.setActivePowMaxValue(((Double) max.getValue()).floatValue());
                        }

                        if (field.equals(ACTIVE_POW_MIN_VALUE)) {
                            Min min = loopBucket.getAggregations().get(field);
                            loopBaseDo.setActivePowMinValue(((Double) min.getValue()).floatValue());
                        }
                        if (field.equals(APPARENT_POW_AVG_VALUE)) {
                            Avg avg = loopBucket.getAggregations().get(field);
                            loopBaseDo.setApparentPowAvgValue(((Double) avg.getValue()).floatValue());
                        }
                        if (field.equals(APPARENT_POW_MAX_VALUE)) {
                            Max max = loopBucket.getAggregations().get(field);
                            loopBaseDo.setApparentPowMaxValue(((Double) max.getValue()).floatValue());
                        }

                        if (field.equals(APPARENT_POW_MIN_VALUE)) {
                            Min min = loopBucket.getAggregations().get(field);
                            loopBaseDo.setApparentPowMinValue(((Double) min.getValue()).floatValue());
                        }
                    });
                    loopBaseDo.setCreateTime(DateTime.now());
                    dataMap.put(loopBucket.getKey(), loopBaseDo);
                }
                result.put(bucket.getKey(), dataMap);
            }

            //获取时间
            Map<Integer, List<PduHdaLoopHourDo>> loopRealtimeDoMap = resList.stream().collect(Collectors.groupingBy(PduHdaLoopHourDo::getPduId));
            loopRealtimeDoMap.keySet().forEach(pduId -> {
                List<PduHdaLoopHourDo> loopRealtimeDos = loopRealtimeDoMap.get(pduId);
                Map<Integer, List<PduHdaLoopHourDo>> loopMap = loopRealtimeDos.stream().collect(Collectors.groupingBy(PduHdaLoopHourDo::getLoopId));
                loopMap.keySet().forEach(loopId -> {
                    List<PduHdaLoopHourDo> list = loopMap.get(loopId);

                    PduHdaLoopBaseDo fieldMap = result.get(Long.parseLong(String.valueOf(pduId))).get(Long.parseLong(String.valueOf(loopId)));

                    Map<Float, DateTime> volMaxMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopHourDo::getVolMaxValue,PduHdaLoopHourDo::getVolMaxTime,(v1, v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));
                    Map<Float, DateTime> volMinMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopHourDo::getVolMinValue,PduHdaLoopHourDo::getVolMinTime,(v1, v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));

                    Map<Float, DateTime> curMaxMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopHourDo::getCurMaxValue,PduHdaLoopHourDo::getCurMaxTime,(v1,v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));
                    Map<Float, DateTime> curMinMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopHourDo::getCurMinValue,PduHdaLoopHourDo::getCurMinTime,(v1,v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));

                    Map<Float, DateTime> activePowMaxMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopHourDo::getActivePowMaxValue,PduHdaLoopHourDo::getActivePowMaxTime,(v1,v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));
                    Map<Float, DateTime> activePowMinMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopHourDo::getActivePowMinValue,PduHdaLoopHourDo::getActivePowMinTime,(v1,v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));

                    Map<Float, DateTime> apparentPowMaxMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopHourDo::getApparentPowMaxValue,PduHdaLoopHourDo::getApparentPowMaxTime,(v1,v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));
                    Map<Float, DateTime> apparentPowMinMap = list.stream().collect(Collectors
                            .toMap(PduHdaLoopHourDo::getApparentPowMinValue,PduHdaLoopHourDo::getApparentPowMinTime,(v1,v2) -> {
                                if (DateUtil.compare(v1 ,v2) < 0) {
                                    return v1;
                                }
                                return v2;
                            }));
                    fieldMap.setCurMinTime(curMinMap.get(fieldMap.getCurMinValue()));
                    fieldMap.setCurMaxTime(curMaxMap.get(fieldMap.getCurMaxValue()));
                    fieldMap.setVolMaxTime(volMaxMap.get(fieldMap.getVolMaxValue()));
                    fieldMap.setVolMinTime(volMinMap.get(fieldMap.getVolMinValue()));
                    fieldMap.setActivePowMaxTime(activePowMaxMap.get(fieldMap.getActivePowMaxValue()));
                    fieldMap.setActivePowMinTime(activePowMinMap.get(fieldMap.getActivePowMinValue()));
                    fieldMap.setApparentPowMaxTime(apparentPowMaxMap.get(fieldMap.getApparentPowMaxValue()));
                    fieldMap.setApparentPowMinTime(apparentPowMinMap.get(fieldMap.getActivePowMinValue()));

                    result.get(Long.parseLong(String.valueOf(pduId))).put(loopId, fieldMap);
                });
            });
            log.info("--------------------" + result);

            return result;

        } catch (Exception e) {
            log.error("获取数据失败：", e);
        }
        return result;
    }
    /**
     * 判断两个值是否相等
     *
     * @param value
     * @param esValue
     */
    private boolean equalsValue(float value, float esValue) {
        BigDecimal doubleValue = BigDecimal.valueOf(value);
        BigDecimal floatValue = BigDecimal.valueOf(esValue);

        log.info("doubleValue: " + doubleValue  + "floatValue : " + floatValue);
        if (doubleValue.equals(floatValue)) {
            return true;
        }

        return false;

    }
}
