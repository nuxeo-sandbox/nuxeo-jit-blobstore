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

	static ThreadLocal<String> lastPollUrl = new ThreadLocal<>();
	
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
			
			lastPollUrl.set(res.headers("Location").get(0));
			
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
	
	public static boolean waitForResult(Properties config, int pollIntervalS, int timeoutS) {

	   NuxeoClient pollClient = createClient(config);
	   
	   String url = getLastPollUrl();
	   
	   boolean running = true;
	   boolean completed = false;
	   long t0 = System.currentTimeMillis();
	   while (running) {

		   if (System.currentTimeMillis()-t0 > timeoutS*1000) {
			   System.out.println("Exit with timeout");
			   return false;
		   }

	   	    try {
				Thread.sleep(1000*pollIntervalS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

	   	   pollClient.header("Content-Type", "application/json");
		   Response res = pollClient.get(url);
		   if (res.code()==200) {
			   try {
				String body = res.body().string();				
				if (!body.contains("RUNNING")) {
					running = false;
					System.out.println("\nRunning completed");
					System.out.println(body);
					completed=true;
				} else {
					System.out.print(".");
				}
			} catch (IOException e1) {
				System.out.println("\nError why polling server");
				e1.printStackTrace();
				running=false;
			}
		   } else {
			   System.out.println("\nServer responded with code " + res.code());
			   running=false;			   
		   }
	   }
	   
	   long duration = (System.currentTimeMillis()-t0)/1000;
	   System.out.println("Exit after " + duration + " s");
	   
	   return completed;
	}
	
	public static String getLastPollUrl() {
		return lastPollUrl.get();
	}
}
