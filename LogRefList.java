package wurmlogs;

import java.util.ArrayList;

/**
 * @author Kevin Bechman
 *
 */
public class LogRefList {
	public ArrayList<LogRef> list;
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
	
	public void add(LogRef ref) {
		list.add(ref);
	}
	
	public LogRef get(int i) {
		return list.get(i);
	}
	
	public boolean contains(LogRef ref) {
		return list.contains(ref);
	}
	
	public int size() { return list.size(); }
	
	public String toString() {
		return "For date: "+date+ " : " +list.toString();
	}
}
