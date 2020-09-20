import java.util.*;

class Data {
  public void Data() {
      System.out.println("Data here!\n");
  }
  int cachePower, blockPower;
  String cacheType;
  HashMap<String, VariableData> variables_map  = new HashMap<>(); 
  List<Loop> loops = new ArrayList<Loop>();
  HashMap<String, Array_ds> arrays_ds  = new HashMap<>();
  long misses;
}