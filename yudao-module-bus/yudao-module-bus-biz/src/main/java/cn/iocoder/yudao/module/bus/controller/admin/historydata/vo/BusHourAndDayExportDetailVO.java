package cn.iocoder.yudao.module.bus.controller.admin.historydata.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 母线始端箱(实时数据) 导出数据")
@Data
@ExcelIgnoreUnannotated
@HeadStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
@ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
@ColumnWidth(30)
@HeadRowHeight(20)
public class BusHourAndDayExportDetailVO {

    @ExcelProperty("记录时间")
    private String create_time;
    @NumberFormat("0.000")
    @ExcelProperty("平均有功功率(kW)")
    private Double pow_active_avg_value;
    @NumberFormat("0.000")
    @ExcelProperty({"有功功率最大值","有功功率最大值(kW)"})
    private Double pow_active_max_value;
    @ExcelProperty({"有功功率最大值","发生时间"})
    private String pow_active_max_time;
    @NumberFormat("0.000")
    @ExcelProperty({"有功功率最小值","有功功率最小值(kW)"})
    private Double pow_active_min_value;
    @ExcelProperty({"有功功率最小值","发生时间"})
    private String pow_active_min_time;
    @NumberFormat("0.000")
    @ExcelProperty("平均无功功率(kVar)")
    private Double pow_reactive_avg_value;
    @NumberFormat("0.000")
    @ExcelProperty({"无功功率最大值","无功功率最大值(kVar)"})
    private Double pow_reactive_max_value;
    @ExcelProperty({"无功功率最大值","发生时间"})
    private String pow_reactive_max_time;
    @NumberFormat("0.000")
    @ExcelProperty({"无功功率最小值","无功功率最小值(kVar)"})
    private Double pow_reactive_min_value;
    @ExcelProperty({"无功功率最小值","发生时间"})
    private String pow_reactive_min_time;
    @NumberFormat("0.000")
    @ExcelProperty("平均视在功率(kVA)")
    private Double pow_apparent_avg_value;
    @NumberFormat("0.000")
    @ExcelProperty({"视在功率最大值","视在功率最大值(kVA)"})
    private Double pow_apparent_max_value;
    @ExcelProperty({"视在功率最大值","发生时间"})
    private String pow_apparent_max_time;
    @NumberFormat("0.000")
    @ExcelProperty({"视在功率最小值","视在功率最小值(kVA)"})
    private Double pow_apparent_min_value;
    @ExcelProperty({"视在功率最小值","发生时间"})
    private String pow_apparent_min_time;
    @ExcelProperty("平均剩余电流(A)")
    @NumberFormat("0.00")
    private Double cur_residual_avg_value;
    @NumberFormat("0.00")
    @ExcelProperty({"剩余电流最大值","剩余电流最大值(A)"})
    private Double cur_residual_max_value;
    @ExcelProperty({"剩余电流最大值","发生时间"})
    private String cur_residual_max_time;
    @NumberFormat("0.00")
    @ExcelProperty({"剩余电流最小值","剩余电流最小值(A)"})
    private Double cur_residual_min_value;
    @ExcelProperty({"剩余电流最小值","发生时间"})
    private String cur_residual_min_time;
    @ExcelProperty("平均零线电流(A)")
    @NumberFormat("0.00")
    private Double cur_zero_avg_value;
    @NumberFormat("0.00")
    @ExcelProperty({"零线电流最大值","零线电流最大值(A)"})
    private Double cur_zero_max_value;
    @ExcelProperty({"零线电流最大值","发生时间"})
    private String cur_zero_max_time;
    @NumberFormat("0.00")
    @ExcelProperty({"零线电流最小值","零线电流最小值(A)"})
    private Double cur_zero_min_value;
    @ExcelProperty({"零线电流最小值","发生时间"})
    private String cur_zero_min_time;

}