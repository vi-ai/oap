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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Created by Igor Petrenko on 14.04.2016.
 */
@EqualsAndHashCode
@ToString
public final class DictionaryRoot implements Dictionary {
   public final String name;
   private final List<DictionaryLeaf> values;

   @JsonIgnore
   private final HashMap<Long, String> indexByExternalId = new HashMap<>();
   @JsonIgnore
   private final HashMap<String, DictionaryLeaf> indexById = new HashMap<>();

   public DictionaryRoot( String name, List<DictionaryLeaf> values ) {
      this.name = name;
      this.values = values;

      for( DictionaryLeaf dv : values ) {
         indexById.put( dv.id, dv );
         indexByExternalId.put( dv.externalId, dv.id );
      }
   }

   @Override
   public final String getOrDefault( long externlId, String defaultValue ) {
      final String id = indexByExternalId.get( externlId );
      if( id == null ) return defaultValue;
      return id;
   }

   @Override
   public final long getOrDefault( String id, long defaultValue ) {
      final DictionaryLeaf rtb = indexById.get( id );
      if( rtb == null ) return defaultValue;
      return rtb.externalId;
   }

   @Override
   public boolean containsValueWithId( String id ) {
      return indexById.containsKey( id );
   }

   @Override
   public List<String> ids() {
      return values.stream().map( v -> v.id ).collect( toList() );
   }

   @Override
   public Map<String, Object> getProperties() {
      return Collections.emptyMap();
   }

   @Override
   public List<DictionaryLeaf> getValues() {
      return values;
   }

   @Override
   public Optional<DictionaryLeaf> getValue( String name ) {
      return Optional.ofNullable( indexById.get( name ) );
   }
}
