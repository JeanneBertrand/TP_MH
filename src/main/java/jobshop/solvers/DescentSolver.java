package jobshop.solvers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Result.ExitCause;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    public static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
        
        public String toString() {
        	return "Machine "+machine+" : ["+firstTask+" to "+lastTask+"]" ; 
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swap with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
        	Task aux = order.tasksByMachine[machine][t1]; 
        	order.tasksByMachine[machine][t1] = order.tasksByMachine[machine][t2] ; 
        	order.tasksByMachine[machine][t2] = aux ; 
        }
        
        public String toString() {
        	return "Machine "+machine+" : swap "+t1+" and "+t2 ; 
        }
    }


    @Override
    public Result solve(Instance instance, long deadline) {
    	//initialization of result
    	GreedySolver greedySolver = new GreedySolver(GreedyPriority.EST_LRPT) ;  
    	Result result = greedySolver.solve(instance, System.currentTimeMillis() + 10);
        ResourceOrder bestOrder = new ResourceOrder(result.schedule); 
      
        
        boolean hasImproved = true ; 
        while (hasImproved && (deadline - System.currentTimeMillis() >1 )) {
        	hasImproved = false; 
        	List<Block> blocks = blocksOfCriticalPath(bestOrder);
        	List<Swap> swaps = new ArrayList<DescentSolver.Swap>() ; 
        	for (Block b : blocks) {
        		swaps.addAll(neighbors(b)) ;
        	}
        	
        	int bestMakespan = bestOrder.toSchedule().makespan() ; 
        	for (Swap s : swaps) {
        		ResourceOrder currentNeighboor = bestOrder.copy() ; 
        		s.applyOn(currentNeighboor);
        		if (currentNeighboor.toSchedule().makespan() < bestMakespan) {
        			bestMakespan = currentNeighboor.toSchedule().makespan() ; 
        			bestOrder = currentNeighboor.copy() ; 
        			hasImproved = true; 
        		}
        	}

        }
        ExitCause exit = (hasImproved ? ExitCause.Timeout : ExitCause.Blocked) ; 
        return new Result(instance, bestOrder.toSchedule(), exit); 
    }

    /** Returns a list of all blocks of the critical path. */
    public List<Block> blocksOfCriticalPath(ResourceOrder order) {
    	Schedule sched = order.toSchedule() ;
    	List<Task> path = sched.criticalPath() ; 
    	List<Block> blocks = new ArrayList<DescentSolver.Block>();
    	
    	int currentMachine = order.instance.machine(path.get(0)) ; 
    	int startCurrentBlock = 0;  
    	for (Task currentTask : path) {
    		int index = path.indexOf(currentTask) ; 
    		if (order.instance.machine(currentTask) != currentMachine || index == path.size()-1) {
    			int endCurrentBlock ; 
    			if (index == path.size()-1) {
    				endCurrentBlock = index ; 
    				if (order.instance.machine(currentTask) != currentMachine) {
    					endCurrentBlock = index-1; 
    				}
    			} else {
    				endCurrentBlock = index-1; 
    			}
    		
    			if (endCurrentBlock > startCurrentBlock) {
    				List<Task> taskOfMachine = Arrays.asList(order.tasksByMachine[currentMachine]) ; 
    				blocks.add(new Block(currentMachine, taskOfMachine.indexOf(path.get(startCurrentBlock)), taskOfMachine.indexOf(path.get(endCurrentBlock)))) ; 
    			}
    			startCurrentBlock = index;
    			currentMachine = order.instance.machine(currentTask) ; 
    		}
    	}
        return blocks;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    public List<Swap> neighbors(Block block) {
    	List<Swap> listOfSwaps = new ArrayList<DescentSolver.Swap>(); 
    	if (block.firstTask+1 == block.lastTask) {
    		listOfSwaps.add(new Swap(block.machine, block.firstTask, block.lastTask)) ; 
    	} else {
    		listOfSwaps.add(new Swap(block.machine, block.lastTask-1, block.lastTask));
    		listOfSwaps.add(new Swap(block.machine, block.firstTask, block.firstTask+1));
    	}
        return listOfSwaps; 
    }

}
