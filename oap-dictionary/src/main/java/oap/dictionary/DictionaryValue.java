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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

/**
 * Created by Igor Petrenko on 29.04.2016.
 */
@EqualsAndHashCode
@ToString
public class DictionaryValue extends DictionaryLeaf implements Dictionary {
   public final List<DictionaryLeaf> values;

   public DictionaryValue( String id, boolean enabled, long externalId ) {
      this( id, enabled, externalId, emptyList(), emptyMap() );
   }

   public DictionaryValue( String id, boolean enabled, long externalId, List<DictionaryLeaf> values ) {
      this( id, enabled, externalId, values, emptyMap() );
   }

   public DictionaryValue(
      String id,
      boolean enabled,
      long externalId,
      Map<String, Object> properties
   ) {
      this( id, enabled, externalId, emptyList(), properties );
   }

   public DictionaryValue(
      String id,
      boolean enabled,
      long externalId,
      List<DictionaryLeaf> values,
      Map<String, Object> properties
   ) {
      super( id, enabled, externalId, properties );
      this.values = values;
   }

   @Override
   public long getOrDefault( String id, long defaultValue ) {
      return getValue( id ).map( v -> v.externalId ).orElse( defaultValue );
   }

   @Override
   public String getOrDefault( long externlId, String defaultValue ) {
      return values.stream().filter( v -> v.externalId == externlId ).findAny().map( d -> d.id ).orElse( defaultValue );
   }

   @Override
   public boolean containsValueWithId( String id ) {
      return getValue( id ).isPresent();
   }

   @Override
   public List<String> ids() {
      return values.stream().map( v -> v.id ).collect( toList() );
   }

   @Override
   public List<DictionaryLeaf> getValues() {
      return values;
   }

   @Override
   public Optional<DictionaryLeaf> getValue( String name ) {
      return values.stream().filter( l -> l.id.equals( name ) ).findAny();
   }
}
