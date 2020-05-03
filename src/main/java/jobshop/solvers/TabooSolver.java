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
		// TODO Auto-generated constructor stub
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
			//System.out.println(bestOrder);
			hasChanged = false ; 
			//System.out.println("juste avant call blocksOfCriticalPath \n"+currentOrder);
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
			}
			currentOrder = bestNeighboor; 
			if (bestMakespan < bestOrder.toSchedule().makespan()) {
				bestOrder = bestNeighboor.copy() ; 
			}
			k++; 
		}
		ExitCause exit = (k<maxIter ? ExitCause.Timeout : ExitCause.Blocked) ; 
		//System.out.println(exit.toString() + " k = "+k);
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
