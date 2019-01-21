package com.warn.service;

import com.warn.dto.Result;
import com.warn.mongodb.model.SensorCollection;

import javax.servlet.http.HttpServletRequest;

public interface AlarmService {
    Result addAlarmFaceTest(HttpServletRequest request);

    void AlarmGravity(SensorCollection sensorCollection);

}
