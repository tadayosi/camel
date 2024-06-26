/*
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
package org.apache.camel.processor.async;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelContext;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.support.processor.DelegateAsyncProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AsyncEndpointCustomAsyncInterceptorTest extends ContextTestSupport {

    private static String beforeThreadName;
    private static String afterThreadName;
    private final MyInterceptor interceptor = new MyInterceptor();

    @Test
    public void testAsyncEndpoint() throws Exception {
        getMockEndpoint("mock:before").expectedBodiesReceived("Hello Camel");
        getMockEndpoint("mock:after").expectedBodiesReceived("Bye Camel");
        getMockEndpoint("mock:result").expectedBodiesReceived("Bye Camel");

        String reply = template.requestBody("direct:start", "Hello Camel", String.class);
        assertEquals("Bye Camel", reply);

        assertMockEndpointsSatisfied();

        assertEquals(8, interceptor.getCounter());

        assertFalse(beforeThreadName.equalsIgnoreCase(afterThreadName), "Should use different threads");
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                context.addComponent("async", new MyAsyncComponent());
                context.getCamelContextExtension().addInterceptStrategy(interceptor);

                from("direct:start").to("mock:before").to("log:before").process(new Processor() {
                    public void process(Exchange exchange) {
                        beforeThreadName = Thread.currentThread().getName();
                    }
                }).to("async:bye:camel").process(new Processor() {
                    public void process(Exchange exchange) {
                        afterThreadName = Thread.currentThread().getName();
                    }
                }).to("log:after").to("mock:after").to("mock:result");
            }
        };
    }

    // START SNIPPET: e1
    private static class MyInterceptor implements InterceptStrategy {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Processor wrapProcessorInInterceptors(
                final CamelContext context, final NamedNode definition, final Processor target, final Processor nextTarget) {

            // use DelegateAsyncProcessor to ensure the interceptor works well
            // with the asynchronous routing
            // engine in Camel.
            // The target is the processor to continue routing to, which we must
            // provide
            // in the constructor of the DelegateAsyncProcessor
            return new DelegateAsyncProcessor(target) {
                @Override
                public boolean process(Exchange exchange, AsyncCallback callback) {
                    // we just want to count number of interceptions
                    counter.incrementAndGet();

                    // invoke processor to continue routing the message
                    return processor.process(exchange, callback);
                }
            };
        }

        public int getCounter() {
            return counter.get();
        }
    }
    // END SNIPPET: e1

}
