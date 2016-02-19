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
package com.citydigitalpulse.webservice.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

import org.glassfish.hk2.utilities.reflection.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import redis.clients.jedis.Jedis;

import com.citydigitalpulse.webservice.dao.UserAccountDAO;
import com.citydigitalpulse.webservice.dao.impl.MessageSavingDAOimpl;
import com.citydigitalpulse.webservice.dao.impl.RedisUtil;
import com.citydigitalpulse.webservice.dao.impl.userAccountDAOimpl;
import com.citydigitalpulse.webservice.model.collector.LocArea;
import com.citydigitalpulse.webservice.model.message.ImpuseValue;
import com.citydigitalpulse.webservice.model.message.QueryOption;
import com.citydigitalpulse.webservice.model.message.ResMsg;
import com.citydigitalpulse.webservice.model.message.StructuredFullMessage;
import com.citydigitalpulse.webservice.model.user.Role;
import com.citydigitalpulse.webservice.tool.Tools;
import com.citydigitalpulse.webservice.tool.NLPPart.alchemyapi.api.AlchemyAPI;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

//即时显示界面的API
@Path("/message")
public class MessageResource {
	private static MessageSavingDAOimpl msgSav = new MessageSavingDAOimpl();
	private static UserAccountDAO userAccountDAO = new userAccountDAOimpl();
	private static String RequreRole = "ROLE_USER";
	// 设置缓存数据的大小
	private static int CACHE_NUMBER = 5000;
	// 用于存储缓存数据的队列
	// private static ArrayList<StructuredFullMessage> cache_messagess = new
	// ArrayList<StructuredFullMessage>(
	// CACHE_NUMBER);
	// 计划用全局缓存数据库代替
	private String UPLOAD_CACHE_KEY = "UploadStructuredMessageTemp";// 先进后出
	private String TAGGED_CACHE_KEY = "TaggedStructuredMessageTemp";// 先进后出
	ObjectMapper mapper = new ObjectMapper();
	// 监听城市列表
	private static ArrayList<String> streamPlaceNames = new ArrayList<String>();
	// 保存统计查询的历史数据，以后可以存到数据库中永久保存
	private static HashMap<QueryOption, Object> auery_history = new HashMap<QueryOption, Object>();

