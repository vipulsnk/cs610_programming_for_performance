import java.util.*;
import static java.util.stream.Collectors.*;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

// FIXME: You should limit your implementation to this class. You are free to add new auxilliary classes. You do not need to touch the LoopNext.g4 file.
class Analysis extends LoopNestBaseListener {
	Data data;
	List<HashMap<String, Long>> results;
    // Possible types
    enum Types {
        Byte, Short, Int, Long, Char, Float, Double, Boolean, String
    }

    // Type of variable declaration
    enum VariableType {
        Primitive, Array, Literal
    }

    // Types of caches supported
    enum CacheTypes {
        DirectMapped, SetAssociative, FullyAssociative,
    }

    // auxilliary data-structure for converting strings
    // to types, ignoring strings because string is not a
    // valid type for loop bounds
    final Map<String, Types> stringToType = Collections.unmodifiableMap(new HashMap<String, Types>() {
        private static final long serialVersionUID = 1L;

        {
            put("byte", Types.Byte);
            put("short", Types.Short);
            put("int", Types.Int);
            put("long", Types.Long);
            put("char", Types.Char);
            put("float", Types.Float);
            put("double", Types.Double);
            put("boolean", Types.Boolean);
        }
    });

    // auxilliary data-structure for mapping types to their byte-size
    // size x means the actual size is 2^x bytes, again ignoring strings
    final Map<Types, Integer> typeToSize = Collections.unmodifiableMap(new HashMap<Types, Integer>() {
        private static final long serialVersionUID = 1L;

        {
            put(Types.Byte, 0);
            put(Types.Short, 1);
            put(Types.Int, 2);
            put(Types.Long, 3);
            put(Types.Char, 1);
            put(Types.Float, 2);
            put(Types.Double, 3);
            put(Types.Boolean, 0);
        }
    });

    // Map of cache type string to value of CacheTypes
    final Map<String, CacheTypes> stringToCacheType = Collections.unmodifiableMap(new HashMap<String, CacheTypes>() {
        private static final long serialVersionUID = 1L;

        {
            put("FullyAssociative", CacheTypes.FullyAssociative);
            put("SetAssociative", CacheTypes.SetAssociative);
            put("DirectMapped", CacheTypes.DirectMapped);
        }
    });

    public Analysis() {
        System.out.println("I am a constructor");
        results = new ArrayList<HashMap<String, Long>>();
    }
    
	private void computeMisses() {
		System.out.println("computeMisses");
		long misses = 0;
		HashMap<String, Long> misses_map  = new HashMap<>();
		for (String array_name : data.arrays_ds.keySet()) {
			misses += computeMissesArray(data.arrays_ds.get(array_name));
			System.out.printf("misses in array: %s are %d%n", array_name, data.arrays_ds.get(array_name).misses);
		}
		for (String array_name : data.arrays_ds.keySet()) {
			System.out.printf("misses in array: %s are %d%n", array_name, data.arrays_ds.get(array_name).misses);
			for (String loop_iterator : data.arrays_ds.get(array_name).cmfloops.keySet()) {
				System.out.printf("iterator: %s: misses %d%n",loop_iterator, data.arrays_ds.get(array_name).cmfloops.get(loop_iterator));
			}
			misses_map.put(array_name, data.arrays_ds.get(array_name).misses);
		}
		results.add(misses_map);
		System.out.printf("total misses: %d%n", misses);
	}

