package com.github.docker.image.file.finder;

public class LayerInfo {
    private String diffId;
    private String chainId;
    private String cacheId;
    private Integer order;

    public String getDiffId() {
        return diffId;
    }

    public void setDiffId(String diffId) {
        this.diffId = diffId;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getCacheId() {
        return cacheId;
    }

    public void setCacheId(String cacheId) {
        this.cacheId = cacheId;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