	/**
	 * 用于从爬虫程序上传最新数据的API
	 * 
	 * @param token
	 * @param raw_id_str
	 * @param user_name
	 * @param text
	 * @param creat_at
	 * @param emotion_text
	 * @param media_types
	 * @param media_urls
	 * @param media_urls_local
	 * @param emotion_medias
	 * @param emotion_all
	 * @param place_type
	 * @param place_name
	 * @param place_fullname
	 * @param country
	 * @param province
	 * @param city
	 * @param query_location_latitude
	 * @param query_location_langtitude
	 * @param hashtags
	 * @param replay_to
	 * @param message_from
	 * @return
	 */
	@POST
	@Path("/uploadnewmessage")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ResMsg uploadNewMessage(
			@FormParam("token") String token,
			@FormParam("num_id") @DefaultValue("0") String num_id,
			@FormParam("raw_id_str") String raw_id_str,
			@FormParam("user_name") String user_name,
			@FormParam("profile_image_url") @DefaultValue("") String profile_image_url,
			@FormParam("text") String text,
			@FormParam("creat_at") String creat_at,
			@FormParam("emotion_text") String emotion_text,
			@FormParam("media_types") String media_types,
			@FormParam("media_urls") String media_urls,
			@FormParam("media_urls_local") String media_urls_local,
			@FormParam("emotion_medias") String emotion_medias,
			@FormParam("emotion_all") String emotion_all,
			@FormParam("place_type") String place_type,
			@FormParam("place_name") String place_name,
			@FormParam("place_fullname") String place_fullname,
			@FormParam("country") String country,
			@FormParam("province") String province,
			@FormParam("city") String city,
			@FormParam("query_location_latitude") String query_location_latitude,
			@FormParam("query_location_langtitude") String query_location_langtitude,
			@FormParam("is_real_location") String is_real_location,
			@FormParam("hashtags") String hashtags,
			@FormParam("replay_to") String replay_to,
			@FormParam("lang") String lang,
			@FormParam("message_from") String message_from) {
		ResMsg res = new ResMsg();
		try {
			if (!token.equals("Imagoodboy")) {
				// System.out.println(token);
				res.setCode(Response.Status.BAD_REQUEST.getStatusCode());
				res.setType(Response.Status.BAD_REQUEST.name());
				res.setMessage("Wrong Token.");
				return res;
			}
			// 根据传来的参数构建对象
			StructuredFullMessage msg = new StructuredFullMessage();
			msg.setNum_id(Long.parseLong(num_id));
			msg.setRaw_id_str(raw_id_str);
			msg.setUser_name(user_name);
			msg.setProfile_image_url(profile_image_url);
			msg.setText(text);
			msg.setCreat_at(Long.parseLong(creat_at));
			msg.setEmotion_text(emotion_text);
			msg.setMedia_types(Tools.buildListFromString(media_types));
			msg.setMedia_urls(Tools.buildListFromString(media_urls));
			msg.setMedia_urls_local(Tools.buildListFromString(media_urls_local));
			msg.setEmotion_medias(Tools.buildListFromString(emotion_medias));
			msg.setEmotion_all(emotion_all);
			msg.setPlace_type(place_type);
			msg.setPlace_name(place_name);
			msg.setPlace_fullname(place_fullname);
			msg.setCountry(country);
			msg.setProvince(province);
			msg.setCity(city);
			msg.setQuery_location_latitude(Double
					.parseDouble(query_location_latitude));
			msg.setQuery_location_langtitude(Double
					.parseDouble(query_location_langtitude));
			msg.setReal_location(Boolean.parseBoolean(is_real_location));
			msg.setHashtags(Tools.buildListFromString(hashtags));
			msg.setReplay_to(replay_to);
			msg.setLang(lang);
			msg.setMessage_from(message_from);
			Jedis cacheDB = RedisUtil.getJedis();
			if (cacheDB.exists(UPLOAD_CACHE_KEY)) {
				// 将该信息保存到缓存队列中
				if (cacheDB.llen(UPLOAD_CACHE_KEY) < 20) {
					// 如果缓存大小小于规定值，则直接存储
					cacheDB.lpush(UPLOAD_CACHE_KEY,
							mapper.writeValueAsString(msg));
				} else {
					// 移除队列头部的元素，然后增加新元素
					cacheDB.rpop(UPLOAD_CACHE_KEY);
					cacheDB.lpush(UPLOAD_CACHE_KEY,
							mapper.writeValueAsString(msg));
				}
			} else {
				cacheDB.lpush(UPLOAD_CACHE_KEY, mapper.writeValueAsString(msg));
			}

			RedisUtil.returnResource(cacheDB);
			// 将该数据存储到数据库?
			res.setCode(Response.Status.OK.getStatusCode());
			res.setType(Response.Status.OK.name());
			res.setMessage("Upload Success.");
			// System.out.println(msg.getText());
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			res.setCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
			res.setType(Response.Status.INTERNAL_SERVER_ERROR.name());
			res.setMessage(e.getLocalizedMessage());
			return res;
		}
	}

