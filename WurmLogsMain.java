package wurmlogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.MaskFormatter;
import javax.swing.text.StyledDocument;

/**
 * @author Kevin Bechman
 * Application for use with text file logs from the MMORPG Wurm Online.
 * This program indexes the entire directory of log files, then allows the user to enter a given date.
 * The logs from that date are then displayed on separate tabs for the user to peruse.
 */
public class WurmLogsMain extends JFrame {
	private static final long serialVersionUID = -5452465131304495441L;
	private File mainDir; // The main directory containing the log files
	private File[] logFiles; // File objects pointing to the log files
	private File[] currDateFiles; // File objects pointing to log files with text falling under current search
									// date
	private String[] currDateLogs; // Strings containing logs to be displayed falling under the current search date
	private Logger logger;
	private int numFiles; // Total number of files (logs, .txt, or otherwise) in the main directory
	private LogTracker logTracker;

	private JPanel topPanel;
	private JTabbedPane tabbedPane;
	private JScrollPane[] scrollTabs;
	private JTextPane[] textTabs;
	private JButton refreshButton, button2;
	private GridBagConstraints gbc;

	// Date variables
	private JFormattedTextField dateField;
	private SimpleDateFormat dateFormat;
	private String searchDateStr;

	/**
	 * Sets up the UI for the application
	 * @throws Exception
	 */
	public WurmLogsMain() throws Exception {
		// Create Logger to display command-prompt information for errors
		logger = Logger.getLogger("WurmLogs.Logger");

		setup();

		/** UI Setup **/

		Container cp = getContentPane();
		cp.setLayout(new GridBagLayout());

		// Setting up default date selection (today's date) and formatter
		dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date today = new Date();
		searchDateStr = dateFormat.format(today); // default date to be searched is today's date

		// Setting up string input format and text field
		MaskFormatter mask = new MaskFormatter("####-##-##");
		mask.setAllowsInvalid(false);

		dateField = new JFormattedTextField(mask);
		dateField.setValue(new String());
		dateField.setText(searchDateStr);
		dateField.setColumns(10);
		dateField.setFont(new Font("SANS_SERIF", Font.PLAIN, 16));

		// Buttons
		refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					refreshDate();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		});

		topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		gbc = new GridBagConstraints();
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		cp.add(topPanel, gbc);

		topPanel.add(dateField);
		topPanel.add(refreshButton);

		// Tabbed Pane setup
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
		// tabbedPane.setPreferredSize(new Dimension(800, 600));

		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 2.0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		cp.add(tabbedPane, gbc);

		// Additional window/frame setup
		// setBounds(100, 100, 1600, 900);
		setPreferredSize(new Dimension(800, 450));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		pack();
	}

	/**
	 * Given a file name, takes away the date, file name ending, and any additional
	 * unneeded characters. e.g. Given "Village.2012-12.txt", returns "Village"
	 * @param fileName name of file to convert
	 * @return converted String
	 */
	public String titleToTab(String fileName) {
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

	/**
	 * Reloads the view with the current date given by the user in the "searchDate" field
	 * @throws IOException
	 */
	public void refreshDate() throws IOException {
		if (!setDate((String) dateField.getValue()))
			return;
		System.out.println("Current date: " + searchDateStr);
		scanFilesForDate();
		refreshView();
	}

	/**
	 * Empties and then re-adds tabs to the tabbedPane which are included in the
	 * currDateLogs array of log text
	 */
	public void refreshView() {
		tabbedPane.removeAll();

		// Add tabs corresponding to the log files
		scrollTabs = new JScrollPane[currDateFiles.length];
		textTabs = new JTextPane[currDateFiles.length];

		// Add tabs with text based on the currDateFiles and currDateLogs arrays
		for (int i = 0; i < currDateFiles.length; i++) {
			textTabs[i] = new JTextPane();
			scrollTabs[i] = new JScrollPane(textTabs[i]);
			scrollTabs[i].setMaximumSize(new Dimension(100, 100));
			textTabs[i].setMaximumSize(new Dimension(100, 100));

			textTabs[i].setEditable(false);
			textTabs[i].setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

			// Set text from logfile as a string on the textpane
			StyledDocument doc = textTabs[i].getStyledDocument();
			try {
				doc.insertString(0, currDateLogs[i], null);
			} catch (Exception e) {
				System.out.println(e);
			}
			textTabs[i].setDocument(doc);

			// Convert file name to tab name:
			String tabName = titleToTab(currDateFiles[i].getName());
			tabbedPane.addTab(tabName, scrollTabs[i]);
			scrollToTop(scrollTabs[i]);
		}
	}

	/**
	 * Iterates through the LogRefList for the date given by the user, and stores the results in global
	 * arrays to be displayed in the UI
	 * @throws IOException
	 */
	public void scanFilesForDate() throws IOException {
		double startTime = System.nanoTime();

		RandomAccessFile fileIn;
		File file;
		String s, logStr;
		WTime loginDate;
		int k = 0; // counter for files to be kept

		String[] logs = new String[logFiles.length];
		File[] files = new File[logFiles.length];

		System.out.println("Search for: " + searchDateStr + "...");
		
		WTime userDate = new WTime(Integer.parseInt(searchDateStr.substring(0, 4)),
				Integer.parseInt(searchDateStr.substring(5, 7)), Integer.parseInt(searchDateStr.substring(8, 10)));
		LogRefList refList = logTracker.get(userDate);
		if (refList == null) {
			System.out.println("No logs of this date found.");
		}

		// For each LogRef, scan through the file starting from the given file pointer
		// location
		for (int i = 0; refList != null && i < refList.size(); i++) {
			logStr = ""; // Reset logStr to build next log's text
			LogRef ref = refList.get(i);
			file = logFiles[ref.fileIndex];
			fileIn = new RandomAccessFile(file, "r");
			fileIn.seek(ref.loc); // Find position in file where date first occurs
			System.out.println("Reading [" + file.getName() + "]...");

			// Start searching from starting point, recording everything until a Logging
			// date diff than current one is found
			String line = fileIn.readLine();
			while (line != null) {
				if (line.length() >= 26 && line.substring(0, 7).equals("Logging")) {
					// Line contains Logging date, so store it to a date
					loginDate = new WTime(Integer.parseInt(line.substring(16, 20)),
							Integer.parseInt(line.substring(21, 23)), Integer.parseInt(line.substring(24, 26)));
					if (loginDate.equals(userDate)) {
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
			logs[k] = logStr;
			files[k] = logFiles[ref.fileIndex];
			k++;
		}
		// Update the global current date log file and string arrays to reflect the new
		// date
		currDateLogs = Arrays.copyOf(logs, k);
		currDateFiles = Arrays.copyOf(files, k);
		System.out
				.println("Scanning files for date took " + (System.nanoTime() - startTime) / 1000000000 + " seconds.");
	}

	/**
	 * Takes a directory path in String format and sets the mainDir File object to
	 * point to that directory
	 * @param userDir the directory the user has denoted as containing the log files
	 */
	public void setMainDirectory(String userDir) {
		// Convert userDir string to a File object
		File file;
		try {
			file = new File(userDir);
		} catch (NullPointerException e) {
			logger.warning("No directory pathname provided.");
			e.printStackTrace();
			return;
		}
		// Check that the file directory exists
		if (!file.exists() || !file.isDirectory()) {
			logger.warning("Provided directory does not exist, or is not a directory.");
			return;
		}
		mainDir = file;
	}

	/**
	 * Sets the current date to be displayed
	 * @param dateStr the user-received String in format "YYYY-MM"
	 * @return true if successful, false if date format is incorrect
	 */
	public boolean setDate(String dateStr) {
		if (dateStr.length() == 10) {
			searchDateStr = dateStr;
			return true;
		} else {
			new JOptionPane().showMessageDialog(this, "Please fill in a proper date.");
			return false;
		}
	}

	/**
	 * Looks through main directory to find all applicable log files, and re-creates
	 * a list of File objects representing the logs
	 */
	public void getFiles() {
		// Create temporary list of File objects
		File[] fileList = mainDir.listFiles(); // All files in the directory
		numFiles = fileList.length;
		File[] keepers = new File[numFiles]; // Only files determined to be log files

		// Search through list of files, discerning which are files containing logs and
		// which are not, keeping only the former
		RandomAccessFile in;
		String str, fName;
		int k = 0;
		for (int i = 0; i < fileList.length; i++) {
			fName = fileList[i].getName();
			if (fName.substring(fName.length() - 3, fName.length()).equals("txt")) {
				try {
					in = new RandomAccessFile(fileList[i], "r");
					str = in.readLine();
				} catch (Exception e) {
					str = null;
				}
				if (str != null) {
					if (str.substring(0, 7).equals("Logging")) {
						keepers[k] = fileList[i]; // If the file starts with "Logging", it is a log file and will be
													// retained
						k++;
					}
				}
			}
		}

		// Once finished, re-assign the updated file list to the global logFiles File
		// array
		logFiles = Arrays.copyOf(keepers, k);
	}

	/**
	 * Performs various pre-processing and setup operations.
	 * @throws IOException
	 */
	public void setup() throws IOException {
		// Get the main log directory:
		mainDir = new File("C:\\Users\\admin\\Documents\\coding\\projects\\current\\wurmlogs\\testlogs2");
		if (!mainDir.isDirectory()) {
			System.out.println("Provided path does not point to a directory.");
			System.exit(1);
		}
		numFiles = mainDir.listFiles().length;

		// Load files into array of File objects
		getFiles();
		if (logFiles == null) {
			System.out.println("No files found.");
			System.exit(1);
		}

		// Setup the LogRefList Hashmap to make finding relevant dates easier
		logTracker = new LogTracker();
		try {
			logTracker.processDates(logFiles);
		} catch (Exception e) {
			System.out.println("ERROR: Exception caught during indexing.");
			e.printStackTrace();
		}
		
		logTracker.orderDates();
		System.out.println(logTracker.getStats());
	}

	/**
	 * Scrolls a given JScrollPane view to the top of its window
	 * @param scrollPane the JScrollPane to be scrolled up
	 */
	public void scrollToTop(JScrollPane scrollPane) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				scrollPane.getVerticalScrollBar().setValue(0);
			}
		});
	}

	/**
	 * Houses the main Thread execution of the Swing UI for this application
	 * @param args unused String array
	 */
	public static void main(String args[]) throws Exception {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					new WurmLogsMain();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

}
