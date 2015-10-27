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
package oap.etl;

import oap.io.IoStreams;
import oap.tsv.Model;
import oap.tsv.Tsv;
import oap.util.Lists;
import oap.util.LongMap;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class CountingKeyJoin implements Join {
    private LongMap map = new LongMap();

    public static Optional<CountingKeyJoin> fromResource( Class<?> contextClass, String name, int field ) {
        return Tsv.fromResource( contextClass, name, Model.withoutHeader().s( field ) )
            .map( s -> s.foldLeft( new CountingKeyJoin(), ( l, list ) -> {
                l.map.increment( (String) list.get( 0 ) );
                return l;
            } ) );
    }

    public static CountingKeyJoin fromFiles( List<Path> files, IoStreams.Encoding encoding, int field ) {
        return Stream.of( files.stream() )
            .foldLeft( new CountingKeyJoin(), ( l, file ) -> {
                Tsv.fromPath( file, encoding, Model.withoutHeader().s( field ) )
                    .forEach( list -> l.map.increment( (String) list.get( 0 ) ) );
                return l;
            } );
    }

    @Override
    public List<Object> on( String key ) {
        return map.containsKey( key ) ? Lists.of( map.get( key ) ) : Lists.of( 0l );
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
