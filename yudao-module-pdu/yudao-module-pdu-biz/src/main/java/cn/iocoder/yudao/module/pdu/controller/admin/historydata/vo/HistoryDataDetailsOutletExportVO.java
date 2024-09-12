package cn.iocoder.yudao.module.pdu.controller.admin.historydata.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - pdu历史数据详情 导出原始输出数据")
@Data
@ExcelIgnoreUnannotated
@HeadRowHeight(20)
@ColumnWidth(20)

public class HistoryDataDetailsOutletExportVO {
    @ExcelProperty("记录时间")
    private String create_time;

    @NumberFormat("0.00")
    @ExcelProperty("电流(A)")
    private Double cur_value;
    @ExcelProperty("有功功率(kW)")
    @NumberFormat("0.000")
    private Double pow_active;
    @NumberFormat("0.000")
    @ExcelProperty("视在功率(kVA)")
    private Double pow_apparent;
    @NumberFormat("0.00")
    @ExcelProperty("功率因素")
    private Double power_factor;
}