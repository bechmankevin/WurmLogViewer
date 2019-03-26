package wurmlogs;

import java.util.ArrayList;

/**
 * @author Kevin Bechman
 * Contains a list of LogRef objects (stored in an ArrayList) that were found for the given date
 */
public class LogRefList {
	private ArrayList<LogRef> list;
	public final WTime date;
	public LogRefList(WTime d) {
		list = new ArrayList<>();
		date = d;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!this.getClass().equals(o.getClass()))
			return false;
		LogRefList lrl = (LogRefList) o;
		if (lrl.date.equals(this.date))	// Equality check
			return true;
		return false;
	}
	
	// ***** Methods to access/change the inner ArrayList ***** //
	public void add(LogRef ref) {
		list.add(ref);
	}
	
	public LogRef get(int i) {
		return new LogRef(list.get(i).fileIndex, list.get(i).loc);
	}
	
	public boolean contains(LogRef ref) {
		return list.contains(ref);
	}
	
	public int size() { return list.size(); }
	
	@Override
	public String toString() {
		return "For date: "+date+ " : " +list.toString();
	}
}
