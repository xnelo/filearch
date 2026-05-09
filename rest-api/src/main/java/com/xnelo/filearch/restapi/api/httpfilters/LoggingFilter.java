package com.xnelo.filearch.restapi.api.httpfilters;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

public class LoggingFilter {
  private static final String MDC_CORRELATION_ID_KEY = "correlation_id";
  private static final String TIMER_PROPERTY_KEY = "ENPOINT_START_TIME";

  @Inject Logger logger;

  @ServerRequestFilter(priority = 10000)
  public void preFilter(ContainerRequestContext requestContext) {
    MDC.put(MDC_CORRELATION_ID_KEY, UUID.randomUUID().toString());

    logger.infof(
        "Begin request. URI:[%s] METHOD:[%s]",
        requestContext.getUriInfo().getAbsolutePath(), requestContext.getMethod());

    long startTime = System.nanoTime();
    requestContext.setProperty(TIMER_PROPERTY_KEY, startTime);
  }

  @ServerResponseFilter(priority = 10000)
  public void postFilter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    long startTime = (long) requestContext.getProperty(TIMER_PROPERTY_KEY);
    long endTime = System.nanoTime();
    long diff = endTime - startTime;
    double diffMS = (double) diff / 1_000_000.0;

    logger.infof(
        "End request. URI:[%s] STATUS_CODE:[%d] DURATION:[%f ms]",
        requestContext.getUriInfo().getAbsolutePath(), responseContext.getStatus(), diffMS);

    MDC.remove(MDC_CORRELATION_ID_KEY);
  }
}
