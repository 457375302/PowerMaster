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

public class BusExportDetailVO {

    @ExcelProperty("记录时间")
    private String create_time;
    @NumberFormat("0.000")
    @ExcelProperty("总有功功率(kW)")
    private Double pow_active;
    @NumberFormat("0.000")
    @ExcelProperty("总视在功率(kVA)")
    private Double pow_apparent;
    @NumberFormat("0.000")
    @ExcelProperty("总无功功率(kVar)")
    private Double pow_reactive;
    @NumberFormat("0.00")
    @ExcelProperty("功率因素")
    private Double power_factor;
//    @NumberFormat("0.00")
//    @ExcelProperty("剩余电流(A)")
//    private Double cur_residual;
//    @NumberFormat("0.00")
//    @ExcelProperty("零线电流(A)")
//    private Double cur_zero;
    @NumberFormat("0.00")
    @ExcelProperty("电流三相不平衡")
    private Double cur_unbalance;
    @NumberFormat("0.0")
    @ExcelProperty("电压三相不平衡")
    private Double vol_unbalance;


}