package cn.iocoder.yudao.module.bus.controller.admin.historydata.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 始端箱环境历史数据（实时数据） 导出数据")
@Data
@ExcelIgnoreUnannotated
public class BusTemHistoryDataVO {
    @ExcelProperty("记录时间")
    private String create_time;
    @NumberFormat("0.0")
    @ExcelProperty("A路温度(℃)")
    private Double tem_a;
    @NumberFormat("0.0")
    @ExcelProperty("B路温度(℃)")
    private Double tem_b;
    @NumberFormat("0.0")
    @ExcelProperty("C路温度(℃)")
    private Double tem_c;
    @NumberFormat("0.0")
    @ExcelProperty("中线温度(℃)")
    private Double tem_n;
}