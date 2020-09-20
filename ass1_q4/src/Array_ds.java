import java.util.ArrayList;
import java.util.List;

class Array_ds {
	String array_name, type;
	int nDims;
	List<Loop> loops = new ArrayList<Loop>();
	List<String> access_seq = new ArrayList<String>();
	long misses;
}