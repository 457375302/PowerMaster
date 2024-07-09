package cn.iocoder.yudao.module.aisle.service.impl;

import cn.iocoder.yudao.framework.common.dto.cabinet.CabinetDTO;
import cn.iocoder.yudao.framework.common.dto.cabinet.CabinetVo;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleBar;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleBox;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleCfg;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetBus;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetCfg;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetPdu;
import cn.iocoder.yudao.framework.common.entity.mysql.room.RoomIndex;
import cn.iocoder.yudao.framework.common.enums.DelEnums;
import cn.iocoder.yudao.framework.common.enums.DisableEnums;
import cn.iocoder.yudao.framework.common.enums.PduBoxEnums;
import cn.iocoder.yudao.framework.common.mapper.*;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.HttpUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.dto.aisle.AisleCabinetDTO;
import cn.iocoder.yudao.framework.common.dto.aisle.AisleDetailDTO;
import cn.iocoder.yudao.module.aisle.service.AisleService;
import cn.iocoder.yudao.framework.common.dto.aisle.AisleBarDTO;
import cn.iocoder.yudao.module.aisle.vo.AisleBusSaveVo;
import cn.iocoder.yudao.framework.common.dto.aisle.AisleSaveVo;
import cn.iocoder.yudao.module.cabinet.api.CabinetApi;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.constant.FieldConstant.*;

/**
 * @author luowei
 * @version 1.0
 * @description: 柜列操作
 * @date 2024/6/21 14:19
 */
@Slf4j
@Service
public class AisleServiceImpl implements AisleService {

    @Resource
    AisleIndexMapper aisleIndexMapper;
    @Resource
    AisleCfgMapper aisleCfgMapper;

    @Resource
    AisleBarMapper aisleBarMapper;

    @Resource
    AisleBoxMapper aisleBoxMapper;

    @Resource
    RedisTemplate redisTemplate;
    @Resource
    RoomIndexMapper roomIndexMapper;
    @Resource
    CabinetIndexMapper cabinetIndexMapper;
    @Resource
    CabinetPduMapper cabinetPduMapper;
    @Resource
    CabinetBusMapper cabinetBusMapper;
    @Resource
    CabinetCfgDoMapper cfgDoMapper;

    @Value("${aisle-refresh-url}")
    public String adder;