	// 从缓存中获得按照指定要求筛选的数据
	@GET
	@Path("/getlatest")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ResMsg getLates(
			@QueryParam("token") @DefaultValue("") String token,
			@QueryParam("message_from") @DefaultValue("") String message_from,
			@QueryParam("keyword") @DefaultValue("") String keyword,
			@QueryParam("location_areas") @DefaultValue("") String location_area_json,
			@QueryParam("lang") @DefaultValue("") String lang,
			@QueryParam("limit") @DefaultValue("1") int limit,
			@QueryParam("skip_num_ids") @DefaultValue("") String skip_num_ids) {
		ResMsg res = new ResMsg();
		try {
			// System.out.println("message_from:" + message_from + " keyword:"
			// + keyword + " city:" + city + " location_lat_min:"
			// + location_lat_min + " location_lat_max:"
			// + location_lat_max + " location_lan_min:"
			// + location_lan_min + " location_lan_max:"
			// + location_lan_max + " lang:" + lang + " limit:" + limit
			// + " skip_num_ids" + skip_num_ids);

			// 检查客户端Token
			if (!token.equals("ArashiArashiFordream")) {
				// System.out.println(token);
				res.setCode(Response.Status.BAD_REQUEST.getStatusCode());
				res.setType(Response.Status.BAD_REQUEST.name());
				res.setMessage("Token is wrong.");
				return res;
			}
			ArrayList<LocArea> location_area = null;
			ObjectMapper mapper = new ObjectMapper();
			if (!"".equals(location_area_json)) {
				// 将json转换为区域数组
				location_area = mapper.readValue(location_area_json,
						new TypeReference<List<LocArea>>() {
						});
			} else {
				// 无区域信息
			}
			// 得到所有的查询条件并且返回最新的符合条件的数据
			ArrayList<StructuredFullMessage> list = new ArrayList<StructuredFullMessage>();
			ArrayList<StructuredFullMessage> history_list = new ArrayList<StructuredFullMessage>();
			Jedis cacheDB = RedisUtil.getJedis();
			List<String> cache = new ArrayList<String>();
			int begain_num = 0;
			StructuredFullMessage temp;
			// cache.addAll(cacheDB.lrange(UPLOAD_CACHE_KEY, 0, -1));
			// System.out.println("Cache size=" + cache.size());
			ArrayList<Long> skips = Tools.buildLongListFromString(skip_num_ids);
			while (cacheDB.llen(UPLOAD_CACHE_KEY) > 0) {
				temp = mapper.readValue(cacheDB.rpop(UPLOAD_CACHE_KEY),
						StructuredFullMessage.class);
				if (isMatch(temp, message_from, keyword, location_area, lang)) {
					// 检测情绪，若正常则返回并且存储到缓存中
					if (setEmotion(temp)) {
						list.add(temp);
						if (list.size() >= limit) {
							for (int i = list.size(); i > 0; i--) {
								cacheDB.lpush(TAGGED_CACHE_KEY, mapper
										.writeValueAsString(list.get(i - 1)));
							}
							// System.out.println("return:" + list.size());
							RedisUtil.returnResource(cacheDB);
							res.setObj(list);
							res.setCode(Response.Status.OK.getStatusCode());
							res.setType(Response.Status.OK.name());
							res.setMessage("Get latest Message Success.");
							return res;
						}
					}
				}
			}
			while ((list.size() + history_list.size()) < limit) {
				// 将历史数据加入
				cache.addAll(cacheDB.lrange(TAGGED_CACHE_KEY, begain_num,
						begain_num + limit + skips.size()));
				for (int i = 0; i < cache.size(); i++) {
					temp = mapper.readValue(cache.get(i),
							StructuredFullMessage.class);
					// System.out.println(temp);
					if (!skips.contains(temp.getNum_id())) {
						// if (isMatch(temp, message_from, keyword, city,
						// location_lat_min, location_lat_max,
						// location_lan_min, location_lan_max, lang)) {
						// System.out.println("skip:" + skips
						// + "temp.getNum_id():" + temp.getNum_id());
						if (isMatch(temp, message_from, keyword, location_area,
								lang)) {
							history_list.add(temp);
							if ((list.size() + history_list.size()) >= limit) {
								break;
							}
						}
					} else {
						// System.out.println("skip");
					}
				}
				begain_num += limit + skips.size();
			}
			// 将新标记的加入内存数据库,反着加进去
			for (int i = list.size(); i > 0; i--) {
				if (cacheDB.llen(TAGGED_CACHE_KEY) < CACHE_NUMBER) {
					// 如果缓存大小小于规定值，则直接存储
					cacheDB.lpush(TAGGED_CACHE_KEY,
							mapper.writeValueAsString(list.get(i - 1)));
				} else {
					// 移除队列头部的元素，然后增加新元素
					cacheDB.rpop(TAGGED_CACHE_KEY);
					cacheDB.lpush(TAGGED_CACHE_KEY,
							mapper.writeValueAsString(list.get(i - 1)));
				}

			}
			// 合并新老记录
			list.addAll(history_list);
			RedisUtil.returnResource(cacheDB);
			System.out.println("return:" + list.size());
			res.setObj(list);
			res.setCode(Response.Status.OK.getStatusCode());
			res.setType(Response.Status.OK.name());
			res.setMessage("Get latest Message Success.");
			return res;

		} catch (Exception e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			res.setCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
			res.setType(Response.Status.INTERNAL_SERVER_ERROR.name());
			res.setMessage(e.getLocalizedMessage());
			return res;
		}
	}

