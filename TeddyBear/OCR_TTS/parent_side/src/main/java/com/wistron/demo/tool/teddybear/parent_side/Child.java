package com.wistron.demo.tool.teddybear.parent_side;

/**
 * Created by king on 16-5-3.
 */
public class Child {
    private String name;
    private String sn;

    public Child(String name, String sn) {
        this.name = name;
        this.sn = sn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    @Override
    public String toString() {
        return name;
    }
}
