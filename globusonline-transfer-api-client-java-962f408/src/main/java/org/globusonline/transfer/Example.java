package org.globusonline.transfer;

import java.security.GeneralSecurityException;
import java.util.*;
import java.io.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;


import org.json.*;

public class Example {
	private JSONTransferAPIClient client;
	private static DateFormat isoDateFormat =
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");


	//source and destination variables
	
	String sourcePath1 = "/Users/sulakhe/test2/test.txt";
	String sourcePath2 = "/Users/sulakhe/test2/test-1.txt";
	
	String destPath = "/home/ryan/test.txt";
	String destPath1 = "/~/test/test.txt";
	String destPath2 = "/~/test/test-1.txt";


	public Example(JSONTransferAPIClient client) {
		this.client = client;
	}

	/**
	 * Run the program and display usage if it is used incorrectly.
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		
		File file = new File("ErrorTest.java");
		System.out.println(file.getAbsolutePath());
		System.out.println(file.getParent());
		System.out.println((file.getAbsoluteFile()).getParentFile().getParent() );
		
		String sourceEndpoint = "sulakhe#my_laptop";
		String destEndpoint = "go#ep1"; //auto-activation
		//String destEndpoint = "sulakhe#DobynsLabData"; //passphrase-activation

		
		if (args.length < 1) {
			System.err.println(
					"Usage: java org.globusonline.transfer.ExampleNew "
							+ "username [cafile certfile keyfile [baseurl]]]");
			System.exit(1);
		}
		String username = args[0];

		String cafile = null;
		if (args.length > 1 && args[1].length() > 0)
			cafile = args[1];

		String certfile = null;
		if (args.length > 2 && args[2].length() > 0)
			certfile = args[2];

		String keyfile = null;
		if (args.length > 3 && args[3].length() > 0)
			keyfile = args[3];

		String baseUrl = null;
		if (args.length > 4 && args[4].length() > 0)
			baseUrl = args[4];

		try {
			JSONTransferAPIClient c = new JSONTransferAPIClient(username,
					cafile, certfile, keyfile, baseUrl);
			System.out.println("base url: " + c.getBaseUrl());
			Example e = new Example(c);

			//e.runAutoActivation("ryan#ryan-laptop", "go#ep1");
			//e.runPasswordActivation(destEndpoint, "ep1_user", "dobyns");
			e.autoActivate(sourceEndpoint);
			e.autoActivate(destEndpoint);
			//e.runPasswordActivation(sourceEndpoint, destEndpoint, "sulakhe", "dobyns");
			
			e.transfer(sourceEndpoint, destEndpoint);

			//display the task that was just completed.
			e.displayTaskList(60);
			
			//e.runProxyActivation();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/** 
	 * 
	 * Perform the transfer with two auto-activating end points: 
	 * 
	 */
	public void runAutoActivation(String sourceEndpoint, String destEndpoint){
		try {
			if (!autoActivate(sourceEndpoint) || !autoActivate(destEndpoint)) {
				System.err.println("Unable to auto activate go tutorial endpoints, "
						+ " exiting");
				return;
			}

			transfer(sourceEndpoint, destEndpoint);

			//display the task that was just completed.
			displayTaskList(60);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void transfer(String sourceEndpoint, String destEndpoint) throws IOException, JSONException, GeneralSecurityException, APIError {



		JSONTransferAPIClient.Result r = client.getResult("/transfer/submission_id");

		String submissionId = r.document.getString("value");
		JSONObject transfer = new JSONObject();
		transfer.put("DATA_TYPE", "transfer");
		transfer.put("submission_id", submissionId);
		JSONObject item = new JSONObject();
		item.put("DATA_TYPE", "transfer_item");
		item.put("source_endpoint", sourceEndpoint);
		item.put("source_path", sourcePath1);
		item.put("destination_endpoint", destEndpoint);
		item.put("destination_path", destPath1);

		JSONObject item2 = new JSONObject();
		item2.put("DATA_TYPE", "transfer_item");
		item2.put("source_endpoint", sourceEndpoint);
		item2.put("source_path", sourcePath2);
		item2.put("destination_endpoint", destEndpoint);
		item2.put("destination_path", destPath2);

		transfer.append("DATA", item);
		transfer.append("DATA", item2);
		
		r = client.postResult("/transfer", transfer, null);

		String taskId = r.document.getString("task_id");
		if (!waitForTask(taskId, 120)) {
			System.out.println(
					"Transfer not complete after 2 minutes, exiting");
			return;
		}	
	}

	/** 
	 *
	 * Perform the transfer with a password-activating end point
	 *  
	 */
	public boolean runPasswordActivation(String destEndpoint, String username, String passphrase){
		try {
			JSONObject res = setActivationRequirements(destEndpoint, username, passphrase);
			
			if (!activate(destEndpoint, res)){
				System.out.println("Failed to activate the endpoint: " + destEndpoint);
				return false;
			}
			
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 
	 * Activate the end point using a proxy - This is not yet working.
	 * 
	 
	public void runProxyActivation()
			throws IOException, JSONException, GeneralSecurityException, APIError {
		printActivationRequirements(destEndpoint,"ryan","qazse4rfv");
		JSONObject reqs = getActivationRequirements(destEndpoint,"ryan","qazse4rfv");
		String pubKey = getPublicKey(reqs);

		if (pubKey != null){
			System.out.println(pubKey);
		}
	}
*/


	public void printActivationRequirements(String endpointName, String username, String passphrase)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		String resource = BaseTransferAPIClient.endpointPath(endpointName)
				+ "/activation_requirements";
		JSONTransferAPIClient.Result r = client.getResult(resource);

		JSONArray fileArray = r.document.getJSONArray("DATA");
		for (int i=0; i < fileArray.length(); i++) {
			JSONObject fileObject = fileArray.getJSONObject(i);
			System.out.println("  " + fileObject.getString("name"));
			Iterator keysIter = fileObject.sortedKeys();
			while (keysIter.hasNext()) {
				String key = (String)keysIter.next();
				if (!key.equals("DATA_TYPE") && !key.equals("LINKS")
						&& !key.endsWith("_link") && !key.equals("name")) {
					System.out.println("    " + key + ": "
							+ fileObject.getString(key));
				}
			}
		}
	}

	public String getPublicKey(JSONObject obj)
			throws IOException, JSONException, GeneralSecurityException, APIError {

		JSONArray contentArr = obj.getJSONArray("DATA");
		for (int i=0; i < contentArr.length(); i++) {
			JSONObject fileObject = contentArr.getJSONObject(i);
			if (fileObject.getString("name").equals("public_key")){
				return fileObject.getString("value");
			}
		}

		return null;
	}

	public JSONObject setActivationRequirements(String endpointName, String username, String passphrase)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		String resource = BaseTransferAPIClient.endpointPath(endpointName)
				+ "/activation_requirements";
		JSONTransferAPIClient.Result r = client.getResult(resource);

		JSONArray fileArray = r.document.getJSONArray("DATA");
		for (int i=0; i < fileArray.length(); i++) {
			JSONObject fileObject = fileArray.getJSONObject(i);
			if (fileObject.getString("name").equals("username")){
				fileObject.put("value", username);
			} else if (fileObject.getString("name").equals("passphrase")){
				fileObject.put("value", passphrase);
			}
		}
		return r.document;
	}

	public boolean activate(String endpointName,JSONObject res)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		String resource = BaseTransferAPIClient.endpointPath(endpointName)
				+ "/activate";
		JSONTransferAPIClient.Result r = client.postResult(resource, res);
		String code = r.document.getString("code");
		if (code.startsWith("AutoActivationFailed")) {
			return false;
		}
		return true;
	}

	public boolean autoActivate(String endpointName)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		String resource = BaseTransferAPIClient.endpointPath(endpointName)
				+ "/autoactivate";
		JSONTransferAPIClient.Result r = client.postResult(resource, null,
				null);
		String code = r.document.getString("code");
		if (code.startsWith("AutoActivationFailed")) {
			return false;
		}
		return true;
	}

