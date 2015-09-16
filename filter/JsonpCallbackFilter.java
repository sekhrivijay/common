package com.sears.search.deals.servlet.filter;


import com.sears.search.deals.config.GlobalConstants;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@WebFilter("/search/*")
public class JsonpCallbackFilter implements Filter {
    private static Logger logger = Logger.getLogger(JsonpCallbackFilter.class.getName());
    public void init(FilterConfig fConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        @SuppressWarnings("unchecked")
        Map<String, String[]> parms = httpRequest.getParameterMap();

        if (parms.containsKey(GlobalConstants.CALLBACK)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Wrapping response with JSONP " + parms.get(GlobalConstants.CALLBACK)[0] );
            }
            OutputStream out = httpResponse.getOutputStream();
            GenericResponseWrapper wrapper = new GenericResponseWrapper(httpResponse);
            chain.doFilter(request, wrapper);
            out.write((parms.get(GlobalConstants.CALLBACK)[0] + GlobalConstants.LEFT_PARENTHESIS).getBytes());
            out.write(wrapper.getData());
            out.write(GlobalConstants.RIGHT_PARENTHESIS_SEMI_COLON.getBytes());
            wrapper.setContentType(GlobalConstants.TEXT_JAVA_SCRIPT);
            out.close();
        } else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
    }
}