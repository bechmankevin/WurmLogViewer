package wurmlogs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import javax.swing.SwingWorker;

/**
 * @author Kevin Bechman
 * 
 * Class for controlling all background aspects of the log viewer, such as indexing and acquiring the files.
 */
public class Engine {
	/**
	 * Background thread in charge of indexing the files once a main directory is specified
	 */
	private class Indexer extends SwingWorker<Void, String> {
		@Override
		protected Void doInBackground() {
			try {
				logTracker.processDates(logFiles);
			} catch (Exception e) {
				System.out.println("Error indexing log files.");
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void done() {
			System.out.println("Indexing files complete!");
			doneIndexing();
		}
	}
	
	private File mainDir;  // The main directory containing the log files
	private File[] logFiles;  // File objects pointing to the log files
	private WTime searchDate;  // Date currently set to be displayed
	
	private WurmLogsMain wlm;  // Reference to GUI class
	private LogTracker logTracker;
	private Indexer indexer;  // SwingWorker used for indexing the log files
	
	private boolean isReadyForIndexing;  // If a main directory has been loaded, this will switch to true
	private boolean isDoneIndexing;  // Set to true when the Indexer finishes its task 
	
	public Engine(WurmLogsMain w) {
		wlm = w;
		logTracker = new LogTracker();
		isReadyForIndexing = false;
		isDoneIndexing = false;
		indexer = new Indexer();
	}
	
	/**
	 * Assigns the mainDir File object based on the given pathname
	 * @param pathname  String pathname of what is to be made the main directory
	 */
	public void setMainDir(String pathname) {
		File file = new File(pathname);
		if (!file.exists() || !file.isDirectory()) {
			System.out.println("Directory at specified pathname does not exist.");
			return;
		}
		mainDir = file;
		isReadyForIndexing = true;  // May not need this if all the methods occur in this order
		filterLogFiles();
	}
	
	/**
	 * Sets a new WTime date object, given the search date string
	 * @param searchDateStr  string in format YYYY-MM-DD, representing the date to search for
	 */
	public void setSearchDate(String searchDateStr) {
		if (searchDateStr.length() != 10) {
			System.out.println("Please fill in a proper date with format YYYY-MM-DD");
			return;
		}
		searchDate = new WTime(Integer.parseInt(searchDateStr.substring(0, 4)),
			Integer.parseInt(searchDateStr.substring(5, 7)), Integer.parseInt(searchDateStr.substring(8, 10)));
	}
	
	/**
	 * For each file, test that it is a .txt file, and then that it begins with "Logging"
	 * If both conditions are met, it is a log file and is retained.
	 * Otherwise, it is ignored.
	 * @param mainDirectory  the file object representing the main logs directory
	 */
	private void filterLogFiles() {
		File[] files = mainDir.listFiles();
		File[] keepers = new File[files.length];
		int k = 0;	// Counter for index of keeper array
		for (File f : files) {
			String s = f.getName();
			// If the file is a text file, continue
			if (s.substring(s.length() - 3, s.length()).equals("txt")) {
				try {
					RandomAccessFile in = new RandomAccessFile(f, "r");
					s = in.readLine();	// Read the first line
					in.close();
				} catch (Exception e) {
					s = null;
				}
				// If the first line starts with "Logging", it is a log file and will be retained
				if (s != null && s.length() > 7 && s.substring(0, 7).equals("Logging")) {
					keepers[k] = f;
					k++;
				}
			}
		}
		// Re-assign the logFiles array with new length and references only to the actual log files
		logFiles = Arrays.copyOf(keepers, k);
	}
	
	/**
	 * Iterates through the LogRefList for the date given by the user, and stores the results in global
	 * arrays to be displayed in the UI
	 * @return  LogPackage containing the log text and titles for the tabs
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public LogPackage getLogsForDate() throws IOException, FileNotFoundException {
		double startTime = System.nanoTime();
		System.out.println("Search for: " + searchDate + "...");
		int k = 0; // counter for files to be kept
		
		// New arrays 
		String[] logTexts = new String[logFiles.length];
		String[] logTitles = new String[logFiles.length];
		
		// Acquire the LRL associated with the searchDate from the hashmap
		LogRefList refList = logTracker.get(searchDate);
		if (refList == null) {
			System.out.println("No logs of this date found.");
		}
		
		// For each LogRef, scan through the file starting from the given file pointer
		// location
		for (int i = 0; refList != null && i < refList.size(); i++) {
			String logStr = ""; // Reset logStr to build next log's text
			LogRef ref = refList.get(i);
			File file = logFiles[ref.fileIndex];
			RandomAccessFile fileIn = new RandomAccessFile(file, "r");
			fileIn.seek(ref.loc); // Find position in file where date first occurs
			System.out.println("Reading [" + file.getName() + "]...");

			// Start searching from starting point, recording everything until a Logging
			// date diff than current one is found
			String line = fileIn.readLine();
			while (line != null) {
				if (line.length() >= 26 && line.substring(0, 7).equals("Logging")) {
					// Line contains Logging date, so store it to a date
					WTime loginDate = new WTime(Integer.parseInt(line.substring(16, 20)),
							Integer.parseInt(line.substring(21, 23)), Integer.parseInt(line.substring(24, 26)));
					if (loginDate.equals(searchDate)) {
						// Logging was within same date, so continue recording
						logStr += line;
						logStr += "\n";
					} else
						break; // Otherwise, exit the loop and stop recording this file; all relevant logs from
								// this file are recorded
				} else {
					// If not a Logging date, record the line
					logStr += line;
					logStr += "\n";
				}

				line = fileIn.readLine(); // Read in next line
			}
			logTexts[k] = logStr;
			logTitles[k] = titleToTab(logFiles[ref.fileIndex].getName());
			k++;
			fileIn.close();
		}
		// Update the arrays to reflect the correct length needed
		logTexts = Arrays.copyOf(logTexts, k);
		logTitles = Arrays.copyOf(logTitles, k);
		
		System.out.println("Scanning files for date took " + 
						   (System.nanoTime() - startTime) / 1000000000 + " seconds.");
		
		// Create a LogPackage and return it
		return new LogPackage(logTexts, logTitles);
	}
	
	/**
	 * Starts Engine processes (assuming the prerequisites to do so are fulfilled)
	 */
	public void startIndexing() {
		if (!isReadyForIndexing) {
			System.out.println("Cannot index files, no main directory selected.");
			return;
		}
		isDoneIndexing = false;
		indexer.execute();  // Start indexing log files
		System.out.println("File indexing has commenced.");
	}
	
	public void doneIndexing() {
		wlm.readyForViewing();
	}
	
	/**
	 * Retrieves LogRefList from the LogTracker's hashmap based on the provided WTime object
	 * @param date  the date whose LRL will be returned
	 * @return  the LRL associated with the given date
	 */
	public LogRefList getLRL(WTime date) {
		return logTracker.get(date);
	}
	
	/**
	 * Given a file name, takes away the date, file name ending, and any additional
	 * unneeded characters. e.g. Given "Village.2012-12.txt", returns "Village"
	 * @param fileName name of file to convert
	 * @return converted String
	 */
	private String titleToTab(String fileName) {
		String retStr = fileName;
		// Get rid of leading underscore, if present
		if (retStr.charAt(0) == '_') {
			retStr = retStr.substring(1, retStr.length());
		}
		// Delete leading "PM__" for PM tabs
		if (retStr.substring(0, 4).equals("PM__")) {
			retStr = retStr.substring(4, retStr.length());
		}
		// Remove last 12 characters from the file name (1 period, 7-char date, another
		// period, and 4 char ".txt")
		retStr = retStr.substring(0, retStr.length() - 12);
		return retStr;
	}
	
	public boolean isDoneIndexing() { return isDoneIndexing; }
	public boolean isReadyForIndexing() { return isReadyForIndexing; }
}
