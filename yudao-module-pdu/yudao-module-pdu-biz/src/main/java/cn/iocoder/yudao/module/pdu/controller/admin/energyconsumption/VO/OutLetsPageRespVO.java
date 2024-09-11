package cn.iocoder.yudao.module.pdu.controller.admin.energyconsumption.VO;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - PDU能耗排名 导出数据")
@Data
@ExcelIgnoreUnannotated
@HeadRowHeight(20)
public class OutLetsPageRespVO {
    @ExcelProperty({"开始电能","电能(kWh)"})
    @NumberFormat("0.0")
    private Double start_ele;
    @ExcelProperty({"开始电能","开始日期"})
    private String start_time;
    @ExcelProperty({"结束电能","电能(kWh)"})
    @NumberFormat("0.0")
    private Double end_ele;
    @ExcelProperty({"结束电能","结束日期"})
    private String end_time;
    @NumberFormat("0.0")
    @ExcelProperty({"耗电量","电能(kWh)"})
    private Double bill_value;
    @ExcelProperty({"耗电量","记录日期"})
    private String create_time;


}
