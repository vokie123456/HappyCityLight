/** 
 * Copyright (C) 2016 City Digital Pulse - All Rights Reserved
 *  
 * Author: Zhongli Li
 *  
 * Design: Zhongli Li and Shiai Zhu
 *  
 * Concept and supervision Abdulmotaleb El Saddik
 *
 */
package com.citydigitalpulse.collector.TwitterGetter.service;

import java.util.ArrayList;

import com.citydigitalpulse.collector.TwitterGetter.dao.InfoGetterDAO;
import com.citydigitalpulse.collector.TwitterGetter.dao.impl.InfoGetterDAO_MySQL;
import com.citydigitalpulse.collector.TwitterGetter.model.EarthSqure;
import com.citydigitalpulse.collector.TwitterGetter.model.RegInfo;

/**
 * 扫描地区表并建立区块表的线程
 * 
 * @author zhonglili
 *
 */
public class ScanRegsThread extends ServiceThread {
	private int time;
	private boolean isRunning = false;
	private InfoGetterDAO db;

	public ScanRegsThread(int time) {
		this.time = time;
		this.settName(this.getClass().getSimpleName());
		this.db = new InfoGetterDAO_MySQL();
	}

	@Override
	public void run() {
		super.run();
		isRunning = true;
		ArrayList<RegInfo> regs;
		// 循环扫描
		while (isRunning) {
			// 1.扫描reg数据库，获取所有状态为0的区域并将所有的reg区域的小区域添加
			regs = db.getRegInfo(0);
			// 2.根据小区域的坐标向数据库中添加Stream区块
			for (int i = 0; i < regs.size(); i++) {
				ArrayList<EarthSqure> ess = (ArrayList<EarthSqure>) db
						.getStreamSqures(regs.get(i));
				for (int j = 0; j < ess.size(); j++) {
					// 在确定搜索区域大小后再存入数据库
					db.saveEarthSqure(ess.get(j));
				}
				// 修改大区域状态为待监听状态（3）
				db.changeRegState(regs.get(i).getRegID(), 3);
			}
			// 如果有新增区域则开始处理线程
			if (regs.size() > 0) {
				StartRegThreads sSqureThread = new StartRegThreads();
				sSqureThread.start();
				System.out.println(sSqureThread.gettName() + " Begining..");
			}
			try {
				sleep(time);
			} catch (InterruptedException e) {
				isRunning = false;
				// e.printStackTrace();
			}
		}
		System.out.println("Thread:" + this.gettName() + " End...");
	}

	/**
	 * 终止线程的方法
	 */
	@Override
	public void stopMe() {
		isRunning = false;
		if (this.isAlive()) {
			super.interrupt();
		}
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public boolean isRunning() {
		return isRunning;
	}

}
