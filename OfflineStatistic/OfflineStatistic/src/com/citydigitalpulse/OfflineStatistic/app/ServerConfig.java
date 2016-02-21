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
package com.citydigitalpulse.OfflineStatistic.app;

public class ServerConfig {
	// // 用于控制爬虫程序的数据库
	// public static String MESSAGE_GETTER_CONTROLLER_DATABASE_URL =
	// "jdbc:mysql://137.122.89.207:3306/happycityproject";
	// public static String MESSAGE_GETTER_CONTROLLER_DATABASE_USER_NAME =
	// "jcs130";
	// public static String MESSAGE_GETTER_CONTROLLER_DATABASE_USER_PASSWORD =
	// "jcsss130";
	//
	// // 用于存储结构化后的数据的数据库
	// public static String MESSAGE_SAVING_DATABASE_URL =
	// "jdbc:mysql://137.122.89.207:3306/MsgSaving";
	// public static String MESSAGE_SAVING_DATABASE_USER_NAME = "jcs130";
	// public static String MESSAGE_SAVING_DATABASE_USER_PASSWORD = "jcsss130";

	// 用于控制爬虫程序的数据库
	public static String MESSAGE_GETTER_CONTROLLER_DATABASE_URL = "jdbc:mysql://localhost:3307/happycityproject";
	public static String MESSAGE_GETTER_CONTROLLER_DATABASE_USER_NAME = "root";
	public static String MESSAGE_GETTER_CONTROLLER_DATABASE_USER_PASSWORD = "jcsss130";

	// 用于存储结构化后的数据的数据库
	public static String MESSAGE_SAVING_DATABASE_URL = "jdbc:mysql://localhost:3307/MsgSaving";
	public static String MESSAGE_SAVING_DATABASE_USER_NAME = "root";
	public static String MESSAGE_SAVING_DATABASE_USER_PASSWORD = "jcsss130";

}
