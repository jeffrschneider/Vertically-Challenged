package com.transcendcomputing.cloudanalysis;

import java.util.ArrayList;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;


/* Identify the Volumes that are unattached, large, old, not part of a CF, no IAM, ...
 *  
 */

public class BlockStorageAnalyzer {

	AmazonEC2Client EC2_Client;
	AmazonCloudWatchClient CW_Client;

	public BlockStorageAnalyzer (AmazonEC2Client ec2_client, AmazonCloudWatchClient cw_client)
	{
		this.EC2_Client = ec2_client;
		this.CW_Client = cw_client;
	}
	
	
	public void getBlockStorage()
	{
		DescribeVolumesRequest VolumeRequest;
		//VolumeRequest.setFilters("detaching");
		
		DescribeVolumesResult EBS_List = EC2_Client.describeVolumes();
		System.out.println("EBS Volume Count:" + EBS_List.getVolumes().size());
		
		for (int i=0; i < EBS_List.getVolumes().size(); i++)
		{
			System.out.println("EBS Volume :" + EBS_List.getVolumes().get(i));
			System.out.println("EBS Volume Attachments :" + EBS_List.getVolumes().get(i).getAttachments());
		}
	}
	
		
	
}
