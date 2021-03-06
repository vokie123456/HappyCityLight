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
package com.citydigitalpulse.collector.TwitterGetter.app;

import com.citydigitalpulse.collector.TwitterGetter.service.InsertIntoDBThread;
import com.citydigitalpulse.collector.TwitterGetter.service.ScanRegsThread;
import com.citydigitalpulse.collector.TwitterGetter.service.ThreadsPool;

/**
 * 
 * @author zhonglili
 *
 */
public class getterMain {

	public static void main(String[] args) {
		getterMain tm = new getterMain();
		// System.out.println(tm.getClass().getSimpleName());
		// tm.doSth();
		// tm.reSetDB();
		tm.startThreads(10000);
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void startThreads(int time) {
		ScanRegsThread sRegThread = new ScanRegsThread(time);
		ThreadsPool.addThread(sRegThread);
		InsertIntoDBThread insertThread = new InsertIntoDBThread();
		insertThread.start();
		// StartRegThreads sSqureThread = new StartRegThreads(time2);
		// ThreadsPool.addThread(sSqureThread);
		// StreamsManagerThread smt = new StreamsManagerThread(time3);
		// ThreadsPool.addThread(smt);

	}

}
