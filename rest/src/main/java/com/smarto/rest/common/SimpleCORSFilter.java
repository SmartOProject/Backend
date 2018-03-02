package com.smarto.rest.common;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class SimpleCORSFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE");
        response.setHeader("Access-Control-Max-Age", "0");

        //List of allowed headers in request
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Origin, Host, " + Response.QUERY_TOTAL_COUNT_HEADER);

        //List of allowed headers in response
        response.setHeader("Access-Control-Expose-Headers", Response.TOTAL_COUNT_HEADER
                + ", " + Response.FETCHED_COUNT_HEADER
                + ", " + Response.HAS_MORE_ROWS_HEADER);


        chain.doFilter(request, res);

    }

    public void init(FilterConfig filterConfig) {}

    public void destroy() {}

}