/*
 * -----------------------------------------------------------------------\
 * SilverWare
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.silverware.microservices.providers.http.invoker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.silverware.microservices.Context;
import org.silverware.microservices.MicroserviceMetaData;
import org.silverware.microservices.providers.MicroserviceProvider;
import org.silverware.microservices.silver.HttpInvokerSilverService;
import org.silverware.microservices.silver.HttpServerSilverService;
import org.silverware.microservices.silver.ProvidingSilverService;
import org.silverware.microservices.silver.cluster.ServiceHandle;
import org.silverware.microservices.silver.http.ServletDescriptor;
import org.silverware.microservices.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class HttpInvokerMicroserviceProvider implements MicroserviceProvider, HttpInvokerSilverService {

   private static final Logger log = LogManager.getLogger(HttpInvokerMicroserviceProvider.class);

   private Context context;
   private HttpServerSilverService http;
   private Set<ProvidingSilverService> microserviceProviders = new HashSet<>();
   private List<ServiceHandle> inboundHandles = new ArrayList<>();

   @Override
   public void initialize(final Context context) {
      this.context = context;

      context.getProperties().putIfAbsent(INVOKER_URL, "invoker");
   }

   @Override
   public Context getContext() {
      return context;
   }

   @Override
   public void run() {
      try {
         log.info("Hello from Http Invoker microservice provider!");
         context.getAllProviders(ProvidingSilverService.class).stream().forEach(silverService -> microserviceProviders.add((ProvidingSilverService) silverService));

         try {
            if (log.isDebugEnabled()) {
               log.debug("Waiting for the Http Microservice provider.");
            }

            while (!Thread.currentThread().isInterrupted()) {

               if (http == null) {
                  http = (HttpServerSilverService) context.getProvider(HttpServerSilverService.class);

                  if (http != null) {
                     if (log.isDebugEnabled()) {
                        log.debug("Discovered Http Silverservice: " + http.getClass().getName());
                     }

                     log.info("Deploying Http Invoker...");

                     http.deployServlet((String) context.getProperties().get(INVOKER_URL), "", Collections.singletonList(getServletDescriptor()));

                     final String invokerUrl = "http://" + context.getProperties().get(HttpServerSilverService.HTTP_SERVER_ADDRESS) + ":" +
                           context.getProperties().get(HttpServerSilverService.HTTP_SERVER_PORT) + "/" +
                           context.getProperties().get(INVOKER_URL) + "/";

                     if (log.isTraceEnabled()) {
                        log.trace("Waiting for invoker to appear at {}", invokerUrl);
                     }

                     if (!Utils.waitForHttp(invokerUrl, 200)) {
                        throw new InterruptedException("Unable to start Http Invoker.");
                     }
                  }
               }

               Thread.sleep(1000);
            }
         } catch (InterruptedException ie) {
            Utils.shutdownLog(log, ie);
         } finally {
         }
      } catch (Exception e) {
         log.error("Http Invoker microservice provider failed: ", e);
      }
   }

   private ServletDescriptor getServletDescriptor() {
      final Properties properties = new Properties();
      properties.setProperty("debug", "false");
      properties.setProperty("historyMaxEntries", "10");
      properties.setProperty("debugMaxEntries", "100");
      properties.setProperty("maxDepth", "15");
      properties.setProperty("maxCollectionSize", "1000");
      properties.setProperty("maxObjects", "0");
      properties.setProperty("detectorOptions", "{}");
      properties.setProperty("canonicalNaming", "true");
      properties.setProperty("includeStackTrace", "true");
      properties.setProperty("serializeException", "false");
      properties.setProperty("discoveryEnabled", "true");

      return null; //new ServletDescriptor("jolokia-agent", org.jolokia.http.AgentServlet.class, "/", properties);
   }

   protected List<ServiceHandle> assureHandles(final MicroserviceMetaData metaData) {
      List<ServiceHandle> result = inboundHandles.stream().filter(serviceHandle -> serviceHandle.getQuery().equals(metaData)).collect(Collectors.toList());
      Set<Object> microservices = context.lookupLocalMicroservice(metaData);//.stream().map(service -> new ServiceHandle((String) context.getProperties().get(HttpServerSilverService.HTTP_SERVER_ADDRESS), metaData, service)).collect(Collectors.toSet());
      Set<Object> haveHandles = result.stream().map(ServiceHandle::getService).collect(Collectors.toSet());
      microservices.removeAll(haveHandles);

      microservices.forEach(microservice -> {
         final ServiceHandle handle = new ServiceHandle((String) context.getProperties().get(HttpServerSilverService.HTTP_SERVER_ADDRESS), metaData, microservice);
         result.add(handle);
         inboundHandles.add(handle);
      });

      return result;
   }

   /**
    * @author Martin Večeřa <marvenec@gmail.com>
    */
   public class HttpInvokerServlet extends HttpServlet {

      private ObjectMapper mapper = new ObjectMapper();

      @Override
      protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
         if (req.getContextPath().endsWith("query")) {
            MicroserviceMetaData metaData = mapper.readValue(req.getInputStream(), MicroserviceMetaData.class);
            List<ServiceHandle> handles = assureHandles(metaData);
            mapper.writeValue(resp.getWriter(), handles);
         }
         super.doPost(req, resp);
      }
   }
}