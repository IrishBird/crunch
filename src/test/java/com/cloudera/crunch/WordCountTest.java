/**
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.crunch;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.newInputStreamSupplier;
import static org.junit.Assert.assertTrue;

import com.cloudera.crunch.DoFn;
import com.cloudera.crunch.Emitter;
import com.cloudera.crunch.PCollection;
import com.cloudera.crunch.PTable;
import com.cloudera.crunch.Pipeline;
import com.cloudera.crunch.impl.mr.MRPipeline;
import com.cloudera.crunch.lib.Aggregate;
import com.cloudera.crunch.type.PTypeFamily;
import com.cloudera.crunch.type.avro.AvroTypeFamily;
import com.cloudera.crunch.type.writable.WritableTypeFamily;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Test;

public class WordCountTest {
  
  public static PTable<String, Long> wordCount(PCollection<String> words, PTypeFamily typeFamily) {
    return Aggregate.count(words.parallelDo(new DoFn<String, String>() {
      @Override
      public void process(String line, Emitter<String> emitter) {
        for (String word : line.split("\\s+")) {
          emitter.emit(word);
        }
      }
    }, typeFamily.strings()));
  }
  
  public static PTable<String, Long> substr(PTable<String, Long> ptable) {
	return ptable.parallelDo(new DoFn<Pair<String, Long>, Pair<String, Long>>() {
	  public void process(Pair<String, Long> input,
		  Emitter<Pair<String, Long>> emitter) {
		if (input.first().length() > 0) {
		  emitter.emit(Pair.of(input.first().substring(0, 1), input.second()));
		}
	  }      
    }, ptable.getPTableType());
  }
  
  @Test
  public void testWritables() throws IOException {
    run(new MRPipeline(WordCountTest.class), WritableTypeFamily.getInstance(), false);
  }

  @Test
  public void testWritablesWithSecond() throws IOException {
	run(new MRPipeline(WordCountTest.class), WritableTypeFamily.getInstance(), true);
  }
  
  @Test
  public void testAvro() throws IOException {
    run(new MRPipeline(WordCountTest.class), AvroTypeFamily.getInstance(), false);
  }
  
  @Test
  public void testAvroWithSecond() throws IOException {
    run(new MRPipeline(WordCountTest.class), AvroTypeFamily.getInstance(), true);
  }
  
  public void run(Pipeline pipeline, PTypeFamily typeFamily, boolean runSecond) throws IOException {
    File input = File.createTempFile("shakes", "txt");
    input.deleteOnExit();
    Files.copy(newInputStreamSupplier(getResource("shakes.txt")), input);
    
    File output = File.createTempFile("output", "");
    String outputPath = output.getAbsolutePath();
    output.delete();
    
    PCollection<String> shakespeare = pipeline.readTextFile(input.getAbsolutePath());
    PTable<String, Long> wordCount = wordCount(shakespeare, typeFamily);
    pipeline.writeTextFile(wordCount, outputPath);
    
    if (runSecond) {
      File substrCount = File.createTempFile("substr", "");
      String substrPath = substrCount.getAbsolutePath();
      substrCount.delete();
      PTable<String, Long> we = substr(wordCount).groupByKey().combineValues(
          CombineFn.<String>SUM_LONGS());
      pipeline.writeTextFile(we, substrPath);
    }
    pipeline.done();
    
    File outputFile = new File(output, "part-r-00000");
    List<String> lines = Files.readLines(outputFile, Charset.defaultCharset());
    boolean passed = false;
    for (String line : lines) {
      if (line.equals("Macbeth\t28")) {
        passed = true;
        break;
      }
    }
    assertTrue(passed);
    
    output.deleteOnExit();
  }  
}
