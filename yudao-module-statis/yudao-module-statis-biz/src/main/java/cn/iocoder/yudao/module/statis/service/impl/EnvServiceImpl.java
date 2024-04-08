package cn.iocoder.yudao.module.statis.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.framework.common.enums.EsIndexEnum;
import cn.iocoder.yudao.module.statis.dao.PduEnvDao;
import cn.iocoder.yudao.module.statis.dao.PduOutletDao;
import cn.iocoder.yudao.module.statis.entity.es.PduEnvBaseDo;
import cn.iocoder.yudao.module.statis.entity.es.PduHdaOutletBaseDo;
import cn.iocoder.yudao.module.statis.service.EnvService;
import cn.iocoder.yudao.module.statis.service.EsHandleService;
import cn.iocoder.yudao.module.statis.service.OutletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @Author: chenwany
 * @Date: 2024/4/3 14:40
 * @Description: 环境数据统计
 */
@Slf4j
@Service
public class EnvServiceImpl implements EnvService {

    @Autowired
    PduEnvDao envDao;

    @Autowired
    EsHandleService esHandleService;

    @Override
    public void hourDeal() {
        log.info("环境按小时数据统计");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        String startTime = DateUtil.formatDateTime(calendar.getTime());
        String endTime = DateUtil.formatDateTime(new Date());

        Map<Integer, Map<Integer, PduEnvBaseDo>> map = envDao.statis(startTime, endTime);
        List<PduEnvBaseDo> list = new ArrayList<>();
        map.keySet().forEach(pduId -> list.addAll(map.get(pduId).values()));
        list.forEach(t-> log.info("环境数据：" + t));
        esHandleService.batchInsert(list, EsIndexEnum.PDU_ENV_HOUR.getIndex());
    }

    @Override
    public void dayDeal() {
        log.info("环境数据按天统计");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -24);
        String startTime = DateUtil.formatDateTime(calendar.getTime());
        String endTime = DateUtil.formatDateTime(new Date());

        Map<Integer, Map<Integer, PduEnvBaseDo>> map = envDao.statis(startTime, endTime);
        List<PduEnvBaseDo> list = new ArrayList<>();
        map.keySet().forEach(pduId -> list.addAll(map.get(pduId).values()));
        esHandleService.batchInsert(list, EsIndexEnum.PDU_ENV_DAY.getIndex());
    }
}
