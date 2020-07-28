package org.nuxeo.data.gen.cli;

import java.io.IOException;
import java.util.Properties;

import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.NuxeoClient.Builder;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class NuxeoClientHelper {

	public static class AutomationAsyncCallInterceptor implements Interceptor {

		@Override
		public Response intercept(Chain chain) throws IOException {

			Request org = chain.request();

			if (!org.url().encodedPath().contains("/automation/") || org.url().encodedPath().contains("/login")) {
				return chain.proceed(org);
			}
			HttpUrl asyncUrl = org.url().newBuilder().addEncodedPathSegment("@async").build();
			Request req = chain.request().newBuilder().headers(org.headers()).method(org.method(), org.body())
					.url(asyncUrl).build();

			Response res = chain.proceed(req);
			System.out.println("Async Automation Execution Scheduled");
			System.out.println("  => status url:" + res.headers("Location"));
			return res;
		}
	}

	public static NuxeoClient createClient(String login, String pwd, String url) {

		Properties config = new Properties();
		config.put("url", url);
		config.put("login", login);
		config.put("password", pwd);
		return createClient(config);
	}

	public static NuxeoClient createClient(Properties config) {
		return createClient(config, false);
	}

	public static NuxeoClient createClient(Properties config, boolean async) {

		String url = config.getProperty("url");
		String login = config.getProperty("login");
		String pwd = config.getProperty("password");

		System.out.println("url=" + url);
		System.out.println("login=" + login);

		Builder builder = new NuxeoClient.Builder().url(url).authentication(login, pwd).readTimeout(24 * 3600)
				.connectTimeout(60).transactionTimeout(24 * 3600);
		
		if (async) {
			builder.interceptor(new AutomationAsyncCallInterceptor());
		}

		NuxeoClient nuxeoClient = builder.connect();

		System.out.println("Nuxeo Client configured");
		if (async) {
			System.out.println(" (async mode actiavted)");

		}
		return nuxeoClient;
	}
	
}
