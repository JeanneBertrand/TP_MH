package jobshop.solvers;

import java.util.ArrayList;
import java.util.List;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Result.ExitCause;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

public class TabooSolver extends DescentSolver {

	private int maxIter;
	private int dureeTaboo;
	private int[][] sTaboo ; 

	public TabooSolver(int maxIter, int dureeTaboo) {
		super(); 
		this.maxIter = maxIter ; 
		this.dureeTaboo = dureeTaboo ; 
	}

	@Override
	public Result solve(Instance instance, long deadline) {
		//initialization of result
		GreedySolver greedySolver = new GreedySolver(GreedyPriority.EST_LRPT) ;  
		ResourceOrder bestOrder = new ResourceOrder(greedySolver.solve(instance, System.currentTimeMillis() + 10).schedule); 
		ResourceOrder currentOrder = bestOrder.copy(); 

		this.sTaboo = new int[instance.numJobs * instance.numTasks][instance.numJobs * instance.numTasks] ; 

		int k = 0 ; 
		boolean hasChanged = true ; 
		while (k<maxIter && (deadline - System.currentTimeMillis() >1 ) && hasChanged) {
			hasChanged = false ; 
			List<Block> blocks = blocksOfCriticalPath(currentOrder);
			List<Swap> swaps = new ArrayList<DescentSolver.Swap>() ; 
			for (Block b : blocks) {
				swaps.addAll(neighbors(b)) ;
			}

			//search the best neighbor among neighbors
			ResourceOrder bestNeighboor = new ResourceOrder(instance) ;
			int bestMakespan = Integer.MAX_VALUE ; 
			
			Swap bestSwap = null ; 
			for (Swap s : swaps) {
				if (!isTaboo(s, currentOrder, k)) {
					ResourceOrder currentNeighboor = currentOrder.copy() ; 
					s.applyOn(currentNeighboor);
					if (currentNeighboor.toSchedule().makespan() < bestMakespan) {
						bestMakespan = currentNeighboor.toSchedule().makespan() ; 
						bestNeighboor = currentNeighboor.copy() ; 
						bestSwap = s ; 
						hasChanged = true ; 
					}
				}
			}
			if (bestSwap != null) {
				addTaboo(bestSwap, currentOrder, k) ; 
				currentOrder = bestNeighboor.copy(); 
			}
			
			
			if (bestMakespan < bestOrder.toSchedule().makespan()) {
				bestOrder = currentOrder.copy() ; 
			}
			k++; 
		}
		ExitCause exit = (k<maxIter ? ExitCause.Timeout : ExitCause.Blocked) ; 
		return new Result(instance, bestOrder.toSchedule(), exit); 
	}

	private void addTaboo(Swap swap, ResourceOrder order, int k) {
		Task task1 = order.tasksByMachine[swap.machine][swap.t1] ; 
		Task task2 = order.tasksByMachine[swap.machine][swap.t2] ; 
		sTaboo[task2.job * order.instance.numTasks+task2.task][task1.job * order.instance.numTasks + task1.task] = k + dureeTaboo ; 		
	}

	private boolean isTaboo(Swap swap, ResourceOrder order, int k) {
		Task task1 = order.tasksByMachine[swap.machine][swap.t1] ; 
		Task task2 = order.tasksByMachine[swap.machine][swap.t2] ; 
		return k < sTaboo[task1.job * order.instance.numTasks + task1.task][task2.job * order.instance.numTasks+task2.task] ; 
	}

}
