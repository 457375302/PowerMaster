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

@Schema(description = "管理后台 - 母线始端箱（小时、天数据） 导出数据")
@Data
@ExcelIgnoreUnannotated
@HeadStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
@ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
@ColumnWidth(30)
@HeadRowHeight(20)
public class BusHourAndDayPageRespVO {

    @ExcelProperty("母线名称")
    private String bus_name;

    @ExcelProperty("位置")
    private String location;

    @ExcelProperty("IP地址")
    private String dev_key;


    @ExcelProperty("记录时间")
    private String create_time;

    @ExcelProperty("平均有功功率(kW)")
    @NumberFormat("0.000")
    private Double pow_active_avg_value;

    @ExcelProperty("最大有功功率时间")
    private String pow_active_max_time;
    @NumberFormat("0.000")

    @ExcelProperty("最大有功功率(kW)")
    private Double pow_active_max_value;

    @ExcelProperty("最小有功功率时间")
    private String pow_active_min_time;
    @NumberFormat("0.000")

    @ExcelProperty("最小有功功率(kW)")
    private Double pow_active_min_value;
    @NumberFormat("0.000")

    @ExcelProperty("平均无功功率(kVar)")
    private Double pow_reactive_avg_value;

    @ExcelProperty("最大无功功率时间")
    private String pow_reactive_max_time;
    @NumberFormat("0.000")

    @ExcelProperty("最大无功功率(kVar)")
    private Double pow_reactive_max_value;

    @ExcelProperty("最小无功功率时间")
    private String pow_reactive_min_time;
    @NumberFormat("0.000")

    @ExcelProperty("最小无功功率(kVar)")
    private Double pow_reactive_min_value;
    @NumberFormat("0.000")

    @ExcelProperty("平均视在功率(kVA)")
    private Double pow_apparent_avg_value;

    @ExcelProperty("最大视在功率时间")
    private String pow_apparent_max_time;
    @NumberFormat("0.000")
    @ExcelProperty("最大视在功率(kVA)")
    private Double pow_apparent_max_value;

    @ExcelProperty("最小视在功率时间")
    private String pow_apparent_min_time;
    @NumberFormat("0.000")

    @ExcelProperty("最小视在功率(kVA)")
    private Double pow_apparent_min_value;
    @NumberFormat("0.00")
    @ExcelProperty("平均剩余电流(A)")
    private Double cur_residual_avg_value;

    @ExcelProperty("最大剩余电流时间")
    private String cur_residual_max_time;
    @NumberFormat("0.00")
    @ExcelProperty("最大剩余电流(A)")
    private Double cur_residual_max_value;

    @ExcelProperty("最小剩余电流时间")
    private String cur_residual_min_time;
    @NumberFormat("0.00")
    @ExcelProperty("最小剩余电流(A)")
    private Double cur_residual_min_value;
    @NumberFormat("0.00")
    @ExcelProperty("平均零线电流(A)")
    private Double cur_zero_avg_value;

    @ExcelProperty("最大零线电流时间")
    private String cur_zero_max_time;
    @NumberFormat("0.00")
    @ExcelProperty("最大零线电流(A)")
    private Double cur_zero_max_value;

    @ExcelProperty("最小零线电流时间")
    private String cur_zero_min_time;
    @NumberFormat("0.00")
    @ExcelProperty("最小零线电流(A)")
    private Double cur_zero_min_value;

    // 相
    @ExcelProperty("相")
    private String line_id;
    @NumberFormat("0.0")
    @ExcelProperty("平均电压(V)")
    private Double vol_avg_value;

    @ExcelProperty("最大电压时间")
    private String vol_max_time;
    @NumberFormat("0.0")
    @ExcelProperty("最大电压(V)")
    private Double vol_max_value;

    @ExcelProperty("最小电压时间")
    private String vol_min_time;
    @NumberFormat("0.0")
    @ExcelProperty("最小电压(V)")
    private Double vol_min_value;
    @NumberFormat("0.00")
    @ExcelProperty("平均电流(A)")
    private Double cur_avg_value;

    @ExcelProperty("最大电流时间")
    private String cur_max_time;
    @NumberFormat("0.00")
    @ExcelProperty("最大电流(A)")
    private Double cur_max_value;

    @ExcelProperty("最小电流时间")
    private String cur_min_time;
    @NumberFormat("0.00")
    @ExcelProperty("最小电流(A)")
    private Double cur_min_value;
    @NumberFormat("0.0")
    @ExcelProperty("平均线电压(V)")
    private Double vol_line_avg_value;

    @ExcelProperty("最大线电压时间")
    private String vol_line_max_time;
    @NumberFormat("0.0")
    @ExcelProperty("最大线电压(V)")
    private Double vol_line_max_value;

    @ExcelProperty("最小线电压时间")
    private String vol_line_min_time;
    @NumberFormat("0.0")
    @ExcelProperty("最小线电压(V)")
    private Double vol_line_min_value;

}