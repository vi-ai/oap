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

import oap.concurrent.SynchronizedThread;
import oap.http.PlainHttpListener;
import oap.http.Protocol;
import oap.http.Server;
import oap.http.Session;
import oap.http.cors.GenericCorsPolicy;
import oap.http.testng.HttpAsserts;
import oap.json.Binder;
import oap.metrics.Metrics;
import oap.testng.Env;
import oap.util.Maps;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static oap.http.Request.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.util.Pair.__;
import static oap.ws.WsParam.From.SESSION;

public class WsServiceSessionTest {

    private final SessionManager sessionManager = new SessionManager( 10, null, "/" );

    private final Server server = new Server( 100 );
    private final WebServices ws = new WebServices( server, sessionManager, GenericCorsPolicy.DEFAULT );

    private SynchronizedThread listener;

    @BeforeClass
    public void startServer() {
        Metrics.resetAll();
        server.start();
        ws.bind( "test", GenericCorsPolicy.DEFAULT, new TestWS(), true, sessionManager, Collections.emptyList(), Protocol.HTTP );

        PlainHttpListener http = new PlainHttpListener( server, Env.port() );
        listener = new SynchronizedThread( http );
        listener.start();
    }

    @AfterClass
    public void stopServer() {
        listener.stop();
        server.stop();
        server.unbind( "test" );

        HttpAsserts.reset();
        Metrics.resetAll();
    }

    @Test
    public void testShouldVerifySessionPropagation() throws IOException {

        final Session session = new Session();
        LinkedHashMap<Integer, Integer> map = Maps.of( __( 1, 2 ) );
        session.set( "map", map );

        sessionManager.put( "123456", session );

        assertGet( HttpAsserts.HTTP_PREFIX + "/test/", Maps.empty(), Maps.of( __( "Cookie", "Authorization=987654321; SID=123456" ) ) )
                .hasCode( 200 )
                .hasBody( Binder.json.marshal( map ) );
    }


    private class TestWS {

        @WsMethod( path = "/", method = GET )
        public Map<Integer, Integer> test( @WsParam( from = SESSION ) Map<Integer, Integer> map ) {
            return map;
        }
    }
}
