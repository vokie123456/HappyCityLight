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
package com.citydigitalpulse.OfflineStatistic.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zhongli Li
 *
 */
public class HotTopic implements Comparable<HotTopic> {
	private String text;
	private List<ImgAndScore> images;
	private PulseValue pulse;
	private int count;

	/**
	 * 
	 */
	public HotTopic() {
		super();
		this.text = "";
		this.images = new ArrayList<ImgAndScore>();
		this.setPulse(new PulseValue());
		this.count = 0;
	}

	public HotTopic(String topic) {
		this();
		this.text = topic;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<ImgAndScore> getImages() {
		return images;
	}

	public void setImages(List<ImgAndScore> images) {
		this.images = images;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public PulseValue getPulse() {
		return pulse;
	}

	public void setPulse(PulseValue pulse) {
		this.pulse = pulse;
	}

	/**
	 * 增加一条新记录并统计
	 * 
	 * @Author Zhongli Li Email: lzl19920403@gmail.com
	 * @param temp
	 */
	public void addNewRecord(StructuredFullMessage temp) {
		this.count++;
		if(this.pulse.getTimestamp()==0){
			this.pulse.setTimestamp(temp.getCreat_at());
		}
		this.pulse.addNewValue(temp.getEmotion_text(),
				temp.getEmotion_text_value());
		for (int i = 0; i < temp.getMedia_types().size(); i++) {
			if (temp.getMedia_types().get(i).equals("photo")) {
				ImgAndScore imgs = new ImgAndScore();
				imgs.setImg_url(temp.getMedia_urls().get(i));
				if (temp.getMedia_urls_local().size() > 0) {
					imgs.setImg_url_local(temp.getMedia_urls_local().get(i));
				}
				this.images.add(imgs);
			} else {
				// The Media is not image.
			}
		}
	}

	@Override
	public int compareTo(HotTopic o) {
		return (o.getCount() - this.getCount());
	}

	@Override
	public String toString() {
		return "HotTopic [text=" + text + ", images=" + images + ", pulse="
				+ pulse + ", count=" + count + "]";
	}

}
