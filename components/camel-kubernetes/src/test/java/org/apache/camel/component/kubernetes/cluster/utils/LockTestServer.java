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
package org.apache.camel.component.kubernetes.cluster.utils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.http.Headers;
import io.fabric8.mockwebserver.http.RecordedRequest;
import io.fabric8.mockwebserver.utils.ResponseProvider;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Test server to interact with Kubernetes for locking on a ConfigMap.
 */
public class LockTestServer<T extends HasMetadata> extends KubernetesMockServer {

    private static final Logger LOG = LoggerFactory.getLogger(LockTestServer.class);

    private static final PodStatus READY = new PodStatusBuilder().withPhase("Running").addNewCondition().withType("Ready")
            .withStatus("true").endCondition().build();
    private static final PodStatus FAILED = new PodStatusBuilder().withPhase("Failed").build();
    private static final PodStatus NOT_READY = new PodStatusBuilder().withPhase("Running").addNewCondition().withType("Ready")
            .withStatus("false").endCondition().build();

    private boolean refuseRequests;

    private Long delayRequests;

    private Set<String> pods;

    private Map<String, ResourceLockSimulator<T>> simulators;

    public LockTestServer() {
        this(Collections.emptySet());
    }

    public LockTestServer(Collection<String> initialPods) {

        this.pods = new TreeSet<>(initialPods);
        this.simulators = new HashMap<>();

        // Other resources
        expect().get().withPath("/api/v1/namespaces/test/pods")
                .andReply(200,
                        request -> new PodListBuilder().withNewMetadata().withResourceVersion("1").and()
                                .withItems(getCurrentPods().stream()
                                        .map(name -> new PodBuilder()
                                                .withNewMetadata()
                                                .withName(name)
                                                .endMetadata()
                                                .withStatus(getPodStatus(name))
                                                .build())
                                        .collect(Collectors.toList()))
                                .build())
                .always();

    }

    static PodStatus getPodStatus(String podName) {
        if (podName.startsWith("badpod")) {
            return FAILED;
        } else if (podName.startsWith("notreadypod")) {
            return NOT_READY;
        } else {
            return READY;
        }
    }

    @Override
    public NamespacedKubernetesClient createClient() {
        // Avoid exponential retry backoff from slowing down tests
        NamespacedKubernetesClient namespacedKubernetesClient = super.createClient();
        RequestConfig requestConfig = namespacedKubernetesClient.getConfiguration().getRequestConfig();
        requestConfig.setRequestRetryBackoffInterval(1000);
        requestConfig.setRequestRetryBackoffLimit(0);
        return namespacedKubernetesClient;
    }

