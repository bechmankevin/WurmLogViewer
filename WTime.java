package wurmlogs;

/**
 * @author Kevin Bechman
 * Represents a given date in the log files
 */
public class WTime implements Comparable<WTime> {
	private int year, month, day;
	public WTime (int ye, int mo, int da) {
		year = ye;
		month = mo;
		day = da;
	}
	
	@Override
	public int hashCode() {
		int h = day * month * year;
		return h;
	}
	
	@Override
	public String toString() {
		return ""+year+"-"+month+"-"+day+"";
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;
		if (!this.getClass().equals(o.getClass()))
			return false;
		WTime wt = (WTime) o;
		if (this.compareTo(wt) == 0)
			return true;
		return false;
	}
	
	@Override
	public int compareTo(WTime wt) {
		if (wt == null)
			return 1;
		if (this.year > wt.year)
			return 1;
		else if (this.year < wt.year)
			return -1;
		else if (this.month > wt.month)
			return 1;
		else if (this.month < wt.month)
			return -1;
		else if (this.day > wt.day)
			return 1;
		else if (this.day < wt.day)
			return -1;
		else return 0;
	}
	
	public int getDay() { return day; }
	public int getMonth() { return month; }
	public int getYear() { return year; }
}
