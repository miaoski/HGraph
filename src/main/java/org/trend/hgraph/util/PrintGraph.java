/**
 * 
 */
package org.trend.hgraph.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.trend.hgraph.Graph;
import org.trend.hgraph.HBaseGraphFactory;
import org.trend.hgraph.Vertex;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

/**
 * @author scott_miao
 *
 */
public class PrintGraph extends Configured implements Tool {

  public PrintGraph(Configuration conf) {
    super(conf);
  }

  @Override
  public int run(String[] args) throws Exception {
    String vertexId = null;
    int level = 1;
    if (null == args || args.length == 0) {
      System.err.println("No argument specified !!");
      printUsage();
      return -1;
    } else {
      int idx = 0;
      String cmd = null;
      while (idx < args.length) {
        cmd = args[idx];
        if (cmd.startsWith("-")) {
          if ("-l".equals(cmd)) {
            idx++;
            cmd = args[idx];
            try {
              level = Integer.parseInt(cmd);
            } catch (NumberFormatException e) {
              System.err.println("Parge level from argument:" + cmd + " failed");
              System.err.println("argument:" + cmd + " shall be a numeric value");
              printUsage();
              return -1;
            }
          } else {
            System.err.println("Not recognized argument:" + cmd + " !!");
            printUsage();
            return -1;
          }
        } else {
          if (idx != args.length - 1) {
            System.err.println("The argument combination is wrong !!");
            printUsage();
            return -1;
          }
          vertexId = args[idx];
        }
        idx++;
      }

      Graph graph = null;
      try {
        graph = HBaseGraphFactory.open(this.getConf());
        printGraph(graph, vertexId, level);
      } catch (Exception e) {
        System.err.println("something wrong while printing graph:" + e);
        e.printStackTrace();
        throw e;
      } finally {
        graph.shutdown();
      }
    }

    return 0;
  }

  private static void printGraph(Graph g, String id, int l) {
    int a = 1;
    if (a <= l) {
      long ecnt = 0;
      Vertex v = g.getVertex(id);
      if (null == v) {
        System.out.println("Not record found for id:" + id);
        return;
      }
      System.out.println("**start to print for id:" + id + ", with level:" + l);
      System.out.println("level:" + a);

      ecnt = v.getEdgeCount();
      System.out.println("v=" + v);
      System.out.println("has " + ecnt + " edge(s)");

      a++;
      if (a <= l) {
        for (Edge e : v.getEdges()) {
          doPrintGraph(e, a, l);
        }
      }
    }
  }

  private static void doPrintGraph(Edge e, int cl, int ml) {
    if (cl <= ml) {
      System.out.println("level:" + cl);
      System.out.println("e=" + e);
      Vertex v = (Vertex) e.getVertex(Direction.OUT);
      System.out.println("v=" + v);
      int nl = cl + 1;
      if (nl <= cl) {
        long ecnt = v.getEdgeCount();
        System.out.println("has " + ecnt + " edge(s)");
        for (Edge edge : v.getEdges()) {
          doPrintGraph(edge, nl, ml);
        }
      }
    }
  }

  private static void printUsage() {
    String name = PrintGraph.class.getSimpleName();
    System.out.println("Usage: " + name + " [-l <level-to-print>] <vertex-id>");
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    Configuration conf = HBaseConfiguration.create();
    PrintGraph pg = new PrintGraph(conf);
    int code = 0;
    try {
      code = ToolRunner.run(pg, args);
    } catch (Exception e) {
      e.printStackTrace();
      code = -1;
    }
    System.exit(code);
  }

}
