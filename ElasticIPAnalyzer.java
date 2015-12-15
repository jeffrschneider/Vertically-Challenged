package com.transcendcomputing.cloudanalysis;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;

public class ElasticIPAnalyzer {

	private AmazonEC2Client EC2_Client;
	private DescribeAddressesResult AddressResult;
	private List<Address> AddressResults;
	
	public ElasticIPAnalyzer(AmazonEC2Client ec2client)
	{
		this.EC2_Client = ec2client;
		AddressResult = EC2_Client.describeAddresses();
		AddressResults =  AddressResult.getAddresses();
	}

	public void listElasticIP()
	{
		for(int i=0; i< AddressResults.size(); i++){
			AddressResults.get(i);
			System.out.println("Elasttic IP: " + AddressResults.get(i)); }

	}
	
	public int getElasticIPTotalCount()
	{
		return AddressResults.size();
	}
	
	public void getElasticIPList() 
	{

	}
   
	public ArrayList<String> getElasticIPUnusedList() 
	{
		ArrayList<String> UnusedList = new ArrayList<String>();

		for(int i=0; i< AddressResults.size(); i++)
		{
			if (AddressResults.get(i).getInstanceId().length() <1)
			{
							UnusedList.add(AddressResults.get(i).getPublicIp());
			}
		}
		return UnusedList;
	}
	
	public int getElasticIPUnusedCount() 
	{
		int unusedcnt =0;
		
		for(int i=0; i< AddressResults.size(); i++){
			if (AddressResults.get(i).getInstanceId().length() <1) unusedcnt ++; //This is a hack but it works 
			//System.out.println("InstanceID >>>" + AddressResults.get(i).getInstanceId()); 
			}
		return unusedcnt;
	}
	
}