    @Resource
    CabinetApi cabinetApi;
    /**
     * 柜列保存
     * @param aisleSaveVo 保存参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer aisleSave(AisleSaveVo aisleSaveVo) {

        try {
            //柜列信息
            AisleIndex index = new AisleIndex();
            index.setName(aisleSaveVo.getAisleName());
            index.setLength(aisleSaveVo.getLength());
            index.setRoomId(aisleSaveVo.getRoomId());
            index.setType(aisleSaveVo.getType());
            index.setPduBar(aisleSaveVo.getPduBar());

            if (Objects.nonNull(aisleSaveVo.getId())){
                //编辑
                AisleIndex aisleIndex = aisleIndexMapper.selectOne(new LambdaQueryWrapper<AisleIndex>()
                        .eq(AisleIndex::getId,aisleSaveVo.getId()));
                if (Objects.nonNull(aisleIndex)){
                    index.setId(aisleSaveVo.getId());
                    aisleIndexMapper.updateById(index);

                    //修改配置表
                    AisleCfg aisleCfg = aisleCfgMapper.selectOne(new LambdaQueryWrapper<AisleCfg>()
                            .eq(AisleCfg::getAisleId,aisleIndex.getId()));
                    AisleCfg cfg = new AisleCfg();
                    cfg.setAisleId(aisleIndex.getId());
                    cfg.setDirection(aisleSaveVo.getDirection());
                    cfg.setXCoordinate(aisleSaveVo.getXCoordinate());
                    cfg.setYCoordinate(aisleSaveVo.getYCoordinate());

                    if (Objects.nonNull(aisleCfg)){
                        //修改
                        cfg.setId(aisleCfg.getId());
                        aisleCfgMapper.updateById(cfg);
                    }else {
                        aisleCfgMapper.insert(cfg);
                    }
                }

            }else {
                //新增
                aisleIndexMapper.insert(index);
                AisleCfg cfg = new AisleCfg();
                cfg.setAisleId(index.getId());
                cfg.setDirection(aisleSaveVo.getDirection());
                cfg.setXCoordinate(aisleSaveVo.getXCoordinate());
                cfg.setYCoordinate(aisleSaveVo.getYCoordinate());
                aisleCfgMapper.insert(cfg);
            }
            //母线信息
            List<AisleBar> aisleBars = aisleBarMapper.selectList(new LambdaQueryWrapper<AisleBar>()
                    .eq(AisleBar::getAisleId,index.getId()));
            List<AisleBarDTO> barVos = new ArrayList<>();
            if (Objects.nonNull(aisleSaveVo.getBarA())){
                barVos.add(aisleSaveVo.getBarA());
            }
            if (Objects.nonNull(aisleSaveVo.getBarB())){
                barVos.add(aisleSaveVo.getBarB());
            }

            if (!CollectionUtils.isEmpty(barVos)){
                if (!CollectionUtils.isEmpty(aisleBars)){
                    List<Integer> ids = aisleBars.stream().map(AisleBar::getId).collect(Collectors.toList());
                    ids.forEach(this::deleteBus);

                }
                AisleBusSaveVo busSaveVo = new AisleBusSaveVo();
                busSaveVo.setAisleId(index.getId());
                busSaveVo.setBarVos(barVos);
                aisleBusSave(busSaveVo);
            }else {
                //删除绑定关系
                if (!CollectionUtils.isEmpty(aisleBars)){
                    List<Integer> ids = aisleBars.stream().map(AisleBar::getId).collect(Collectors.toList());
                    ids.forEach(this::deleteBus);
                }
            }
            //机柜信息
            if (!CollectionUtils.isEmpty(aisleSaveVo.getCabinetList())){
                //删除
                List<Integer> ids =  aisleSaveVo.getCabinetList().stream().map(CabinetVo::getId).filter(id -> id >0).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(ids)){
                    cabinetIndexMapper.update(new LambdaUpdateWrapper<CabinetIndex>()
                            .eq(CabinetIndex::getIsDeleted,DelEnums.NO_DEL.getStatus())
                            .eq(CabinetIndex::getRoomId,aisleSaveVo.getRoomId())
                            .eq(CabinetIndex::getAisleId,index.getId())
                            .notIn(CabinetIndex::getId,ids)
                            .set(CabinetIndex::getIsDeleted,DelEnums.DELETE.getStatus()));
                }else {
                    cabinetIndexMapper.update(new LambdaUpdateWrapper<CabinetIndex>()
                            .eq(CabinetIndex::getIsDeleted,DelEnums.NO_DEL.getStatus())
                            .eq(CabinetIndex::getRoomId,aisleSaveVo.getRoomId())
                            .eq(CabinetIndex::getAisleId,index.getId())
                            .set(CabinetIndex::getIsDeleted,DelEnums.DELETE.getStatus()));
                }

                //新增/保存
                aisleSaveVo.getCabinetList().forEach(cabinetVo -> {
                    cabinetVo.setRoomId(aisleSaveVo.getRoomId());
                    cabinetVo.setAisleId(index.getId());
                    if (Objects.nonNull(cabinetVo.getIndex())
                            && StringUtils.isNotEmpty(aisleSaveVo.getDirection())
                            && "x".equals(aisleSaveVo.getDirection())){
                        //横向
                        cabinetVo.setXCoordinate(aisleSaveVo.getXCoordinate() + cabinetVo.getIndex() - 1);
                        cabinetVo.setYCoordinate(aisleSaveVo.getYCoordinate());
                    }
                    if (Objects.nonNull(cabinetVo.getIndex())
                            && StringUtils.isNotEmpty(aisleSaveVo.getDirection())
                            && "y".equals(aisleSaveVo.getDirection())){
                        //纵向
                        cabinetVo.setYCoordinate(aisleSaveVo.getYCoordinate() + cabinetVo.getIndex() - 1);
                        cabinetVo.setXCoordinate(aisleSaveVo.getXCoordinate());
                    }
                    try {
                        cabinetApi.saveCabinet(cabinetVo);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }else {
                cabinetIndexMapper.update(new LambdaUpdateWrapper<CabinetIndex>()
                        .eq(CabinetIndex::getIsDeleted,DelEnums.NO_DEL.getStatus())
                        .eq(CabinetIndex::getRoomId,aisleSaveVo.getRoomId())
                        .eq(CabinetIndex::getAisleId,index.getId())
                        .set(CabinetIndex::getIsDeleted,DelEnums.DELETE.getStatus()));
            }

            return index.getId();
        }finally {
            //刷新柜列计算服务缓存
            log.info("刷新计算服务缓存 --- " + adder);
            HttpUtil.get(adder);
        }

    }



    private Integer aisleSave2(AisleSaveVo aisleSaveVo) {

        try {
            AisleIndex index = new AisleIndex();
            index.setName(aisleSaveVo.getAisleName());
            index.setLength(aisleSaveVo.getLength());
            index.setRoomId(aisleSaveVo.getRoomId());
            index.setType(aisleSaveVo.getType());
            index.setPduBar(aisleSaveVo.getPduBar());

            if (Objects.nonNull(aisleSaveVo.getId())){
                //编辑
                AisleIndex aisleIndex = aisleIndexMapper.selectOne(new LambdaQueryWrapper<AisleIndex>()
                        .eq(AisleIndex::getId,aisleSaveVo.getId()));
                if (Objects.nonNull(aisleIndex)){
                    index.setId(aisleSaveVo.getId());
                    aisleIndexMapper.updateById(index);

                    //修改配置表
                    AisleCfg aisleCfg = aisleCfgMapper.selectOne(new LambdaQueryWrapper<AisleCfg>()
                            .eq(AisleCfg::getAisleId,aisleIndex.getId()));
                    AisleCfg cfg = new AisleCfg();
                    cfg.setAisleId(aisleIndex.getId());
                    cfg.setDirection(aisleSaveVo.getDirection());
                    cfg.setXCoordinate(aisleSaveVo.getXCoordinate());
                    cfg.setYCoordinate(aisleSaveVo.getYCoordinate());

                    if (Objects.nonNull(aisleCfg)){
                        //修改
                        cfg.setId(aisleCfg.getId());
                        aisleCfgMapper.updateById(cfg);
                        //柜列位置变动修改
                        if (aisleSaveVo.getXCoordinate() != aisleCfg.getXCoordinate()
                                || aisleSaveVo.getYCoordinate() != aisleCfg.getYCoordinate()
                                || !aisleSaveVo.getDirection().equals(aisleCfg.getDirection()) ){
                            //修改柜列下机柜
                            List<CabinetIndex> cabinetIndexList = cabinetIndexMapper.selectList(new LambdaQueryWrapper<CabinetIndex>()
                                    .eq(CabinetIndex::getAisleId,aisleIndex.getId())
                                    .eq(CabinetIndex::getIsDeleted,DelEnums.NO_DEL.getStatus())
                                    .eq(CabinetIndex::getIsDisabled,DisableEnums.ENABLE.getStatus()));

                            if (!CollectionUtils.isEmpty(cabinetIndexList)){
                                List<Integer> cabinetIds = cabinetIndexList.stream().map(CabinetIndex::getId).collect(Collectors.toList());
                                List<CabinetCfg> cabinetCfgList = cfgDoMapper.selectList(new LambdaQueryWrapper<CabinetCfg>()
                                        .in(CabinetCfg::getCabinetId,cabinetIds));
                                Map<Integer,CabinetCfg> cfgMap = cabinetCfgList.stream().collect(Collectors.toMap(CabinetCfg::getCabinetId,Function.identity()));
                                Map<Integer,Integer>  indexMap = new HashMap<>();
                                if ("x".equals(aisleCfg.getDirection())){
                                    //横向  计算机柜位置
                                    cabinetIds.forEach(id -> {
                                        Integer i = cfgMap.get(id).getXCoordinate()-aisleCfg.getXCoordinate();
                                        indexMap.put(id,i);
                                    });
                                }
                                if ("y".equals(aisleCfg.getDirection())){
                                    //纵向  计算机柜位置
                                    cabinetIds.forEach(id -> {
                                        Integer i = cfgMap.get(id).getYCoordinate()-aisleCfg.getYCoordinate();
                                        indexMap.put(id,i);
                                    });
                                }

                                //修改
                                cabinetCfgList.forEach(cabinetCfg ->{
                                    int x = 0;
                                    int y = 0;
                                    if ("x".equals(aisleSaveVo.getDirection())){
                                        //横向  计算机柜位置
                                        x = aisleSaveVo.getXCoordinate() + indexMap.get(cabinetCfg.getCabinetId());
                                        y = aisleSaveVo.getYCoordinate();
                                    }
                                    if ("y".equals(aisleSaveVo.getDirection())){
                                        //纵向  计算机柜位置
                                        y = aisleSaveVo.getYCoordinate() + indexMap.get(cabinetCfg.getCabinetId());
                                        x = aisleSaveVo.getXCoordinate();
                                    }
                                    cabinetCfg.setYCoordinate(y);
                                    cabinetCfg.setXCoordinate(x);
                                    cfgDoMapper.updateById(cabinetCfg);
                                });
                            }

                        }

                    }else {
                        aisleCfgMapper.insert(cfg);
                    }
                }

            }else {
                //新增
                aisleIndexMapper.insert(index);
                AisleCfg cfg = new AisleCfg();
                cfg.setAisleId(index.getId());
                cfg.setDirection(aisleSaveVo.getDirection());
                cfg.setXCoordinate(aisleSaveVo.getXCoordinate());
                cfg.setYCoordinate(aisleSaveVo.getYCoordinate());
                aisleCfgMapper.insert(cfg);
            }
            return index.getId();
        }finally {
            //刷新柜列计算服务缓存
            log.info("刷新计算服务缓存 --- " + adder);
            HttpUtil.get(adder);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void aisleBusSave(AisleBusSaveVo busSaveVo) {

        try {
            Integer aisleId = busSaveVo.getAisleId();

            if (!CollectionUtils.isEmpty(busSaveVo.getBarVos())){
                //绑定始端箱
                List<AisleBarDTO> barVos = busSaveVo.getBarVos();
                barVos.forEach(barVo -> {
                    AisleBar  bar = BeanUtils.toBean(barVo,AisleBar.class);
                    bar.setAisleId(aisleId);
                    bar.setBarKey(barVo.getDevIp() + SPLIT_KEY_BUS + barVo.getBusName());
                    AisleBar aisleBar = aisleBarMapper.selectById(bar.getId());
                    if (Objects.nonNull(aisleBar)){
                        aisleBarMapper.updateById(bar);
                    }else {
                        aisleBarMapper.insert(bar);
                    }

                    List<AisleBox> boxList = barVo.getBoxList();
                    if (!CollectionUtils.isEmpty(boxList)){
                        boxList.forEach(box ->{
                            box.setAisleId(aisleId);
                            box.setAisleBarId(bar.getId());
                            box.setBarKey(bar.getBarKey() + SPLIT_KEY_BUS + box.getBoxName());
                            AisleBox aisleBox = aisleBoxMapper.selectById(bar.getId());
                            if (Objects.nonNull(aisleBox)){
                                aisleBoxMapper.updateById(box);

                            }else {
                                aisleBoxMapper.insert(box);
                            }
                        });
                    }

                });

            }
        }finally {
            //刷新柜列计算服务缓存
            log.info("刷新计算服务缓存 --- " + adder);
            HttpUtil.get(adder);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchDeleteBox(List<Integer> boxIds) {
        try {
            if (!CollectionUtils.isEmpty(boxIds)){
                aisleBoxMapper.deleteBatchIds(boxIds);
            }
        }finally {
            log.info("刷新计算服务缓存 --- " + adder);
            HttpUtil.get(adder);
        }


    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteBus(Integer barId) {
        try {
            //删除母线需要先删除插接箱
            List<AisleBox>  boxList = aisleBoxMapper.selectList(new LambdaQueryWrapper<AisleBox>()
                    .eq(AisleBox::getAisleBarId,barId));
            if (!CollectionUtils.isEmpty(boxList)){
                aisleBoxMapper.delete(new LambdaQueryWrapper<AisleBox>()
                        .eq(AisleBox::getAisleBarId,barId));
            }
            aisleBarMapper.deleteById(barId);
        }finally {
            log.info("刷新计算服务缓存 --- " + adder);
            HttpUtil.get(adder);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteAisle(Integer aisleId) {
        try {
            //删除柜列
            AisleIndex aisleIndex = aisleIndexMapper.selectById(aisleId);
            if (Objects.nonNull(aisleIndex)){
                //逻辑删除
                if (aisleIndex.getIsDelete().equals(DelEnums.NO_DEL.getStatus())){
                    aisleIndexMapper.update(new LambdaUpdateWrapper<AisleIndex>()
                            .eq(AisleIndex::getId, aisleId)
                            .set(AisleIndex::getIsDelete, DelEnums.DELETE.getStatus()));

                }else {
                    //物理删除
                    //1.删除绑定关系
                    aisleBoxMapper.delete(new LambdaQueryWrapper<AisleBox>()
                            .eq(AisleBox::getAisleId,aisleId));
                    aisleBarMapper.delete(new LambdaQueryWrapper<AisleBar>()
                            .eq(AisleBar::getAisleId,aisleId));
                    //2.删除配置
                    aisleCfgMapper.delete(new LambdaQueryWrapper<AisleCfg>()
                            .eq(AisleCfg::getAisleId,aisleId));
                    //3.删除柜列
                    aisleIndexMapper.deleteById(aisleId);
                }
            }
            //删除key
            String key = REDIS_KEY_AISLE + aisleId;

            boolean flag = redisTemplate.delete(key);
            log.info("key: " + key + " flag : " + flag);
        }finally {
            log.info("刷新计算服务缓存 --- " + adder);
            HttpUtil.get(adder);
        }

    }

    @Override
    public AisleDetailDTO getAisleDetail(Integer aisleId) {
        AisleDetailDTO detailDTO = new AisleDetailDTO();

        AisleIndex aisleIndex = aisleIndexMapper.selectById(aisleId);

        AisleCfg aisleCfg = aisleCfgMapper.selectOne(new LambdaQueryWrapper<AisleCfg>()
                .eq(AisleCfg::getAisleId,aisleId));

        if (Objects.nonNull(aisleIndex)){
            detailDTO.setAisleName(aisleIndex.getName());
            detailDTO.setId(aisleId);
            detailDTO.setLength(aisleIndex.getLength());
            detailDTO.setType(aisleIndex.getType());
            detailDTO.setPduBar(aisleIndex.getPduBar());
            Integer roomId = aisleIndex.getRoomId();
            RoomIndex roomIndex = roomIndexMapper.selectById(roomId);
            if (Objects.nonNull(roomIndex)){
                detailDTO.setRoomName(roomIndex.getName());
                detailDTO.setRoomId(roomId);
            }

        }
        if (Objects.nonNull(aisleCfg)){
            detailDTO.setDirection(aisleCfg.getDirection());
            detailDTO.setXCoordinate(aisleCfg.getXCoordinate());
            detailDTO.setYCoordinate(aisleCfg.getYCoordinate());
        }

        //母线
        List<AisleBar>  aisleBars = aisleBarMapper.selectList(new LambdaQueryWrapper<AisleBar>()
                .eq(AisleBar::getAisleId,aisleId));
        if (!CollectionUtils.isEmpty(aisleBars)){
            aisleBars.forEach(aisleBar -> {
                AisleBarDTO barVo = BeanUtils.toBean(aisleBar, AisleBarDTO.class);
                List<AisleBox> boxList = aisleBoxMapper.selectList(new LambdaQueryWrapper<AisleBox>()
                        .eq(AisleBox::getAisleBarId,aisleBar.getId()));
                barVo.setBoxList(boxList);
                if ("A".equals(aisleBar.getPath())){
                    detailDTO.setBarA(barVo);
                }
                if ("B".equals(aisleBar.getPath())){
                    detailDTO.setBarB(barVo);
                }
            });
        }

        //机柜
        List<CabinetIndex> cabinetIndexList = cabinetIndexMapper.selectList(new LambdaQueryWrapper<CabinetIndex>()
                .eq(CabinetIndex::getAisleId,aisleId)
                .eq(CabinetIndex::getIsDeleted,DelEnums.NO_DEL.getStatus())
                .eq(CabinetIndex::getIsDisabled, DisableEnums.ENABLE.getStatus()));
        List<CabinetDTO> aisleCabinetDTOList = new ArrayList<>();
       if (!CollectionUtils.isEmpty(cabinetIndexList)){
           cabinetIndexList.forEach(cabinetIndex ->{
               CabinetDTO cabinetDTO = cabinetApi.getDetail(cabinetIndex.getId());
               if ("x".equals(aisleCfg.getDirection())){
                   //横向
                   cabinetDTO.setIndex(cabinetDTO.getXCoordinate() - aisleCfg.getXCoordinate() + 1);
               }
               if ("y".equals(aisleCfg.getDirection())){
                   //纵向
                   cabinetDTO.setIndex(cabinetDTO.getYCoordinate() - aisleCfg.getYCoordinate() + 1);
               }
               aisleCabinetDTOList.add(cabinetDTO);
           });
       }
       detailDTO.setCabinetList(aisleCabinetDTOList);
       return detailDTO;
    }




}
