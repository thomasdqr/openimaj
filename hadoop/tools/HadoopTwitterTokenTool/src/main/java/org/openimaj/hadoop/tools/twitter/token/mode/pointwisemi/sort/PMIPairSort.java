package org.openimaj.hadoop.tools.twitter.token.mode.pointwisemi.sort;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.openimaj.hadoop.mapreduce.stage.helper.SequenceFileTextStage;
import org.openimaj.util.pair.IndependentPair;

/**
 * Sort pairs by PMI within timeperiods
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>, Sina Samangooei <ss@ecs.soton.ac.uk>
 *
 */
public class PMIPairSort extends SequenceFileTextStage<BytesWritable,BytesWritable,BytesWritable,BytesWritable,NullWritable,Text>{
	/**
	 * The minimum PMI
	 */
	public static final String MINP_KEY = "org.openimaj.hadoop.tools.twitter.token.outputmode.pointwisemi.maxp";
	/**
	 * the output name
	 */
	public static final String PMI_NAME = "pmi_sort";
	/**
	 * The minimum number of pairs
	 */
	public static final String MINPAIRCOUNT_KEY = "org.openimaj.hadoop.tools.twitter.token.outputmode.pointwisemi.minpaircount";
	/**
	 * The location of the pairmi
	 */
	public static final String PAIRMI_LOC = "org.openimaj.hadoop.tools.twitter.token.outputmode.pointwisemi.location";
	private double minp;
	private Path outpath;
	private int minPairCount;

	/**
	 * @param minp the minimum PMI value
	 * @param outpath for loading the PMIStats file
	 */
	public PMIPairSort(double minp,Path outpath) {
		this.minp = minp;
		this.outpath = outpath;
	}
	
	/**
	 * @param minp the minimum PMI value
	 * @param minPairCount the minimum number of pairs to emit
	 * @param outpath for loading the PMIStats file
	 */
	public PMIPairSort(double minp, int minPairCount,Path outpath) {
		this.minp = minp;
		this.outpath = outpath;
		this.minPairCount = minPairCount;
	}

	@Override
	public Class<? extends Mapper<BytesWritable, BytesWritable, BytesWritable, BytesWritable>> mapper() {
		return PMISortMapper.class;
	}
	
	@Override
	public Class<? extends Reducer<BytesWritable, BytesWritable, NullWritable,Text>> reducer() {
		return PMISortReducer.class;
	}
	@Override
	public String outname() {
		return PMI_NAME;
	}
	@Override
	public void setup(Job job) {
		job.getConfiguration().setFloat(MINP_KEY, (float) this.minp);
		job.getConfiguration().setInt(MINPAIRCOUNT_KEY, this.minPairCount);
		job.getConfiguration().set(PAIRMI_LOC,this.outpath.toString());
		((JobConf)job.getConfiguration()).setOutputValueGroupingComparator(PMISortValueGroupingComparator.class);
		((JobConf)job.getConfiguration()).setOutputKeyComparatorClass(PMISortKeyComparator.class);
		job.setPartitionerClass(PMISortPartitioner.class);
	}

	/**
	 * write time pmi to a byte array
	 * @param timet
	 * @param pmi
	 * @return a byte array encoding of time and pmi
	 * @throws IOException
	 */
	public static byte[] timePMIBinary(long timet, double pmi) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeLong(timet);
		dos.writeDouble(pmi);
		dos.flush();
		dos.close();
		return baos.toByteArray();
	}
	
	/**
	 * read time and pmi from a byte array. class {@link PMIPairSort#parseTimeBinary(byte[], int, int)} with 
	 * start = 0 and len = bytes.length
	 * @param bytes the bytes to parse
	 * @return time and pmi pair
	 * @throws IOException
	 */
	public static IndependentPair<Long, Double> parseTimeBinary(byte[] bytes) throws IOException {
		return parseTimeBinary(bytes,0,bytes.length);
	}
	
	/**
	 * use a {@link ByteArrayInputStream} and a {@link DataInputStream} to read a byte[] 
	 * @param bytes
	 * @param start offset into bytes
	 * @param len length to read
	 * @return the time pmi pair
	 * @throws IOException
	 */
	public static IndependentPair<Long, Double> parseTimeBinary(byte[] bytes,int start, int len) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes,start,len);
		DataInputStream dis = new DataInputStream(bais);
		return IndependentPair.pair(dis.readLong(), dis.readDouble());
	}
}
