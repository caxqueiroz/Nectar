/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.The ASF licenses this file
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

package com.zinnia.nectar.regression.hadoop.primitive.jobs;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.chain.ChainMapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.zinnia.nectar.regression.hadoop.primitive.mapreduce.DoubleSumReducer;
import com.zinnia.nectar.regression.hadoop.primitive.mapreduce.SigmaXYMapper;
import com.zinnia.nectar.regression.language.primitive.Preferences;
import com.zinnia.nectar.util.hadoop.FieldSeperator;

public class SigmaXYJob {

	private String inputFilePath;
	private String outputFilePath; 
	private int x,y;
	public SigmaXYJob(String inputFilePath,String outputFilePath,int x,int y) {
		super();
		this.outputFilePath=outputFilePath;
		this.inputFilePath = inputFilePath;
		this.x=x;
		this.y=y;
	}
	public void createRunHadoop() throws IOException, InterruptedException
	{
		JobControl jobControl = new JobControl("sigmajob");

		Job job = new Job();
		job.setJarByClass(SigmaXYJob.class);
		
		
		ChainMapper.addMapper(job, FieldSeperator.FieldSeperationMapper.class,LongWritable.class,Text.class,NullWritable.class,Text.class,job.getConfiguration());
		
		ChainMapper.addMapper(job, SigmaXYMapper.class,NullWritable.class,Text.class,Text.class,DoubleWritable.class,job.getConfiguration());
		
		job.getConfiguration().set("fields.spec",x + "," +y);
				
		
		
		job.setReducerClass(DoubleSumReducer.class);
		FileInputFormat.addInputPath(job, new Path(inputFilePath));
		FileOutputFormat.setOutputPath(job,new Path(outputFilePath));
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);
		job.setInputFormatClass(TextInputFormat.class);
		
		ControlledJob controlledJob = new ControlledJob(job.getConfiguration());
		jobControl.addJob(controlledJob);
		Thread thread = new Thread(jobControl);
		thread.start();
		while(!jobControl.allFinished())
		{
			Thread.sleep(10000);
		}
		
		FileSystem fs = FileSystem.get(job.getConfiguration());
		fs.copyToLocalFile(new Path(outputFilePath),new Path("/tmp/"+outputFilePath));
		System.exit(0);

		

	}
	public static void main(String args[])
	{
		int x= Integer.parseInt(args[2]);
		int y=Integer.parseInt(args[3]);
		
		SigmaXYJob sigmaJob = new SigmaXYJob(args[0],args[1],x,y);
		try {
			sigmaJob.createRunHadoop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}