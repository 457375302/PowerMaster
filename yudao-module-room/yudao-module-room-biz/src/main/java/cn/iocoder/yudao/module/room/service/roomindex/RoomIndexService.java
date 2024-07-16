package cn.iocoder.yudao.module.room.service.roomindex;

import java.time.LocalDateTime;
import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.room.controller.admin.roomindex.vo.*;
import cn.iocoder.yudao.module.room.dal.dataobject.roomindex.RoomIndexDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 机房索引 Service 接口
 *
 * @author clever
 */
public interface RoomIndexService {

    /**
     * 创建机房索引
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Integer createIndex(@Valid RoomIndexSaveReqVO createReqVO);

    /**
     * 更新机房索引
     *
     * @param updateReqVO 更新信息
     */
    void updateIndex(@Valid RoomIndexSaveReqVO updateReqVO);

    /**
     * 删除机房索引
     *
     * @param id 编号
     */
    void deleteIndex(Integer id);

    /**
     * 获得机房索引
     *
     * @param id 编号
     * @return 机房索引
     */
    RoomIndexDO getIndex(Integer id);

    /**
     * 获得机房索引分页
     *
     * @param pageReqVO 分页查询
     * @return 机房索引分页
     */
    PageResult<RoomIndexDO> getIndexPage(RoomIndexPageReqVO pageReqVO);

    Map getReportConsumeDataById(Integer id, Integer timeType, LocalDateTime oldTime, LocalDateTime newTime);

    Map getReportPowDataById(Integer id, Integer timeType, LocalDateTime oldTime, LocalDateTime newTime);

    Map getRoomPFLine(Integer id, Integer timeType, LocalDateTime oldTime, LocalDateTime newTime);

    PageResult<RoomBalanceRes> getRoomBalancePage(RoomIndexPageReqVO pageReqVO);

    PageResult<RoomEQRes> getEqPage(RoomIndexPageReqVO pageReqVO);

    RoomActivePowDTO getActivePow(RoomPowVo vo);

    List<RoomEqTrendDTO> eqTrend(int id, String type);

    RoomEleChainDTO getEleChain(int id);
}