package com.github.docker.image.file.finder.test;

import com.alibaba.fastjson.JSON;
import com.github.docker.image.file.finder.DockerClientBuilder;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.SearchImagesCmd;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.SearchItem;
import org.junit.Test;

import java.util.List;

public class DockerClientTest {

    private DockerClient dockerClient = DockerClientBuilder.build("tcp://192.168.40.128:2375");

    @Test
    public void testListImagesCmd() {
        ListImagesCmd cmd = dockerClient.listImagesCmd();
        List<Image> images = cmd.exec();
        System.out.println(JSON.toJSONString(images, true));
    }

    @Test
    public void testSearchImagesCmd() {
        SearchImagesCmd cmd = dockerClient.searchImagesCmd("python:latest");
        List<SearchItem> items = cmd.exec();
        System.out.println(JSON.toJSONString(items, true));
    }

    @Test
    public void testInspectImageCmd() {
        InspectImageCmd cmd = dockerClient.inspectImageCmd("sha256:a31efa943659ac369c5b3a7474cdf0e8a526624124da612a6a1668393934e403");
        InspectImageResponse response = cmd.exec();
        System.out.println(JSON.toJSONString(response, true));
    }
}
