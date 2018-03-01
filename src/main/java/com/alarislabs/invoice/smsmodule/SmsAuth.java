package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.HttpException;
import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.Response;
import com.alarislabs.invoice.common.Auth;
import com.alarislabs.invoice.common.AuthType;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SmsAuth {

    static int getAuthenticAccId(HttpServletRequest request, int accId, String username, String password) throws HttpException {


        String remoteHost = Response.getRemoteAddr(request);

        if (accId > 0 && Conf.isTrustedIp(remoteHost)) {

            //1. Trusted host, no authorization needed
            return accId;

        } else if (accId > 0) {

            //2. Authorization using token
            Auth auth = new Auth(request.getHeader("authorization"), remoteHost);

            //Extract carrier ID
            int carrierId;
            try {

                String key = "account:" + Integer.toString(accId);

                if (!Conf.getMainRedis().hexists(key, "car_id"))
                    throw new HttpException("Account ID:" + accId + " not found", HttpStatus.BAD_REQUEST);
                else
                    carrierId = Integer.parseInt(Conf.getMainRedis().hget(key, "car_id"));

            } catch (Exception e) {
                if (e instanceof HttpException)
                    throw (HttpException)e;
                else
                    throw new HttpException("Cannot get data for account from Redis", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (auth.getAuthType() != AuthType.Bearer) {

                throw new HttpException("Bearer authorization expected, got " + auth.getAuthType(), HttpStatus.FORBIDDEN);

            } else if (carrierId != auth.getCarrierId()) {

                throw new HttpException("Account ID:" + accId + " of Carrier ID: " + carrierId + " not corresponding with token Carrier ID: " + auth.getCarrierId(), HttpStatus.FORBIDDEN);

            } else {
                //Authorization successful
                return accId;
            }

        } else if (username != null && password != null) {

            String[] ipParts = remoteHost.split("\\.");

            if (ipParts.length < 4)
                throw new HttpException("Invalid remote host: expected IP address, got " + remoteHost, HttpStatus.INTERNAL_SERVER_ERROR);

            List<String> wildCardForms = new ArrayList<>();
            wildCardForms.add(remoteHost);
            wildCardForms.add(ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".*");
            wildCardForms.add(ipParts[0] + "." + ipParts[1] + ".*.*");
            wildCardForms.add(ipParts[0] + ".*.*.*");
            wildCardForms.add("*.*.*.*");

            //3. Authorization using connection
            for(String item : wildCardForms) {

                Map<String,String> map;

                try {
                    map = Conf.getMainRedis().hgetAll("sms_rtl_connection:" + item + ":" + username);
                } catch (Exception e) {
                    throw new HttpException("Cannot get data from Redis, please contact your administrator", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                if (map != null && password.equals(map.get("pwd")) && map.containsKey("acc_id")) {
                    return Integer.parseInt(map.get("acc_id"));
                }

            }

            throw new HttpException("Authorization failed for " + username + "@" + remoteHost + ", please check user name, password and host on Connection tab", HttpStatus.FORBIDDEN);

        } else {

            //4. No authorization processed
            throw new HttpException("Either acc_id or username/password must be set", HttpStatus.BAD_REQUEST);

        }

    }

}
