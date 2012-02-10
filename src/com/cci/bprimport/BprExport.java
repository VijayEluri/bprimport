package com.cci.bprimport;

import com.cisco.provisioning.cpe.*;
import com.cisco.provisioning.cpe.api.*;
import java.io.*;
import java.util.*;

/**
 * BprImport reads in a | delimineted file that contains information about DOCSIS
 * cable modems that need to be imported into BAC. BprImport initializes a connection
 * to the BAC RDU component, creates a batch, adds the modems to the batch, posts the batch,
 * verifies it was successful, and then closes the connection to the RDU.
 * <p>
 * Usage: java bprexport <rdu address> <port> <username> <password> <file name>
 * <p>
 * @author Matt Reath
 * @version 0.1
 * 
 */
public class BprExport {

	/**
	 * Entry point into the BprImport application.
	 * 
	 * @param args	Contains the CLI arguments
	 */
	public static void main(String[] args) {
		
		BprExport bpr = new BprExport();
		
		// Step 1 - Connect to the RDU
		System.out.print("Connecting to the RDU...");
		bpr.initializeConnectionToRDU(args[0], Integer.parseInt(args[1]), args[2], args[3]);
		System.out.print("Success\n\n");
		
		// Step 2 - Create a batch to use
		//System.out.print("Creating the batch...");
		//bpr.startBatch();
		//System.out.print("Success\n\n");
		
		List modems = null;
		
		//System.out.print("Adding devices to the batch");
		// try {
			// get all DOCSIS modem records
			// Search based on a mac address pattern of *
		
			modems = bpr.getAllDOCSISModems();
					// 	
					// FileReader input = new FileReader(args[4]);
					// 					
					// BufferedReader bufRead  = new BufferedReader(input);
								
			// 		String line;
			// 		
			// 		line = bufRead.readLine();
			// 		
			// 		while(line != null) {
			// 			//System.out.println(line);
			// 			
			// 			String temp [] = null;
			// 			
			// 			temp = line.split("\\|");
			// 			
			// 			// temp[0] = Owner ID
			// 			// temp[1] = MAC Address
			// 			// temp[2] = Class of Service
			// 			
			// 			/*
			// 			System.out.println("ID: " + temp[0]);
			// 			System.out.println("MAC: " + temp[1]);
			// 			System.out.println("COS: " + temp[2]);
			// 			System.out.println("---------------");
			// 			*/
			// 			
			// 			bpr.addCableModem(temp[0], temp[1], temp[2]);
			// 			System.out.print(".");
			// 			
			// 			// Get next line
			// 			line = bufRead.readLine();
			// 		}
			
		// } catch (FileNotFoundException fnfe) {
		// 		System.out.println(fnfe.getMessage());
		// 	} catch (IOException ioe) {
		// 		System.out.println(ioe.getMessage());
		// 	}
		
		System.out.print("Success\n");
		
		//System.out.print("Sending batch to the RDU (this may take several minutes)...");
		// Step 4 - Post the batch to the RDU
		//bpr.postBatch();
		//System.out.print("Success\n\n");
		
		System.out.print("Disconnecting...");
		// Step 5 - Verify the batch.
		bpr.endBatch();
		// Step 6 - Disconnect
		bpr.disconnect();
		System.out.print("Success\n");
		
	}
	
	public BprExport() {
		
	}
	
	/**
	 * Initializes a connection to the BAC RDU server.
	 * 
	 * @param hostname	The FQDN or IP of the BAC RDU server
	 * @param port		The port on which to communicate to the server. The default is 49187.
	 * @param userName	User name used to connect to the RDU server
	 * @param password	Password used to connect to the RDU server
	 */
	public void initializeConnectionToRDU(String hostname, int port, String userName, String password) {
		connection = null;
		
		try
		{
			connection =
				PACEConnectionFactory.getInstance(hostname, port, userName, password);
		}
		catch(PACEConnectionException e)
		{
			// Connection failed
			System.out.println(e.getMessage());
			System.exit(0);
		}
		catch(BPRAuthenticationException e)
		{
			// Authentication failure
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Inserts an add cable modem command into the batch.
	 * 
	 * @param ownerID			Customer/Account ID for this cable modem
	 * @param macAddress		MAC address of the cable modem
	 * @param classOfService	The class of service to assign to this modem
	 */
	public void addCableModem(String ownerID, String macAddress, String classOfService) {
		if(batch != null) {
			batch.add(
					DeviceType.DOCSIS, 
					macAddress,
					null,
					null,
					ownerID,
					classOfService,
					"provisioned-docsis",
					null);
		}
	}
	
	public List getAllDOCSISModems() {
		
		List modems = null;
		String startMac = null;
		BatchStatus curStatus = null;
		CommandStatus comStatus = null;
		
		MACAddressPattern pattern = new MACAddressPattern("*");
		MACAddressSearchType mst = MACAddressSearchType.getByDeviceType(DeviceType.DOCSIS, pattern);
		MACAddressSearchFilter msf = new MACAddressSearchFilter(mst,false,20);
		
		while((modems == null || modems.size() == 20)) {
			
			startBatch();
			System.out.println("Searching ...");
			batch.search(msf,startMac);
			postBatch();
			System.out.println("Complete!");
			
			if(0 < status.getCommandCount()) {
				comStatus = status.getCommandStatus(0);
			}
			
			if (status.isError() 
			|| status.isWarning()
			|| comStatus == null
			|| comStatus.isError())
			{
				System.out.println("batch error");
			}
			
			modems = (List)comStatus.getData();
			
			// need to process the modem counts
		
			System.out.println(modems.size());
			
		}
		
		
		return modems;
		
	}
	
	/**
	 * Initializes the batch using the connection object.
	 */
	public void startBatch() {
		if(connection != null) {
			batch = connection.newBatch(ActivationMode.NO_ACTIVATION,ConfirmationMode.NO_CONFIRMATION,PublishingMode.NO_PUBLISHING);
		}
	}
	
	/**
	 * Posts the batch to the RDU server.
	 */
	public void postBatch() {
		status = null;
		
		try {
			status = batch.post();
		}
		catch(ProvisioningException e)
		{
			System.out.println("Error");
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Verifies the status of the batch.
	 */
	public void endBatch() {
		
		CommandStatus cStatus = null;
		
		if(status.isError()) {
			System.out.println("There were errors during batch processing.");
		
			cStatus = status.getFailedCommandStatus();
			
			if (cStatus != null && cStatus.getErrorMessage() != null) {
				System.out.println(cStatus.getErrorMessage());
			}
			else
			{
				System.out.println(status.getBatchID() + ": " + status.getErrorMessage());
			}
			
		} else {
			System.out.println("Batch was processed successfully.");
		}
	}
	
	/**
	 * Disconnects from the RDU server.
	 */
	public void disconnect() {
		connection.releaseConnection();
	}
	
	
	private PACEConnection connection;
	private Batch batch;
	private BatchStatus status;

}