	private boolean isMatch(StructuredFullMessage temp, String message_from,
			String keyword, ArrayList<LocArea> location_area, String lang) {

		if (!temp.isReal_location()) {
			return false;
		}
		// 语言，传入对象为列表
		if (!"".equals(lang) && !"null".equals(lang)) {
			// System.out.println("lang: " + lang);
			if (!isEquals(lang.trim().split(","), temp.getLang())) {
				return false;
			}
		}
		// 消息来源，传入对象为列表
		if (!"".equals(message_from) && !"null".equals(message_from)) {
			// System.out.println("message_from" + message_from);
			if (!isEquals(message_from.trim().split(","),
					temp.getMessage_from())) {
				return false;
			}
		}
		// 是否包含关键字
		if (!"".equals(keyword) && !"null".equals(keyword)) {
			if (!isContains(keyword.trim().split(","), temp.getText())) {
				return false;
			}
		}
		if (location_area != null && location_area.size() != 0) {
			// 是否在指定地理坐标和范围
			for (int i = 0; i < location_area.size(); i++) {
				if (temp.getQuery_location_latitude() < location_area.get(i)
						.getNorth()
						&& temp.getQuery_location_latitude() > location_area
								.get(i).getSouth()
						&& temp.getQuery_location_langtitude() < location_area
								.get(i).getEast()
						&& temp.getQuery_location_langtitude() > location_area
								.get(i).getWest()) {
					return true;
				}
			}
			return false;
		}
		// System.out.println(temp + "符合条件");
		return true;
	}

	private void updateEmotions(ArrayList<StructuredFullMessage> list) {
		for (int i = 0; i < list.size(); i++) {
			StructuredFullMessage temp = list.get(i);
			if ("".equals(temp.getEmotion_text())) {
				setEmotion(temp);
			}
		}

	}

	private boolean setEmotion(StructuredFullMessage temp) {
		// 如果没有感情数据则通过API获取并且将情感标记存入数据库
		if (temp.getLang().equals("en")) {
			try {
				String emotion_text = getTextEmotion(temp.getText());
				temp.setEmotion_text(emotion_text);
				// 将情感标记存入数据库
				msgSav.updateTextEmotion(temp.getNum_id(), emotion_text);
				// 更新缓存中的数据
				return true;
			} catch (XPathExpressionException | IOException | SAXException
					| ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// 临时设置为未知
				temp.setEmotion_text("unknown");
				// temp.setEmotion_text("neutral");
				return false;
			}
		} else {
			// 不是英语调用其他方法
			// 临时设置为未知
			temp.setEmotion_text("unknown");
			// temp.setEmotion_text("neutral");
			return false;
		}
	}

	private String getTextEmotion(String text) throws XPathExpressionException,
			IOException, SAXException, ParserConfigurationException {
		String emotion = "";
		double score;
		AlchemyAPI alchemyObj = AlchemyAPI
				.GetInstanceFromString("b232c9bbb50818d45e1ecd2f14ea0bc47bdea8d1");
		Document doc = alchemyObj.TextGetTextSentiment(text);
		// System.out.println(getStringFromDocument(doc));
		// 使用 DOM解析返回的XML文档
		emotion = doc.getElementsByTagName("type").item(0).getTextContent();
		if (emotion.equals("neutral")) {
			score = 0;
		} else {
			score = Double.parseDouble(doc.getElementsByTagName("score")
					.item(0).getTextContent());
		}
		return emotion;
	}

