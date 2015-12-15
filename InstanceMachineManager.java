package com.transcendcomputing.cloudanalysis;

import java.util.List;
import java.util.ArrayList;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;  
import com.amazonaws.services.ec2.model.StopInstancesResult; 
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;  
import com.amazonaws.services.ec2.model.TerminateInstancesResult;  

/*
 * Useful References:
 * http://docs.amazonwebservices.com/AWSEC2/latest/UserGuide/Using_ChangingAttributesWhileInstanceStopped.html
 * http://alestic.com/2011/02/ec2-change-type 
 */
public class InstanceMachineManager {
	
	private AmazonEC2Client EC2_Client;
	private StartInstancesRequest InstanceRequest;
	private StartInstancesResult InstanceResult;
	private Instance OriginalInstance; 
	private List<Reservation> reservations; 
	private DescribeInstancesRequest request;
	
	
	public InstanceMachineManager(AmazonEC2Client ec2_client)
	{
		this.EC2_Client = ec2_client;
	}
	
	/* 
	 * This method allows a user to change the machine type / size of a running EC2 instance. This can
	 * be used to take a machine that is over-burdened and to increase it's size or to decrease the size 
	 * of a machine. With VPC, we should be able to 'remember' the IP address and not play so many games 
	 * re-associating IP's with the various services.
	 * 
	 *  There are two ways to "end" an instance life: "Terminate" and "Stop". The stop function remembers many of the 
	 *  characteristics of the instance when it is started back up. However, some applications may not have be amenable 
	 *  to a graceful stop. In such cases, it might be better to do a full termination and restart. See: 
	 *  http://docs.amazonwebservices.com/AWSEC2/latest/UserGuide/Stop_Start.html
	 *  http://docs.amazonwebservices.com/AWSEC2/latest/APIReference/ApiReference-query-StopInstances.html
	 */
	
	public void swapRunningInstanceType(String instanceID, String TargetMachineType) throws Exception
	{
		request = new DescribeInstancesRequest();
		//ArrayList<String> myArr = new ArrayList<String>();
		ArrayList<String> instancesList = new ArrayList<String>();
		instancesList.add(instanceID);
		request.setInstanceIds(instancesList);
		
		DescribeInstancesResult Instances = EC2_Client.describeInstances(request);
		reservations = Instances.getReservations();	
		OriginalInstance = reservations.get(0).getInstances().get(0);
		
		//Deny request if it's a Reserved Instance
		// In theory, the call we're making shouldn't ever return a Reserved Instance. There's another call
		// for doing that: DescribeReservedInstancesRequest getReservedInstances() 

		
		//Deny request if it's an RDS, Beanstalk & ElastiCache instance
		// In theory, the call we're making shouldn't return instances created by other services like RDS
		// They have their own calls like, DescribeDBInstancesRequest

		//Deny request if it's a Spot Instance
		if (OriginalInstance.getSpotInstanceRequestId() != null)
		{
			throw new Exception("Denied. You can not change Spot instance types. Spot Instance Value = " + 
					reservations.get(0).getInstances().get(0).getSpotInstanceRequestId());
			
		}
		
		//Deny request if the instance is part of a VPC
		if (OriginalInstance.getVpcId() != null)
		{
			throw new Exception("Denied. You can not change types in a VPC. The VPC id = " + 
					reservations.get(0).getInstances().get(0).getVpcId());
		}
		
		//If the machine is EBS backed, we'll use the "stop" command on the instance. Otherwise, we'll assume it's
		//S3 backed and we'll need to use "terminate".
		System.out.println(OriginalInstance.getRootDeviceType().toLowerCase());
		if (OriginalInstance.getRootDeviceType().equalsIgnoreCase("ebs"))  		{
			swapRunningInstanceTypeWithStop(instanceID, TargetMachineType);
		} else {
			// to do: swapRunningInstanceTypeWithTerminate();
		}
		
 	}
	
