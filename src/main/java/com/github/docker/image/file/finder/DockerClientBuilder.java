package com.github.docker.image.file.finder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.time.Duration;

public class DockerClientBuilder {

    /**
     * @param dockerHost docker发开API地址，例如：tcp://192.168.40.128:2375
     * @return
     */
    public static DockerClient build(String dockerHost) {
        DockerClientConfig custom = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(false)
                .withDockerCertPath(null)
                .withRegistryUsername(null)
                .withRegistryPassword(null)
                .withRegistryEmail(null)
                .withRegistryUrl(null)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(custom.getDockerHost())
                .sslConfig(custom.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(custom, httpClient);
    }
}
