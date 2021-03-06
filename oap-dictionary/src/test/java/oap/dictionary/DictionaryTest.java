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

package oap.dictionary;

import oap.testng.AbstractTest;
import oap.util.Maps;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

/**
 * Created by Igor Petrenko on 15.04.2016.
 */
public class DictionaryTest extends AbstractTest {
   @Test
   public void testParse() {
      assertThat( Dictionaries.getDictionary( "test-dictionary" ).name ).isEqualTo( "test-dictionary" );
      List<? extends Dictionary> dictValues = Dictionaries.getDictionary( "test-dictionary" ).getValues();
      assertThat( dictValues ).contains( new DictionaryValue( "id1", true, '1',
            Arrays.asList(
               new DictionaryLeaf( "id11", true, 11, Maps.of( __( "title", "title11" ) ) ),
               new DictionaryLeaf( "id12", true, 12, Maps.of( __( "title", "title12" ) ) )
            ),
            Maps.of( __( "title", "title1" ) ) )
         , new DictionaryLeaf( "id2", true, 50, Maps.of( __( "title", "title2" ) ) )
      );
      assertTrue(dictValues.get( 2 ).getTags().contains( "tag1" ));
      assertTrue(dictValues.get( 2 ).getTags().contains( "tag2" ));
   }
}
