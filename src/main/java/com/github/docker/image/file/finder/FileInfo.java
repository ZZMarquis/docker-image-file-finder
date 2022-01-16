package com.github.docker.image.file.finder;

public class FileInfo {
    /**
     * 文件在镜像中的路径
     */
    private String pathInImage;

    /**
     * 文件在宿主机上的绝对路径
     */
    private String absolutePath;

    /**
     * 类型，目录或文件。见{@link FileType}中的定义。
     */
    private Integer fileType;

    private LayerInfo layerInfo;

    public String getPathInImage() {
        return pathInImage;
    }

    public void setPathInImage(String pathInImage) {
        this.pathInImage = pathInImage;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    public LayerInfo getLayerInfo() {
        return layerInfo;
    }

    public void setLayerInfo(LayerInfo layerInfo) {
        this.layerInfo = layerInfo;
    }
}
