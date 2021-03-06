/*
 * *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.apache.crunch.io.hbase;

import org.apache.crunch.Pair;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

class HTableIterable implements Iterable<Pair<ImmutableBytesWritable, Result>> {
  private final Table table;
  private final Scan[] scans;
  private final Connection connection;

  public HTableIterable(Connection connection, Table table, Scan... scans) {
    this.table = table;
    this.connection = connection;
    this.scans = scans;
  }

  @Override
  public Iterator<Pair<ImmutableBytesWritable, Result>> iterator() {
      return new HTableIterator(connection, table, Arrays.asList(scans));
  }
}
