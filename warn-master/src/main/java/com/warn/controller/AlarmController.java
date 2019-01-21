package com.warn.controller;

import com.warn.dto.Result;
import com.warn.exception.NullFromDBException;
import com.warn.mongodb.model.SensorCollection;
import com.warn.service.AlarmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * 预警管理
 */
@Controller
@RequestMapping("/alarm")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AlarmController {
    @Autowired
    AlarmService alarmService;


    @ResponseBody
    @RequestMapping(value = "/face", method = RequestMethod.POST)
    public Result faceAlarmTest(HttpServletRequest request){
        return alarmService.addAlarmFaceTest(request);
    }

    @ResponseBody
    @RequestMapping(value = "/sensorGravity", method = RequestMethod.GET)
    public Result sensorGravity(SensorCollection sensorCollection){
        SystemController.logger.info("离床预警"+"节点ID"+sensorCollection.getSensorPointID()+"数据"+sensorCollection.getSensorData());
    try{
       alarmService.AlarmGravity(sensorCollection);
       return new Result(true);
    }catch (NullFromDBException e){
        SystemController.logger.info("读取数据库Null值异常");
        SystemController.logger.info(e.getMessage());
        return new Result(false);
    }catch (Exception e){
        SystemController.logger.info("算法内部异常");
        SystemController.logger.info(e.getMessage());
        return new Result(false);
    }
    }
}
