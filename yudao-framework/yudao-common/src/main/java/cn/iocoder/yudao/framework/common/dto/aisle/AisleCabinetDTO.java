package cn.iocoder.yudao.framework.common.dto.aisle;

import cn.iocoder.yudao.framework.common.entity.mysql.rack.RackIndex;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author luowei
 * @version 1.0
 * @description: 机柜详情
 * @date 2024/4/28 15:27
 */
@Schema(description = "管理后台 - 机柜详情 Response VO")
@Data
public class AisleCabinetDTO {

    /**
     * 机柜id
     */
    @Schema(description = "机柜id", example = "1")
    private int id;
    /**
     * 机柜名称
     */
    @Schema(description = "机柜名称", example = "机柜123")
    private String cabinetName;


    /**
     * 电力容量
     */
    @Schema(description = "电力容量", example = "2")
    private float powCapacity;

    /**
     * 数据来源
     */
    @Schema(description = "数据来源 0：PDU 1：母线", example = "0")
    private int pduBox;


    /**
     * 机柜高度
     */
    @Schema(description = "机柜高度", example = "5")
    private int cabinetHeight;


    /**
     * 机柜类型
     */
    @Schema(description = "机柜类型", example = "IT机柜")
    private String type;
    /**
     * x
     */
    @Schema(description = "x坐标", example = "xxx")
    @JsonProperty(value="xCoordinate")
    private int xCoordinate;

    /**
     * y
     */
    @Schema(description = "y坐标", example = "yyy")
    @JsonProperty(value="yCoordinate")
    private int yCoordinate;

    /**
     * 所属于公司
     */
    @Schema(description = "所属于公司", example = "公司名")
    private String company;

    /**
     * 标记位
     */
    private Integer index;
}
