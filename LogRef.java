package wurmlogs;

/**
 * @author Kevin Bechman
 * Class used to define references for the starting location of a given date's logs in a particular file
 */
public class LogRef {
	public final int fileIndex;  // Index of file in main file list
	public final long loc;   // Location of where the date starts in this file
	
	public LogRef(int i, long l) {
		fileIndex = i;
		loc = l;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!this.getClass().equals(o.getClass()))
			return false;
		LogRef lr = (LogRef) o;
		if (this.fileIndex == lr.fileIndex)	// LogRef equality ONLY considers the FileIndex - loc is not significant
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		return "[" + fileIndex + ", " + loc + "]";
	}
}
