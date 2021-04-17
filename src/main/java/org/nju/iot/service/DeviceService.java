package org.nju.iot.service;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.nju.iot.dao.DeviceDao;
import org.nju.iot.dao.RequestLogDao;
import org.nju.iot.form.DeviceForm;
import org.nju.iot.model.DeviceEntity;
import org.nju.iot.model.RequestLogEntity;
import org.nju.iot.utils.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class DeviceService {
	@Autowired
	private DeviceDao deviceDao;
	@Autowired
	private RequestLogDao requestLogDao;

	private static final int QOS1 = 0;
	private static final int QOS2 = 1;
	private static final int QOS3 = 2;


	//添加设备
	public long addDevice(DeviceForm form) {
		DeviceEntity device = new DeviceEntity();
		device.setId(form.getId());
		device.setDeviceName(form.getDeviceName());
		device.setDeviceType(form.getDeviceType());
		device.setCreateTime(new Timestamp(System.currentTimeMillis()));
		device.setStatus(form.getStatus());
		device.setGroupId(form.getGroupId());
		device.setCredential(form.getCredential());
		deviceDao.save(device);

		DeviceManage.Verify(form.getId(),form.getCredential(),form.getDeviceType());
		//查看是否成功添加设备
		if(DeviceManage.hasDevice(form.getId())){
			System.out.println("设备添加成功");
			return form.getId();
		}
		else{
			System.out.println("设备添加失败");
			return -1;
		}
	}

	//设备接入
	public boolean deviceConnection(long deviceId, String deviceName, int type) {
		RequestLogEntity log = new RequestLogEntity();
		log.setId(deviceId);
		log.setDeviceId(deviceId);
		log.setRequestTime(new Timestamp(System.currentTimeMillis()));
		requestLogDao.save(log);
		return true;
	}

	//设备调试
	public void deviceTest(String status, long deviceId){
		setStatus(status, QOS1, deviceId);
	}

	//获取设备列表
	public List<DeviceEntity> getDeviceList() {
		return deviceDao.findAll();
	}

	//获取单个设备详情
	public DeviceEntity getDetail(long deviceId){
		return deviceDao.getOne(deviceId);
	}

	//设备影子
	public String getShadow(long deviceId) {
		return deviceDao.getOne(deviceId).getStatus();
	}

	//删除设备
	public boolean deleteDevice(long deviceId) {
		deviceDao.deleteById(deviceId);
		return true;
	}

	//更新设备影子表，向该设备对应get topic发送消息，通知设备更新状态
	private void setStatus(String status,int qos, long deviceId){
		//更新设备影子表
		DeviceEntity deviceEntity = deviceDao.getOne(deviceId);
		deviceEntity.setStatus(status);
		deviceDao.updateStatus(status,deviceId);

		//向该设备对应get topic发送消息，通知设备更新状态
		MqttService.publish(String.valueOf(deviceId),"/shadow/get/" + deviceId,status,qos);
	}

}
