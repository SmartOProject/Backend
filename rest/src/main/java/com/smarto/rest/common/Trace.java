package com.smarto.rest.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

class Trace {

    private static final Logger Log = LoggerFactory.getLogger(Trace.class);

    private static boolean enableTraceRequest;
    private static boolean enableTraceResponse;

    static void setEnableTraceRequest(boolean enableTraceRequest) {
        Trace.enableTraceRequest = enableTraceRequest;
    }

    static void setEnableTraceResponse(boolean enableTraceResponse) {
        Trace.enableTraceResponse = enableTraceResponse;
    }

    static void traceRequest(HttpServletRequest request, String body) {

        if (enableTraceRequest) {

            StringBuilder buf = new StringBuilder();

            buf.append("\n  Request ");
            buf.append(request.getMethod());
            buf.append(":");
            buf.append(request.getRequestURI());
            buf.append("\n");

            //Headers
            buf.append("  Headers\n");
            for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements(); ) {

                String header = headerNames.nextElement();
                buf.append("    ");
                buf.append(header);
                buf.append(" -> ");
                buf.append(request.getHeader(header));
                buf.append("\n");
            }

            //Parameters
            boolean flag = true;
            for (Enumeration<String> parameterNames = request.getParameterNames(); parameterNames.hasMoreElements(); ) {

                if (flag) {
                    buf.append("  Parameters\n");
                    flag = false;
                }

                String parameter = parameterNames.nextElement();
                buf.append("    ");
                buf.append(parameter);
                buf.append(":");
                buf.append(request.getParameter(parameter));
                buf.append("\n");
            }

            //Body
            if (body != null) {
                buf.append("  Body: ");
                buf.append(body);
            }

            Log.info(buf.toString());
        }
    }

    static void traceResponse(Response response) {

        if (enableTraceResponse) {

            Log.info("\n  Response"
                    + "\n    Http status -> " + response.getStatus()
                    + "\n    Body -> " + response.getBody()
            );
        }
    }


}
