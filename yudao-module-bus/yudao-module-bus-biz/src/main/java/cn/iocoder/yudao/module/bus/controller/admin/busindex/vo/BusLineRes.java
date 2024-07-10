package cn.iocoder.yudao.module.bus.controller.admin.busindex.vo;

import lombok.Data;

@Data
public class BusLineRes  extends BusResBase {

    private Float L1MaxCur;

    private String L1MaxCurTime;

    private Float L1MaxVol;

    private String L1MaxVolTime;

    private Float L1MaxPow;

    private String L1MaxPowTime;

    private Float L2MaxCur;

    private String L2MaxCurTime;

    private Float L2MaxVol;

    private String L2MaxVolTime;

    private Float L2MaxPow;

    private String L2MaxPowTime;

    private Float L3MaxCur;

    private String L3MaxCurTime;

    private Float L3MaxVol;

    private String L3MaxVolTime;

    private Float L3MaxPow;

    private String L3MaxPowTime;

    private String Location;

    private Integer status;
}
