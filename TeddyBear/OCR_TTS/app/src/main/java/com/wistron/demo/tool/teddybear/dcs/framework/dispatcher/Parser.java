package com.wistron.demo.tool.teddybear.dcs.framework.dispatcher;

import com.wistron.demo.tool.teddybear.dcs.util.ObjectMapperUtil;

import org.codehaus.jackson.JsonProcessingException;

import java.io.IOException;

/**
 * 字节数据转换成对象
 * Created by ivanjlzhang on 17-9-22.
 */

public class Parser {
    protected <T> T parse(byte[] bytes, Class<T> clazz) throws IOException {
        try {
            return ObjectMapperUtil.instance().getObjectReader().withType(clazz).readValue(bytes);
        } catch (JsonProcessingException e) {
            String unparsedContent = new String(bytes, "UTF-8");
            String message = String.format("failed to parse %1$s", clazz.getSimpleName());
            throw new DcsJsonProcessingException(message, e, unparsedContent);
        }
    }
}
