package com.app.web.msg;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

public class EmailSenderUtil {

    protected String sha256(String str) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] inputByteArray = str.getBytes(StandardCharsets.UTF_8);
        messageDigest.update(inputByteArray);
        byte[] resultByteArray = messageDigest.digest();
        return byteArrayToHex(resultByteArray);
    }

    public String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }
    
    public int getRandom() {
        return ThreadLocalRandom.current().nextInt(100000, 1000000);
    }
    
    public HttpURLConnection getPostHttpConn(String url) throws Exception {
        URL object = new URL(url);
        HttpURLConnection conn;
        conn = (HttpURLConnection) object.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        return conn;
	}
    
  
    public EmailSingleSenderResult jsonToEmailSingleSenderResult(JsonNode json) {
    	EmailSingleSenderResult result = new EmailSingleSenderResult();
    	
    	result.result = json.path("result").asInt();
    	result.errmsg = json.path("errmsg").asText();
    	if (0 == result.result) {
	    	result.sequenceId = json.path("sequenceId").asText();
	    	JsonNode surplus = json.get("surplus");
	    	if(surplus != null && !surplus.isNull()) {
	    		result.surplus = surplus.asInt();
	    	}
    	}
    	return result;
    }

   
    

}
