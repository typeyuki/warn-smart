package com.warn.service.impl;

import com.warn.controller.SystemController;
import com.warn.dao.BedDao;
import com.warn.dao.DataDao;
import com.warn.dao.ThresholdDao;
import com.warn.dao.WarnHistoryDao;
import com.warn.dto.*;
import com.warn.dwr.Remote;
import com.warn.entity.*;
import com.warn.exception.NullFromDBException;
import com.warn.exception.WarnException;
import com.warn.mongodb.model.SensorCollection;
import com.warn.service.AlarmService;
import com.warn.service.SensorService;
import com.warn.service.WarnHistoryService;
import com.warn.util.Commons;
import com.warn.util.common.Const;
import org.apache.commons.codec.binary.Base64;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.xml.crypto.Data;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AlarmServiceImpl implements AlarmService {
    @Autowired
    WarnHistoryDao warnHistoryDao;
    @Autowired
    BedDao bedDao;
    @Autowired
    DataDao dataDao;
    @Autowired
    SensorService sensorService;
    @Autowired
    ThresholdDao thresholdDao;
    @Autowired
    WarnHistoryService warnHistoryService;

    private static Map<OldMan,Boolean> gravity = new HashMap<OldMan,Boolean>();
    private static Map<OldMan,String> gravityBed = new HashMap<OldMan,String>();
    private static Map<OldMan,ScheduledExecutorService> gravityTimer = new HashMap<OldMan,ScheduledExecutorService>();

    public Result addAlarmFaceTest(HttpServletRequest request) {
        String value1;
        WarnData warnData = new WarnData();
        Map<String,String> keyValue = new HashMap<String, String>();
        try {
            ServletInputStream inputStream = request.getInputStream();
            StringBuffer buffer = new StringBuffer();
            byte[] b = new byte[1024];
            int len = 0;
            while((len = inputStream.read(b) )!= -1){
                buffer.append(new String(b,0,len));
            }
            JSONObject json = JSONObject.fromObject(buffer.toString());
            String data = json.get("data").toString();
            // data = new String(data.getBytes("ISO8859-1"),"utf-8");
            byte[] base64 = Base64.decodeBase64(data.getBytes("utf-8"));
            PrivateKey privateKey = Commons.getPrivateKey(Const.privateKey);
            String value = Commons.decrypt(base64, privateKey);
            value1 = value;
            String parameter[] = value.split("&");
            for(int i=0;i < parameter.length;i++){
                String[] param = parameter[i].split("=");
                keyValue.put(param[0],param[1]);
            }

            String alarmTime = Commons.longToDate(new Long(keyValue.get("alarmTime")));
            AlarmFace alarmFace = new AlarmFace();
            alarmFace.setAlarmTime(alarmTime);
            alarmFace.setEquipIp(keyValue.get("equipIp"));
            alarmFace.setIdCard(keyValue.get("idCard"));
            alarmFace.setPicture(keyValue.get("picture"));
            alarmFace.setPosition(keyValue.get("position"));
            alarmFace.setName(keyValue.get("name"));
            DwrData dwrData = new DwrData();
            dwrData.setType("alarm_face");
            dwrData.setAlarmFace(alarmFace);
            warnHistoryService.addWarnHistory(dwrData);
//            Date d = new Date();
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
//            String dateNowStr = sdf.format(d);
//            warnData.setTimeW(dateNowStr);
//            OldMan oldMan = new OldMan();
//            oldMan.setOldName(keyValue.get("name"));
//            Warn warn = new Warn();
//            warn.setFlag(keyValue.get("position"));
//            warn.setInTime(keyValue.get("equipIp"));
//            DwrData dwrData = new DwrData();
//            dwrData.setType("face");
//            dwrData.setWarn(warn);
//            dwrData.setOldMan(oldMan);
//            warnHistoryDao.addWarnHistory(warnData);
//
            Remote.noticeNewOrder(dwrData);


        } catch (Exception e) {
            return new Result(false,e.getMessage());
        }

        return new Result(true, value1);
    }

    @Override
    public void AlarmGravity(SensorCollection sensorCollection) throws WarnException{
        try{
        final Bed bed = bedDao.getBedBySPId(sensorCollection.getSensorPointID());
        if(bed == null)
            throw new NullFromDBException("离床预警：找不到对应床位！");
        final OldMan oldMan = dataDao.getOldManByOid(bed.getOldMan().getOid());
        if(oldMan == null)
            throw new NullFromDBException("离床预警：找不到对应老人！");
        if(sensorCollection.getSensorData() == 15){
            if(gravityTimer.get(oldMan) != null){
                gravityTimer.get(oldMan).shutdown();
                gravityTimer.remove(oldMan);
            }

        }
        if(sensorCollection.getSensorData() == 240){
            String[] ctime = sensorCollection.getTime().split(" ");
            final String time = ctime[1];
            final ThresholdGravity thresholdGByOid = thresholdDao.getThresholdGByOid(oldMan.getOid());
            if(thresholdGByOid == null)
                throw new NullFromDBException("离床预警：找不到阈值！");
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Date d = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
                    String currentTime = sdf.format(d);
                    SystemController.logger.info("当前时间：" + currentTime + "   离床时间：" + time);
                    int value = sensorService.intervalTime(currentTime, time) / 60;//当前时间与最初时间的差值 单位分钟
                    SystemController.logger.info("老人已经离床：" + value + "分钟");
                    if(value >= thresholdGByOid.getThresholdGravity()){
                        if(moment_timeDeal(time,thresholdGByOid.getTimes()).isInTime() || moment_timeDeal(currentTime,thresholdGByOid.getTimes()).isInTime()){
                            AlarmGravity alarmGravity = new AlarmGravity();
                            alarmGravity.setBedNum(bed.getBedNum());
                            alarmGravity.setRoomNum(bed.getRoomNum());
                            alarmGravity.setTime(time + "-" + currentTime);
                            alarmGravity.setValue(value);
                            alarmGravity.setMapX(bed.getMapX());
                            alarmGravity.setMapY(bed.getMapY());
                            DwrData dwrData = new DwrData();
                            dwrData.setType("alarm_gravity");
                            dwrData.setAlarmGravity(alarmGravity);
                            warnHistoryService.addWarnHistory(dwrData);
                            Remote.noticeNewOrder(dwrData);
                        }
                    }
                }
            };
            ScheduledExecutorService service = Executors
                    .newSingleThreadScheduledExecutor();
            // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
            service.scheduleAtFixedRate(runnable, 1, 60, TimeUnit.SECONDS);
            gravityTimer.put(oldMan,service);
        }

    }catch(NullFromDBException ex1){
            throw ex1;
        }catch(Exception e){
            throw  new WarnException("gravity inner error" + e.getMessage());
        }

    }

    public MomentInTime moment_timeDeal(String moment, String time){
        MomentInTime momentInTime=new MomentInTime();
        String[] times=time.split("-");// /xx:xx-yy:yy
        //模型时间段位 20:30:00-06:30:00的情况
        if(times[1].compareTo(times[0])<0){
            if(moment.compareTo(times[0])>0||moment.compareTo(times[1])<0){
                momentInTime.setInTime(true);
//                //距结束的时长  因为规律模型是 hh:mm 手动构造 hh:mm:ss形式
//                momentInTime.setToEnd(intervalTime(times[1] + ":00", moment));
                //时间段 时长
                momentInTime.setTime(sensorService.intervalTime(times[1] + ":00", times[0] + ":00"));
            } else {
                momentInTime.setInTime(false);
            }
        }else {
            //直接比较字符串  不要拆分成 时分秒
            if (moment.compareTo(times[0]) >= 0 && moment.compareTo(times[1]) < 0) {
                momentInTime.setInTime(true);
//                //距结束的时长  因为规律模型是 hh:mm 手动构造 hh:mm:ss形式
//                momentInTime.setToEnd(intervalTime(times[1] + ":00", moment));
                //时间段 时长
                momentInTime.setTime(sensorService.intervalTime(times[1] + ":00", times[0] + ":00"));
            } else {
                momentInTime.setInTime(false);
            }
        }
        return momentInTime;
    }

}