    public void addSimulator(ResourceLockSimulator<?> paramLockSimulator) {
        ResourceLockSimulator<T> lockSimulator = (ResourceLockSimulator<T>) paramLockSimulator;
        if (this.simulators.containsKey(lockSimulator.getResourceName())) {
            return;
        }
        this.simulators.put(lockSimulator.getResourceName(), lockSimulator);

        if (this.simulators.size() == 1) {
            // Global methods defined once
            expect().post().withPath(lockSimulator.getAPIPath() + "/namespaces/test/" + lockSimulator.getResourcePath())
                    .andReply(new ResponseProvider<Object>() {

                        private Headers headers = new Headers.Builder().build();
                        private Map<Integer, String> lockNames = new HashMap<>();

                        @Override
                        public int getStatusCode(RecordedRequest request) {
                            if (refuseRequests) {
                                return 500;
                            }

                            T resource;
                            try {
                                resource = convert(request, lockSimulator.getResourceClass());
                            } catch (Exception e) {
                                LOG.error("Error during resource conversion", e);
                                return 500;
                            }

                            if (resource == null) {
                                LOG.error("No resource received");
                                return 500;
                            }
                            ResourceLockSimulator<T> lockSimulator = simulators.get(resource.getMetadata().getName());
                            if (resource.getMetadata() == null
                                    || !lockSimulator.getResourceName().equals(resource.getMetadata().getName())) {
                                LOG.error("Illegal resource received");
                                return 500;
                            }

                            if (resource.getMetadata().getNamespace() == null) {
                                resource.getMetadata().setNamespace("test");
                            }

                            boolean done = lockSimulator.setResource(resource, true);
                            if (done) {
                                lockNames.put(LockTestServer.super.getRequestCount(), lockSimulator.getResourceName());
                                return 201;
                            }
                            return 500;
                        }

                        @Override
                        public Object getBody(RecordedRequest recordedRequest) {
                            delayIfNecessary();

                            if (lockNames.containsKey(LockTestServer.super.getRequestCount())) {
                                T resource
                                        = simulators.get(lockNames.get(LockTestServer.super.getRequestCount())).getResource();
                                if (resource != null) {
                                    return resource;
                                }
                            }

                            return "";
                        }

                        @Override
                        public Headers getHeaders() {
                            return headers;
                        }

                        @Override
                        public void setHeaders(Headers headers) {
                            this.headers = headers;
                        }
                    }).always();
        }

        expect().get()
                .withPath(lockSimulator.getAPIPath() + "/namespaces/test/" + lockSimulator.getResourcePath() + "/"
                          + lockSimulator.getResourceName())
                .andReply(new ResponseProvider<Object>() {

                    private Headers headers = new Headers.Builder().build();

                    @Override
                    public int getStatusCode(RecordedRequest request) {
                        if (refuseRequests) {
                            return 500;
                        }

                        if (lockSimulator.getResource() != null) {
                            return 200;
                        }

                        return 404;
                    }

                    @Override
                    public Object getBody(RecordedRequest recordedRequest) {
                        delayIfNecessary();
                        T resource = lockSimulator.getResource();
                        if (resource != null) {
                            return resource;
                        }
                        return "";
                    }

                    @Override
                    public Headers getHeaders() {
                        return headers;
                    }

                    @Override
                    public void setHeaders(Headers headers) {
                        this.headers = headers;
                    }
                }).always();

        expect().put()
                .withPath(lockSimulator.getAPIPath() + "/namespaces/test/" + lockSimulator.getResourcePath() + "/"
                          + lockSimulator.getResourceName())
                .andReply(new ResponseProvider<Object>() {

                    private Headers headers = new Headers.Builder().build();

                    @Override
                    public int getStatusCode(RecordedRequest request) {
                        if (refuseRequests) {
                            return 500;
                        }

                        T resource;
                        try {
                            resource = convert(request, lockSimulator.getResourceClass());
                        } catch (Exception e) {
                            LOG.error("Error during resource conversion", e);
                            return 500;
                        }

                        boolean done = lockSimulator.setResource(resource, false);
                        if (done) {
                            return 200;
                        }
                        return 409;
                    }

                    @Override
                    public Object getBody(RecordedRequest recordedRequest) {
                        delayIfNecessary();
                        T resource = lockSimulator.getResource();
                        if (resource != null) {
                            return resource;
                        }

                        return "";
                    }

                    @Override
                    public Headers getHeaders() {
                        return headers;
                    }

                    @Override
                    public void setHeaders(Headers headers) {
                        this.headers = headers;
                    }
                }).always();
    }

    public boolean isRefuseRequests() {
        return refuseRequests;
    }

    public void setRefuseRequests(boolean refuseRequests) {
        this.refuseRequests = refuseRequests;
    }

    public synchronized Collection<String> getCurrentPods() {
        return new TreeSet<>(this.pods);
    }

    public synchronized void removePod(String pod) {
        this.pods.remove(pod);
    }

    public synchronized void addPod(String pod) {
        this.pods.add(pod);
    }

    public Long getDelayRequests() {
        return delayRequests;
    }

    public void setDelayRequests(Long delayRequests) {
        this.delayRequests = delayRequests;
    }

    private void delayIfNecessary() {
        if (delayRequests != null) {
            try {
                Thread.sleep(delayRequests);
            } catch (InterruptedException e) {
                throw new RuntimeCamelException(e);
            }
        }
    }

    private T convert(RecordedRequest request, Class<T> targetClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return mapper.readValue(request.getBody().readByteArray(), targetClass);
    }

}
