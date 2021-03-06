/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
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
package oap.http.nio;

import oap.http.ClasspathResourceHandler;
import oap.io.Resources;
import oap.util.Strings;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.util.Optional;

public class NioClasspathResourceHandler implements HttpAsyncRequestHandler<HttpRequest> {
    private static MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap();
    private static Logger logger = LoggerFactory.getLogger( ClasspathResourceHandler.class.getName() );
    private String prefix;
    private String location;

    public NioClasspathResourceHandler( String prefix, String location ) {
        this.prefix = prefix;
        this.location = location;
    }

    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest( HttpRequest request,
        HttpContext context ) throws HttpException, IOException {
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle( HttpRequest req, HttpAsyncExchange httpExchange,
        HttpContext context ) throws HttpException, IOException {

        String resource = location + Strings.substringAfter( req.getRequestLine().getUri(), prefix );
        if( logger.isTraceEnabled() ) logger.trace( req.getRequestLine().toString() + " -> " + resource );
        Optional<byte[]> file = Resources.read( getClass(), resource );
        if( file.isPresent() ) {
            ByteArrayEntity entity = new ByteArrayEntity( file.get() );
            entity.setContentType( mimeTypes.getContentType( req.getRequestLine().getUri() ) );
            httpExchange.getResponse().setEntity( entity );
        } else httpExchange.getResponse().setStatusCode( 404 );
    }
}
