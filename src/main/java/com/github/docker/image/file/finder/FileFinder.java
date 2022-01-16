package com.github.docker.image.file.finder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class FileFinder {

    private static final int MAX_RECURSIVE_DEPTH = 100;
    private static final String DIGEST_PREFIX = "sha256:";
    private static final String CACHE_ID_FILE_FMT = "/var/lib/docker/image/overlay2/layerdb/sha256/%s/cache-id";
    private static final String DIFF_DIR_FMT = "/var/lib/docker/overlay2/%s/diff";

    private DockerClient dockerClient;
    private String startDirPath;
    private List<FileType> fileTypes;
    private Pattern fullPathRegex;
    private Pattern fileNameRegex;

    /**
     * @param dockerClient
     * @param startDirPath  指定目录查找，例如：/etc，则只查找
     * @param fileTypes
     * @param fullPathRegex
     * @param fileNameRegex
     */
    public FileFinder(DockerClient dockerClient, String startDirPath, List<FileType> fileTypes, Pattern fullPathRegex, Pattern fileNameRegex) {
        this.dockerClient = dockerClient;
        this.fileTypes = fileTypes;
        this.fullPathRegex = fullPathRegex;
        this.fileNameRegex = fileNameRegex;
        if (StringUtils.isNotBlank(startDirPath) && startDirPath.endsWith("/")) {
            this.startDirPath = startDirPath.substring(0, startDirPath.length() - 1);
        } else {
            this.startDirPath = startDirPath;
        }
    }

    /**
     * @param imageId 镜像ID，例如：sha256:a31efa943659ac369c5b3a7474cdf0e8a526624124da612a6a1668393934e403
     * @return
     */
    public List<FileInfo> find(String imageId) throws NoSuchAlgorithmException, IOException {
        List<LayerInfo> layerInfos = getLayerInfos(imageId);
        List<FileInfo> fileInfos = new ArrayList<>();
        for (LayerInfo layerInfo : layerInfos) {
            findFilesInLayer(layerInfo, fileInfos);
        }
        return fileInfos;
    }

    private List<LayerInfo> getLayerInfos(String imageId) throws NoSuchAlgorithmException, IOException {
        List<String> diffIds = getDiffIds(imageId);
        List<LayerInfo> layerInfos = new ArrayList<>(diffIds.size());
        for (int i = 0; i < diffIds.size(); ++i) {
            LayerInfo layerInfo = new LayerInfo();
            layerInfo.setOrder(i);
            layerInfo.setDiffId(diffIds.get(i));
            layerInfos.add(layerInfo);
        }

        getChainIds(layerInfos);

        getCacheIds(layerInfos);

        return layerInfos;
    }

    private void findFilesInLayer(LayerInfo layerInfo, List<FileInfo> fileInfos) throws IOException {
        String diffDirPath = String.format(DIFF_DIR_FMT, layerInfo.getCacheId());
        File diffDir = new File(diffDirPath);
        File[] files = diffDir.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            recursiveFindFiles(0, file, diffDirPath, layerInfo, fileInfos);
        }
    }

    private void recursiveFindFiles(int recursiveDepth, File file, String diffDirPath, LayerInfo layerInfo, List<FileInfo> fileInfos) throws IOException {
        if (recursiveDepth > MAX_RECURSIVE_DEPTH) {
            return;
        }
        if (".".equals(file.getName()) || "..".equals(file.getName())) {
            return;
        }
        if (isSymbolicLink(file)) {
            addFileInfo(file, FileType.SYMBOLIC_LINK, layerInfo, diffDirPath, fileInfos);
        } else if (file.isFile()) {
            addFileInfo(file, FileType.FILE, layerInfo, diffDirPath, fileInfos);
        } else if (file.isDirectory()) {
            if (StringUtils.isBlank(startDirPath)) {
                // 如果查找的起始目录是null，那都代表全量查找。
                // 如果查找的起始目录是空字符串，表示从根目录/下开始查找，其实也是全量查找。
                addFileInfo(file, FileType.DIR, layerInfo, diffDirPath, fileInfos);
                for (File subFile : Objects.requireNonNull(file.listFiles())) {
                    recursiveFindFiles(++recursiveDepth, subFile, diffDirPath, layerInfo, fileInfos);
                }
            } else {
                String absolutePath = file.getAbsolutePath();
                String pathInImage = absolutePath.substring(diffDirPath.length());
                if (isParentDir(startDirPath, pathInImage)
                        || isParentDir(pathInImage, startDirPath)) {
                    // 当前目录和指定的起始目录互为父目录时，都应该继续往下查找
                    addFileInfo(file, FileType.DIR, layerInfo, diffDirPath, fileInfos);
                    for (File subFile : Objects.requireNonNull(file.listFiles())) {
                        recursiveFindFiles(++recursiveDepth, subFile, diffDirPath, layerInfo, fileInfos);
                    }
                }
            }
        }
    }

    private boolean passRegexJudge(String filename, String pathInImage) {
        if (fileNameRegex != null) {
            if (!fileNameRegex.matcher(filename).matches()) {
                return false;
            }
        }
        if (fullPathRegex != null) {
            if (!fullPathRegex.matcher(pathInImage).matches()) {
                return false;
            }
        }
        return true;
    }

    private boolean isParentDir(String parentDirPath, String dirPath) {
        if (!dirPath.startsWith(parentDirPath)) {
            return false;
        }
        String suffix = dirPath.substring(parentDirPath.length());
        if (suffix.startsWith("/")) {
            return true;
        } else {
            return false;
        }
    }

    private void addFileInfo(File file, FileType fileType, LayerInfo layerInfo, String diffDirPath, List<FileInfo> fileInfos) {
        if (!fileTypes.contains(fileType)) {
            return;
        }

        String absolutePath = file.getAbsolutePath();
        String pathInImage = absolutePath.substring(diffDirPath.length());
        if (!passRegexJudge(file.getName(), pathInImage)) {
            return;
        }

        FileInfo fileInfo = new FileInfo();
        fileInfo.setLayerInfo(layerInfo);
        fileInfo.setFileType(fileType);
        fileInfo.setAbsolutePath(absolutePath);
        fileInfo.setPathInImage(pathInImage);
        fileInfos.add(fileInfo);
    }

    private boolean isSymbolicLink(File file) throws IOException {
        return !Objects.equals(file.getAbsolutePath(), file.getCanonicalPath());
    }

    private List<String> getDiffIds(String imageId) {
        InspectImageCmd cmd = dockerClient.inspectImageCmd(imageId);
        InspectImageResponse response = cmd.exec();
        return Objects.requireNonNull(response.getRootFS()).getLayers();
    }

    private void getChainIds(List<LayerInfo> layerInfos) throws NoSuchAlgorithmException {
        // 第一层，dffId就是chainId。之后每一层的chainId计算公式为：chainID(n)=sha256sum(chainID(n-1)) diffID(n))
        layerInfos.get(0).setChainId(layerInfos.get(0).getDiffId());
        String parentLayerChainId = layerInfos.get(0).getDiffId();
        for (int i = 1; i < layerInfos.size(); ++i) {
            String currentLayerDiffId = layerInfos.get(i).getDiffId();
            String currentLayerChainId = computeChainId(parentLayerChainId, currentLayerDiffId);
            layerInfos.get(i).setChainId(currentLayerChainId);
            parentLayerChainId = currentLayerChainId;
        }
    }

    private void getCacheIds(List<LayerInfo> layerInfos) throws IOException {
        for (int i = 0; i < layerInfos.size(); ++i) {
            String rawChainId = layerInfos.get(0).getChainId().substring(DIGEST_PREFIX.length());
            String cacheIdFilePath = String.format(CACHE_ID_FILE_FMT, rawChainId);
            layerInfos.get(0).setCacheId(readCacheId(cacheIdFilePath));
        }
    }

    private String readCacheId(String cacheIdFilePath) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(cacheIdFilePath, "r")) {
            byte[] bytes = new byte[(int) raf.length()];
            raf.read(bytes);
            return new String(bytes);
        }
    }

    public static String computeChainId(String parentLayerChainId, String currentLayerDiffId) throws NoSuchAlgorithmException {
        byte[] src = (parentLayerChainId + " " + currentLayerDiffId).getBytes();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(src);
        return DIGEST_PREFIX + ByteUtils.toHexString(bytes);
    }
}
