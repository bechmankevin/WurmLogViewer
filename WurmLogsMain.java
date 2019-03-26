package wurmlogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
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
	//private File[] logFiles; // File objects pointing to the log files
	//private File[] currDateFiles; // File objects pointing to log files with text falling under current search
									// date
	//private String[] currDateLogs; // Strings containing logs to be displayed falling under the current search date
	//private Logger logger;

	private JPanel topPanel;
	private JTabbedPane tabbedPane;
	private JScrollPane[] scrollTabs;
	private JTextPane[] textTabs;
	private JButton refreshButton;
	private GridBagConstraints gbc;

	// Date variables
	private JFormattedTextField dateField;
	private SimpleDateFormat dateFormat;
	private String searchDateStr;
	
	// Directory pathname
	private JTextField dirField;
	private JButton dirButton;
	
	private Engine engine;

	/**
	 * Sets up the UI for the application
	 * @throws Exception
	 */
	public WurmLogsMain() throws Exception {
		// Create Logger to display command-prompt information for errors
		//logger = Logger.getLogger("WurmLogs.Logger");
		
		engine = new Engine(this);
		
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
		
		// Refresh
		refreshButton = new JButton("Refresh");
		refreshButton.setEnabled(false);
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
		
		// ***** Directory Path Components *****
		dirField = new JTextField();
		dirField.setText("No directory pathname selected.");
		dirField.setFont(new Font("SANS_SERIF", Font.PLAIN, 16));
		
		
		dirButton = new JButton("Search this directory");
		dirButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				indexDirectory();
			}
		});
		
		topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		gbc = new GridBagConstraints();
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		cp.add(topPanel, gbc);
		
		// Add top panel components
		topPanel.add(dateField);
		topPanel.add(refreshButton);
		topPanel.add(dirField);
		topPanel.add(dirButton);

		// Tabbed Pane setup
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 2.0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		cp.add(tabbedPane, gbc);

		// Additional window/frame setup
		setPreferredSize(new Dimension(800, 450));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		pack();
	}
	
	public void indexDirectory() {
		refreshButton.setEnabled(false);
		dirButton.setEnabled(false);
		engine.setMainDir(dirField.getText());
		engine.startIndexing();
	}
	
	public void readyForViewing() {
		refreshButton.setEnabled(true);
		dirButton.setEnabled(true);
		System.out.println("Ready for viewing!");
	}

	/**
	 * Reloads the view with the current date given by the user in the "searchDate" field
	 * @throws IOException
	 */
	public void refreshDate() throws IOException {
		engine.setSearchDate((String) dateField.getValue());
		System.out.println("Current date: " + searchDateStr);
		LogPackage p = engine.getLogsForDate();
		refreshTabs(p);
	}

	/**
	 * Empties and then re-adds tabs to the tabbedPane which are included in the
	 * currDateLogs array of log text
	 */
	public void refreshTabs(LogPackage p) {
		tabbedPane.removeAll();

		// Add tabs corresponding to the log files
		scrollTabs = new JScrollPane[p.logTexts.length];
		textTabs = new JTextPane[p.logTexts.length];

		// Add tabs with text based on the currDateFiles and currDateLogs arrays
		for (int i = 0; i < p.logTexts.length; i++) {
			textTabs[i] = new JTextPane();
			scrollTabs[i] = new JScrollPane(textTabs[i]);
			scrollTabs[i].setMaximumSize(new Dimension(100, 100));
			textTabs[i].setMaximumSize(new Dimension(100, 100));
			textTabs[i].setEditable(false);
			textTabs[i].setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

			// Set text from logfile as a string on the textpane
			StyledDocument doc = textTabs[i].getStyledDocument();
			try {
				doc.insertString(0, p.logTexts[i], null);
			} catch (Exception e) {
				System.out.println(e);
			}
			textTabs[i].setDocument(doc);

			// Convert file name to tab name:
			String tabName = p.logTitles[i];
			tabbedPane.addTab(tabName, scrollTabs[i]);
			scrollToTop(scrollTabs[i]);
		}
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
	 * @param args directory to be designated as the home directory
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
