package wurmlogs;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Kevin Bechman
 *
 */
public class LogTracker implements Serializable {
	private static final long serialVersionUID = 4575686813167177630L;
	private HashMap<WTime, LogRefList> list;	// Hashmap with lists of LogRefLists as the value, WTime as key
	private int numLogRefLists;
	private int numLogRefs;
	private ArrayList<WTime> allDates;	// All dates detected while indexing
	
	public LogTracker() {
		list = new HashMap<>();
		allDates = new ArrayList<WTime>();
		numLogRefLists = numLogRefs = 0;
	}
	
	/**
	 * Gets the LogRefList assigned to the given date and returns a copy (with no references attached)
	 * @param date the WTime class to be hashed
	 * @return a copy of the LogRefList stored with this WTime object
	 */
	public LogRefList get(WTime date) {
		WTime dateCopy = new WTime(date.year, date.month, date.day);
		LogRefList lrl = list.get(date);
		if (lrl == null)
			return null;
		LogRefList lrlCopy = new LogRefList(dateCopy);
		for (int i=0; i<lrl.size(); i++) {
			lrlCopy.add(lrl.get(i));
		}
		return lrlCopy;
	}
	
	/**
	 * Sets up the hashmap containing the lists of LogRef's
	 * This allows a WTime date to be used as a key to return the list of log indexes and file locations for relevant logs
	 * @param logFiles 
	 * @throws Exception
	 */
	public void processDates(File[] logFiles) throws Exception {
		System.out.println("Indexing log files...");
		double startTime = System.nanoTime();
		
		// Loop through each file, opening a file reader
		for (int i=0; i<logFiles.length; i++) {
			//*** TEST TO SEE HOW LONG IT TAKES WHEN OMITTING EVENT/COMBAT/SKILLS ***//
			/*String fName = logFiles[i].getName();
			if (fName.substring(0, 4).equals("_Ski") || fName.substring(0, 4).equals("_Eve") ||
					fName.substring(0, 4).equals("_Com")) {}*/
			RandomAccessFile fileIn = new RandomAccessFile(logFiles[i], "r");
			long pointerPos = 0;	// Beginning of file (file should always start with "Logging")
			String line = fileIn.readLine();
			
			// For every line in the file, check if it is a Logging statement.
			// If so, record the login date, find its spot in the hashmap, and add the LRL if it does not already exist.
			// If the LRL does exist, add it (with the new LogRef)
			while(line != null) {
				if (line.length() >= 26 && line.substring(0, 7).equals("Logging")) {
					// Create object representing this Logging date
					WTime loginDate = new WTime(Integer.parseInt(line.substring(16, 20)), 
											    Integer.parseInt(line.substring(21, 23)), 
												Integer.parseInt(line.substring(24, 26)));
					recordDate(loginDate);  // Record the date in convenience list of dates
					LogRefList lrl = new LogRefList(loginDate);
					LogRef ref = new LogRef(i, pointerPos);
					LogRefList newLRL = list.get(loginDate);  // Index of LRL representing this given Logging date
					if (newLRL != null) {	// If the LRL exists in the HashMap
						lrl = newLRL;	// Assign to 'lrl' the LRL in the hashmap represented by this date
						// If the LRL does not yet contain a LogRef of this specific file index, add the LogRef
						if (!lrl.contains(ref)) {
							//System.out.println("lrl "+lrl.date+" "+lrl.size());
							lrl.add(ref);
							numLogRefs++;
						}
					}
					// If Date is not in the HashMap, begin the new LRL with the log reference and add the LRL to the hashmap
					else {	
						lrl.add(ref);
						list.put(loginDate, lrl);
						numLogRefs++;
						numLogRefLists++;
					}
				}
				pointerPos = fileIn.getFilePointer();
				line = fileIn.readLine();
			}
			fileIn.close();
		}
		System.out.println("Indexing files took " + (System.nanoTime() - startTime)/1000000000 + " seconds.");
	}
	
	/**
	* Adds new date to list of all dates if it is not already there.
	* If the date is already in the hashmap, it has already been added to the list
	*/
	public void recordDate(WTime date) {
		if (list.get(date) == null) {
			allDates.add(date);
		}
	}
	
	/**
	 * Returns a list of all dates recorded from the logs processed
	 * @return
	 */
	public String listDates() {
		String ret = "";
		for (WTime t : allDates) {
			ret += t.toString() + "\n";
		}
		return ret;
	}
	
	/**
	* Orders the dates chronologically according to WTime's compareTo() method
	*/
	public void orderDates() {
		// Convert ArrayList to array of WTime objects
		WTime[] arr = new WTime[allDates.size()];
		for (int i=0; i<allDates.size(); i++) {
			arr[i] = allDates.get(i);
		}
		// Sort the array
		Arrays.sort(arr);
		// Convert the array back into an ArrayList
		ArrayList<WTime> newList = new ArrayList<>();
		for (int i=0; i<arr.length; i++) {
			newList.add(arr[i]);
		}
		// Reassign ordered list to old object
		allDates = newList;
	}
	
	/**
	 * Returns String with hashmap book-keeping
	 * @return the String object
	 */
	public String getStats() {
		return "LogRefLists: " + numLogRefLists + "\n" +
			   "LogRefs: " + numLogRefs;
	}
	
	@Override
	public String toString() {
		return list.toString();
	}
}
