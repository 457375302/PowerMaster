package cn.iocoder.yudao.module.bus.controller.admin.historydata.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 母线插接箱 导出数据")
@Data
@ExcelIgnoreUnannotated
@ColumnWidth(20)
@HeadRowHeight(20)
public class BoxExportLineDetailVO {

    @ExcelProperty("记录时间")
    private String create_time;
    @NumberFormat("0.000")
    @ExcelProperty("有功功率(kW)")
    private Double pow_active;
    @NumberFormat("0.000")
    @ExcelProperty("无功功率(kW)")
    private Double pow_reactive;
    @NumberFormat("0.00")
    @ExcelProperty("功率因素")
    private Double power_factor;
    @NumberFormat("0.000")
    @ExcelProperty("视在功率(kVA)")
    private Double pow_apparent;
    @ExcelProperty("电压(V)")
    @NumberFormat("0.0")
    private Double vol_value;
    @ExcelProperty("电流(A)")
    @NumberFormat("0.00")
    private Double cur_value;
    @ExcelProperty("负载率")
    @NumberFormat("0.00")
    private Double load_rate;
    @ExcelProperty("电流谐波含量")
    @NumberFormat("0.0")
    private Double cur_thd;
}