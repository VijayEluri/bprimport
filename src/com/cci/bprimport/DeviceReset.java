package com.cci.bprimport;

import com.cisco.provisioning.cpe.*;
import com.cisco.provisioning.cpe.api.*;
import java.io.*;
import java.util.*;

/**
 * DeviceReset - simple utility for resetting devices in BAC
 * <p>
 * Usage: java com.cci.bprimport.DeviceReset <rdu address> <port> <username> <password> <mac address>
 * <p>
 * @author Matt Reath
 * @version 0.1
 * 
 */
public class DeviceReset {

	/**
	 * Entry point into the BprImport application.
	 * 
	 * @param args	Contains the CLI arguments
	 */
	public static void main(String[] args) {
		
		DeviceReset bpr = new DeviceReset();
		
		// Step 1 - Connect to the RDU
		System.out.print("Connecting to the RDU...");
		bpr.initializeConnectionToRDU(args[1], Integer.parseInt(args[2]), args[3], args[4]);
		System.out.print("Success\n\n");
		
		bpr.startBatch();
		bpr.resetDevice(args[5]);
		bpr.postBatch();
		
		
		bpr.disconnect();
		System.out.print("Success\n");
		
	}
	
	public DeviceReset() {
		
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
		catch(AuthenticationException e)
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
	public void resetDevice(String macAddress) {
		if(batch != null) {
			
			DeviceID id = new MACAddress(macAddress);
			batch.performOperation(DeviceOperation.RESET, id, null);
			
			
		}
	}
	
	
	
	/**
	 * Initializes the batch using the connection object.
	 */
	public void startBatch() {
		if(connection != null) {
			batch = connection.newBatch();
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
