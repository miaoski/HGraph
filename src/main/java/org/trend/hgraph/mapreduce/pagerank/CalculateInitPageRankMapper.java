/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trend.hgraph.mapreduce.pagerank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.trend.hgraph.HBaseGraphConstants;

/**
 * A <code>Mapper</code> for calculating initial pagerank value from HBase.
 * @author scott_miao
 */
public class CalculateInitPageRankMapper extends TableMapper<BytesWritable, DoubleWritable> {

  private String edgeTableName;
  
  enum Counters {VERTEX_COUNT}

  /*
   * (non-Javadoc)
   * @see org.apache.hadoop.mapreduce.Mapper#map(java.lang.Object, java.lang.Object,
   * org.apache.hadoop.mapreduce.Mapper.Context)
   */
  @Override
  protected void map(final ImmutableBytesWritable key, final Result value, final Context context)
      throws IOException, InterruptedException {
    String rowKey = Bytes.toString(key.get());
    double pageRank = getPageRank(value);
    List<String> outgoingRowKeys = null;

    context.getCounter(Counters.VERTEX_COUNT).increment(1);
    outgoingRowKeys = collectOutgoingRowKeys(context.getConfiguration(), edgeTableName, rowKey);
    dispatchPageRank(outgoingRowKeys, pageRank, new ContextWriterStrategy() {
      @Override
      public void write(String key, double value) throws IOException, InterruptedException {
        context.write(new BytesWritable(Bytes.toBytes(key)), new DoubleWritable(value));
      }
    });
  }

  static void dispatchPageRank(List<String> outgoingRowKeys, double pageRank,
      ContextWriterStrategy strategy) throws IOException, InterruptedException {
    String outgoingRowKey = null;
    double outgoingCnt = outgoingRowKeys.size();
    double pageRankForEachOutgoing = pageRank / outgoingCnt;
    for (Iterator<String> it = outgoingRowKeys.iterator(); it.hasNext();) {
      outgoingRowKey = it.next();
      strategy.write(outgoingRowKey, pageRankForEachOutgoing);
    }
  }

  interface ContextWriterStrategy {
    void write(String key, double value) throws IOException, InterruptedException;
  }

  static List<String> collectOutgoingRowKeys(Configuration conf, String tableName, String rowKey)
      throws IOException {
    String outgoingRowKey = null;
    List<String> outgoingRowKeys = null;
    HTable edgeTable = null;
    ResultScanner rs = null;
    try {
      edgeTable = new HTable(conf, tableName);
      Scan scan = new Scan();

      scan.setStartRow(Bytes.toBytes(rowKey
          + HBaseGraphConstants.HBASE_GRAPH_TABLE_EDGE_DELIMITER_1));
      scan.setStopRow(Bytes.toBytes(rowKey + "~"));
      scan.setFilter(new FirstKeyOnlyFilter());

      rs = edgeTable.getScanner(scan);
      outgoingRowKeys = new ArrayList<String>();
      for (Result r : rs) {
        outgoingRowKey = getOutgoingRowKey(r);
        outgoingRowKeys.add(outgoingRowKey);
      }
    } catch (IOException e) {
      System.err.println("access htable:" + tableName + " failed");
      e.printStackTrace(System.err);
      throw e;
    } finally {
      edgeTable.close();
    }
    return outgoingRowKeys;
  }

  private static String getOutgoingRowKey(Result r) {
    String rowKey;
    String outgoingRowKey;
    int idx;
    rowKey = Bytes.toString(r.getRow());
    idx = rowKey.indexOf(HBaseGraphConstants.HBASE_GRAPH_TABLE_EDGE_DELIMITER_1);
    idx =
        rowKey.indexOf(HBaseGraphConstants.HBASE_GRAPH_TABLE_EDGE_DELIMITER_2, idx
            + HBaseGraphConstants.HBASE_GRAPH_TABLE_EDGE_DELIMITER_1.length());
    outgoingRowKey =
        rowKey.substring(idx + HBaseGraphConstants.HBASE_GRAPH_TABLE_EDGE_DELIMITER_2.length(),
          rowKey.length());
    return outgoingRowKey;
  }

  private double getPageRank(Result value) {
    byte[] colValue =
        value.getValue(
          Bytes.toBytes(HBaseGraphConstants.HBASE_GRAPH_TABLE_COLFAM_PROPERTY_NAME),
          Bytes.toBytes("pageRank"
              + HBaseGraphConstants.HBASE_GRAPH_TABLE_COLFAM_PROPERTY_NAME_DELIMITER + "Double"));
    double pageRank = 0.0D;
    if (null != colValue) {
      pageRank = Bytes.toDouble(colValue);
    }
    return pageRank;
  }

  /*
   * (non-Javadoc)
   * @see org.apache.hadoop.mapreduce.Mapper#setup(org.apache.hadoop.mapreduce.Mapper.Context)
   */
  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    edgeTableName =
        context.getConfiguration().get(HBaseGraphConstants.HBASE_GRAPH_TABLE_EDGE_NAME_KEY);
    Validate.notEmpty(edgeTableName, HBaseGraphConstants.HBASE_GRAPH_TABLE_EDGE_NAME_KEY
        + " shall be set before running this Mapper:" + this.getClass().getName());
  }

}
