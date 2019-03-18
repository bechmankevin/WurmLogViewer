package wurmlogs;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Kevin Bechman
 *
 */
public class LogTracker implements Serializable {
	private static final long serialVersionUID = 4575686813167177630L;
	private ArrayList<ArrayList<LogRefList>> list;  // Hashmap with lists of LogRefLists as the value, WTime as key
	private final int M;
	private int numLogRefLists;
	private int numLogRefs;
	private ArrayList<WTime> allDates;	// All dates detected while indexing
	
	public LogTracker() {
		this(1009);
	}
	public LogTracker(int num) {
		M = num;
		list = new ArrayList<ArrayList<LogRefList>>(M);
		for (int i=0; i<M; i++) {
			list.add(new ArrayList<LogRefList>());
		}
		//System.out.println("Hashmap of size " + M + " created.");
		allDates = new ArrayList<WTime>();
		numLogRefLists = numLogRefs = 0;
	}
	
	
	
	public int hash(WTime date) {
		return date.hashCode() % M;
	}
	
	public void put(WTime date, LogRefList lrl) {
		int index = hash(date);
		// Currently, a put() action should never deal with a conflict of the same LRL, only the same index/date (which is what the arraylist is for)
		if (list.get(index).contains(lrl)) {
			System.out.println("ERROR: Hashtable already contains something, this shouldn't happen.");
			System.exit(1);
		}
		else {
			list.get(index).add(lrl);
		}
	}
	
	public LogRefList get(WTime date) {
		int index = hash(date);
		// Search this index's ArrayList for the right LRL
		for(LogRefList lrl : list.get(index)) {
			if (lrl.date.equals(date)) {
				return lrl;
			}
		}
		return null;	// If LRL is not found in the ArrayList
	}
	
	// Sets up the Hashmap containing the lists of LogRef's
	// This allows a date to be used as a key to return the list of log indexes and file locations for relevant logs
	public void processDates(File[] logFiles) throws Exception {
		// Scan through each log file, recording the pointer location
		System.out.println("Indexing log files...");
		double startTime = System.nanoTime();
		
		for (int i=0; i<logFiles.length; i++) {
			//System.out.println(logFiles[i].getName());
			RandomAccessFile fileIn = new RandomAccessFile(logFiles[i], "r");
			long pointerPos = 0;	// Beginning of file (file should always start with "Logging")
			String line = fileIn.readLine();
			
			// For every line in the file, check if it is a Logging statement.
			// If so, record the login date, find its spot in the hashmap, and add the LRL if it does not already exist.
			// If the LRL does exist, add it (with the new LogRef)
			while(line != null) {
				//System.out.println(line);  //d
				//System.out.println("pos: " + pointerPos);  //d
				if (line.length() >= 26 && line.substring(0, 7).equals("Logging")) {
					// Logging date message exists, so check to see if it is in hashmap
					WTime loginDate = new WTime(Integer.parseInt(line.substring(16, 20)), 
											    Integer.parseInt(line.substring(21, 23)), 
												Integer.parseInt(line.substring(24, 26)));
					recordDate(loginDate);
					int hashIndex = hash(loginDate);
					LogRefList lrl = new LogRefList(loginDate);
					LogRef ref = new LogRef(i, pointerPos);
					int listIndex = list.get(hashIndex).indexOf(lrl);  // Index of LRL representing this given Logging date
					if (listIndex != -1) {	// If the LRL exists in the ArrayList
						lrl = list.get(hashIndex).get(listIndex);	// Assign to 'lrl' the LRL in the hashmap represented by this date
						// If the LRL does not yet contain a LogRef of this specific file index, add the LogRef
						if (!lrl.contains(ref)) {
							lrl.add(ref);
							numLogRefs++;
						}
					}
					else {	// If Date is not in the HashMap, begin the new LRL with the log reference and add the LRL to the hashmap
						lrl.add(ref);
						list.get(hashIndex).add(lrl);
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
	*/
	public void recordDate(WTime date) {
		if (!allDates.contains(date)) {
			allDates.add(date);
		}
	}
	
	
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
		// Reassign
		allDates = newList;
	}
	
	@Override
	public String toString() {
		return list.toString();
	}
	
	public String getStats() {
		return "LogRefLists: " + numLogRefLists + "\n" +
			   "LogRefs: " + numLogRefs;
	}
}