    private long computeMissesArray(Array_ds array_ds) {
    	System.out.println("computeMissesArray");
    	System.out.printf("array name is: %s%n", array_ds.array_name);
    	List<String> loops_below = new ArrayList<String>();
    	long misses = 1;
    	long cmf;
    	System.out.println(array_ds.loops.size());
    	for(int i = array_ds.loops.size()-1; i>=0; i--) {
    		Loop loop = array_ds.loops.get(i);
    		System.out.println("iterator: " + loop.iterator);
    		String cmdr = findCmdr(loop.iterator, array_ds.access_seq, loops_below);
    		if(cmdr.equals(loop.iterator)) {
    			System.out.println("Commander is this loop: " + cmdr);
    			int levCmd = findLoc(cmdr, array_ds.access_seq);
    			cmf = computeCmfCore(levCmd, Long.parseLong(loop.stride), Long.parseLong(data.variables_map.get(loop.upperBound).value), array_ds);
    			misses*=cmf;
    		}else {
    			int levCmd = findLoc(cmdr, array_ds.access_seq);
    			if(check_misses(misses, levCmd, cmdr, array_ds)) {
    				System.out.println("Eviction happened");
    				cmf = (Long.parseLong(data.variables_map.get(loop.upperBound).value)/Long.parseLong(loop.stride));
    				misses*=cmf;
    			}else {
    				System.out.println("Eviction did not happen");
    				int levCmdLoop = findLoc(loop.iterator, array_ds.access_seq);
    				cmf = computeCmfCore(levCmdLoop, Long.parseLong(loop.stride), Long.parseLong(data.variables_map.get(loop.upperBound).value), array_ds);
    				misses*=cmf;
    			}
    		}
    		data.arrays_ds.get(array_ds.array_name).cmfloops.put(loop.iterator, cmf);
    		loops_below.add(loop.iterator);
    	}
    	data.arrays_ds.get(array_ds.array_name).misses = misses;
		return misses;
	}


private boolean check_misses(long misses, int levCmd, String cmdr, Array_ds array_ds) {
    // TODO Auto-generated method stub
    System.out.println("check_misses");
    long blockPower = Long.parseLong(data.variables_map.get("blockPower").value);
    long blockSize = (long) Math.pow(2, blockPower);
    long wordSize = (long) Math.pow(2, typeToSize.get(stringToType.get(array_ds.type)));
    blockSize = blockSize/wordSize;
    long cachePower = Long.parseLong(data.variables_map.get("cachePower").value);
    long cacheSize = (long) Math.pow(2, cachePower);
    cacheSize = cacheSize/wordSize;
    long rowSize = 0;
    long colSize = 0;
    long arraySize = 0;
    long k = 1;
    long nSets = 1;
    if(array_ds.nDims >= 2) {
        rowSize = Long.parseLong(data.variables_map.get(array_ds.ub_seq.get(array_ds.ub_seq.size()-1)).value);
        System.out.println("rowSize: ");
        System.out.println(rowSize);
    }
    if(array_ds.nDims >= 3) {
        colSize = Long.parseLong(data.variables_map.get(array_ds.ub_seq.get(array_ds.ub_seq.size()-2)).value);
        arraySize = colSize*rowSize;
        System.out.println("colSize: ");
        System.out.println(colSize);
        System.out.println("arraySize: ");
        System.out.println(arraySize);
    }
    System.out.printf("loc: %d, array_name: %s, blockPower: %d, blockSize: %d, wordSize: %d%n", levCmd, array_ds.array_name, blockPower, blockSize, wordSize);
    if(data.variables_map.get("cacheType").value.equals("\"FullyAssociative\"")) {
        System.out.println("FullyAssociative cache");
        nSets = (long) Math.pow(2, cachePower-blockPower); 
        return misses > nSets;
    }else {
    	System.out.println(data.variables_map.get("cacheType").value);
        if(data.variables_map.get("cacheType").value.equals("\"DirectMapped\"")) {
            k = 1;
        }else {
            k = Long.parseLong(data.variables_map.get("setSize").value);
        }
    }    
    nSets = (long) (Math.pow(2, cachePower-blockPower)/k);
    System.out.printf("k is: %d, nSets: %d%n", k, nSets);
    switch (levCmd) {
        case 1:
            return false;
        case 2:
            return rowSize > cacheSize;
        case 3:
            long nbr = rowSize/blockSize;
            return misses > (k*(nSets/nbr));
        case 4:
            long nba = arraySize/blockSize;
            return misses > (k*(nSets/nba));
        default:
            System.out.println("Error: Undefined loc: ");
            System.out.println(levCmd);
            break;
    }
   
    return false;
}



private long computeCmfCore(int levCmd, long stride, long ub, Array_ds array_ds) {
    // TODO Auto-generated method stub
	System.out.println("computeCmfCore");
    long blockPower = Long.parseLong(data.variables_map.get("blockPower").value);
    long blockSize = (long) Math.pow(2, blockPower);
    long wordSize = (long) Math.pow(2, typeToSize.get(stringToType.get(array_ds.type)));
    blockSize = blockSize/wordSize;
    long rowSize = 0;
    long colSize = 0;
    long arraySize = 0;
    if(array_ds.nDims >= 2) {
        rowSize = Long.parseLong(data.variables_map.get(array_ds.ub_seq.get(array_ds.ub_seq.size()-1)).value);
        System.out.println("rowSize: ");
        System.out.println(rowSize);
    }
    if(array_ds.nDims >= 3) {
        colSize = Long.parseLong(data.variables_map.get(array_ds.ub_seq.get(array_ds.ub_seq.size()-2)).value);
        arraySize = colSize*rowSize;
        System.out.println("colSize: ");
        System.out.println(colSize);
        System.out.println("arraySize: ");
        System.out.println(arraySize);
    }
    System.out.printf("loc: %d, stride: %d, ub: %d, array_name: %s, blockPower: %d, blockSize: %d, wordSize: %d%n", levCmd, stride, ub, array_ds.array_name, blockPower, blockSize, wordSize);
    
    switch(levCmd) {
    case 1:
    	System.out.println("case 1");
        return 1;
    case 2:
    	System.out.println("case 2");
        if(blockSize > stride){
        	System.out.println("blockSize > stride");
        	if(ub/blockSize < 1) 
        		return 1;
            return ub/blockSize;
        }else{
        	System.out.println("blockSize <= stride");
            return ub/stride;
        }
    case 3:
    	System.out.println("case 3");
        if(blockSize > rowSize){
        	System.out.println("blockSize > rowSize");
            long nRows = blockSize/rowSize;
            if(nRows > stride){
                if(ub/nRows < 1) 
            		return 1;
                return ub/nRows;
            }else{
                return ub/stride;
            }
        }else{
        	System.out.println("blockSize <= rowSize");
            return ub/stride;
        }
    case 4:
    	System.out.println("case 4");
        if(blockSize > arraySize){
        	System.out.println("blockSize > arraySize");
            long nArr = blockSize/arraySize;
            if(nArr > stride){
                if(ub/nArr < 1) 
            		return 1;
                return ub/nArr;
            }else{
                return ub/stride;
            }
        }else{
        	System.out.println("blockSize <= arraySize");
            return ub/stride;
        }
    default:
        System.out.println("Error: Undefined loc: ");
        System.out.println(levCmd);
    }
//		long arraySize = Long.parseLong(data.variables_map.get(ar/).value);
    return 0;
}

