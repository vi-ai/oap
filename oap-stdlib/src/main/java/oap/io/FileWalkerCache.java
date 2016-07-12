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

package oap.io;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class FileWalkerCache {
   private final HashMap<Path, ArrayList<Path>> map = new HashMap<>();

   public DirectoryStream<Path> newDirectoryStream( Path dir,
                                                    DirectoryStream.Filter<? super Path> filter ) throws IOException {
      final ArrayList<Path> list = map.get( dir );
      if( list == null ) return java.nio.file.Files.newDirectoryStream( dir, ( file ) -> {
         map.computeIfAbsent( dir, ( d ) -> new ArrayList<>() ).add( file );
         return filter.accept( file );
      } );

      return new DirectoryStream<Path>() {
         @Override
         public Iterator<Path> iterator() {
            return list.iterator();
         }

         @Override
         public void close() throws IOException {

         }
      };
   }

}
