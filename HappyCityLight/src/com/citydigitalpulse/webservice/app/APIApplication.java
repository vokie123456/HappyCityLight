package com.citydigitalpulse.webservice.app;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;

import com.citydigitalpulse.webservice.api.CollectorControlResource;
import com.citydigitalpulse.webservice.api.MarkPageResource;
import com.citydigitalpulse.webservice.api.UserResource;
import com.citydigitalpulse.webservice.api.MessageOnMapResource;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class APIApplication extends ResourceConfig {
	public APIApplication() {
		// 加载Resource
		register(UserResource.class);
		register(MarkPageResource.class);
		register(MessageOnMapResource.class);
		register(CollectorControlResource.class);

		// 注册数据转换器
		register(JacksonJsonProvider.class);
		// 跨域访问
		register(CORSFilter.class);
		// Logging.
		register(LoggingFilter.class);
	}
}