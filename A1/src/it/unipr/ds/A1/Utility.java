package it.unipr.ds.A1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import jxl.Workbook;
import jxl.format.Colour;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/*
 * Class containing helpful static methods used across other classes
 */
public class Utility {

	/**
	 * Method that reads a map and a value, and return the first key in the map 
	 * that corresponds to that value
	 * @param <K> key type
	 * @param <V> value type
	 * @param map the map from which the key will be extracted
	 * @param value the value to search in the map
	 * @return the first key in the map that corresponds to value
	 */
	public static <K, V> K getKey(Map<K, V> map, V value) {
		for (Entry<K, V> entry : map.entrySet()) {
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Method that reads and parses a .properties file, containing the address and
	 * the port of the master node 
	 * TODO: Maybe this file could contain something else?
	 * 
	 * @param properties String that identifies the properties file
	 * @return a String, containing master_addr and master_port separated by ":"
	 * @throws IOException
	 */
	public static String readConfig(final String properties, final String property) throws IOException {
		// Create a reader object on the properties file
		FileReader reader = new FileReader(properties);

		// Create properties object
		Properties p = new Properties();

		// Add a wrapper around reader object
		p.load(reader);

		// Access properties data
		String serversProperty = p.getProperty(property);

		return serversProperty;
	}

	/**
	 * Method that writes on the .properties file all the necessary parameters for the system:
	 * master=address,port
	 * Used to initialize the config file after Master node starting
	 * 
	 * @param properties String that identifies the properties file
	 * @param address    String that identifies the address (key)
	 * @param port       Int that identifies the port (value)
	 * @param M          Int that identifies the number of messages to be exchanged
	 * @param LP         Float that identifies the probability that a send operation will fail
	 * @throws IOException
	 */
	public static void writeConfig(final String properties, final String address, final int port, final int M,
			final float LP) throws IOException {
		// Create an OutputStream to write on file
		try (OutputStream output = new FileOutputStream(properties)) {
			Properties p = new Properties();
			p.setProperty("master", address + "," + port);
			p.setProperty("M", String.valueOf(M));
			p.setProperty("LP", String.valueOf(LP));

			p.store(output, null);
		} catch (IOException io) {
			io.printStackTrace();
		}

	}

	/**
	* Method that sends an Object obj over a socket s
	* @param s The socket over which m will be sent
	* @param obj The object to be sent
	*/
	public static void send(Socket s, Object obj) {
		ObjectOutputStream os = null;
		try {
			os = new ObjectOutputStream(s.getOutputStream());

			os.writeObject(obj);
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	/**
	* Method that receives and returns an Object obj over a Socket s
	* @param s The socket over which obj will be received
	* @return obj The object to be received
	*/
	public static Object receive(Socket s) {
		ObjectInputStream is = null;
		Object obj = null;

		try {
			is = new ObjectInputStream(s.getInputStream());
			obj = is.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		return obj;
	}
	
	public static void createExcel(File file, Map<Integer, Statistics> map) {
		if(file.exists())
			file.delete();
			
        WritableWorkbook workbook;
        
            try {
                workbook = Workbook.createWorkbook(file);
                
                WritableSheet sheet = workbook.createSheet("report", 0);
                
                WritableCellFormat headerFormat = new WritableCellFormat();
                WritableFont font = new WritableFont(WritableFont.ARIAL, 12, WritableFont.NO_BOLD);
                
                headerFormat.setFont(font);
                headerFormat.setBackground(Colour.LIGHT_BLUE);
                headerFormat.setWrap(true);
                
                int colSize = 30;
                int nCol = 0;
                
                Label headerLabel = new Label(nCol, 0, "NodeID", headerFormat);
                sheet.setColumnView(nCol, colSize);
                sheet.addCell(headerLabel);
                ++nCol;
                
                headerLabel = new Label(nCol, 0, "TotalTime [ms]", headerFormat);
                sheet.setColumnView(nCol, colSize);
                sheet.addCell(headerLabel);
                ++nCol;
                
                headerLabel = new Label(nCol, 0, "AverageTime [ms]", headerFormat);
                sheet.setColumnView(nCol, colSize);
                sheet.addCell(headerLabel);
                ++nCol;
                
                headerLabel = new Label(nCol, 0, "Num Sent", headerFormat);
                sheet.setColumnView(nCol, colSize);
                sheet.addCell(headerLabel);
                ++nCol;
                
                headerLabel = new Label(nCol, 0, "Num Resent", headerFormat);
                sheet.setColumnView(nCol, colSize);
                sheet.addCell(headerLabel);
                ++nCol;
                
                headerLabel = new Label(nCol, 0, "Num Received", headerFormat);
                sheet.setColumnView(nCol, colSize);
                sheet.addCell(headerLabel);
                ++nCol;
                
                headerLabel = new Label(nCol, 0, "Num Lost", headerFormat);
                sheet.setColumnView(nCol, colSize);
                sheet.addCell(headerLabel);
                ++nCol;
                
                WritableCellFormat cellFormat = new WritableCellFormat();
                cellFormat.setWrap(true);

                for(int i = 0; i < map.size(); ++i) {
                    Label nodeLbl = new Label(0, i+2, Integer.toString(map.get(i).nodeID), cellFormat);
                    Label totLbl = new Label(1, i+2, Double.toString(map.get(i).totTime), cellFormat);
                    Label avgLbl = new Label(2, i+2, Double.toString(map.get(i).avgTime), cellFormat);
                    Label sentLbl = new Label(3, i+2, Integer.toString(map.get(i).numSent), cellFormat);
                    Label resentLbl = new Label(4, i+2, Integer.toString(map.get(i).numResent), cellFormat);
                    Label rcvLbl = new Label(5, i+2, Integer.toString(map.get(i).numReceived), cellFormat);
                    Label lostLbl = new Label(6, i+2, Integer.toString(map.get(i).numLost), cellFormat);    
                    
                    sheet.addCell(nodeLbl);
                    sheet.addCell(totLbl);
                    sheet.addCell(avgLbl);
                    sheet.addCell(sentLbl);
                    sheet.addCell(resentLbl);
                    sheet.addCell(rcvLbl);
                    sheet.addCell(lostLbl);
                }

                workbook.write();
                workbook.close();
            } catch (IOException | WriteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
	
}
