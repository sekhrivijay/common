package com.sears.search.deals.servlet;

import com.google.gson.Gson;
import com.monitoring.deals.DealsServiceJmx;
import com.monitoring.deals.DealsServiceJmxMBean;
import com.sears.search.deals.config.GlobalConstants;
import com.sears.search.deals.jmx.JmxHelper;
import com.sears.search.deals.jmx.util.JmxUtil;
import com.sears.search.deals.service.QueryService;
import com.sears.search.deals.service.impl.QueryServiceImpl;
import com.sears.search.deals.service.request.ServiceRequest;
import com.sears.search.deals.util.MiscUtil;
import com.sears.search.deals.util.ServletUtil;
import com.sears.search.deals.util.SolrServerInitializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.concurrent.*;

@WebServlet(name = "/QueryServlet", urlPatterns = "/search")
public class QueryServlet extends HttpServlet {
    private static Logger logger = Logger.getLogger(QueryServlet.class.getName());
    private static DealsServiceJmxMBean autofillServiceJmxMBean = new DealsServiceJmx();
    private QueryService queryService;
    private ExecutorService executorService;

    @Override
    public void init() throws ServletException {
        SolrClient solrClient = SolrServerInitializer.INSTANCE.getSolrClient();
        Gson gson = new Gson();
        executorService = Executors.newFixedThreadPool(GlobalConstants.SERVICE_THREAD_POOL_SIZE);

        queryService = new QueryServiceImpl(solrClient, executorService, gson);
        JmxUtil.startJmxTimer();
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(autofillServiceJmxMBean, new ObjectName(autofillServiceJmxMBean.getClass().getPackage().getName() + ":type=" + autofillServiceJmxMBean.getClass().getSimpleName()));
        } catch (JMException exception) {
            logger.error(exception);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        response.setContentType(GlobalConstants.APPLICATION_JSON);
        if (StringUtils.isEmpty(request.getParameter(GlobalConstants.Q))) {
            return;
        }
        ServiceRequest serviceRequest = ServletUtil.buildServiceRequest(request);
        JmxHelper jmxHelper = JmxUtil.getJmxHelper(serviceRequest);
        try (PrintWriter out = response.getWriter()) {
            jmxHelper.incrementTotalRequestCount();
            out.println(queryService.query(serviceRequest));
            long totalTime = System.currentTimeMillis() - startTime;
            jmxHelper.addTotalResponseTimeSite(totalTime);
            MiscUtil.logTotalTimeTaken(logger, "QueryServlet", startTime);
        } catch (TimeoutException timeoutException) {
            jmxHelper.incrementTotalTimeoutCount();
            jmxHelper.incrementExceptionCount();
            logger.error(timeoutException);
            throw new ServletException(timeoutException);
        } catch (SolrServerException | ExecutionException | InterruptedException ex) {
            jmxHelper.incrementExceptionCount();
            logger.error(ex);
            throw new ServletException(ex);
        }
    }


    @Override
    public void destroy() {
        super.destroy();
        try {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            executorService.shutdownNow();

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
