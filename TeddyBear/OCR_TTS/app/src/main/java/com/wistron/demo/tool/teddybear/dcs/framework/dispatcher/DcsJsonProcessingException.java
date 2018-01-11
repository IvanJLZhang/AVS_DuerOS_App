package com.wistron.demo.tool.teddybear.dcs.framework.dispatcher;

import org.codehaus.jackson.JsonProcessingException;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class DcsJsonProcessingException extends JsonProcessingException {
    private String unparsedCotent;

    public DcsJsonProcessingException(String message, JsonProcessingException exception, String unparsedCotent) {
        super(message, exception);
        this.unparsedCotent = unparsedCotent;
    }

    public String getUnparsedCotent() {
        return unparsedCotent;
    }

}
