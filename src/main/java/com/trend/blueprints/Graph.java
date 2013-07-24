/**
 * 
 */
package com.trend.blueprints;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.UnsupportedDataTypeException;
import javax.management.RuntimeErrorException;

import org.apache.commons.lang.Validate;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.GraphQuery;

/**
 * @author scott_miao
 *
 */
public class Graph implements com.tinkerpop.blueprints.Graph {
  
  private final HTablePool POOL;
  private final Configuration CONF;
  
  private final String VERTEX_TABLE_NAME;
  private final String EDGE_TABLE_NAME;
  
  private static final Logger LOG = LoggerFactory.getLogger(Graph.class);
  
  /**
   * @param pool
   * @param conf
   */
  protected Graph(HTablePool pool, Configuration conf) {
    super();
    this.POOL = pool;
    this.CONF = conf;
    
    String vertexTableName = this.CONF.get(HBaseGraphConstants.HBASE_GRAPH_TABLE_VERTEX_NAME_KEY);
    Validate.notEmpty(vertexTableName, HBaseGraphConstants.HBASE_GRAPH_TABLE_VERTEX_NAME_KEY + " shall not be null or empty");
    this.VERTEX_TABLE_NAME = vertexTableName;
    
    String edgeTableName = this.CONF.get(HBaseGraphConstants.HBASE_GRAPH_TABLE_EDGE_NAME_KEY);
    Validate.notEmpty(edgeTableName, HBaseGraphConstants.HBASE_GRAPH_TABLE_EDGE_NAME_KEY + " shall not be null or empty");
    this.EDGE_TABLE_NAME = edgeTableName;
    
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#addEdge(java.lang.Object, com.tinkerpop.blueprints.Vertex, com.tinkerpop.blueprints.Vertex, java.lang.String)
   */
  public Edge addEdge(Object arg0, com.tinkerpop.blueprints.Vertex arg1, 
      com.tinkerpop.blueprints.Vertex arg2, String arg3) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#addVertex(java.lang.Object)
   */
  public Vertex addVertex(Object arg0) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#getEdge(java.lang.Object)
   */
  public Edge getEdge(Object key) {
    if(null == key) return null;
    
    Result r = getResult(key, this.EDGE_TABLE_NAME);
    if(r.isEmpty()) return null;
    
    Edge edge = new Edge(r, this.POOL, this.VERTEX_TABLE_NAME, this.EDGE_TABLE_NAME);
    return edge;
  }

  private Result getResult(Object key, String tableName) {
    HTableInterface table = this.POOL.getTable(tableName);
    Get get = new Get(Bytes.toBytes(key.toString()));
    Result r;
    try {
      r = table.get(get);
    } catch (IOException e) {
      LOG.error("getEdge failed", e);
      throw new RuntimeException(e);
    } finally {
      this.POOL.putTable(table);
    }
    return r;
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#getEdges()
   */
  public Iterable<com.tinkerpop.blueprints.Edge> getEdges() {
    HTableInterface table = this.POOL.getTable(EDGE_TABLE_NAME);
    Scan scan = new Scan();
    ResultScanner rs = null;
    List<com.tinkerpop.blueprints.Edge> edges = new ArrayList<com.tinkerpop.blueprints.Edge>();
    
    try {
      rs = table.getScanner(scan);
      for(Result r : rs) {
        edges.add(new Edge(r, this.POOL, this.VERTEX_TABLE_NAME, this.EDGE_TABLE_NAME));
      }
    } catch (IOException e) {
      LOG.error("getEdges failed", e);
      throw new RuntimeException(e);
    } finally {
      this.POOL.putTable(table);
    }
    return edges;
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#getEdges(java.lang.String, java.lang.Object)
   */
  public Iterable<com.tinkerpop.blueprints.Edge> getEdges(String key, Object value) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#getFeatures()
   */
  public Features getFeatures() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#getVertex(java.lang.Object)
   */
  public Vertex getVertex(Object id) {
    if(null == id) return null;
    
    Result r = getResult(id, this.VERTEX_TABLE_NAME);
    if(r.isEmpty()) return null;
    
    Vertex vertex = new Vertex(r, this.POOL, this.VERTEX_TABLE_NAME, this.EDGE_TABLE_NAME);
    return vertex;
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#getVertices()
   */
  public Iterable<com.tinkerpop.blueprints.Vertex> getVertices() {
    HTableInterface table = this.POOL.getTable(VERTEX_TABLE_NAME);
    Scan scan = new Scan();
    ResultScanner rs = null;
    List<com.tinkerpop.blueprints.Vertex> vertices = new ArrayList<com.tinkerpop.blueprints.Vertex>();
    
    try {
      rs = table.getScanner(scan);
      for(Result r : rs) {
        vertices.add(new Vertex(r, this.POOL, this.VERTEX_TABLE_NAME, this.EDGE_TABLE_NAME));
      }
    } catch (IOException e) {
      LOG.error("getVertices failed", e);
      throw new RuntimeException(e);
    } finally {
      this.POOL.putTable(table);
    }
    return vertices;
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#getVertices(java.lang.String, java.lang.Object)
   */
  public Iterable<com.tinkerpop.blueprints.Vertex> getVertices(String key, Object value) {
    if(null == key || "".equals(key) || null == value) return null;
    HTableInterface table = this.POOL.getTable(VERTEX_TABLE_NAME);
    Properties.Pair<byte[], byte[]> pair = null;
    Scan scan = new Scan();
    List<com.tinkerpop.blueprints.Vertex> vertices = new ArrayList<com.tinkerpop.blueprints.Vertex>();
    try {
      pair = Properties.keyValueToBytes(key, value);
    } catch (UnsupportedDataTypeException e) {
      LOG.error("valueToBytes failed", e);
      throw new RuntimeException(e);
    }
    SingleColumnValueFilter filter = new SingleColumnValueFilter(
        Bytes.toBytes(HBaseGraphConstants.HBASE_GRAPH_TABLE_COLFAM_PROPERTY_NAME), pair.key, CompareOp.EQUAL, pair.value);
    filter.setFilterIfMissing(true);
    scan.setFilter(filter);
    try {
      ResultScanner rs = table.getScanner(scan);
      for(Result r : rs) {
        vertices.add(new Vertex(r, this.POOL, this.VERTEX_TABLE_NAME, this.EDGE_TABLE_NAME));
      }
    } catch (IOException e) {
      LOG.error("getScanner failed", e);
      throw new RuntimeException(e);
    }
    return vertices;
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#query()
   */
  public GraphQuery query() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#removeEdge(com.tinkerpop.blueprints.Edge)
   */
  public void removeEdge(com.tinkerpop.blueprints.Edge arg0) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#removeVertex(com.tinkerpop.blueprints.Vertex)
   */
  public void removeVertex(com.tinkerpop.blueprints.Vertex arg0) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.tinkerpop.blueprints.Graph#shutdown()
   */
  public void shutdown() {
    try {
      this.POOL.close();
    } catch (IOException e) {
      LOG.error("close POOL failed", e);
      throw new RuntimeException(e);
    }
  }

}