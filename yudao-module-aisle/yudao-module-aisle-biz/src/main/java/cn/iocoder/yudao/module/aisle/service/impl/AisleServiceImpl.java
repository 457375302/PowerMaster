package cn.iocoder.yudao.module.aisle.service.impl;

import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleBar;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleBox;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleCfg;
import cn.iocoder.yudao.framework.common.entity.mysql.aisle.AisleIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetCfg;
import cn.iocoder.yudao.framework.common.entity.mysql.cabinet.CabinetIndex;
import cn.iocoder.yudao.framework.common.entity.mysql.room.RoomIndex;
import cn.iocoder.yudao.framework.common.enums.DelEnums;
import cn.iocoder.yudao.framework.common.enums.DisableEnums;
import cn.iocoder.yudao.framework.common.mapper.*;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.dto.aisle.AisleCabinetDTO;
import cn.iocoder.yudao.framework.common.dto.aisle.AisleDetailDTO;
import cn.iocoder.yudao.module.aisle.service.AisleService;
import cn.iocoder.yudao.framework.common.dto.aisle.AisleBarDTO;
import cn.iocoder.yudao.module.aisle.vo.AisleBusSaveVo;
import cn.iocoder.yudao.module.aisle.vo.AisleSaveVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
    CabinetCfgMapper cabinetCfgMapper;

    /**
     * 柜列保存
     * @param aisleSaveVo 保存参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer aisleSave(AisleSaveVo aisleSaveVo) {

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
                            List<CabinetCfg> cabinetCfgList = cabinetCfgMapper.selectList(new LambdaQueryWrapper<CabinetCfg>()
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
                                    y = aisleSaveVo.getYCoordinate() + indexMap.get(cabinetCfg.getId());
                                    x = aisleSaveVo.getXCoordinate();
                                }
                                cabinetCfg.setYCoordinate(y);
                                cabinetCfg.setXCoordinate(x);
                                cabinetCfgMapper.updateById(cabinetCfg);
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
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void aisleBusSave(AisleBusSaveVo busSaveVo) {

        Integer aisleId = busSaveVo.getAisleId();

        if (!CollectionUtils.isEmpty(busSaveVo.getBarVos())){
            //绑定始端箱
            List<AisleBarDTO> barVos = busSaveVo.getBarVos();
            barVos.forEach(barVo -> {
                AisleBar  bar = BeanUtils.toBean(barVo,AisleBar.class);
                bar.setAisleId(aisleId);
                bar.setBarKey(barVo.getDevIp() + SPLIT_KEY_BUS + barVo.getBusName());
                if (bar.getId()>0){
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

                        if (box.getId()>0){
                            aisleBoxMapper.updateById(box);

                        }else {
                            aisleBoxMapper.insert(box);
                        }
                    });
                }

            });

        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchDeleteBox(List<Integer> boxIds) {
        if (!CollectionUtils.isEmpty(boxIds)){
            aisleBoxMapper.deleteBatchIds(boxIds);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteBus(Integer barId) {
        //删除母线需要先删除插接箱
        List<AisleBox>  boxList = aisleBoxMapper.selectList(new LambdaQueryWrapper<AisleBox>()
                .eq(AisleBox::getAisleBarId,barId));
        if (!CollectionUtils.isEmpty(boxList)){
            aisleBoxMapper.delete(new LambdaQueryWrapper<AisleBox>()
                    .eq(AisleBox::getAisleBarId,barId));
        }
        aisleBarMapper.deleteById(barId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteAisle(Integer aisleId) {
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
        List<AisleCabinetDTO> aisleCabinetDTOList = new ArrayList<>();
       if (!CollectionUtils.isEmpty(cabinetIndexList)){
           List<Integer> cabinetIds = cabinetIndexList.stream().map(CabinetIndex::getId).collect(Collectors.toList());
           List<CabinetCfg> cabinetCfgList = cabinetCfgMapper.selectList(new LambdaQueryWrapper<CabinetCfg>()
                   .in(CabinetCfg::getCabinetId,cabinetIds));
           Map<Integer,CabinetCfg> cfgMap;
           if (!CollectionUtils.isEmpty(cabinetCfgList)){
                cfgMap = cabinetCfgList.stream().collect(Collectors.toMap(CabinetCfg::getCabinetId, Function.identity()));
           } else {
               cfgMap = new HashMap<>();
           }

           cabinetIndexList.forEach(cabinetIndex ->{
               AisleCabinetDTO cabinetDTO = BeanUtils.toBean(cabinetIndex,AisleCabinetDTO.class);
               CabinetCfg cfg = cfgMap.get(cabinetIndex.getId());
               if (Objects.nonNull(cfg)){
                   cabinetDTO.setCabinetName(cfg.getCabinetName());
                   cabinetDTO.setCabinetHeight(cfg.getCabinetHeight());
                   cabinetDTO.setCompany(cfg.getCompany());
                   cabinetDTO.setXCoordinate(cfg.getXCoordinate());
                   cabinetDTO.setYCoordinate(cfg.getYCoordinate());
                   cabinetDTO.setType(cfg.getType());
                   if ("x".equals(aisleCfg.getDirection())){
                       //横向
                       cabinetDTO.setIndex(cfg.getXCoordinate() - aisleCfg.getXCoordinate() + 1);
                   }
                   if ("y".equals(aisleCfg.getDirection())){
                       //纵向
                       cabinetDTO.setIndex(cfg.getYCoordinate() - aisleCfg.getYCoordinate() + 1);
                   }
               }
               aisleCabinetDTOList.add(cabinetDTO);

           });
       }
       detailDTO.setCabinetList(aisleCabinetDTOList);
       return detailDTO;
    }
}
