package com.min.demo.dao;


import com.min.demo.domain.DeviceValue;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceValueDao {

    @Insert("insert into  device_datas values(#{time},#{name},#{timeseries.Temperature},#{timeseries.Humidity})")
    public void insertOne(DeviceValue value);

    @Insert("<script>"  +
            "insert into device_datas VALUES " +
            "<foreach collection='list' item='item' index='index' separator=','> " +
            "(#{item.time},#{item.name},#{item.timeseries.Temperature},#{item.timeseries.Humidity}) " +
            "</foreach>" +
            "</script>")
    public void insertList(@Param("list") List<DeviceValue> list);
}
