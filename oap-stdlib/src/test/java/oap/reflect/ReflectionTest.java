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
package oap.reflect;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.testng.Asserts.assertString;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class ReflectionTest {
    @Test
    public void newInstance() {
        Reflection ref = Reflect.reflect( "oap.reflect.Bean" );
        assertThat( ref.<Bean>newInstance() ).isEqualTo( new Bean( 10 ) );
        assertThat( ref.<Bean>newInstance( Maps.of( __( "i", 1 ) ) ) )
            .isEqualTo( new Bean( 1 ) );
        assertThat( ref.<Bean>newInstance( Maps.of( __( "i", 1 ), __( "x", 2 ) ) ) )
            .isEqualTo( new Bean( 1, 2 ) );
        assertThat( ref.<Bean>newInstance( Maps.of( __( "x", 2 ), __( "i", 1 ) ) ) )
            .isEqualTo( new Bean( 1, 2 ) );
        assertThat( ref.<Bean>newInstance( Maps.of(
            __( "list", Lists.of(
                Maps.of(
                    __( "i", 1 ),
                    __( "list", Lists.of(
                        Maps.of( __( "i", 5 ), __( "x", 6 ) )
                    ) ),
                    __( "map", Maps.of(
                        __( "b", Maps.of( __( "i", 7 ), __( "x", 8 ) ) )
                    ) )
                ),
                Maps.of( __( "i", 3 ), __( "x", 4 ) ) )
            ),
            __( "map", Maps.of(
                __( "a", Maps.of( __( "i", 9 ), __( "x", 10 ) ) )
            ) ),
            __( "i", 1 ) ) ) )
            .isEqualTo( new Bean( 1, Lists.of(
                new Bean( 1, Lists.of( new Bean( 5, 6 ) ), Maps.of( __( "b", new Bean( 7, 8 ) ) ) ),
                new Bean( 3, 4 )
            ), Maps.of( __( "a", new Bean( 9, 10 ) ) ) ) );
    }

    @Test
    public void newInstanceComplex() {
        Reflection ref = Reflect.reflect( "oap.reflect.Bean" );
        Bean expected = new Bean( 2, 1 );
        expected.str = "bbb";
        expected.l = Lists.of( "a", "b" );
        assertThat( ref.<Bean>newInstance( Maps.of(
            __( "x", 1 ),
            __( "i", 2L ),
            __( "str", "bbb" ),
            __( "l", Lists.of( "a", "b" ) )
        ) ) ).isEqualTo( expected );
    }

    @Test
    public void fields() {
        Bean bean = new Bean( 10 );
        assertThat( Reflect.reflect( bean.getClass() ).fields.values().stream().map( f -> f.get( bean ) ) )
            .containsExactly( 10, 1, "aaa", null, Optional.empty(), null, null );
    }

    @Test
    public void assignableFrom() {
        assertThat( Reflect.reflect( Bean.class )
            .field( "l" )
            .type()
            .assignableFrom( List.class ) ).isTrue();
    }


    @Test
    public void annotation() {
        assertThat( Reflect
            .reflect( Bean.class )
            .field( "x" ).isAnnotatedWith( Ann.class ) ).isTrue();
        assertThat( Reflect
            .reflect( Bean.class )
            .field( "x" )
            .annotationOf( Ann.class ).get( 0 )
            .a() ).isEqualTo( 10 );
    }

    @Test
    public void typeRef() {
        Reflection reflection = Reflect.reflect( new TypeRef<List<Map<RetentionPolicy, List<Integer>>>>() {
        } );
        assertString( reflection.toString() ).isEqualTo(
            "Reflection(java.util.List<java.util.Map<java.lang.annotation.RetentionPolicy, java.util.List<java.lang.Integer>>>)" );
    }

    @Test
    public void getCollectionElementType() {
        assertThat( Reflect.reflect( StringList.class ).getCollectionComponentType().underlying ).isEqualTo( String.class );
    }

    @Test
    public void get() {
        Bean bean = new Bean( 1, "bbb" );
        DeepBean deepBean = new DeepBean( bean, Optional.of( bean ), Maps.of(
            __( "x", Maps.of(
                __( "1", 1 ),
                __( "2", 2 )
            ) )
        ) );
        assertThat( Reflect.<Integer>get( deepBean, "bean.x" ) ).isEqualTo( 1 );
        assertString( Reflect.<String>get( deepBean, "bean.str" ) ).isEqualTo( "bbb" );
        assertString( Reflect.<String>get( deepBean, "beanOptional.str" ) ).isEqualTo( "bbb" );
        assertThat( Reflect.<Integer>get( deepBean, "map.[x].[1]" ) ).isEqualTo( 1 );
        assertThat( Reflect.<Integer>get( deepBean, "map.[x].[2]" ) ).isEqualTo( 2 );
        assertThat( Reflect.<Integer>get( deepBean, "map.[x].[3]" ) ).isNull();
        assertThat( Reflect.<Integer>get( deepBean, "map.[z]" ) ).isNull();
        assertThat( Reflect.<String>get( new DeepBean( new Bean(), Optional.empty() ), "beenOptional.str" ) )
            .isNull();
    }

    @Test
    public void set() {
        DeepBean deepBean = new DeepBean( new Bean( 10, "aaa" ), Optional.empty() );

        Reflect.set( deepBean, "bean.str", "new string" );
        Reflect.set( deepBean, "bean.x.y.z", "anything" );
        Reflect.set( deepBean, "map.[x]", Maps.empty() );
        Reflect.set( deepBean, "map.[x].[1]", 1 );
        Reflect.set( deepBean, "bean.optional", "optional present" );
        Reflect.set( deepBean, "bean.i", 42 );


        assertThat( deepBean )
            .isEqualTo( new DeepBean(
                new Bean( 42, "new string", Optional.of( "optional present" ) ),
                Optional.empty(),
                Maps.of( __( "x", Maps.of( __( "1", 1 ) ) ) )
            ) );
    }

    @Test
    public void constructor() {
        assertThat( Reflect.reflect( MatchingConstructor.class ).constructors ).hasSize( 2 );
        assertThatExceptionOfType( ReflectException.class )
            .isThrownBy( () -> Reflect.reflect( MatchingConstructor.class ).newInstance() )
            .withMessage( "class oap.reflect.MatchingConstructor: cannot find matching constructor: {} candidates: [oap.reflect.MatchingConstructor(int i,java.util.List<java.lang.Integer> list), oap.reflect.MatchingConstructor(java.util.List<java.lang.Integer> list)]" );
        assertThat( Reflect.reflect( NoConstructors.class ).constructors ).hasSize( 1 );
        assertThatExceptionOfType( ReflectException.class )
            .isThrownBy( () -> Reflect.reflect( MatchingConstructor.class ).newInstance( Maps.empty() ) )
            .withMessage( "class oap.reflect.MatchingConstructor: cannot find matching constructor: {} candidates: [oap.reflect.MatchingConstructor(int i,java.util.List<java.lang.Integer> list), oap.reflect.MatchingConstructor(java.util.List<java.lang.Integer> list)]" );
    }

    @Test
    public void method() throws NoSuchMethodException {
        assertThat( Reflect.reflect( C.class )
            .method( I.class.getDeclaredMethod( "m", String.class ) ) )
            .isNotNull();
    }
}

