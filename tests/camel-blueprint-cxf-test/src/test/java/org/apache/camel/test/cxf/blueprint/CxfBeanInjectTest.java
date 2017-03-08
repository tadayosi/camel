/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.test.cxf.blueprint;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.component.cxf.CXFTestSupport;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.cxf.BusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.junit.Test;

public class CxfBeanInjectTest extends CamelBlueprintTestSupport {

    private final String routerEndpointAddress = String.format(
        "http://localhost:%s/%s/router", CXFTestSupport.getPort1(), getClass().getSimpleName());

    @Override
    protected String getBlueprintDescriptor() {
        return "org/apache/camel/test/cxf/blueprint/CxfBeanInject.xml";
    }

    @Override
    protected String getBundleDirectives() {
        return "blueprint.aries.xml-validation:=false";
    }

    @Override
    protected String useOverridePropertiesWithConfigAdmin(Dictionary<String, String> props) {
        props.put("router.address", routerEndpointAddress);
        props.put("router.port", Integer.toString(CXFTestSupport.getPort1()));
        return "my-placeholders";
    }

    @Test
    public void testReverseProxy() {
        SimpleService client = createClient();
        setHttpHeaders(client, "X-Forwarded-Proto", "https");

        String result = client.op("test");
        assertEquals("Scheme should be set to 'https'",
            "scheme: https, x-forwarded-proto: https", result);
    }

    protected void setHttpHeaders(SimpleService client, String header, String value) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(header, Arrays.asList(value));
        ClientProxy.getClient(client).getRequestContext().put(Message.PROTOCOL_HEADERS, headers);
    }

    private SimpleService createClient() {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setAddress(routerEndpointAddress);
        factory.setServiceClass(SimpleService.class);
        factory.setBus(BusFactory.getDefaultBus());
        return (SimpleService) factory.create();
    }
}
