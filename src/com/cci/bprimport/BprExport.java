package com.cci.bprimport;

import com.cisco.provisioning.cpe.*;
import com.cisco.provisioning.cpe.api.*;
import com.cisco.provisioning.cpe.api.search.*;
import com.cisco.*;
import com.cisco.provisioning.cpe.constants.*;
import java.io.*;
import java.util.*;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

/**
 * BprExport connects to an RDU device, queries all MTA and DOCSIS devices out of the
 * database and writes them to a file. 
 * <p>
 * Usage: com.cci.bprimport.BprExport <rdu address> <port> <username> <password> <file name>
 * <p>
 * @author Matt Reath
 * @version 0.2
 * 
 */
public class BprExport {

	/**
	 * Entry point into the BprExport application.
	 * 
	 * @param args	Contains the CLI arguments
	 */
	public static void main(String[] args) {
		
		BprExport bpr = new BprExport();
		
		// Step 1 - Connect to the RDU
		System.out.print("Connecting to the RDU...");
		bpr.initializeConnectionToRDU(args[0], Integer.parseInt(args[1]), args[2], args[3]);
		System.out.print("Success\n\n");
		
		List modems = null;
		
		modems = bpr.getAllDOCSISModems();
		//modems = bpr.getAllMTAs();
		
		
		// Step 5 - Verify the batch.
		bpr.endBatch();
		// Step 6 - Disconnect
		System.out.print("Disconnecting...");
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
		catch(AuthenticationException e)
		{
			// Authentication failure
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}
	
	private RecordSearchResults searchDevice(DeviceSearchType dst, SearchBookmark sb) throws Exception {
	
		RecordSearchResults rs = null;
		CommandStatus comStatus = null;
		
		startBatch();
		batch.searchDevice(dst, sb, 100);
		postBatch();
		
		if(0 < status.getCommandCount()) {
				comStatus = status.getCommandStatus(0);
		}
			
		if (status.isError()) {
			System.out.println("\n" + status.getErrorMessage());
		} else if (comStatus.isError()) {
			System.out.println("\n" + comStatus.getErrorMessage());
		} else if (status.isWarning()) {
			System.out.println("\n" + status.getErrorMessage());
		} else if (comStatus == null) {
			System.out.println("\ncomStatus was null");
		}
		
		rs = (RecordSearchResults)comStatus.getData();
		return rs;
		
	}
	
	private SearchBookmark printRecordSearchResults(RecordSearchResults rs, BufferedWriter buf) throws Exception {
	
		SearchBookmark sb = rs.getSearchBookmark();
		CommandStatus comStatus = null;
		List<RecordData> rdlist = rs.getRecordData();
		Iterator<RecordData> iter = rdlist.iterator();
		
		while(iter.hasNext()) {
			
			RecordData rdObj = iter.next();
			Key keyObj = rdObj.getPrimaryKey();
			
			//System.out.println("DeviceOID: " + ((DeviceID)keyObj).getDeviceId());
			
			List<Key> deviceList = rdObj.getSecondaryKeys();
			
			if (deviceList != null && !deviceList.isEmpty()) {
			
				for (int i = 0; i <deviceList.size(); i++)
				{
					Key key = deviceList.get(i);
					//System.out.println("DeviceID :" + key.toString());
					DeviceID id = new MACAddress(key.toString());
					Map detailMap = null;
				
				startBatch();
				batch.getDetails(id,null);
				postBatch();
				
				if(0 < status.getCommandCount()) {
					comStatus = status.getCommandStatus(0);
				}

				if (status.isError()) {
					System.out.println("\n" + status.getErrorMessage());
				} else if (comStatus.isError()) {
					System.out.println("\n" + comStatus.getErrorMessage());
				} else if (status.isWarning()) {
					System.out.println("\n" + status.getErrorMessage());
				} else if (comStatus == null) {
					System.out.println("\ncomStatus was null");
				}

				detailMap = (Map)comStatus.getData();
				modem_count++;
				
				String ownerID = (String)detailMap.get("/ownerID");
				String dhcpCriteria = (String)detailMap.get("/provisioning/dhcpCriteria");
				String cos = (String)detailMap.get("/provisioning/classOfService");
				String macAddress = (String)detailMap.get("/network/macAddress");
				String dversion = (String)detailMap.get("/network/docsisVersion");

				String device = ownerID + "|" + key.toString() + "|" + cos + "|" + dhcpCriteria;
				System.out.println(device);
					
				}
			}
			
		}
	
		return sb;
	
	}
	
	public List getAllDOCSISModems() {
		
		List modems = null;
		String startMac = null;
		BatchStatus curStatus = null;
		CommandStatus comStatus = null;
		//int modem_count = 0;
		
		// MACAddressPattern pattern = new MACAddressPattern("*");
		// MACAddressSearchType mst = MACAddressSearchType.getByDeviceType(DeviceType.DOCSIS, pattern);
		// MACAddressSearchFilter msf = new MACAddressSearchFilter(mst,true,100);
		
		DeviceSearchType dst = DeviceSearchType.getByDeviceType(
		DeviceType.getDeviceType(DeviceTypeValues.DOCSIS_MODEM),ReturnParameters.ALL);
		
		
		try {
			// create output file
			BufferedWriter buff = new BufferedWriter(new FileWriter(new File("duo_county_docsis_export.txt"), false));
	
			System.out.print("Exporting ...");
			modem_count = 0;
			RecordSearchResults rs = null;
			SearchBookmark sb = null;
		
			rs = searchDevice(dst, sb);
			sb = rs.getSearchBookmark();
			 
			while(sb != null) {
				
				sb = printRecordSearchResults(rs,buff);
				
				rs = searchDevice(dst, sb);
			}
			
			/*
		while((modems == null || modems.size() == 100)) {
				
			
			startBatch();
			
			batch.search(msf,startMac);
			postBatch();
			
			
			if(0 < status.getCommandCount()) {
				comStatus = status.getCommandStatus(0);
			}
			
			if (status.isError()) {
				System.out.println("\n" + status.getErrorMessage());
			} else if (comStatus.isError()) {
				System.out.println("\n" + comStatus.getErrorMessage());
			} else if (status.isWarning()) {
				System.out.println("\n" + status.getErrorMessage());
			} else if (comStatus == null) {
				System.out.println("\ncomStatus was null");
			}
			
		
			modems = (List)comStatus.getData();
			
			Iterator iter = modems.iterator();
			// need to process the modem counts
			while (iter.hasNext()) {
				startMac = (String) iter.next();
				
				Map detailMap = null;
				
				startBatch();
				batch.getDetails(startMac,false);
				postBatch();
				
				if(0 < status.getCommandCount()) {
					comStatus = status.getCommandStatus(0);
				}

				if (status.isError()) {
					System.out.println("\n" + status.getErrorMessage());
				} else if (comStatus.isError()) {
					System.out.println("\n" + comStatus.getErrorMessage());
				} else if (status.isWarning()) {
					System.out.println("\n" + status.getErrorMessage());
				} else if (comStatus == null) {
					System.out.println("\ncomStatus was null");
				}

				detailMap = (Map)comStatus.getData();
				modem_count++;
				
				String ownerID = (String)detailMap.get("/ownerID");
				String dhcpCriteria = (String)detailMap.get("/provisioning/dhcpCriteria");
				String cos = (String)detailMap.get("/provisioning/classOfService");
				String macAddress = (String)detailMap.get("/network/macAddress");
				String dversion = (String)detailMap.get("/network/docsisVersion");
				
				//write out to file
				System.out.print("#");

				String device = ownerID + "|" + macAddress + "|" + cos + "|" + dhcpCriteria;
				
				buff.write(device);
				buff.newLine();
				
			}
			//System.out.println(modems.size());
			
		}
	
		*/
		buff.close();
		System.out.println("\n" + Integer.toString(modem_count) + " modem(s) exported.");
	} catch (Exception e) { 
		System.out.println(e.toString());
		}
		
		return modems;
		
	}
	
	/*
	public List getAllMTAs() {
		
		List modems = null;
		String startMac = null;
		BatchStatus curStatus = null;
		CommandStatus comStatus = null;
		int modem_count = 0;
		
		MACAddressPattern pattern = new MACAddressPattern("*");
		MACAddressSearchType mst = MACAddressSearchType.getByDeviceType(DeviceType.PACKET_CABLE_MTA, pattern);
		MACAddressSearchFilter msf = new MACAddressSearchFilter(mst,true,100);
		
		try {
		// create output file
		BufferedWriter buff = new BufferedWriter(new FileWriter(new File("duo_county_pktcbl_export.txt"), false));
	
		System.out.print("Exporting ...");
	
		while((modems == null || modems.size() == 100)) {
				
			
			startBatch();
			
			batch.search(msf,startMac);
			postBatch();
			
			
			if(0 < status.getCommandCount()) {
				comStatus = status.getCommandStatus(0);
			}
			
			if (status.isError()) {
				System.out.println("\n" + status.getErrorMessage());
			} else if (comStatus.isError()) {
				System.out.println("\n" + comStatus.getErrorMessage());
			} else if (status.isWarning()) {
				System.out.println("\n" + status.getErrorMessage());
			} else if (comStatus == null) {
				System.out.println("\ncomStatus was null");
			}
			
		
			modems = (List)comStatus.getData();
			
			Iterator iter = modems.iterator();
			// need to process the modem counts
			while (iter.hasNext()) {
				startMac = (String) iter.next();
				
				Map detailMap = null;
				
				startBatch();
				batch.getDetails(startMac,false);
				postBatch();
				
				if(0 < status.getCommandCount()) {
					comStatus = status.getCommandStatus(0);
				}

				if (status.isError()) {
					System.out.println("\n" + status.getErrorMessage());
				} else if (comStatus.isError()) {
					System.out.println("\n" + comStatus.getErrorMessage());
				} else if (status.isWarning()) {
					System.out.println("\n" + status.getErrorMessage());
				} else if (comStatus == null) {
					System.out.println("\ncomStatus was null");
				}

				detailMap = (Map)comStatus.getData();
				modem_count++;
				
				String ownerID = (String)detailMap.get("/ownerID");
				String dhcpCriteria = (String)detailMap.get("/provisioning/dhcpCriteria");
				String cos = (String)detailMap.get("/provisioning/classOfService");
				String macAddress = (String)detailMap.get("/network/macAddress");
				String dversion = (String)detailMap.get("/network/docsisVersion");
				
				//write out to file
				System.out.print("#");

				String device = ownerID + "|" + macAddress + "|" + cos + "|" + dhcpCriteria;
				
				buff.write(device);
				buff.newLine();
				
			}
			//System.out.println(modems.size());
			
		}
	
		buff.close();
		System.out.println("\n" + Integer.toString(modem_count) + " mta(s) exported.");
	} catch (Exception e) { 
		System.out.println(e.toString());
		}
		
		return modems;
		
	}
	*/
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
	private int modem_count;

}
