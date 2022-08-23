package com.min.demo.service;


import com.min.demo.dao.DeviceValueDao;
import com.min.demo.domain.DeviceValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DeviceValueService {

    Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private DeviceValueDao deviceValueDao;
    List<DeviceValue> list;

    public void insertOne(DeviceValue value){
        deviceValueDao.insertOne(value);
    }

    public void insertAll(List<DeviceValue> list){
//        deviceValueDao.insertList(list);
        this.list = new ArrayList<>(list);
        logger.info("成功插入数据{}条",list.size());
    }

    public List<DeviceValue> transport(){
        return list;
    }
}
