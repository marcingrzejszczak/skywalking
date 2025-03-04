/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.meter.analyzer.k8s;

import static java.util.Objects.requireNonNull;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.skywalking.library.kubernetes.KubernetesPods;
import org.apache.skywalking.library.kubernetes.KubernetesServices;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import lombok.SneakyThrows;

public class K8sInfoRegistry {

    private final static K8sInfoRegistry INSTANCE = new K8sInfoRegistry();
    private final LoadingCache<String/* podName.namespace */, String /* serviceName.namespace */> podServiceMap;
    private final LoadingCache<String/* podIP */, String /* podName.namespace */> ipPodMap;
    private final LoadingCache<String/* serviceIP */, String /* serviceName.namespace */> ipServiceMap;
    private static final String SEPARATOR = ".";

    private K8sInfoRegistry() {
        ipPodMap = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(3))
            .build(CacheLoader.from(ip -> KubernetesPods.INSTANCE
                .list()
                .stream()
                .filter(it -> it.getStatus() != null)
                .filter(it -> it.getMetadata() != null)
                .filter(it -> Objects.equals(it.getStatus().getPodIP(), ip))
                .map(it -> metadataID(it.getMetadata()))
                .findFirst()
                .orElse("")));
        ipServiceMap = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(3))
            .build(CacheLoader.from(ip -> KubernetesServices.INSTANCE
                .list()
                .stream()
                .filter(it -> it.getSpec() != null)
                .filter(it -> it.getStatus() != null)
                .filter(it -> it.getMetadata() != null)
                .filter(it -> (it.getSpec().getClusterIPs() != null &&
                    it.getSpec().getClusterIPs().stream()
                        .anyMatch(clusterIP -> Objects.equals(clusterIP, ip)))
                    || (it.getStatus().getLoadBalancer() != null &&
                        it.getStatus().getLoadBalancer().getIngress() != null &&
                        it.getStatus().getLoadBalancer().getIngress().stream()
                            .anyMatch(ingress -> Objects.equals(ingress.getIp(), ip))))
                .map(it -> metadataID(it.getMetadata()))
                .findFirst()
                .orElse("")));
        podServiceMap = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(3))
            .build(CacheLoader.from(podMetadataID -> {
                final Optional<V1Pod> pod = KubernetesPods.INSTANCE
                    .list()
                    .stream()
                    .filter(it -> it.getMetadata() != null)
                    .filter(it -> Objects.equals(
                        metadataID(it.getMetadata()),
                        podMetadataID))
                    .findFirst();

                if (!pod.isPresent()
                    || pod.get().getMetadata() == null
                    || pod.get().getMetadata().getLabels() == null) {
                    return "";
                }

                final Optional<V1Service> service = KubernetesServices.INSTANCE
                    .list()
                    .stream()
                    .filter(it -> it.getMetadata() != null)
                    .filter(it -> it.getSpec() != null)
                    .filter(it -> requireNonNull(it.getSpec()).getSelector() != null)
                    .filter(it -> {
                        final Map<String, String> labels = pod.get().getMetadata().getLabels();
                        final Map<String, String> selector = it.getSpec().getSelector();
                        return hasIntersection(selector.entrySet(), labels.entrySet());
                    })
                    .findFirst();
                if (!service.isPresent()) {
                    return "";
                }
                return service.get().getMetadata().getName()
                    + SEPARATOR
                    + service.get().getMetadata().getNamespace();
            }));
    }

    public static K8sInfoRegistry getInstance() {
        return INSTANCE;
    }

    @SneakyThrows
    public String findServiceName(String namespace, String podName) {
        return this.podServiceMap.get(podName + SEPARATOR + namespace);
    }

    @SneakyThrows
    public String findPodByIP(String ip) {
        return this.ipPodMap.get(ip);
    }

    @SneakyThrows
    public String findServiceByIP(String ip) {
        return this.ipServiceMap.get(ip);
    }

    private boolean hasIntersection(Collection<?> o, Collection<?> c) {
        Objects.requireNonNull(o);
        Objects.requireNonNull(c);
        for (final Object value : o) {
            if (!c.contains(value)) {
                return false;
            }
        }
        return true;
    }

    String metadataID(final V1ObjectMeta metadata) {
        return metadata.getName() + SEPARATOR + metadata.getNamespace();
    }
}
