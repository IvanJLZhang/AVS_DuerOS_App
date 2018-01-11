package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import java.io.Serializable;

/**
 * Created by king on 16-5-18.
 */
public class Story implements Serializable {
    private String name;
    private String path;

    public Story(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
