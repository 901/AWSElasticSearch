package com.amazonaws.lambda.shakespeare;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

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

public class LambdaFunctionHandler implements RequestHandler<Object, String> {

	private static final String service_name = "es";
	private static final String regionName = "us-east-2";
	private static final String host = "search-challenge-k4o63ifclu555nen5d463di2xu.us-east-2.es.amazonaws.com";
	private static final String endpoint_root = "https://" + host;
	private static final String path = "/";
	private static final String endpoint = endpoint_root + path;
	private static final String pretty = "true";
	private static String responseValue; //contains string value of AmazonHttpResponse (contains results of query)
	
    @Override
    public String handleRequest(Object input, Context context) {
    	
		Request<?> request = generateRequest();
		performSigningSteps(request);
		sendRequest(request);
        return responseValue.toString();
    }
	
	private static Request<?> generateRequest() {
		Request<?> request = new DefaultRequest<Void>(service_name);
		request.setContent(new ByteArrayInputStream(generatePayload().getBytes()));
		request.addHeader("Content-type", "application/json");
		request.setEndpoint(URI.create(endpoint));
		request.setResourcePath("/shakespeare/_search");
		request.setHttpMethod(HttpMethodName.GET);
		request.addParameter("pretty",pretty);
		System.out.println(request.getResourcePath());
		return request;
	}
	
	private static String generatePayload() {
		String json = "{\"query\": "+
							"{"+
								"\"nested\": {"+
									"\"path\": \"dataset\"," +
									"\"query\": {" + 
										"\"bool\":  {" + 
											"\"must\": {" +
												"\"match\": {" + 
													"\"dataset.description\": \"Chem\"}"
													+ "}]}}}}}";	
		return json;
	}
	
	private static Request<?> performSigningSteps(Request<?> requestToSign) {
		AWS4Signer signer = new AWS4Signer();
		signer.setServiceName(service_name);
		signer.setRegionName(regionName);
		
		//get credentials
		AWSCredentialsProvider credsProvider = new DefaultAWSCredentialsProviderChain();
		AWSCredentials creds = credsProvider.getCredentials();
		
		signer.sign(requestToSign, creds);
		return requestToSign;
	}
	
	private static void sendRequest(Request<?> request) {
		ExecutionContext context = new ExecutionContext(true);
		
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		AmazonHttpClient client = new AmazonHttpClient(clientConfiguration);
		
		MyHttpResponseHandler<Void> responseHandler = new MyHttpResponseHandler<Void>();
		MyErrorHandler errorHandler = new MyErrorHandler();
		Response<Void> response = client.execute(request, responseHandler, errorHandler, context);
	}
	
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
