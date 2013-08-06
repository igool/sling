/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.impl;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.Constants;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.util.SimpleConstraintChecker;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks a single JMX attribute */
@Component(configurationFactory=true, policy=ConfigurationPolicy.REQUIRE, metatype=true)
@Service
public class JmxAttributeHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<String, String> info = new HashMap<String, String>();
    private String mbeanName;
    private String attributeName;
    private String constraint;

    @Property
    public static final String PROP_OBJECT_NAME = "mbean.name";
    
    @Property
    public static final String PROP_ATTRIBUTE_NAME = "attribute.name";
    
    @Property
    public static final String PROP_CONSTRAINT = "attribute.value.constraint";
    
    @Property(cardinality=50)
    public static final String PROP_TAGS = Constants.HC_TAGS;
    
    @Property
    public static final String PROP_NAME = Constants.HC_NAME;
    
    @Activate
    public void activate(ComponentContext ctx) {
        mbeanName = PropertiesUtil.toString(ctx.getProperties().get(PROP_OBJECT_NAME), "");
        attributeName = PropertiesUtil.toString(ctx.getProperties().get(PROP_ATTRIBUTE_NAME), "");
        constraint = PropertiesUtil.toString(ctx.getProperties().get(PROP_CONSTRAINT), "");
        
        info.put(PROP_OBJECT_NAME, mbeanName);
        info.put(PROP_ATTRIBUTE_NAME, attributeName);
        info.put(PROP_CONSTRAINT, constraint);
        info.put(Constants.HC_NAME, PropertiesUtil.toString(ctx.getProperties().get(Constants.HC_NAME), ""));
        info.put(Constants.HC_TAGS, 
                Arrays.asList(PropertiesUtil.toStringArray(ctx.getProperties().get(Constants.HC_TAGS), new String[] {})).toString());
        
        log.info("Activated with HealthCheck name={}, objectName={}, attribute={}, constraint={}", 
                new Object[] { info.get(Constants.HC_NAME), mbeanName, attributeName, constraint });
    }
    
    @Override
    public Result execute(ResultLog log) {
        final Result result = new Result(this, log);
        log.debug("Checking {} / {} with constraint {}", new Object[] { mbeanName, attributeName, constraint });
        try {
            final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName objectName = new ObjectName(mbeanName);
            if(jmxServer.queryNames(objectName, null).size() == 0) {
                log.error("MBean not found: {}", objectName);
            }
            final Object value = jmxServer.getAttribute(objectName, attributeName);
            log.debug("{} {} returns {}", new Object[] { mbeanName, attributeName, value });
            new SimpleConstraintChecker().check(value, constraint, log);
        } catch(Exception e) {
            log.warn(e.toString(), e);
        }
        return result;
    }

    @Override
    public Map<String, String> getInfo() {
        return info;
    }
}
