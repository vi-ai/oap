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

package oap.application.maven;

import oap.io.Files;
import oap.io.Resources;
import oap.util.Strings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Properties;

import static java.nio.file.attribute.PosixFilePermission.*;

@Mojo( name = "dockerfile", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class DockerfileMojo extends AbstractMojo {
    @Parameter( defaultValue = "${project.build.directory}/docker" )
    private String destinationDirectory;

    @Parameter( defaultValue = "false" )
    private boolean failIfUnsupportedOperationException;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        PosixFilePermission[] permissions = {
            OWNER_EXECUTE, OWNER_READ, OWNER_WRITE,
            GROUP_EXECUTE, GROUP_READ,
            OTHERS_EXECUTE, OTHERS_READ };

        dockerfile( "/docker/Dockerfile", "Dockerfile", permissions );
    }

    private void dockerfile( String resourceFile, String dockerfile, PosixFilePermission... permissions ) {
        Properties properties = project.getProperties();
        Path path = Paths.get( destinationDirectory, dockerfile );
        Resources.readString( getClass(), resourceFile )
            .ifPresent( value -> Files.writeString( path,
                Strings.substitute( value, properties::getProperty ) ) );
        if( permissions.length > 0 ) {
            try {
                Files.setPosixPermissions( path, permissions );
            } catch( UnsupportedOperationException e ) {
                if( failIfUnsupportedOperationException ) throw e;
                getLog().error( e.getMessage() );
            }
        }
    }
}
