import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class FoF {

  public static class Map extends Mapper<LongWritable, Text, Text, Text> {
    // private final static IntWritable one = new IntWritable(1);
    private Text word_key = new Text();
    private Text word_val = new Text();

    public void map(LongWritable key, Text value, Context context)
        throws IOException, InterruptedException {
      String line = value.toString();
      ArrayList<String> vals =
          new ArrayList<String>(Arrays.asList(line.split("\\s+")));
      String keyStr = vals.get(0);
      for (int i = 0; i < vals.size(); i++) {
        for (int j = 0; j < vals.size(); j++) {
          String node = vals.get(j);
          if (keyStr.equals(node) || keyStr.compareTo(node) < 0) {
            continue;
          }
          word_key.set(vals.get(i));
          word_val.set(keyStr + "," + node);
          context.write(word_key, word_val);
        }
      }
    }
  }

  public static class Reduce extends
 Reducer<Text, Text, Text, Text> {

    public void reduce(Text key, Iterable<Text> values, Context context)
        throws IOException, InterruptedException {
      String keyStr = key.toString();
      HashSet<String> neighbors = new HashSet<String>();
      ArrayList<Connection> connections = new ArrayList<Connection>();
      for (Text vals : values) {
        String nodes[] = vals.toString().split(",");
        if (nodes[0].equals(keyStr)) {
          neighbors.add(nodes[1]);
        } else if(nodes[1].equals(keyStr)) {
          neighbors.add(nodes[0]);
        } else {
          connections.add(new Connection(nodes));
        }
      }
      for (Connection conn : connections) {
        if (neighbors.contains(conn.nodes[0])
            && neighbors.contains(conn.nodes[1])) {
          context.write(key, new Text("<" + keyStr + ", " + conn.nodes[0]
              + ", " + conn.nodes[1] + ">"));
        }
      }
    }

    private class Connection {
      public String[] nodes;

      Connection(String[] nodes) {
        this.nodes = nodes;
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();

    @SuppressWarnings("deprecation")
    Job job = new Job(conf, "FoF");

    job.setJarByClass(FoF.class);

    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    job.setMapperClass(Map.class);
    job.setReducerClass(Reduce.class);

    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));

    job.waitForCompletion(true);
  }

}