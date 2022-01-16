package com.github.docker.image.file.finder.test;

import com.alibaba.fastjson.JSON;
import com.github.docker.image.file.finder.DockerClientBuilder;
import com.github.docker.image.file.finder.FileFinder;
import com.github.docker.image.file.finder.FileInfo;
import com.github.docker.image.file.finder.FileType;
import com.github.dockerjava.api.DockerClient;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class FileFinderTest {

    private DockerClient dockerClient = DockerClientBuilder.build("tcp://192.168.40.128:2375");

    @Test
    public void computeChainId() throws NoSuchAlgorithmException {
        String parentChainId = "sha256:799760671c382bd2492346f4c36ee4033cf917400be4354c8b096ecef88df34b";
        String currentDiffId = "sha256:4e61e63529c26e95bef3cf769d07847dee7590b37b6b685186ce5c37a509b06d";
        System.out.println(FileFinder.computeChainId(parentChainId, currentDiffId));
    }

    @Test
    public void testFind1() throws IOException, NoSuchAlgorithmException {
        FileFinder finder = new FileFinder(
                dockerClient,
                "/etc",
                Arrays.asList(FileType.FILE, FileType.SYMBOLIC_LINK, FileType.DIR),
                null,
                null
        );
        List<FileInfo> fileInfos = finder.find("sha256:a31efa943659ac369c5b3a7474cdf0e8a526624124da612a6a1668393934e403");
        System.out.println(JSON.toJSONString(fileInfos, true));
    }
}