	private void swapRunningInstanceTypeWithStop (String instanceID, String TargetMachineType) throws Exception
	{

		//Jeff: What's the business rule if it's part of an Auto Scale Group?
		

		// Start building up the EC2 runInstances(RunInstancesRequest)
		//---------------------------------------------------------------------------------------
		InstanceRequest = new StartInstancesRequest();
		
		//---------------------------------------------------------------------------------------
		// Capture attributes of the running instance before we turn it off
		//---------------------------------------------------------------------------------------
		String Architecture = OriginalInstance.getArchitecture();
		String Hypervisor = OriginalInstance.getHypervisor();
		String InstanceID = OriginalInstance.getInstanceId();
		String ImageID = OriginalInstance.getImageId();
		String InstanceLifeCycle = OriginalInstance.getInstanceLifecycle();
		String Platform = OriginalInstance.getPlatform();
		String PrivateDNSName = OriginalInstance.getPrivateDnsName();
		String PublicDNSName = OriginalInstance.getPublicDnsName();
		String PublicIPAddress = OriginalInstance.getPublicIpAddress();
		String RootDeviceName = OriginalInstance.getRootDeviceName();
		String RootDeviceType = OriginalInstance.getRootDeviceType();
		String SpotInstanceRequest = OriginalInstance.getSpotInstanceRequestId();
		//---------------------------------------------------------------------------------------
		
		
		// ==============   STOP the running instance ========================
		System.out.println("***************** STOPPING");
		ArrayList<String> InstanceIDs = new ArrayList<String>();
		InstanceIDs.add(instanceID);
		
		StopInstancesRequest StopRequest = new StopInstancesRequest();
		StopRequest.setInstanceIds(InstanceIDs);
		StopInstancesResult result = EC2_Client.stopInstances(StopRequest);

		String state;
		while (! result.getStoppingInstances().get(0).getCurrentState().getName().equalsIgnoreCase("stopped")) {
			state = result.getStoppingInstances().get(0).getCurrentState().getName();
			System.out.println("Stopped State = " + state);
			Thread.sleep(1000);
			result = EC2_Client.stopInstances(StopRequest);
		}
		
		
		// ==============   Modify the stopped instance properties ========================
		System.out.println("***************** MODIFYING STOPPED INSTANCE");
		ModifyInstanceAttributeRequest	Mods = new ModifyInstanceAttributeRequest();
		Mods.setInstanceId(instanceID);
		Mods.setInstanceType(TargetMachineType.toLowerCase());
		EC2_Client.modifyInstanceAttribute(Mods);
		
		// ==============   Restart the instance ========================
		System.out.println("***************** STARTING");
		//InstanceRequest.setImageId(ImageID);
		//InstanceRequest.setMinCount(1);
		//InstanceResult = EC2_Client.runInstances(InstanceRequest);
		
		ArrayList<String> theList = new ArrayList<String>();
		theList.add(instanceID);
		request.setInstanceIds(theList);
		InstanceRequest.setInstanceIds(theList);	
		InstanceResult = EC2_Client.startInstances(InstanceRequest);
				
		// ==============   Modify instance run-time properties to original settings ========================
		System.out.println("***************** MODIFYING RUNNING INSTANCE PROPERTIES");
		String NewInstanceID = InstanceResult.getStartingInstances().get(0).getInstanceId();
		DescribeInstancesRequest InstRequest = new DescribeInstancesRequest().withInstanceIds(NewInstanceID);
		DescribeInstancesResult NewInstanceResult = EC2_Client.describeInstances(InstRequest);
		
		Instance NewInstance = NewInstanceResult.getReservations().get(0).getInstances().get(0);
		
		
		System.out.println("Original DNS = " + OriginalInstance.getPrivateDnsName() + "  Changing to :" + PrivateDNSName );
		NewInstance.setPrivateDnsName(PrivateDNSName);
		System.out.println("Revised DNS = " + NewInstance.getPrivateDnsName());
		
		NewInstance.setPublicDnsName(PublicDNSName);
		NewInstance.setPublicIpAddress(PublicIPAddress);
		NewInstance.setInstanceLifecycle(InstanceLifeCycle);


		//Remember the Route 53 DNS associations
		//if (OriginalInstance.getPublicDnsName() != null)
		
	
		//Remember the Elastic Load Balancers that it was connected to
		
		
		
		System.out.println("***************** DONE");
	}
	
	/* ON TERMINATE - - EBS
	 * By default, any volumes that you attach as you launch the instance are automatically deleted when the instance 
	 * terminates. However, any volumes that you attached to a running instance persist even after the instance terminates. 
	 * You can change the default behavior using the DeleteOnTermination flag.
	 */
	private void swapRunningInstanceWithTerminate(String instanceID, String TargetMachineType)
	{
		//Turn the DeleteOnTermination Flag to False. This will prevent us from losing the EBS volumes
		for (int i=0; i < OriginalInstance.getBlockDeviceMappings().size(); i++)
			OriginalInstance.getBlockDeviceMappings().get(i).getEbs().setDeleteOnTermination(Boolean.FALSE);
		
		/*
		//Move the old monitoring settings over
		System.out.println("MONITOR = " + OriginalInstance.getMonitoring().getState());
		if (OriginalInstance.getMonitoring().getState() == "enabled")  
			InstanceRequest.setMonitoring(Boolean.TRUE); 
		else InstanceRequest.setMonitoring(Boolean.FALSE);
		
		*/
		
	}
	

}
