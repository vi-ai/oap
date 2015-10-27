/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
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
package oap.ws.apache;

import oap.application.Application;
import oap.ws.RawService;
import oap.ws.Service;
import oap.ws.WsConfig;
import oap.ws.WsException;
import org.apache.http.ExceptionLogger;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public class NioServer implements Closeable {

    private static Logger logger = LoggerFactory.getLogger( Server.class );
    private UriHttpAsyncRequestHandlerMapper mapper = new UriHttpAsyncRequestHandlerMapper();
    private final int port;
    private HttpServer server;

    public NioServer( int port ) throws WsException {
        this.port = port;
        this.mapper.register( "/static/*", new NioClasspathResourceHandler( "/static", "/WEB-INF" ) );

        IOReactorConfig config = IOReactorConfig.custom()
            .setSoTimeout( 500 )
            .setTcpNoDelay( true )
            .setSoKeepAlive( true )
            .build();

        server = ServerBootstrap.bootstrap()
            .setListenerPort( port )
            .setServerInfo( "OAP Server/1.0" )
            .setIOReactorConfig( config )
            .setExceptionLogger( ExceptionLogger.STD_ERR )
            .setHandlerMapper( mapper )
            .create();
    }

    public void bind( String context, Object impl ) {
        String binding = "/" + context + "/*";
        String serviceName = impl.getClass().getSimpleName();
        RawService service = impl instanceof RawService ? (RawService) impl : new Service( impl );
        this.mapper.register( binding, new NioServiceHandler( serviceName, "/" + context, service ) );
        logger.info( serviceName + " bound to " + binding );
    }

    public void unbind( String context ) {
        this.mapper.unregister( "/" + context + "/*" );
    }

    public void start() {
        try {
            logger.info( "binding web services..." );

            for( WsConfig config : WsConfig.fromClassPath() )
                for( WsConfig.Service service : config.services )
                    bind( service.context, Application.service( service.service ) );

            logger.info( "starting [localhost:" + port + "]..." );

            server.start();

        } catch( Exception e ) {
            logger.error( e.getMessage() + " [" + server.getEndpoint().getAddress() + ":" + port + "]", e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public void stop() {
        server.shutdown( 1, TimeUnit.SECONDS );
        try {
            server.awaitTermination( 60, TimeUnit.SECONDS );
        } catch( InterruptedException e ) {
            e.printStackTrace();
        }

        for( WsConfig config : WsConfig.fromClassPath() )
            for( WsConfig.Service service : config.services ) unbind( service.context );

        logger.info( "server gone down" );
    }

    @Override
    public void close() {
        stop();
    }
}
