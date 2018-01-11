package com.wistron.demo.tool.teddybear.dcs.util;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;

import java.io.IOException;

/**
 * json序列化成对象和反序列化
 * Created by ivanjlzhang on 17-9-22.
 */

public class ObjectMapperUtil {
    private static ObjectMapper objectMapper;

    private static class ObjectMapperFactoryHolder {
        private static final ObjectMapperUtil instance = new ObjectMapperUtil();
    }

    public static ObjectMapperUtil instance() {
        return ObjectMapperFactoryHolder.instance;
    }

    private ObjectMapperUtil() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
    }

    public ObjectReader getObjectReader() {
        return objectMapper.reader();
    }

    public ObjectReader getObjectReader(Class<?> clazz) {
        return objectMapper.reader().withType(clazz);
    }

    public ObjectWriter getObjectWriter() {
        return objectMapper.writer();
    }


    public String objectToJson(Object obj) {
        String result = "";
        try {
            result = getObjectWriter().writeValueAsString(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
