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

package org.apache.skywalking.oap.meter.analyzer.dsl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.openapi.models.V1LoadBalancerIngress;
import io.kubernetes.client.openapi.models.V1LoadBalancerStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import io.kubernetes.client.openapi.models.V1ServiceStatus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.library.kubernetes.KubernetesPods;
import org.apache.skywalking.library.kubernetes.KubernetesServices;
import org.apache.skywalking.oap.meter.analyzer.dsl.tagOpt.Retag;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@Slf4j
@PowerMockIgnore("javax.net.ssl.*")
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest({KubernetesPods.class, KubernetesServices.class})
public class K8sTagTest {

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public ImmutableMap<String, SampleFamily> input;

    @Parameterized.Parameter(2)
    public String expression;

    @Parameterized.Parameter(3)
    public Result want;

    @Parameterized.Parameter(4)
    public boolean isThrow;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {
                "Pod2Service",
                of("container_cpu_usage_seconds_total", SampleFamilyBuilder.newBuilder(
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "default", "container", "my-nginx", "cpu", "total", "pod",
                                  "my-nginx-5dc4865748-mbczh"
                              ))
                          .value(2)
                          .name("container_cpu_usage_seconds_total")
                          .build(),
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "kube-system", "container", "kube-state-metrics", "cpu", "total", "pod",
                                  "kube-state-metrics-6f979fd498-z7xwx"
                              ))
                          .value(1)
                          .name("container_cpu_usage_seconds_total")
                          .build()
                ).build()),
                "container_cpu_usage_seconds_total.retagByK8sMeta('service' , K8sRetagType.Pod2Service , 'pod' , 'namespace')",
                Result.success(SampleFamilyBuilder.newBuilder(
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "default", "container", "my-nginx", "cpu", "total", "pod",
                                  "my-nginx-5dc4865748-mbczh",
                                  "service", "nginx-service.default"
                              ))
                          .value(2)
                          .name("container_cpu_usage_seconds_total")
                          .build(),
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "kube-system", "container", "kube-state-metrics", "cpu", "total", "pod",
                                  "kube-state-metrics-6f979fd498-z7xwx",
                                  "service", "kube-state-metrics.kube-system"
                              ))
                          .value(1)
                          .name("container_cpu_usage_seconds_total")
                          .build()
                ).build()),
                false,
                },
            {
                "Pod2Service_no_pod",
                of("container_cpu_usage_seconds_total", SampleFamilyBuilder.newBuilder(
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "default", "container", "my-nginx", "cpu", "total", "pod",
                                  "my-nginx-5dc4865748-no-pod"
                              ))
                          .value(2)
                          .name("container_cpu_usage_seconds_total")
                          .build(),
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "kube-system", "container", "kube-state-metrics", "cpu", "total", "pod",
                                  "kube-state-metrics-6f979fd498-z7xwx"
                              ))
                          .value(1)
                          .name("container_cpu_usage_seconds_total")
                          .build()
                ).build()),
                "container_cpu_usage_seconds_total.retagByK8sMeta('service' , K8sRetagType.Pod2Service , 'pod' , 'namespace')",
                Result.success(SampleFamilyBuilder.newBuilder(
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "default", "container", "my-nginx", "cpu", "total", "pod",
                                  "my-nginx-5dc4865748-no-pod" , "service", Retag.BLANK
                              ))
                          .value(2)
                          .name("container_cpu_usage_seconds_total")
                          .build(),
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "kube-system", "container", "kube-state-metrics", "cpu", "total", "pod",
                                  "kube-state-metrics-6f979fd498-z7xwx",
                                  "service", "kube-state-metrics.kube-system"
                              ))
                          .value(1)
                          .name("container_cpu_usage_seconds_total")
                          .build()
                ).build()),
                false,
                },
            {
                "Pod2Service_no_service",
                of("container_cpu_usage_seconds_total", SampleFamilyBuilder.newBuilder(
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "default", "container", "my-nginx", "cpu", "total", "pod",
                                  "my-nginx-5dc4865748-no-service"
                              ))
                          .value(2)
                          .name("container_cpu_usage_seconds_total")
                          .build(),
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "kube-system", "container", "kube-state-metrics", "cpu", "total", "pod",
                                  "kube-state-metrics-6f979fd498-z7xwx"
                              ))
                          .value(1)
                          .name("container_cpu_usage_seconds_total")
                          .build()
                ).build()),
                "container_cpu_usage_seconds_total.retagByK8sMeta('service' , K8sRetagType.Pod2Service , 'pod' , 'namespace')",
                Result.success(SampleFamilyBuilder.newBuilder(
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "default", "container", "my-nginx", "cpu", "total", "pod",
                                  "my-nginx-5dc4865748-no-service" , "service", Retag.BLANK
                              ))
                          .value(2)
                          .name("container_cpu_usage_seconds_total")
                          .build(),
                    Sample.builder()
                          .labels(
                              of(
                                  "namespace", "kube-system", "container", "kube-state-metrics", "cpu", "total", "pod",
                                  "kube-state-metrics-6f979fd498-z7xwx",
                                  "service", "kube-state-metrics.kube-system"
                              ))
                          .value(1)
                          .name("container_cpu_usage_seconds_total")
                          .build()
                ).build()),
                false,
                },
            {
                "IPAddress_to_name",
                of("rover_network_profiling_process_write_bytes", SampleFamilyBuilder.newBuilder(
                    Sample.builder()
                        .labels(
                            of("service", "test", "instance", "test-instance", "side", "client",
                                "client_address", "1.1.1.1", "server_address", "2.2.2.2")
                        )
                        .value(2)
                        .name("rover_network_profiling_process_write_bytes")
                        .build()
                ).build()),
                "rover_network_profiling_process_write_bytes.forEach(['client', 'server'] , " +
                    "{prefix, tags -> tags[prefix + '_process_id'] = ProcessRegistry.generateVirtualRemoteProcess(tags.service, tags.instance, tags[prefix + '_address'])})",
                Result.success(SampleFamilyBuilder.newBuilder(
                    Sample.builder()
                        .labels(
                            of("service", "test", "instance", "test-instance", "side", "client",
                                "client_address", "1.1.1.1", "client_process_id", IDManager.ProcessID.buildId(
                                    IDManager.ServiceInstanceID.buildId(IDManager.ServiceID.buildId("test", true), "test-instance"),
                                    "my-nginx-5dc4865748-mbczh.default"),
                                "server_address", "2.2.2.2", "server_process_id", IDManager.ProcessID.buildId(
                                    IDManager.ServiceInstanceID.buildId(IDManager.ServiceID.buildId("test", true), "test-instance"),
                                    "kube-state-metrics.kube-system"))
                        )
                        .value(2)
                        .name("rover_network_profiling_process_write_bytes")
                        .build()
                ).build()),
                false,
            }
            });
    }

    @SneakyThrows
    @Before
    public void setup() {
        PowerMockito.mockStatic(KubernetesServices.class);
        PowerMockito.mockStatic(KubernetesPods.class);

        Whitebox.setInternalState(KubernetesServices.class, "INSTANCE",
                                  Mockito.mock(KubernetesServices.class)
        );
        Whitebox.setInternalState(KubernetesPods.class, "INSTANCE",
                                  Mockito.mock(KubernetesPods.class)
        );

        PowerMockito.when(KubernetesServices.INSTANCE, "list").thenReturn(ImmutableList.of(
                mockService("nginx-service", "default", of("run", "nginx"), "2.2.2.1"),
                mockService("kube-state-metrics", "kube-system", of("run", "kube-state-metrics"), "2.2.2.2")));
        PowerMockito.when(KubernetesPods.INSTANCE, "list").thenReturn(ImmutableList.of(
            mockPod("my-nginx-5dc4865748-mbczh", "default", of("run", "nginx"), "1.1.1.1"),
            mockPod("kube-state-metrics-6f979fd498-z7xwx", "kube-system", of("run", "kube-state-metrics"), "1.1.1.2")));
    }

    @Test
    public void test() {
        Expression e = DSL.parse(expression);
        Result r = null;
        try {
            r = e.run(input);
        } catch (Throwable t) {
            if (isThrow) {
                return;
            }
            log.error("Test failed", t);
            fail("Should not throw anything");
        }
        if (isThrow) {
            fail("Should throw something");
        }
        assertThat(r, is(want));
    }

    private V1Service mockService(String name, String namespace, Map<String, String> selector, String ipAddress) {
        V1Service service = new V1Service();
        V1ObjectMeta serviceMeta = new V1ObjectMeta();
        V1ServiceSpec v1ServiceSpec = new V1ServiceSpec();

        serviceMeta.setName(name);
        serviceMeta.setNamespace(namespace);
        service.setMetadata(serviceMeta);
        v1ServiceSpec.setSelector(selector);
        service.setSpec(v1ServiceSpec);

        final V1ServiceStatus v1ServiceStatus = new V1ServiceStatus();
        final V1LoadBalancerStatus balancerStatus = new V1LoadBalancerStatus();
        final V1LoadBalancerIngress loadBalancerIngress = new V1LoadBalancerIngress();
        loadBalancerIngress.setIp(ipAddress);
        balancerStatus.setIngress(Arrays.asList(loadBalancerIngress));
        v1ServiceStatus.setLoadBalancer(balancerStatus);
        service.setStatus(v1ServiceStatus);

        return service;
    }

    private V1Pod mockPod(String name, String namespace, Map<String, String> labels, String ipAddress) {
        V1Pod v1Pod = new V1Pod();
        V1ObjectMeta podMeta = new V1ObjectMeta();
        podMeta.setName(name);
        podMeta.setNamespace(namespace);
        podMeta.setLabels(labels);
        final V1PodStatus status = new V1PodStatus();
        status.setPodIP(ipAddress);
        v1Pod.setStatus(status);
        v1Pod.setMetadata(podMeta);

        return v1Pod;
    }
}
