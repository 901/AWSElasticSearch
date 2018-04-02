package com.amazonaws.lambda.gbif;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceResponse;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.ExecutionContext;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.http.HttpResponseHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class GbifLambdaHandler implements RequestHandler<Object, String> {

	private static final String service_name = "es";
	private static final String regionName = "us-east-2";
	private static final String host = "search-challenge-k4o63ifclu555nen5d463di2xu.us-east-2.es.amazonaws.com";
	private static final String endpoint_root = "https://" + host;
	private static final String path = "/";
	private static final String endpoint = endpoint_root + path;
	private static final String pretty = "true";
	private static String responseValue; //contains string value of AmazonHttpResponse (contains results of query)
	
	/**
	 * handleRequest is the AWS Lambda Handler for any API Requests coming in to .../gbif
	 * Generates a requests based on the incoming query to search the Global Biodiversity Information Facility Database
	 * Queries an ElasticSearch instance using an AWS4 Signed Query
	 * @param input = API Request event input {LinkedHashMap}
	 * @param context = context of the environment and execution
	 * Query String parameters possible:
	 * key sex sciName kingdom phylum class order family 
	 * genus species country vernacularName year rightsHolder
	 */
    public String handleRequest(Object input, Context context) {
    	//converts the incoming input to a Json using Google's GSON library
    	JsonParser parser = new JsonParser();
    	String jsonString = new Gson().toJson(input, Map.class);
    	JsonObject obj = parser.parse(jsonString).getAsJsonObject();
    	
    	//test if map is empty, then return full database if so
    	if(obj.get("params").getAsJsonObject().get("querystring").getAsJsonObject().size()==0) {
    		System.out.println("in map-empty");
    		Request<?> request = fullRequest();
    		performSigningSteps(request);
    		sendRequest(request);
    		return responseValue.toString();
    	}
    	
    	//creates a request from the queries provided (ex: matching Kingdoms and Sex)
		Request<?> request = generateRequest(obj.get("params").getAsJsonObject().get("querystring"));
		System.out.println("made request");
		
		//signs the request using AWS4
		performSigningSteps(request);
		System.out.println("signed request");
		
		//make the request
		sendRequest(request);
		return responseValue.toString();
    }
	
    /**
     * Sets the headers and parameters for the AWS Request
     * content must contain the ElasticSearch query body (generated using generatePayload())
     * Sets the endpoint, headers, and parameters for query (if any)
     * @param map Json mapping of query
     * @return AWS Request object
     */
	private static Request<?> generateRequest(JsonElement map) {
		Request<?> request = new DefaultRequest<Void>(service_name);
		request.setContent(new ByteArrayInputStream(generatePayload(map).getBytes()));
		request.addHeader("Content-type", "application/json");
		request.setEndpoint(URI.create(endpoint));
		request.setResourcePath("/gbif/_search");
		request.setHttpMethod(HttpMethodName.GET);
		request.addParameter("pretty",pretty);
		System.out.println(request.getResourcePath());
		return request;
	}
	
	/*
	 * Similar to generateRequest, but makes a full request to the DB
	 */
	private static Request<?> fullRequest() {
		Request<?> request = new DefaultRequest<Void>(service_name);
		request.setContent(new ByteArrayInputStream("".getBytes()));
		request.addHeader("Content-type", "application/json");
		request.setEndpoint(URI.create(endpoint));
		request.setResourcePath("/gbif/_search");
		request.setHttpMethod(HttpMethodName.GET);
		request.addParameter("pretty",pretty);
		request.addParameter("q", "*");
		return request;
	}
	
	/**
	 * Generates the ElasticSearch Query correlating to the HTTP Get query parameters
	 * @param map query string mapping as key, value pairs
	 * @return JSON formatted string in format of an ES query -> to be used as request content
	 */
	private static String generatePayload(JsonElement map) {
		StringBuilder sb = new StringBuilder();
		
		for(Map.Entry<String, JsonElement> entry: map.getAsJsonObject().entrySet()) {
			/*
			 * If a query includes a "sex" query, make sure to explicitly look for male vs female
			 * because "male" can match with both "female" and "male" in ES.
			 */
			if(entry.getKey() == "sex") {		
				String temp = "{\"term\" : { \"" + entry.getKey() + "\":\"" + entry.getValue() + "\"},";
				sb.append(temp);
				continue;
			}
			String temp = "{\"match\": {";
			sb.append(temp);
			temp = "\"" + entry.getKey() + "\":";
			sb.append(temp);
			temp =  entry.getValue() + "}},";
			sb.append(temp);
		}
		
		sb.deleteCharAt(sb.toString().lastIndexOf(','));
		String json = "{\"query\": {"+
								"\"bool\": {"+ 
									"\"should\": [" +
										sb.toString() +
							"]}"+
						"}}";
		System.out.println(json);
		return json;
	}
	
	/**
	 * Performs AWS4 Signing steps according to AWS ElasticSearch/API Gateway Best Practices
	 * Uses credentials stored in Environment Variables to authenticate as an authorized IAM user 
	 * @param requestToSign request object to be signed
	 * @return signed request (AWS Request Object)
	 */
	private static Request<?> performSigningSteps(Request<?> requestToSign) {
		AWS4Signer signer = new AWS4Signer();
		signer.setServiceName(service_name);
		signer.setRegionName(regionName);
		
		//get credentials from environment
		AWSCredentialsProvider credsProvider = new DefaultAWSCredentialsProviderChain();
		AWSCredentials creds = credsProvider.getCredentials();
		
		signer.sign(requestToSign, creds);
		return requestToSign;
	}
	
	/**
	 * Uses an AmazonHttpClient to execute a request (query) on the host (ES Cluster)
	 * @param request
	 */
	private static void sendRequest(Request<?> request) {
		ExecutionContext context = new ExecutionContext(true);
		
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		AmazonHttpClient client = new AmazonHttpClient(clientConfiguration);
		
		MyHttpResponseHandler<Void> responseHandler = new MyHttpResponseHandler<Void>();
		MyErrorHandler errorHandler = new MyErrorHandler();
		Response<Void> response = client.execute(request, responseHandler, errorHandler, context);
	}
	
	/*
	 * Helper Method to convert an InputStream to a String
	 */
	public static String convertStreamToString(java.io.InputStream is) {
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		String x = sb.toString();
		return x;
	}
	
	public static class MyHttpResponseHandler<T> implements HttpResponseHandler<AmazonWebServiceResponse<T>> {
		
		@Override
		public AmazonWebServiceResponse<T> handle(com.amazonaws.http.HttpResponse response)	throws Exception {
			InputStream responseStream = response.getContent();
			String responseString = convertStreamToString(responseStream);
			responseValue = responseString;
			
			
			AmazonWebServiceResponse<T> awsResponse = new AmazonWebServiceResponse<T>();
			return awsResponse;
		}
		
		@Override
		public boolean needsConnectionLeftOpen() {
			return false;
		}
		
	}
	
	public static class MyErrorHandler implements HttpResponseHandler<AmazonServiceException> {
		@Override
		public AmazonServiceException handle(com.amazonaws.http.HttpResponse response) throws Exception {
			System.out.println("In error handler!");
			AmazonServiceException ase = new AmazonServiceException("!!Service Exception!!");
			ase.setStatusCode(response.getStatusCode());
			ase.setErrorCode(response.getStatusText());
			return ase;
		}
		
		public boolean needsConnectionLeftOpen() {
			return false;
		}
	}

}