interface I {
    void m( String a );
}

@Target( ElementType.FIELD )
@Retention( RetentionPolicy.RUNTIME )
@interface Ann {
    int a() default 1;
}


class C implements I {
    public void m( String a ) {

    }
}

@EqualsAndHashCode
@ToString
class Bean {
    int i;
    @Ann( a = 10 )
    int x = 1;
    String str = "aaa";
    List<String> l;
    Optional<String> optional = Optional.empty();
    List<Bean> list;
    Map<String, Bean> map;

    public Bean() {
        this( 10 );
    }

    public Bean( int i ) {
        this.i = i;
    }

    public Bean( int i, String str ) {
        this.i = i;
        this.str = str;
    }

    public Bean( int i, String str, Optional<String> optional ) {
        this.i = i;
        this.str = str;
        this.optional = optional;
    }

    public Bean( int i, List<Bean> list, Map<String, Bean> map ) {
        this.i = i;
        this.list = list;
        this.map = map;
    }

    public Bean( int i, int x ) {
        this.i = i;
        this.x = x;
    }
}


class StringList extends ArrayList<String> {

}

@EqualsAndHashCode
@ToString
class DeepBean {
    public Bean bean = new Bean();
    public Optional<Bean> beanOptional = Optional.of( new Bean() );
    public Map<String, Map<String, Integer>> map = Maps.empty();

    public DeepBean( Bean bean, Optional<Bean> beanOptional ) {
        this.bean = bean;
        this.beanOptional = beanOptional;
    }

    public DeepBean( Bean bean, Optional<Bean> beanOptional, Map<String, Map<String, Integer>> map ) {
        this.bean = bean;
        this.beanOptional = beanOptional;
        this.map = map;
    }

    public DeepBean() {
    }
}

class MatchingConstructor {
    public MatchingConstructor( int i, List<Integer> list ) {}

    public MatchingConstructor( List<Integer> list ) {}
}

class NoConstructors {}