package cn.iocoder.yudao.module.bus.controller.admin.busindex.vo;

import lombok.Data;

import java.util.List;

@Data
public class BusTemDetailRes {

    private List<Float> temAvgValueA;

    private List<Float> temAvgValueB;

    private List<Float> temAvgValueC;

    private List<Float> temAvgValueN;

    private List<String> temAvgTime;

}
