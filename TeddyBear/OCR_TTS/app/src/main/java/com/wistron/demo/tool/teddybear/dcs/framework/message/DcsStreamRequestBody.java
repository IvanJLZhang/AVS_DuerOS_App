package com.wistron.demo.tool.teddybear.dcs.framework.message;

import com.wistron.demo.tool.teddybear.dcs.http.OkHttpMediaType;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Pipe;
/**
 * Created by ivanjlzhang on 17-9-21.
 */

public class DcsStreamRequestBody extends RequestBody {
    private final Pipe pipe = new Pipe(8192);

    private final BufferedSink mSink = Okio.buffer(pipe.sink());

    public BufferedSink sink(){return mSink;}
    @Override
    public MediaType contentType() {
        return OkHttpMediaType.MEDIA_STREAM_TYPE;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        sink.writeAll(pipe.source());
    }
}
