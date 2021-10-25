package com.thirdegg.test;

import com.thirdegg.binco.Binco;

import java.util.List;

@Binco(id = 3)
public interface DataJava {

    @Binco.Field(id = 1)
    SubData getSubData();

    @Binco.Field(id = 2)
    List<SubData> getGoogles();
}
