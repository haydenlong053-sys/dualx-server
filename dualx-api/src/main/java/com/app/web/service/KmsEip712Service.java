package com.app.web.service;

import java.util.Map;

public interface KmsEip712Service {

    String getWalletAddress();

    Map<String, Object> signEip712(String message,String domainName,String domainVersion,Long chainId,String verifyingContract);


}