	private static String getStringFromDocument(Document doc) {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);

			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * 两个数组（队列）进行比较
	 * 
	 * @param split
	 * @param hashtags
	 * @return
	 */
	private boolean isEquals(String[] split, List<String> dest) {
		for (int i = 0; i < dest.size(); i++) {
			for (int j = 0; j < split.length; j++) {
				if (dest.get(i).toLowerCase().equals(split[j].toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 查询数组中是否有某元素
	 * 
	 * @param split
	 * @param message_from
	 * @return
	 */
	private boolean isContains(String[] split, String dest) {
		for (int i = 0; i < split.length; i++) {
			if (dest.toLowerCase().indexOf(split[i].toLowerCase()) != -1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 查询数组中是否有某元素
	 * 
	 * @param split
	 * @param message_from
	 * @return
	 */
	private boolean isEquals(String[] split, String dest) {
		for (int i = 0; i < split.length; i++) {
			if (dest.toLowerCase().equals(split[i].toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	// 从数据库中获得按照指定要求筛选的数据
	@GET
	@Path("/getfiltereddata")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ResMsg getFilteredData(
			@QueryParam("userID") long userID,
			@QueryParam("token") @DefaultValue("") String token,
			@QueryParam("message_from") @DefaultValue("") String message_from,
			@QueryParam("place_name") @DefaultValue("") String place_name,
			@QueryParam("keyword") @DefaultValue("") String keyword,
			// 如果缩放级别小于阈值，城市由不同颜色的六边形组成，只返回符合规定的并且有实际经纬度的信息
			// 如果缩放级别大于阈值，城市的六边形由相同颜色组成，返回时间戳和具幸福值
			@QueryParam("zoom_level") @DefaultValue("10") int zoom_level,
			@QueryParam("time_start") @DefaultValue("0") long time_start,
			@QueryParam("time_end") @DefaultValue("0") long time_end,
			@QueryParam("location_areas") @DefaultValue("") String location_area_json,
			@QueryParam("lang") @DefaultValue("") String lang) {
		ResMsg res = new ResMsg();
		int max_zoom_level = 10;
		try {
			if (!userAccountDAO.tokenCheck(userID, token)) {
				res.setCode(Response.Status.BAD_REQUEST.getStatusCode());
				res.setType(Response.Status.BAD_REQUEST.name());
				res.setMessage("Token expaired. Please login again.");
				return res;
			}
			// 检察权限,若权限不足则返回错误信息
			if (!userAccountDAO.getUserRolesByUserId(userID).contains(
					new Role(RequreRole))) {
				res.setCode(Response.Status.BAD_REQUEST.getStatusCode());
				res.setType(Response.Status.BAD_REQUEST.name());
				res.setMessage("Primition decline.");
				return res;
			}
			// 保存请求的参数
			QueryOption option = new QueryOption();
			option.setLangs(lang);
			option.setLocation_area_json(location_area_json);
			option.setMessage_sources(message_from);
			option.setKeywords(keyword);
			option.setPlace_name(place_name);
			option.setTime_start(time_start);
			option.setTime_end(time_end);

			ArrayList<LocArea> location_area = null;
			ObjectMapper mapper = new ObjectMapper();
			// 将json转换为区域数组
			location_area = mapper.readValue(location_area_json,
					new TypeReference<List<LocArea>>() {
					});
			ArrayList<String> langs = new ArrayList<String>();
			ArrayList<String> message_sources = new ArrayList<String>();
			ArrayList<String> keyword_list = new ArrayList<String>();
			if (!"".equals(lang)) {
				String[] temp = lang.split(",");
				for (int i = 0; i < temp.length; i++) {
					langs.add(temp[i].toLowerCase());
				}
			}
			if (!"".equals(message_from)) {
				String[] temp = message_from.split(",");
				for (int i = 0; i < temp.length; i++) {
					message_sources.add(temp[i].toLowerCase());
				}
			}
			if (!"".equals(keyword)) {
				String[] temp = keyword.split(",");
				for (int i = 0; i < temp.length; i++) {
					keyword_list.add(temp[i].toLowerCase());
				}
			}
			// 根据缩放级别判断返回什么信息
			if (zoom_level >= max_zoom_level) {
				option.setIs_true_location(true);
				if (auery_history.containsKey(option)) {
					res.setObj(auery_history.get(option));
				} else {
					ArrayList<StructuredFullMessage> queryResult = msgSav
							.getFilteredMessages(time_start, time_end,
									place_name, location_area, langs,
									message_sources, true, keyword_list);
					auery_history.put(option, queryResult);
					res.setObj(queryResult);
				}
				// 返回有具体坐标的消息列表
				res.setCode(Response.Status.OK.getStatusCode());
				res.setType(Response.Status.OK.name());
				res.setMessage("Get filtered data Success.");
				return res;
			} else {
				option.setIs_true_location(false);
				// 如果之前查询过，直接返回结果
				if (auery_history.containsKey(option)) {
					res.setObj(auery_history.get(option));
				} else {
					ArrayList<StructuredFullMessage> queryResult = msgSav
							.getFilteredMessages(time_start, time_end,
									place_name, location_area, langs,
									message_sources, false, keyword_list);
					auery_history.put(option, queryResult);
					res.setObj(queryResult);
				}
				// 返回数据
				res.setCode(Response.Status.OK.getStatusCode());
				res.setType(Response.Status.OK.name());
				res.setMessage("Get filtered data Success.");
				return res;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			res.setCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
			res.setType(Response.Status.INTERNAL_SERVER_ERROR.name());
			res.setMessage(e.getLocalizedMessage());
			return res;
		}
	}

	/**
	 * 奖统计好的数据发送回前台
	 * 
	 * @param filteredMessages
	 * @return
	 */
	private ArrayList<ImpuseValue> getStatisticalData(
			ArrayList<StructuredFullMessage> filteredMessages, long time_start,
			long time_end, long detTime) {
		int resSize = (int) Math.ceil((time_end - time_start)
				/ (double) detTime);
		ArrayList<ImpuseValue> res = new ArrayList<ImpuseValue>(resSize);
		System.out.println("result size:" + resSize);
		// 数列初始化
		for (int i = 0; i < resSize; i++) {
			res.add(new ImpuseValue());
		}
		for (int i = 0; i < filteredMessages.size(); i++) {
			StructuredFullMessage temp = filteredMessages.get(i);
			// 取余数得到在第几个小时
			int num = (int) ((temp.getCreat_at() - time_start) / detTime);
			// if (res.get(num) == null) {
			// res.set(num, new ImpuseValue(time_start + num * detTime));
			// }
			if (res.get(num).getTimestamp() == 0) {
				res.get(num).setTimestamp(time_start + num * detTime);
			}
			// 根据情绪累加计数器
			res.get(num).addNewValue(temp.getEmotion_text());
		}
		return res;
	}

	// 从数据库中获得按照指定要求筛选的数据
	@GET
	@Path("/getfilteredresult")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ResMsg getFilteredResult(
			@QueryParam("userID") long userID,
			@QueryParam("token") @DefaultValue("") String token,
			@QueryParam("message_from") @DefaultValue("") String message_from,
			@QueryParam("place_name") @DefaultValue("") String place_name,
			@QueryParam("keyword") @DefaultValue("") String keyword,
			@QueryParam("zoom_level") @DefaultValue("10") int zoom_level,
			@QueryParam("unit") @DefaultValue("hour") String unit,
			@QueryParam("time_start") @DefaultValue("0") long time_start,
			@QueryParam("time_end") @DefaultValue("0") long time_end,
			@QueryParam("location_areas") @DefaultValue("") String location_area_json,
			@QueryParam("lang") @DefaultValue("") String lang) {
		ResMsg res = new ResMsg();
		try {
			if (!userAccountDAO.tokenCheck(userID, token)) {
				res.setCode(Response.Status.BAD_REQUEST.getStatusCode());
				res.setType(Response.Status.BAD_REQUEST.name());
				res.setMessage("Token expaired. Please login again.");
				return res;
			}
			// 检察权限,若权限不足则返回错误信息
			if (!userAccountDAO.getUserRolesByUserId(userID).contains(
					new Role(RequreRole))) {
				res.setCode(Response.Status.BAD_REQUEST.getStatusCode());
				res.setType(Response.Status.BAD_REQUEST.name());
				res.setMessage("Primition decline.");
				return res;
			}
			// 根据缩放级别判断返回什么信息
			res = getFilteredData(userID, token, message_from, place_name,
					keyword, zoom_level, time_start, time_end,
					location_area_json, lang);
			if (res.getCode() != Response.Status.OK.getStatusCode()) {
				System.out.println("Error in querys");
				return res;
			} else {
				ArrayList<StructuredFullMessage> queryResult = (ArrayList<StructuredFullMessage>) res
						.getObj();
				// 间隔的毫秒数
				long detTime = 3600000;
				if (unit.equals("hour")) {

				} else if (unit.equals("day")) {
					detTime = detTime * 24;
				} else {

				}
				res.setObj(getStatisticalData(queryResult, time_start,
						time_end, detTime));
				res.setMessage("Get the Result Success.");
				return res;
			}

		} catch (Exception e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			res.setCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
			res.setType(Response.Status.INTERNAL_SERVER_ERROR.name());
			res.setMessage(e.getLocalizedMessage());
			return res;
		}
	}

	public static ArrayList<String> getStreamPlaceNames() {
		return streamPlaceNames;
	}

	public static void setStreamPlaceNames(ArrayList<String> streamPlaceNames) {
		MessageResource.streamPlaceNames = streamPlaceNames;
	}

}