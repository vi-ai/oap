/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oap.ws;

import com.google.common.io.ByteStreams;
import oap.util.Try;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;

public abstract class Request {
    public abstract String requestLine();

    public abstract String baseUrl();

    public abstract HttpMethod httpMethod();

    public abstract Context context();

    public Optional<String> parameter( String mapping, String name ) {
        return ServiceUtil.pathParam( mapping, requestLine(), name );
    }

    public abstract Optional<String> parameter( String name );

    public abstract List<String> parameters( String name );

    public abstract Optional<InputStream> body();

    public Optional<byte[]> readBody() {
        return body().map( Try.mapOrThrow( ByteStreams::toByteArray, WsClientException.class ) );
    }

    public abstract Optional<String> header( String name );

    //    @todo delete
    public abstract boolean isBodyJson();

    public abstract InetAddress remoteAddress();


    public enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD
    }

}
