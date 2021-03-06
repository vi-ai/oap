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

package oap.media;

import lombok.val;
import oap.application.Application;
import oap.concurrent.SynchronizedThread;
import oap.http.PlainHttpListener;
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.io.Files;
import oap.io.Resources;
import oap.media.postprocessing.VastMediaProcessing;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Cuid;
import oap.util.Pair;
import oap.ws.SessionManager;
import oap.ws.WebServices;
import oap.ws.WsConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static oap.http.testng.HttpAsserts.HTTP_PREFIX;
import static oap.http.testng.HttpAsserts.assertUploadFile;
import static oap.http.testng.HttpAsserts.reset;
import static oap.io.CommandLine.shell;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 06.02.2017.
 */
public class WsFileUploaderTest extends AbstractTest {
    private ArrayList<Pair<Media, MediaInfo>> medias = new ArrayList<>();
    private Server server;
    private WebServices ws;
    private SynchronizedThread listener;
    private Path path;

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        path = Env.tmpPath( "/tmp" );

        Files.ensureDirectory( path );

        medias.clear();

        server = new Server( 100 );
        ws = new WebServices( server, new SessionManager( 10, null, "/" ),
            GenericCorsPolicy.DEFAULT, WsConfig.CONFIGURATION.fromResource( getClass(), "ws-multipart.conf" )
        );

        val service = new WsFileUploader( path, 1024 * 9, -1,
            singletonList( new VastMediaProcessing(
                shell( "ffprobe -v quiet -print_format xml -show_format -show_streams {FILE}" ), 10000L
            ) )
        );
        service.addListener( ( media, mediaInfo ) -> WsFileUploaderTest.this.medias.add( __( media, mediaInfo ) ) );
        Application.register( "upload", service );
        ws.start();
        listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
        listener.start();
    }

    @AfterMethod
    @Override
    public void afterMethod() throws Exception {
        listener.stop();
        server.stop();
        ws.stop();
        reset();

        Application.unregisterServices();

        Cuid.resetToDefaults();
    }

    @Test
    public void testUploadVideo() throws IOException {
        val path = Resources.filePath( getClass(), "SampleVideo_1280x720_1mb.mp4" ).get();

        Cuid.reset( "p", 1 );

        val resp = new AtomicReference<WsFileUploader.MediaResponse>();

        assertUploadFile( HTTP_PREFIX + "/upload/", "test/test2", path )
            .isOk()
            .is( r -> resp.set( r.<WsFileUploader.MediaResponse>unmarshal( WsFileUploader.MediaResponse.class ).get() ) );

        assertThat( resp.get().id ).isEqualTo( "1p" );
        assertThat( resp.get().info.get( "vast" ) ).isNotNull();
        assertThat( resp.get().info.get( "Content-Type" ) ).isEqualTo( "video/mp4" );

        assertThat( medias ).hasSize( 1 );
        assertThat( medias.get( 0 )._1.id ).startsWith( "test/test2/1p.mp4" );
        assertThat( medias.get( 0 )._1.name ).isEqualTo( "SampleVideo_1280x720_1mb.mp4" );
        assertThat( medias.get( 0 )._1.contentType ).isEqualTo( "video/mp4" );
        assertThat( medias.get( 0 )._2.get( "vast" ) ).isNotNull();
        assertThat( resp.get().info.get( "Content-Type" ) ).isEqualTo( "video/mp4" );
    }

    @Test
    public void testUploadImage() throws IOException {
        val path = Resources.filePath( getClass(), "qt.png" ).get();

        Cuid.reset( "p", 1 );

        val resp = new AtomicReference<WsFileUploader.MediaResponse>();

        assertUploadFile( HTTP_PREFIX + "/upload/", "test/test2", path )
            .isOk()
            .is( r -> resp.set( r.<WsFileUploader.MediaResponse>unmarshal( WsFileUploader.MediaResponse.class ).get() ) );

        assertThat( resp.get().id ).isEqualTo( "1p" );
        assertThat( resp.get().info.get( "Content-Type" ) ).isEqualTo( "image/png" );

        assertThat( medias ).hasSize( 1 );
        assertThat( medias.get( 0 )._1.id ).isEqualTo( "test/test2/1p.png" );
        assertThat( medias.get( 0 )._1.name ).isEqualTo( "qt.png" );
        assertThat( medias.get( 0 )._1.contentType ).isEqualTo( "image/png" );
        assertThat( resp.get().info.get( "Content-Type" ) ).isEqualTo( "image/png" );
    }
}
