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

package oap.etl.accumulator;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.tsv.Model;

import java.util.List;

@ToString
@EqualsAndHashCode( exclude = { "sum" } )
public class IntegerSumAccumulator implements Accumulator {
   private int field;
   private int sum;

   public IntegerSumAccumulator( int field ) {
      this.field = field;
   }

   @Override
   public void accumulate( List<Object> values ) {
      this.sum += ( ( Number ) values.get( this.field ) ).intValue();
   }

   @Override
   public void reset() {
      this.sum = 0;
   }

   @Override
   public Integer result() {
      return this.sum;
   }

   @Override
   public IntegerSumAccumulator clone() {
      return new IntegerSumAccumulator( field );
   }

   @Override
   public Model.ColumnType getModelType() {
      return Model.ColumnType.INT;
   }
}
