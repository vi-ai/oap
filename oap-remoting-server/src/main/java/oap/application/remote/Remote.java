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
package oap.application.remote;

import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import oap.application.Application;
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.HttpServer;
import oap.http.Protocol;
import oap.http.Request;
import oap.http.Response;
import oap.http.cors.GenericCorsPolicy;
import oap.util.Result;
import oap.util.Try;

import java.lang.reflect.InvocationTargetException;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

@Slf4j
public class Remote implements Handler {
    private final ThreadLocal<oap.application.remote.FST> fst = new ThreadLocal<FST>() {
        public FST initialValue() {
            return new FST( serialization );
        }
    };

    private final FST.SerializationMethod serialization;
    private final GenericCorsPolicy cors = GenericCorsPolicy.DEFAULT;

    private final HttpServer server;
    private final String context;
    private final Protocol protocol;

    public Remote( FST.SerializationMethod serialization, final HttpServer server, final String context ) {
        this(serialization, server, context, Protocol.HTTP);
    }

    public Remote( FST.SerializationMethod serialization, final HttpServer server, final String context, Protocol protocol ) {
        this.serialization = serialization;
        this.server = server;
        this.context = context;
        this.protocol = protocol;
    }

    public void start() {
        server.bind( context, cors, this, protocol );
    }

    @Override
    public void handle( final Request request, final Response response ) {
        FST fst = this.fst.get();

        RemoteInvocation invocation = request.body
            .map( Try.map( bytes -> ( RemoteInvocation ) fst.conf.asObject( ByteStreams.toByteArray( bytes ) ) ) )
            .orElseThrow( () -> new RemoteInvocationException( "no invocation data" ) );

        log.trace( "invoke {}", invocation );

        Object service = Application.service( invocation.service );

        if( service == null )
            response.respond( HttpResponse.status( HTTP_NOT_FOUND, invocation.service + " not found" ) );
        else {
            Result<Object, Throwable> result;
            try {
                result = Result.success( service.getClass()
                    .getMethod( invocation.method, invocation.types() )
                    .invoke( service, invocation.values() ) );
            } catch( NoSuchMethodException | IllegalAccessException e ) {
                result = Result.failure( e );
                log.trace( "Method [{}] doesn't exist or access isn't allowed", invocation.method );
            } catch( InvocationTargetException e ) {
                result = Result.failure( e.getCause() );
                log.trace( "Exception occurred on call to method [{}]", invocation.method );
            }
            response.respond( HttpResponse.bytes( fst.conf.asByteArray( result ), APPLICATION_OCTET_STREAM ) );
        }
    }


    @Override
    public String toString() {
        return getClass().getName();
    }
}
