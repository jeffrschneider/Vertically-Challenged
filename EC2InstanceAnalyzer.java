package com.transcendcomputing.cloudanalysis;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;


public class EC2InstanceAnalyzer {
	AmazonEC2Client EC2_Client;
	AmazonCloudWatchClient CW_Client;


	public EC2InstanceAnalyzer (AmazonEC2Client ec2_client, AmazonCloudWatchClient cw_client)
	{
		this.EC2_Client = ec2_client;
		this.CW_Client = cw_client;
	}
	
	// Get list of instances that are currently running
	public ArrayList<Reservation> getCurrentInstances(AmazonEC2Client ec2) {
		
		DescribeInstancesResult Instances = ec2.describeInstances();
		List<Reservation> reservations = Instances.getReservations();
		ArrayList<Reservation> newInstances = new ArrayList(reservations);
		
		return newInstances;
    }

	
	public int getCPUThresholdViolationPct(String InstanceID, int thresholdForAverage, int thresholdForMaximum)
	{
		float violationPct = 0;
		int MaxAverages = 0;
		//review some time period - 3,600,000 milliseconds in an hour
		Long timeperiod = (7 * 24 * 3600000L); // days * hours * millisecs   
		Integer samplingInterval = (1 * 3600); // specified in seconds 
		
		Date endTime = new Date(); // that means now
		Date startTime = new Date(endTime.getTime() - timeperiod);  
		
		List<Dimension> dimensions = new ArrayList<Dimension>();
		Dimension dim = new Dimension();
		dim.setName("InstanceId"); 	
		dim.setValue(InstanceID);
		dimensions.add(dim);
		
		List<String> statistics = new ArrayList<String>();
		statistics.add("Average");    // statistic --> Average
		statistics.add("Maximum");    // statistic --> Average
		 
		try {
	    GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest();
	    getMetricStatisticsRequest.setPeriod(samplingInterval); 
	    getMetricStatisticsRequest.setStatistics (statistics);
	    getMetricStatisticsRequest.setStartTime(startTime);
	    getMetricStatisticsRequest.setEndTime(new Date());
	    getMetricStatisticsRequest.setMetricName("CPUUtilization");
	    getMetricStatisticsRequest.setNamespace("AWS/EC2");
	    getMetricStatisticsRequest.setDimensions(dimensions);
	    getMetricStatisticsRequest.setUnit("Percent");
	    
		GetMetricStatisticsResult getMetricStatisticsResult = CW_Client.getMetricStatistics(getMetricStatisticsRequest);

		List<Datapoint> dp = getMetricStatisticsResult.getDatapoints();

		// Let's count the number of times that the Average CPU is greater than 40% 
		// and the number of times the Maximum CPU is greater than 70%. Each time either of them 
		// are exceeded, we'll consider it a 'threshold violation'. Then, we'll figure out the 
		// aggregate percentage of threshold violations for and return that number. 
		int thresholdViolations =0;
		
		for (int cnt=0; cnt < dp.size(); cnt++) {

			if (dp.get(cnt).getAverage() > thresholdForAverage)
			{
				thresholdViolations++;
			} else 	if (dp.get(cnt).getMaximum()> thresholdForMaximum)
				thresholdViolations++;
	    	//System.out.println("Average :" + dp.get(cnt).getAverage() + "     Maximum : " + dp.get(cnt).getMaximum());
	    }
		
		float f = 0;
		if (dp.size() > 0) 
			f= (float)thresholdViolations / dp.size();
		
		System.out.println("Violation count =" + thresholdViolations + "   out of " + dp.size() + " =" + f*100 +"%" );
				
		violationPct = f * 100; //

		} catch (AmazonServiceException ase) {
	
			System.out.println("Caught an AmazonServiceException, which means the request was made  "
		    + "to Amazon EC2, but was rejected with an error response for some reason.");
		    System.out.println("Error Message:    " + ase.getMessage());
		    System.out.println("HTTP Status Code: " + ase.getStatusCode());
		    System.out.println("AWS Error Code:   " + ase.getErrorCode());
		    System.out.println("Error Type:       " + ase.getErrorType());
		    System.out.println("Request ID:       " + ase.getRequestId());
	
		}
		System.out.println ("Percent of threshold violations for instance " + InstanceID + " is "+ violationPct +"%");
		return (int)violationPct;
	}

}
