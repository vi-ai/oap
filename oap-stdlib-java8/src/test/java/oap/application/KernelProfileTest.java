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

package oap.application;

import lombok.val;
import oap.util.Lists;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class KernelProfileTest {
    @BeforeMethod
    public void unregister() {
        Application.unregisterServices();
    }

    @Test
    public void profileName() {
        try( val kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module.yaml" ) ) ) ) {
            startWithProfile( kernel, "profile-name" );

            assertThat( kernel.<TestProfile1>service( "profile1" ) ).isPresent();
            assertThat( kernel.<TestProfile2>service( "profile2" ) ).isNotPresent();
            assertThat( kernel.<TestProfile3>service( "profile3" ) ).isPresent();
        }
    }

    @Test
    public void serviceProfiles() {
        val modules = Lists.of( urlOfTestResource( getClass(), "module-profiles.yaml" ) );

        try( val kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "profile-name1" );
            assertThat( kernel.<TestProfile1>service( "profile" ) ).isPresent();
        }
        try( val kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "profile-name1", "profile-name2" );
            assertThat( kernel.<TestProfile1>service( "profile" ) ).isNotPresent();
        }
        try( val kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "profile-name2" );
            assertThat( kernel.<TestProfile1>service( "profile" ) ).isNotPresent();
        }
        try( val kernel = new Kernel( modules ) ) {
            startWithProfile( kernel );
            assertThat( kernel.<TestProfile1>service( "profile" ) ).isNotPresent();
        }
    }

    private void startWithProfile( Kernel kernel, String... profiles ) {
        val applicationConfiguration = ApplicationConfiguration.load();
        applicationConfiguration.profiles.addAll( asList( profiles ) );
        kernel.start( applicationConfiguration );
    }

    @Test
    public void profileName2() {
        try( val kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module.yaml" ) ) ) ) {
            startWithProfile( kernel, "profile-name-2" );

            assertThat( kernel.<TestProfile1>service( "profile1" ) ).isNotPresent();
            assertThat( kernel.<TestProfile2>service( "profile2" ) ).isPresent();
            assertThat( kernel.<TestProfile3>service( "profile3" ) ).isPresent();
        }
    }

    @Test
    public void profile3() {
        try( val kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module3.yaml" ) ) ) ) {
            startWithProfile( kernel, "profile-name" );
            assertThat( kernel.<TestContainer>service( "container" ) ).isPresent();
        }
    }

    @Test
    public void profile4() {
        try( val kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module4.yaml" ) ) ) ) {
            startWithProfile( kernel, "run" );
            assertThat( kernel.<Object>service( "container" ) ).isPresent().get().isInstanceOf( TestContainer2.class );
        }
    }

    @Test
    public void moduleProfiles() {
        try( val kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module-profile.yaml" ) ) ) ) {
            startWithProfile( kernel, "test1" );
            assertThat( kernel.<Object>service( "module-profile" ) ).isPresent().get().isInstanceOf( TestProfile1.class );
        }

        try( val kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module-profile.yaml" ) ) ) ) {
            startWithProfile( kernel );
            assertThat( kernel.<Object>service( "module-profile" ) ).isNotPresent();
        }
    }

    public interface TestProfile {

    }

    public static class TestProfile1 implements TestProfile {
    }

    public static class TestProfile2 implements TestProfile {
    }

    public static class TestProfile3 implements TestProfile {
    }

    public static class TestContainer {
        public TestContainer( TestProfile profile ) {
            assertThat( profile ).isNotNull();
        }
    }

    public static class TestContainer2 {
        public TestContainer2() {
        }
    }
}
