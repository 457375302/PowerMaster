package cn.iocoder.yudao.framework.common.entity.es.pdu.ele.total;

import cn.iocoder.yudao.module.statis.entity.BaseDo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author chenwany
 * @Date: 2024/3/28 14:47
 * @Description: pdu总电能表(实时)
 */
@Data
public class PduEleTotalRealtimeDo extends BaseDo {


    /**
     * 电能
     */
    @JsonProperty("ele_active")
    private double ele;


}
