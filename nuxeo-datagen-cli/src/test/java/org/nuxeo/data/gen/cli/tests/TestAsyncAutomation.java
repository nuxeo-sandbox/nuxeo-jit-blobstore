package org.nuxeo.data.gen.cli.tests;

import java.util.Properties;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.client.NuxeoClient;
import org.nuxeo.data.gen.cli.NuxeoClientHelper;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.automation.test.AutomationServerFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.data.gen.cli.test:OSGI-INF/operations.xml")
public class TestAsyncAutomation {

	@Inject
	protected ServletContainerFeature servletContainerFeature;

	protected String getBaseURL() {
		int port = servletContainerFeature.getPort();
		return "http://localhost:" + port;
	}

	@Test
	public void shouldCallAsyncAndWait() {

		Properties config = new Properties();
		config.put("url", getBaseURL());
		config.put("login", "Administrator");
		config.put("password", "Administrator");

		NuxeoClient client = NuxeoClientHelper.createClient(config, true);

		Object response = client.operation(NOPOperation.ID).execute();
		
		String pollUrl = NuxeoClientHelper.getLastPollUrl();
		System.out.println(pollUrl);
		
		NuxeoClientHelper.waitForResult(config, 1, 20);	

		
	}

}
