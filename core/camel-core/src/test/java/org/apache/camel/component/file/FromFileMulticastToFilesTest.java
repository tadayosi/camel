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
package org.apache.camel.component.file;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

public class FromFileMulticastToFilesTest extends ContextTestSupport {

    @Test
    public void testFromFileMulticastToFiles() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(fileUri("?initialDelay=0&delay=10")).multicast().pipeline()
                        .transform(body().prepend("HEADER:"))
                        .to(fileUri("out/?fileName=header.txt")).to("mock:header").end().pipeline()
                        .transform(body().prepend("FOOTER:"))
                        .to(fileUri("out/?fileName=footer.txt")).to("mock:footer").end().end()
                        .to("mock:end");
            }
        });
        context.start();

        MockEndpoint header = getMockEndpoint("mock:header");
        header.expectedBodiesReceived("HEADER:foo");
        header.expectedFileExists(testFile("out/header.txt"));

        MockEndpoint footer = getMockEndpoint("mock:footer");
        footer.expectedBodiesReceived("FOOTER:foo");
        footer.expectedFileExists(testFile("out/footer.txt"));

        MockEndpoint end = getMockEndpoint("mock:end");
        end.expectedMessageCount(1);
        end.expectedFileExists(testFile(".camel/foo.txt"));

        template.sendBodyAndHeader(fileUri(), "foo", Exchange.FILE_NAME, "foo.txt");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFromFileMulticastParallelToFiles() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(fileUri("?initialDelay=0&delay=10")).multicast().parallelProcessing().pipeline()
                        .transform(body().prepend("HEADER:"))
                        .to(fileUri("out/?fileName=header.txt")).to("mock:header").end().pipeline()
                        .transform(body().prepend("FOOTER:"))
                        .to(fileUri("out/?fileName=footer.txt")).to("mock:footer").end().end()
                        .to("mock:end");
            }
        });
        context.start();

        MockEndpoint header = getMockEndpoint("mock:header");
        header.expectedBodiesReceived("HEADER:foo");
        header.expectedFileExists(testFile("out/header.txt"));

        MockEndpoint footer = getMockEndpoint("mock:footer");
        footer.expectedBodiesReceived("FOOTER:foo");
        footer.expectedFileExists(testFile("out/footer.txt"));

        MockEndpoint end = getMockEndpoint("mock:end");
        end.expectedMessageCount(1);
        end.expectedFileExists(testFile(".camel/foo.txt"));

        template.sendBodyAndHeader(fileUri(), "foo", Exchange.FILE_NAME, "foo.txt");

        assertMockEndpointsSatisfied();
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }
}
