package cn.iocoder.yudao.module.bus.controller.admin.busindex.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class BusHarmonicLineRes {

    private List<LineSeries> series = new ArrayList<>();

    private List<String> time = new ArrayList<>();
}

