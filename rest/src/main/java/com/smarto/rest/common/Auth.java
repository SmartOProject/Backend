package com.smarto.rest.common;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.regex.Pattern;

public class Auth {

    private AuthType authType;
    private String authData;
    private int carrierId;
    private static final Logger Log = LoggerFactory.getLogger(Auth.class);


    private final static byte[] sharedSecret = getSharedSecret();

    //Generate random secret key
    private static byte[] getSharedSecret() {

        byte[] res = new byte[32];

        try {

            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            Log.trace("IP: " + ip.getHostAddress());
            //Log.trace("Mac: " + Utils.bytesToHex(network.getHardwareAddress()));
            MessageDigest md5 = MessageDigest.getInstance("SHA-256");
            String keyBase = /*Utils.bytesToHex(network.getHardwareAddress()) + */"#" + ip.getHostAddress() + "#" + ip.getHostName();
            res = md5.digest(keyBase.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {

            Log.error("Exception in method Auth.getSystemId(), legacy key generation used", e);
            (new SecureRandom()).nextBytes(res);
        }

        return res;
    }

    //Generate JWT token
    private static String getToken(net.minidev.json.JSONObject payLoad) throws Exception {

        JWSSigner signer = new MACSigner(sharedSecret);
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(payLoad));
        jwsObject.sign(signer);
        return jwsObject.serialize();
    }

    //Wrapper for getToken
    static String getToken(String login, String remoteAddr, int carrierId) throws Exception {


        net.minidev.json.JSONObject payLoad = new net.minidev.json.JSONObject();
        payLoad.put("login", login);
        payLoad.put("remote_addr", remoteAddr);
        payLoad.put("carrier_id", Integer.toString(carrierId));
        Long expDate = System.currentTimeMillis() / 1000 + 3600 /* 1 hour */;
        payLoad.put("exp", expDate.toString());

        return getToken(payLoad);
    }

    //Verify and extract payload from token
    private static net.minidev.json.JSONObject getPayload(String token) {

        try {

            JWSObject jwsObject = JWSObject.parse(token);
            JWSVerifier verifier = new MACVerifier(sharedSecret);

            if (jwsObject.verify(verifier)) {
                return jwsObject.getPayload().toJSONObject();
            }
            else {
                System.out.println("Token verification failed");
                return null;
            }

        }
        catch (Exception e) {
            System.out.println("BAD token");
            return null;
        }

    }

    public Auth(String header, String remoteAddr) {

        if (Utils.hasValue(header)) {

            authType = AuthType.Unauthorized;
            String[] parts = header.split(" ");

            if (parts.length == 2) {

                Pattern bearerPattern = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);
                Pattern basePattern = Pattern.compile("^Basic$", Pattern.CASE_INSENSITIVE);


                if (bearerPattern.matcher(parts[0]).matches()) {

                    net.minidev.json.JSONObject payLoad = getPayload(parts[1]);

                    if (payLoad != null) {
                        if (payLoad.containsKey("remote_addr") && payLoad.containsKey("login") && payLoad.containsKey("exp")) {

                            Long currentTime = System.currentTimeMillis() / 1000;
                            Long expTime = Long.valueOf(payLoad.get("exp").toString());

                            if (!remoteAddr.equals(payLoad.get("remote_addr"))) {
                                authType = AuthType.Expired;
                                authData = "Remote address " + remoteAddr + " not allowed for this token";
                            } else if (currentTime.compareTo(expTime) >= 0) {
                                authType = AuthType.Expired;
                                authData = "Token is expired";
                            } else {
                                authType = AuthType.Bearer;
                                authData = payLoad.get("login").toString();
                                carrierId = Integer.parseInt(payLoad.get("carrier_id").toString());
                            }
                        } else
                            authData = "Payload must contain keys: remote_addr, login, exp";
                    }
                    else
                        authData = "Invalid token";
                }
                else if (basePattern.matcher(parts[0]).matches()) {
                    authType = AuthType.Basic;
                    authData = parts[1];
                }
                else
                    authData = "Authorization type not supported";
            }
            else
                authData = "Authorization header malformed";

        }
        else {
            authType = AuthType.Empty;
            authData = "Authorization header missing";
        }
    }

    public AuthType getAuthType() { return authType; }

    String getAuthData() { return authData; }

    public int getCarrierId() { return carrierId; }

}