	public void displayLs(String endpointName, String path)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		Map<String, String> params = new HashMap<String, String>();
		if (path != null) {
			params.put("path", path);
		}
		String resource = BaseTransferAPIClient.endpointPath(endpointName)
				+ "/ls";
		JSONTransferAPIClient.Result r = client.getResult(resource, params);
		System.out.println("Contents of " + path + " on "
				+ endpointName + ":");

		JSONArray fileArray = r.document.getJSONArray("DATA");
		for (int i=0; i < fileArray.length(); i++) {
			JSONObject fileObject = fileArray.getJSONObject(i);
			System.out.println("  " + fileObject.getString("name"));
			Iterator keysIter = fileObject.sortedKeys();
			while (keysIter.hasNext()) {
				String key = (String)keysIter.next();
				if (!key.equals("DATA_TYPE") && !key.equals("LINKS")
						&& !key.endsWith("_link") && !key.equals("name")) {
					System.out.println("    " + key + ": "
							+ fileObject.getString(key));
				}
			}
		}
	}

	public String copyEndpoint(String endpointName, String copyName)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		String resource = BaseTransferAPIClient.endpointPath(endpointName);
		JSONTransferAPIClient.Result r = client.getResult(resource);
		JSONObject endpoint = r.document;
		endpoint.put("name", copyName);
		endpoint.remove("username");
		endpoint.remove("canonical_name");

		r = client.postResult("/endpoint", endpoint);
		return r.document.getString("code");
	}

	public String setEndpointDescription(String endpointName,
			String description)
					throws IOException, JSONException, GeneralSecurityException, APIError {
		String resource = BaseTransferAPIClient.endpointPath(endpointName);
		JSONObject o = new JSONObject();
		o.put("DATA_TYPE", "endpoint");
		o.put("description", description);

		JSONTransferAPIClient.Result r = client.putResult(resource, o);
		return r.document.getString("code");
	}

	public String deleteEndpoint(String endpointName)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		String resource = BaseTransferAPIClient.endpointPath(endpointName);
		JSONTransferAPIClient.Result r = client.deleteResult(resource);
		return r.document.getString("code");
	}

	//Task based functions

	public void displayTasksummary()
			throws IOException, JSONException, GeneralSecurityException, APIError {
		JSONTransferAPIClient.Result r = client.getResult("/tasksummary");
		System.out.println("Task Summary for " + client.getUsername()
				+ ": ");
		Iterator keysIter = r.document.sortedKeys();
		while (keysIter.hasNext()) {
			String key = (String)keysIter.next();
			if (!key.equals("DATA_TYPE"))
				System.out.println("  " + key + ": "
						+ r.document.getString(key));
		}
	}

	public boolean waitForTask(String taskId, int timeout)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		String status = "ACTIVE";
		JSONTransferAPIClient.Result r;

		String resource = "/task/" +  taskId;
		Map<String, String> params = new HashMap<String, String>();
		params.put("fields", "status");

		while (timeout > 0 && status.equals("ACTIVE")) {
			r = client.getResult(resource, params);
			status = r.document.getString("status");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				return false;
			}
			timeout -= 10;
		}

		if (status.equals("ACTIVE"))
			return false;
		return true;
	}

	public void displayTaskList(long maxAge)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		Map<String, String> params = new HashMap<String, String>();
		if (maxAge > 0) {
			long minTime = System.currentTimeMillis() - 1000 * maxAge;
			params.put("filter", "request_time:"
					+ isoDateFormat.format(new Date(minTime)) + ",");
		}
		JSONTransferAPIClient.Result r = client.getResult("/task_list",
				params);

		int length = r.document.getInt("length");
		if (length == 0) {
			System.out.println("No tasks were submitted in the last "
					+ maxAge + " seconds");
			return;
		}
		JSONArray tasksArray = r.document.getJSONArray("DATA");
		for (int i=0; i < tasksArray.length(); i++) {
			JSONObject taskObject = tasksArray.getJSONObject(i);
			System.out.println("Task " + taskObject.getString("task_id")
					+ ":");
			displayTask(taskObject);
		}
	}

	private static void displayTask(JSONObject taskObject)
			throws JSONException {
		Iterator keysIter = taskObject.sortedKeys();
		while (keysIter.hasNext()) {
			String key = (String)keysIter.next();
			if (!key.equals("DATA_TYPE") && !key.equals("LINKS")
					&& !key.endsWith("_link")) {
				System.out.println("  " + key + ": "
						+ taskObject.getString(key));
			}
		}
	}
}
