package com.wistron.demo.tool.teddybear.dcs.framework.message;

/**
 * 服务器端返回应答
 * Created by ivanjlzhang on 17-9-22.
 */

public class DcsResponseBody {
    // 指令
    private Directive directive;

    public Directive getDirective() {
        return directive;
    }

    public void setDirective(Directive directive) {
        this.directive = directive;
    }
}
