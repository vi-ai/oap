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

package oap.etl.configuration;

import lombok.val;
import oap.etl.StringExport;
import oap.etl.Table;
import oap.json.Binder;
import oap.testng.AbstractTest;
import oap.tsv.Model;
import org.testng.annotations.Test;

import static oap.etl.accumulator.AccumulatorType.COUNT;
import static oap.etl.accumulator.AccumulatorType.SUM;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationJoinTest extends AbstractTest {
   @Test
   public void testCountingJoin() {
      val aggregatorConfiguration2 = AggregatorConfigurationBuilder
         .custom()
         .table( "table1" )
         .aggregator( "by_gcol" ).fields( "gcol" )
         .accumulator( "sum" ).operation( SUM ).field( "value1" )
         .accumulator( "sumj" ).operation( SUM ).field( "join-name.count" )
         .export( "export" )
         .join( "join-name" ).table( "table2" ).field( "column" ).accumulator( "count" ).operation( COUNT )
         .build();

      System.out.println( "build: " + aggregatorConfiguration2 );
      System.out.println( Binder.json.marshal( aggregatorConfiguration2 ) );

      val aggregatorConfiguration =
         Binder.json.unmarshalResource( getClass(), Aggregator.class, "configuration.json" );

      System.out.println( "json: " + aggregatorConfiguration );

      assertThat( aggregatorConfiguration2 ).isEqualTo( aggregatorConfiguration );

      final Model model1 = new Model( false ).s( "gcol", 0 ).s( "column", 1 ).i( "value1", 2 );
      final Model model2 = new Model( false ).s( "column", 0 ).i( "value2", 1 );

      val export = new StringExport();

      AggregatorBuilder
         .custom()
         .withModel( "table1", model1 )
         .withModel( "table2", model2 )

         .withTable( "table1", Table.fromString( "s\ta\t10\n" +
            "s\ta\t20\n" +
            "s2\ta\t10\n", model1 ) )
         .withTable( "table2", Table.fromString( "a\t20\n" +
            "a\t21\n" +
            "a1\t21\n", model2 ) )

         .withConfiguration( aggregatorConfiguration )

         .withExport( "export", ( s ) -> export )
         .build();

      assertString( export.toString() ).isEqualTo( "s\t30\t4\n" +
         "s2\t10\t2\n" );
   }

   @Test
   public void testBidImpJoin() {
      val aggregatorConfiguration =
         Binder.json.unmarshalResource( getClass(), Aggregator.class, "bid_imp_configuration.json" );

      final Model bidModel = new Model( false ).s( "BID_ID", 0 ).s( "EXCHANGE", 1 ).i( "BID_PRICE", 2 );
      final Model impressionModel = new Model( false ).s( "BID_ID", 0 ).i( "PRICE", 1 );
      final Model clickModel = new Model( false ).s( "BID_ID", 0 );

      val export = new StringExport();

      AggregatorBuilder
         .custom()
         .withModel( "bid", bidModel )
         .withModel( "impression", impressionModel )
         .withModel( "click", clickModel )

         .withTable( "bid", Table.fromString(
            "ID0989\tSMAATO\t896\n" +
               "ID0990\tSMAATO\t890\n" +
               "ID0991\tOPERA\t253\n", bidModel ) )
         .withTable( "impression", Table.fromString(
            "ID0989\t800\n" +
               "ID0991\t200\n", impressionModel ) )
         .withTable( "click", Table.fromString(
            "ID0989\n", clickModel ) )

         .withConfiguration( aggregatorConfiguration )

         .withExport( "export", ( s ) -> export )
         .build();

      assertString( export.toString() ).isEqualTo(
         "OPERA\t1\t1\t200\t253\t0\n" +
            "SMAATO\t2\t1\t800\t896\t1\n" );
   }
}
