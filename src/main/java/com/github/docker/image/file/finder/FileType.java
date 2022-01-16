package com.github.docker.image.file.finder;

public enum FileType {
    DIR(1), FILE(2), SYMBOLIC_LINK(3);

    private Integer code;

    FileType(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