	private int findLoc(String cmdr, List<String> access_seq) {
		// TODO Auto-generated method stub
		System.out.println("findLoc");
		int loc = 1;
		for(int i=access_seq.size()-1; i >=0; i--) {
			loc +=1;
			if(cmdr.equals(access_seq.get(i))) {
				return loc;
			}
		}
		return 1;
	}

	private String findCmdr(String iterator, List<String> access_seq, List<String> loops_below) {
		// TODO Auto-generated method stub
		System.out.println("findCmdr");
		int max=0;
		String cmdr = null;
		for(int i=0; i<loops_below.size(); i++) {
			if(max < findLoc(loops_below.get(i), access_seq)) {
				max = findLoc(loops_below.get(i), access_seq);
				cmdr = loops_below.get(i);
			}
		}
		if(max < findLoc(iterator, access_seq)) {
			max = findLoc(iterator, access_seq);
			cmdr = iterator;
		}
		return cmdr;
	}

	// FIXME: Feel free to override additional methods from
    // LoopNestBaseListener.java based on your needs.
    // Method entry callback
    @Override
    public void enterMethodDeclaration(LoopNestParser.MethodDeclarationContext ctx) {
        System.out.println("enterMethodDeclaration");
        this.data = new Data();
        data.misses = 0;
    }
    @Override
    public void exitMethodDeclaration(LoopNestParser.MethodDeclarationContext ctx) {
        System.out.println("exitMethodDeclaration");
        computeMisses();
        
    }
    

