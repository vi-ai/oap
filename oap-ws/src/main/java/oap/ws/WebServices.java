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
package oap.ws;

import com.google.common.annotations.VisibleForTesting;
import oap.application.Application;
import oap.http.cors.CorsPolicy;
import oap.http.HttpResponse;
import oap.http.HttpServer;
import oap.http.Protocol;
import oap.json.Binder;
import oap.util.Lists;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class WebServices {
    private static Logger logger = getLogger( WebServices.class );

    static {
        HttpResponse.registerProducer( ContentType.APPLICATION_JSON.getMimeType(), Binder.json::marshal );
    }

    private final List<WsConfig> wsConfigs;
    private final HttpServer server;
    private final SessionManager sessionManager;
    private final CorsPolicy globalCorsPolicy;

    public WebServices( HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy ) {
        this( server, sessionManager, globalCorsPolicy, WsConfig.CONFIGURATION.fromClassPath() );
    }

    public WebServices( HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy, WsConfig... wsConfigs ) {
        this( server, sessionManager, globalCorsPolicy, Lists.of( wsConfigs ) );
    }



    public WebServices( HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy, List<WsConfig> wsConfigs ) {
        this.wsConfigs = wsConfigs;
        this.server = server;
        this.sessionManager = sessionManager;
        this.globalCorsPolicy = globalCorsPolicy;
    }

    public void start() {
        logger.info( "binding web services..." );

        for( WsConfig config : wsConfigs ) {
            final List<Interceptor> interceptors = config.interceptors.stream()
                .map( Application::service )
                .map( Interceptor.class::cast )
                .collect( Collectors.toList() );

            for( Map.Entry<String, WsConfig.Service> entry : config.services.entrySet() ) {
                final WsConfig.Service serviceConfig = entry.getValue();
                final Object service = Application.service( serviceConfig.service );

                if( service == null ) throw new IllegalStateException( "Unknown service " + serviceConfig.service );

                CorsPolicy corsPolicy = serviceConfig.corsPolicy != null ? serviceConfig.corsPolicy : globalCorsPolicy;
                bind( entry.getKey(), corsPolicy, service, serviceConfig.sessionAware,
                    sessionManager, interceptors, serviceConfig.protocol );
            }
            for( Map.Entry<String, WsConfig.Service> entry : config.handlers.entrySet() ) {
                final WsConfig.Service handlerConfig = entry.getValue();
                CorsPolicy corsPolicy = handlerConfig.corsPolicy != null ? handlerConfig.corsPolicy : globalCorsPolicy;
                server.bind( entry.getKey(), corsPolicy, Application.service( handlerConfig.service ), handlerConfig.protocol );
            }
        }
    }

    public void stop() {
        for( WsConfig config : wsConfigs ) {
            config.handlers.keySet().forEach( server::unbind );
            config.services.keySet().forEach( server::unbind );
        }
    }

    @VisibleForTesting
    public void bind( String context, CorsPolicy corsPolicy, Object impl, boolean sessionAware, SessionManager sessionManager,
                      List<Interceptor> interceptors, Protocol protocol ) {
        server.bind( context, corsPolicy, new WsService( impl, sessionAware, sessionManager,interceptors ), protocol );
    }

}
