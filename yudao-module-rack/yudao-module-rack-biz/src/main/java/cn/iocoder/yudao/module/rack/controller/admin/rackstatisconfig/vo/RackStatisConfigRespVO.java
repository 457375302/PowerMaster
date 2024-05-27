package cn.iocoder.yudao.module.rack.controller.admin.rackstatisconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 机架计算服务配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class RackStatisConfigRespVO {

    @Schema(description = "主键id", requiredMode = Schema.RequiredMode.REQUIRED, example = "4351")
    @ExcelProperty("主键id")
    private Integer id;

    @Schema(description = "计费方式 1固定计费 2分段计费", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("计费方式 1固定计费 2分段计费")
    private Integer billMode;

    @Schema(description = "按天统计历史数据任务")
    @ExcelProperty("按天统计历史数据任务")
    private String dayCron;

    @Schema(description = "按小时统计历史数据任务")
    @ExcelProperty("按小时统计历史数据任务")
    private String hourCron;

    @Schema(description = "电量按天统计任务")
    @ExcelProperty("电量按天统计任务")
    private String eqDayCron;

    @Schema(description = "电量按周执行任务")
    @ExcelProperty("电量按周执行任务")
    private String eqWeekCron;

    @Schema(description = "按月统计电量任务")
    @ExcelProperty("按月统计电量任务")
    private String eqMonthCron;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "存储任务")
    @ExcelProperty("存储任务")
    private String storeCron;

    @Schema(description = "redis key过期时间")
    @ExcelProperty("redis key过期时间")
    private Integer redisExpire;

    @Schema(description = "电能存储任务")
    @ExcelProperty("电能存储任务")
    private String eleStoreCron;

    @Schema(description = "redis保存定时")
    @ExcelProperty("redis保存定时")
    private String redisCron;

}