package com.app.web.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
 
public class EmailSingleSender {
	private static final String URL = "https://live.moduyun.com/directmail/v1/singleSendMail";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final String accesskey;
	private final String secretkey;
	
	private final EmailSenderUtil util = new EmailSenderUtil();

	public EmailSingleSender(String accesskey,String secretkey) {
		this.accesskey = accesskey;
		this.secretkey = secretkey;
	}

	
	/**
	 * 单个邮件发送
	 * @param type 邮件类型，0 事务投递，其他的为商业投递量
	 * @param fromEmail 用户管理控制台中配置的发信地址
	 * @param toEmail  邮件接收地址
	 * @param needToReply  是否需要回复邮件
	 * @param replyEmail 回复邮件的邮件地址
	 * @param fromAlias  发信人昵称
	 * @param subject  邮件主题
	 * @param htmlBody 邮件内容
	 * @param clickTrace 数据跟踪功能  1 为打开数据跟踪功能; 0 为关闭数据跟踪功能。该参数默认值为0。
	 * @param ext  拓展字段
	 * @return
	 * @throws Exception
	 */
	public EmailSingleSenderResult send(
			int type,
			String fromEmail,
			String toEmail,
			String fromAlias,
			boolean needToReply,
			String replyEmail,
			String subject,
			String htmlBody,
			String clickTrace,
			String ext) throws Exception {
		if(type<0||type>1) {
			type=0;
		}

		if (null == ext) {
			ext = "";
		}

		// 按照协议组织 post 请求包体
        long random = util.getRandom();
        long curTime = System.currentTimeMillis()/1000;

		Map<String, Object> data = new LinkedHashMap<>();

        data.put("sig", util.sha256(String.format("secretkey=%s&random=%d&time=%d&fromEmail=%s",
        		secretkey, random, curTime, fromEmail)));
        data.put("time", curTime);
        data.put("type", type);
        data.put("fromEmail", fromEmail);
        data.put("needToReply", needToReply);
        data.put("toEmail", toEmail);
        data.put("fromAlias", fromAlias);
        data.put("subject", subject);
        data.put("htmlBody", htmlBody);
        data.put("clickTrace", clickTrace);
        data.put("ext", ext);
        if(needToReply) {
        	 data.put("replyEmail", replyEmail);
        }else {
        	 data.put("replyEmail", "");
        }

        // 与上面的 random 必须一致
		String wholeUrl = String.format("%s?accesskey=%s&random=%d", URL, accesskey, random);
        HttpURLConnection conn = util.getPostHttpConn(wholeUrl);

        try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
            wr.write(OBJECT_MAPPER.writeValueAsString(data));
            wr.flush();
        }

        // 显示 POST 请求返回的内容
        StringBuilder sb = new StringBuilder();
        int httpRspCode = conn.getResponseCode();
        EmailSingleSenderResult result;
        if (httpRspCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            
            JsonNode json = OBJECT_MAPPER.readTree(sb.toString());
            result = util.jsonToEmailSingleSenderResult(json);
        } else {
        	result = new EmailSingleSenderResult();
        	result.result = httpRspCode;
        	result.errmsg = "http error " + httpRspCode + " " + conn.getResponseMessage();
        }
        
        return result;
	}

}
