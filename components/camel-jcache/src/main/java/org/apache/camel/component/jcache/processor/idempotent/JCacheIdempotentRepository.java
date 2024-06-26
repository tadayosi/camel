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
package org.apache.camel.component.jcache.processor.idempotent;

import javax.cache.Cache;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.api.management.ManagedOperation;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.component.jcache.JCacheConfiguration;
import org.apache.camel.component.jcache.JCacheHelper;
import org.apache.camel.component.jcache.JCacheManager;
import org.apache.camel.spi.Configurer;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.spi.Metadata;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;

@Metadata(label = "bean",
          description = "Idempotent repository that uses JCache to store message ids.",
          annotations = { "interfaceName=org.apache.camel.spi.IdempotentRepository" })
@Configurer(metadataOnly = true)
@ManagedResource(description = "JCache based message id repository")
public class JCacheIdempotentRepository extends ServiceSupport implements CamelContextAware, IdempotentRepository {

    private CamelContext camelContext;
    private Cache<String, Boolean> cache;
    private JCacheManager<String, Boolean> cacheManager;

    @Metadata(description = "Configuration for JCache")
    private JCacheConfiguration configuration;

    public JCacheIdempotentRepository() {
        this.configuration = new JCacheConfiguration();
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public JCacheConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JCacheConfiguration configuration) {
        this.configuration = configuration;
    }

    public Cache<String, Boolean> getCache() {
        return cache;
    }

    public void setCache(Cache<String, Boolean> cache) {
        this.cache = cache;
    }

    @Override
    @ManagedOperation(description = "Adds the key to the store")
    public boolean add(String key) {
        return cache.putIfAbsent(key, true);
    }

    @Override
    @ManagedOperation(description = "Does the store contain the given key")
    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    @Override
    @ManagedOperation(description = "Remove the key from the store")
    public boolean remove(String key) {
        return cache.remove(key);
    }

    @Override
    @ManagedOperation(description = "Clear the store")
    public void clear() {
        cache.clear();
    }

    public void setCacheName(String cacheName) {
        configuration.setCacheName(cacheName);
    }

    @ManagedAttribute(description = "The processor name")
    public String getCacheName() {
        return configuration.getCacheName();
    }

    @Override
    public boolean confirm(String key) {
        return cache.replace(key, false, true);
    }

    @Override
    protected void doStart() throws Exception {
        ObjectHelper.notNull(camelContext, "camelContext");
        ObjectHelper.notNull(configuration, "configuration");

        if (cache != null) {
            cacheManager = new JCacheManager<>(cache);
        } else {
            cacheManager = JCacheHelper.createManager(getCamelContext(), configuration);
            cache = cacheManager.getCache();
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (cacheManager != null) {
            cacheManager.close();
        }
    }
}
