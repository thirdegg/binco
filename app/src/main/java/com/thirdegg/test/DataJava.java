package com.thirdegg.test;

import com.thirdegg.binco.Binco;

import java.util.List;

@Binco(id = 5)
public interface DataJava {

    @Binco.Field(id = 1)
    Data.Status.SubData getSubData();

    @Binco.Field(id = 2)
    List<Data.Status.SubData> getGoogles();
}
