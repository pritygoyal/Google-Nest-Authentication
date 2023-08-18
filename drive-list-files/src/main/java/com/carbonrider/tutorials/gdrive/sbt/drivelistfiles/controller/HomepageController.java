
package com.carbonrider.tutorials.gdrive.sbt.drivelistfiles.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.smartdevicemanagement.v1.SmartDeviceManagementScopes;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Controller
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class HomepageController {

	
	private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static  String accesstoken="";
	private static final List<String> SCOPES1 = Collections.singletonList(SmartDeviceManagementScopes.SDM_SERVICE
			 );

	
	private static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER";

	@Value("${google.oauth.callback.uri}")
	private String CALLBACK_URI;

	@Value("${google.secret.key.path}")
	private Resource gdSecretKeys;

	@Value("${google.credentials.folder.path}")
	private Resource credentialsFolder;
	
	@Value("${google.credentials.device_project_id}")
	private Resource deviceProjectId;
	
	@Value("${google.credentials.app_client_id}")
	private Resource appClientId;
	
	@Value("${google.credentials.project_id}")
	private Resource projectId;
	
	private GoogleAuthorizationCodeFlow flow;
	
	
	@PostConstruct
	public void init() throws Exception {
		GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY,
				new InputStreamReader(gdSecretKeys.getInputStream()));
		flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, secrets, SCOPES1).setAuthorizationServerEncodedUrl("https://nestservices.google.com/partnerconnections/<project_id>/auth").build();
	}

	@GetMapping(value = { "/" })
	public String showHomePage() throws Exception {
		boolean isUserAuthenticated = false;
		Credential credential = flow.loadCredential(USER_IDENTIFIER_KEY);
		if (credential != null) {
			boolean tokenValid = credential.refreshToken();
			if (tokenValid) {
				isUserAuthenticated = true;
			}
		}

		return isUserAuthenticated ? "dashboard.html" : "index.html";
	}
	
	//Get Mapping for Google signing.
	@GetMapping(value = { "/googlesignin" })
	public void doGoogleSignIn(HttpServletResponse response) throws Exception {
		GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
		String redirectURL = url.setRedirectUri(CALLBACK_URI).setAccessType("offline").build();
		response.sendRedirect(redirectURL+"&prompt=consent");
	}

	//GBet Mapping for oAuth Authentication
	@GetMapping(value = { "/oauth" })
	public String saveAuthorizationCode(HttpServletRequest request) throws Exception {
		String code = request.getParameter("code");
		if (code != null) {
			saveToken(code);
			
			return "dashboard.html";
		}
		
		return "index.html";
	}

	private void saveToken(String code) throws Exception {
		GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(CALLBACK_URI).execute();
	  	  
		 accesstoken=response.getAccessToken();
	     flow.createAndStoreCredential(response, USER_IDENTIFIER_KEY);
	}

	
	
	//Get Mapping to get connected device info like Temperature, humidity etc.
	@GetMapping(value = { "/devices" })
	public void devicename(HttpServletResponse response) throws Exception {
	 String get_url= "https://smartdevicemanagement.googleapis.com/v1/enterprises/<project_id>/devices";
	 URL obj = new URL(get_url);
	 HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 con.setRequestMethod("GET");
	 con.setRequestProperty("Content-Type", "application/json");
	 String a_token="Bearer "+ accesstoken;
	 con.setRequestProperty("Authorization", a_token);
	  int response1=con.getResponseCode();
	 if (response1 == HttpURLConnection.HTTP_OK) { 
		 
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response2 = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response2.append(inputLine);
			}
			in.close();
			JSONObject jsonObj = new JSONObject(response2.toString());
			
		
			
			System.out.println(jsonObj);
			JSONArray devices = jsonObj.getJSONArray("devices");
			 JSONObject sonObject = devices.getJSONObject(0);
			 JSONObject traits = sonObject.getJSONObject("traits");
			 JSONObject AmTemp = traits.getJSONObject("sdm.devices.traits.Temperature");
			 double Temp=AmTemp.getDouble("ambientTemperatureCelsius");
			 JSONObject AmHumi = traits.getJSONObject("sdm.devices.traits.Humidity");
			 double Humidity=AmHumi.getDouble("ambientHumidityPercent");
			 System.out.println("device Temperature  is : " +Temp);
			System.out.println("device Humidity is : " +Humidity);
		
		} else {
			System.out.println("GET request did not work.");
		}
	} 
	
	

	
}
	

