package com.app.web.msg;

public class EmailSingleSenderResult {

	public int result;
	public String errmsg ;
	public Integer surplus;
	public String sequenceId;
	@Override
	public String toString() {
		return "EmailSingleSenderResult [result=" + result + ", errmsg=" + errmsg + ", surplus=" + surplus
				+ ", sequenceId=" + sequenceId + "]";
	}
	
	
}
