import java.util.*;
import static java.util.stream.Collectors.*;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

// FIXME: You should limit your implementation to this class. You are free to add new auxilliary classes. You do not need to touch the LoopNext.g4 file.
class Analysis extends LoopNestBaseListener {
	Data data;
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
        this.data = new Data();
        data.misses = 0;
    }
    
	private void computeMisses() {
		System.out.println("computeMisses");
		long misses = 0;
		for (String array_name : data.arrays_ds.keySet()) {
			misses += computeMissesArray(data.arrays_ds.get(array_name));
		}
	}

    private long computeMissesArray(Array_ds array_ds) {
    	System.out.println("computeMissesArray");
    	List<String> loops_below = new ArrayList<String>();
    	long misses = 0;
    	System.out.println(array_ds.loops.size());
    	for(int i = array_ds.loops.size()-1; i>=0; i--) {
    		Loop loop = array_ds.loops.get(i);
    		System.out.println("iterator: " + loop.iterator);
    		String cmdr = findCmdr(loop.iterator, array_ds.access_seq, loops_below);
    		if(cmdr.equals(loop.iterator)) {
    			System.out.println("Commander is this loop: " + cmdr);
    			int levCmd = findLoc(cmdr, array_ds.access_seq);
    			long cmf = computeCmfCore(levCmd, Long.parseLong(loop.stride), Long.parseLong(loop.upperBound));
    			misses+=cmf;
    		}else {
    			int levCmd = findLoc(cmdr, array_ds.access_seq);
    			if(misses > check_misses(levCmd, cmdr)) {
    				System.out.println("Eviction happened");
    				long cmf = (Long.parseLong(loop.upperBound)/Long.parseLong(loop.stride));
    				misses+=cmf;
    			}else {
    				System.out.println("Eviction did not happen");
    				int levCmdLoop = findLoc(loop.iterator, array_ds.access_seq);
    				long cmf = computeCmfCore(levCmdLoop, Long.parseLong(loop.stride), Long.parseLong(loop.upperBound));
    				misses+=cmf;
    			}
    		}
    		loops_below.add(loop.iterator);
    	}
    	data.arrays_ds.get(array_ds.array_name).misses = misses;
		return misses;
	}

	private long check_misses(int levCmd, String cmdr) {
		// TODO Auto-generated method stub
		return 0;
	}

	private long computeCmfCore(int levCmd, long parseLong, long parseLong2) {
		// TODO Auto-generated method stub
		return 0;
	}

	private int findLoc(String cmdr, List<String> access_seq) {
		// TODO Auto-generated method stub
		return 0;
	}

	private String findCmdr(String iterator, List<String> access_seq, List<String> loops_below) {
		// TODO Auto-generated method stub
		return null;
	}

	// FIXME: Feel free to override additional methods from
    // LoopNestBaseListener.java based on your needs.
    // Method entry callback
    @Override
    public void enterMethodDeclaration(LoopNestParser.MethodDeclarationContext ctx) {
        System.out.println("enterMethodDeclaration");
        
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
        try {
            FileOutputStream fos = new FileOutputStream("Results.obj");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // FIXME: Serialize your data to a file
            // oos.writeObject();
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
