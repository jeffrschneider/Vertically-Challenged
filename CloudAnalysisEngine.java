package com.transcendcomputing.cloudanalysis;

import java.util.ArrayList;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
//import org.apache.http.HttpEntityEnclosingRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

/**
 * @author jschneider
 * Related Links:
 * http://aws.amazon.com/premiumsupport/trustedadvisor/
 * http://gigaom2.files.wordpress.com/2012/07/ri-top-level.jpg?w=908
 * http://gigaom2.files.wordpress.com/2012/07/ri-recommendations.jpg?w=906&h=464
 * http://blog.cloudyn.com/reserved-instances-more-flexibility-than-you-think/
 */
public class CloudAnalysisEngine {

	
	public static final String[] AWSCloudWatchEndpointName = new String[10];
	public static final String[] AWSCloudWatchEndpointAddress = new String[10]; 
	public static final String[] AWSMachineSize = new String[10]; 		
	public String T1_MICRO = "t1.micro";
	public String M1_SMALL = "m1.small";
	public String M1_MEDIUM = "m1.medium";
	public String M1_LARGE = "m1.large";
	public String M1_XLARGE = "m1.xlarge";
	public String M2_XLARGE = "m2.xlarge"; // M2's are High Memory
	public String M2_2XLARGE = "m2.2xlarge";
	public String M2_4XLARGE = "m2.4xlarge";
	public String C1_MEDIUM = "c1.medium"; // C1's are High CPU machines
	public String C1_XLARGE = "c1.xlarge"; 
	public String CC1_4XLARGE = "cc1.4xlarge"; //Cluster GPU 4 Extra Large Instance
	public String CG1_8XLARGE = "cg1.8xlarge"; //Cluster GPU 8 Extra Large Instance
	public String HI1_4xLARGE = "hi1.4xlarge"; //High I/O Quadruple Extra Large Instance 
	
	/**
	 * This is the primary starting point for the Cloud Analysis Engine
	 */
	public CloudAnalysisEngine() {
		
		AmazonEC2Client EC2_Client;
		AmazonCloudWatchClient CW_Client;
		InstanceMachineManager IMM; 
		ElasticIPAnalyzer EIP;
		BlockStorageAnalyzer EBS;
		
		//Set up the security credentials and create clients to AWS Services
		String accessKey = "YOUR KEY HERE"; //Keys
		String secretKey = "YOUR SECRET KEY HERE";
			
		//String accessKey = "YOUR KEY HERE"; //account key
		//String secretKey = "YOUR SECRET KEY HERE";
		CW_Client = this.addCloudAccount(accessKey, secretKey, AWSCloudWatchEndpointAddress[0]);
		
		EC2_Client = new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));
		
		// ******************************
		// EC2 Instance Analysis based on CloudWatch Data
		// ******************************
		System.out.println("=================== EC2 Instance Locator");
		EC2InstanceAnalyzer AIU = new EC2InstanceAnalyzer(EC2_Client, CW_Client);
		ArrayList<Reservation> instances = AIU.getCurrentInstances(EC2_Client);
		System.out.println("Instance Count: " + instances.size());
		
		// ******************************
		// EC2 Instance Analysis based on CloudWatch Data
		// ******************************
		System.out.println("=================== EC2 Instance Analyzer");
		String instType;
		int pctThresholdViolation; 
		
		/*
		for (int i=0; i< instances.size(); i++)
		{
			instType = instances.get(i).getInstances().get(0).getInstanceType();
			String InstanceID = instances.get(i).getInstances().get(0).getInstanceId();
			
			pctThresholdViolation = AIU.getCPUThresholdViolationPct(InstanceID, 30, 80);	// 2nd param = average cpu; 3rd param = max cpu		
			if (pctThresholdViolation <25)
			{
				if (instType.equals(M1_SMALL))
				{
					System.out.println("CANDIDATE FOUND: m1.small --> t1.micro");
				}
				if (instType.equals(M1_MEDIUM))
				{
					System.out.println("CANDIDATE FOUND: m1.medium --> m1.small");
				}
				if (instType.equals(M1_LARGE))
				{
					System.out.println("CANDIDATE FOUND: m1.large --> m1.medium");
				}			
				if (instType.equals(M1_XLARGE))
				{
					System.out.println("CANDIDATE FOUND: m1.Xlarge --> m1.large");
				}
			}
		}
		*/

		
		//This routine will swap the instance size. For now, it'll be hard-coded rather than allowing our
		// instance analyzer to pick the instances. 
		IMM = new InstanceMachineManager(EC2_Client);
		try {
			IMM.swapRunningInstanceType("i-5d4ad821", T1_MICRO);
		} catch(Exception e) { e.printStackTrace(); }
		
		
		/*
		
		// ******************************
		// Elastic IP Analysis
		// ******************************
		//  Since AWS users pay for "Allocated but unused IP addresses", this routine helps users find them. 
		System.out.println("=================== Elastic IP Analyzer");
		EIP = new ElasticIPAnalyzer(EC2_Client);
		EIP.listElasticIP();
		EIP.getElasticIPList(); // Lists all of the ElasticIP addresses (in use  or not)
		ArrayList IP_List = EIP.getElasticIPUnusedList();
		System.out.println("Unused EIP Addresses :" + IP_List);
		System.out.println("Unused EIP Count :" + EIP.getElasticIPUnusedCount());
		
		
		// ******************************
		// Block Storage Analysis
		// ******************************
		System.out.println("=================== Block Storage Analyzer");
		EBS = new BlockStorageAnalyzer(EC2_Client, CW_Client);
		EBS.getBlockStorage();

		
		// ******************************
		// Object Storage Analysis
		// ******************************
		
		*/
		
	}
	
	/**
	 * The engine can analyze multiple accounts at once. The user add accounts to be analyzed.
	 */
	public AmazonCloudWatchClient addCloudAccount(String accessKey, String secretKey, String regionEndpoint )
	{
		BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		AmazonCloudWatchClient client = new AmazonCloudWatchClient(credentials);
		client.setEndpoint(regionEndpoint);
		return client;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		AWSCloudWatchEndpointName[0] = "US East (Northern Virginia) Region";
		AWSCloudWatchEndpointName[1] = "US West (Oregon) Region";
		AWSCloudWatchEndpointName[2] = "US West (Northern California) Region";
		AWSCloudWatchEndpointName[3] = "EU (Ireland) Region";
		AWSCloudWatchEndpointName[4] = "Asia Pacific (Singapore) Region";
		AWSCloudWatchEndpointName[5] = "Asia Pacific (Tokyo) Region";
		AWSCloudWatchEndpointName[6] = "South America (Sao Paulo) Region";


		AWSCloudWatchEndpointAddress[0] = "monitoring.us-east-1.amazonaws.com";
		AWSCloudWatchEndpointAddress[1] = "monitoring.us-west-2.amazonaws.com";
		AWSCloudWatchEndpointAddress[2] = "monitoring.us-west-1.amazonaws.com";
		AWSCloudWatchEndpointAddress[3] = "monitoring.eu-west-1.amazonaws.com";
		AWSCloudWatchEndpointAddress[4] = "monitoring.ap-southeast-1.amazonaws.com";
		AWSCloudWatchEndpointAddress[5] = "monitoring.ap-northeast-1.amazonaws.com";
		AWSCloudWatchEndpointAddress[6] = "monitoring.sa-east-1.amazonaws.com";
		
		new CloudAnalysisEngine();

	}


}
