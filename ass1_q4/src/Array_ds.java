import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Array_ds {
	String array_name, type;
	int nDims;
	List<Loop> loops = new ArrayList<Loop>();
	HashMap<String, Long> cmfloops  = new HashMap<>();
	List<String> access_seq = new ArrayList<String>();
	List<String> ub_seq = new ArrayList<String>();
	long misses;
}