	// End of testcase
    @Override
    public void exitMethodDeclarator(LoopNestParser.MethodDeclaratorContext ctx) {
        System.out.println("exitMethodDeclarator");
        
    }

    @Override
    public void exitTests(LoopNestParser.TestsContext ctx) {
    	System.out.println("exitTests");
        try {
            FileOutputStream fos = new FileOutputStream("Results.obj");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // FIXME: Serialize your data to a file
             oos.writeObject(results);
            oos.close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void exitLocalVariableDeclaration(LoopNestParser.LocalVariableDeclarationContext ctx) {	
    }
    
    @Override
    public void exitLocalVariableDeclarationStatement(LoopNestParser.LocalVariableDeclarationStatementContext ctx) {
    	System.out.println("exitLocalVariableDeclarationStatement");
    	if(ctx.localVariableDeclaration().unannType() != null && ctx.localVariableDeclaration().variableDeclarator().literal() != null) {
    		System.out.println("Filling local variables");
    		VariableData vd = new VariableData();
    		vd.type = ctx.localVariableDeclaration().unannType().getText();
    		vd.name = ctx.localVariableDeclaration().variableDeclarator().variableDeclaratorId().getText();
    		vd.value = ctx.localVariableDeclaration().variableDeclarator().literal().getText();
    		data.variables_map.put(vd.name, vd);
    		System.out.println(data.variables_map.size());
    		System.out.println(vd.type + vd.name + vd.value);
    	}
    	if(ctx.localVariableDeclaration().unannStringType() != null && ctx.localVariableDeclaration().variableDeclarator().literal() != null) {
    		System.out.println("Filling local variables");
    		VariableData vd = new VariableData();
    		vd.type = ctx.localVariableDeclaration().unannStringType().getText();
    		vd.name = ctx.localVariableDeclaration().variableDeclarator().variableDeclaratorId().getText();
    		vd.value = ctx.localVariableDeclaration().variableDeclarator().literal().getText();
    		data.variables_map.put(vd.name, vd);
    		System.out.println(data.variables_map.size());
    		System.out.println(vd.type + vd.name + vd.value);
    	}
    	if(ctx.localVariableDeclaration().variableDeclarator().arrayCreationExpression() != null) {
    		System.out.println("Filling arrays");
    		Array_ds array_ds = new Array_ds();
    		array_ds.type = ctx.localVariableDeclaration().unannType().unannArrayType().unannPrimitiveType().getText();
    		array_ds.array_name = ctx.localVariableDeclaration().variableDeclarator().variableDeclaratorId().getText();
    		array_ds.nDims = ctx.localVariableDeclaration().unannType().unannArrayType().dims().getChildCount()/2;
    		System.out.println(array_ds.array_name +" has dimension: ");
    		for(int i=0; i<array_ds.nDims; i++) {    			
    			System.out.println(ctx.localVariableDeclaration().variableDeclarator().arrayCreationExpression().dimExprs().getChild(i).getChild(1).getText());
    			array_ds.ub_seq.add(ctx.localVariableDeclaration().variableDeclarator().arrayCreationExpression().dimExprs().getChild(i).getChild(1).getText());
    		}
    		data.arrays_ds.put(array_ds.array_name, array_ds);
    		System.out.println(data.arrays_ds.size());
//    		System.out.println(vd.type + vd.name + vd.value);
    	}
    }
    @Override
    public void exitVariableDeclarator(LoopNestParser.VariableDeclaratorContext ctx) {
        System.out.println("inside exitVariableDeclarator");
        
    }

    @Override
    public void exitArrayCreationExpression(LoopNestParser.ArrayCreationExpressionContext ctx) {
    }

    @Override
    public void exitDimExprs(LoopNestParser.DimExprsContext ctx) {
    }

    @Override
    public void exitDimExpr(LoopNestParser.DimExprContext ctx) {
    }

    @Override
    public void exitLiteral(LoopNestParser.LiteralContext ctx) {
    }

    @Override
    public void exitVariableDeclaratorId(LoopNestParser.VariableDeclaratorIdContext ctx) {
    }

    @Override
    public void exitUnannArrayType(LoopNestParser.UnannArrayTypeContext ctx) {
    }

    @Override
    public void enterDims(LoopNestParser.DimsContext ctx) {
    }

    @Override
    public void exitUnannPrimitiveType(LoopNestParser.UnannPrimitiveTypeContext ctx) {
    }

    @Override
    public void exitNumericType(LoopNestParser.NumericTypeContext ctx) {
    }

    @Override
    public void exitIntegralType(LoopNestParser.IntegralTypeContext ctx) {
    }

    @Override
    public void exitFloatingPointType(LoopNestParser.FloatingPointTypeContext ctx) {
    }
    @Override
    public void exitForInit(LoopNestParser.ForInitContext ctx) {
    }
    @Override
    public void exitForStatement(LoopNestParser.ForStatementContext ctx) {
    	System.out.println("exitForStatement");
    	System.out.println("popping a loop");
    	data.loops.remove(data.loops.size() - 1);
    }
    @Override
    public void enterForStatement(LoopNestParser.ForStatementContext ctx) {
    	System.out.println("enterForStatement");
    	System.out.println("pushing a loop");
    	Loop loop = new Loop();
    	loop.iterator = ctx.forInit().localVariableDeclaration().variableDeclarator().variableDeclaratorId().getText();
    	loop.upperBound = ctx.forCondition().relationalExpression().getChild(2).getText();
    	loop.stride = ctx.forUpdate().simplifiedAssignment().getChild(2).getText();
    	System.out.println(loop.iterator + " " + loop.upperBound + " " + loop.stride);
    	data.loops.add(loop);
    }

    @Override
    public void exitForCondition(LoopNestParser.ForConditionContext ctx) {
    }

    @Override
    public void exitRelationalExpression(LoopNestParser.RelationalExpressionContext ctx) {
    }

    @Override
    public void exitForUpdate(LoopNestParser.ForUpdateContext ctx) {
    }

    @Override
    public void exitSimplifiedAssignment(LoopNestParser.SimplifiedAssignmentContext ctx) {
    }

    @Override
    public void exitArrayAccess(LoopNestParser.ArrayAccessContext ctx) {
    	System.out.println("exitArrayAccess");
    	System.out.println("Filling array access sequence details");
    	for(int i=2; i<ctx.getChildCount(); i+=3) {
    		data.arrays_ds.get(ctx.getChild(0).getText()).access_seq.add(ctx.getChild(i).getText());
    		System.out.println(ctx.getChild(0).getText() + " " + ctx.getChild(i).getText());
    	}
    	System.out.println(data.loops.size());
    	for(int i=0; i<data.loops.size(); i++) {
    		Loop newLoop = new Loop();
    		Loop currLoop = data.loops.get(i);
    		newLoop.iterator = currLoop.iterator;
    		newLoop.stride = currLoop.stride;
    		newLoop.upperBound = currLoop.upperBound;
    		data.arrays_ds.get(ctx.getChild(0).getText()).loops.add(newLoop);
    	}
    }

    @Override
    public void exitArrayAccess_lfno_primary(LoopNestParser.ArrayAccess_lfno_primaryContext ctx)  {
    	System.out.println("exitArrayAccess_lfno_primary");
    	System.out.println("Filling array access sequence details");
    	for(int i=2; i<ctx.getChildCount(); i+=3) {
    		data.arrays_ds.get(ctx.getChild(0).getText()).access_seq.add(ctx.getChild(i).getText());
    		System.out.println(ctx.getChild(0).getText() + " " + ctx.getChild(i).getText());
    	}
    	System.out.println("loops size input");
    	System.out.println(data.loops.size());
    	for(int i=0; i<data.loops.size(); i++) {
    		Loop newLoop = new Loop();
    		Loop currLoop = data.loops.get(i);
    		newLoop.iterator = currLoop.iterator;
    		newLoop.stride = currLoop.stride;
    		newLoop.upperBound = currLoop.upperBound;
    		data.arrays_ds.get(ctx.getChild(0).getText()).loops.add(newLoop);
    	}
    }

}
