package wurmlogs;

/**
 * @author Kevin Bechman
 * Used to pass the log text to be displayed on the tabs from the Engine to the GUI class
 */
public class LogPackage {
	public final String[] logTexts;
	public final String[] logTitles;
	
	public LogPackage(String[] logTexts, String[] logTitles) {
		this.logTexts = logTexts;
		this.logTitles = logTitles;
	}
}
