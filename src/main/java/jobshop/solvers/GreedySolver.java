package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

public class GreedySolver implements Solver {
	//priority rule to use, 1 for SPT 0 for LRPT
	GreedyPriority priority ; 
	int[] nextTask ; 


	int[] endJobs ; 
	int[] releaseTimeOfMachine ; 
	int EST ; 

	public GreedySolver(GreedyPriority priority) {
		// TODO Auto-generated constructor stub
		this.priority = priority ; 

	}

	@Override
	public Result solve(Instance instance, long deadline) {
		// Initialization : les tâches réalisables sont les premières tâches de chaque job
		nextTask = new int[instance.numJobs] ; 
		endJobs = new int[instance.numJobs] ; 
		releaseTimeOfMachine = new int[instance.numMachines] ; 
		ResourceOrder sol = new ResourceOrder(instance) ; 

		boolean hasRealisable = true ; 
		while (hasRealisable) {
			//find the task with the SPT or the LRPT
			Task taskPrio = new Task(0, 0) ;
			/*pour chaque job quand il fini
			pour chaque machine quand elle est dispo*/
			switch (this.priority) {
			case EST_SPT : 
				Task[] selectedTask = tasksEST(instance) ; 
				int EST_SPT = Integer.MAX_VALUE ; 
				for(Task currentTask : selectedTask){
					if (currentTask != null) {
						if(instance.duration(currentTask) < EST_SPT){
							EST_SPT = instance.duration(currentTask);
							taskPrio = currentTask ; 

						}
					}
				}
				break;
			case EST_LRPT:
				Task[] selectedTasks = tasksEST(instance) ; 
				int EST_LRPT = 0 ; 
				for (Task currentTask : selectedTasks) {
					if (currentTask != null) {
						int jobDuration = 0;
						for(int t = currentTask.task; t<instance.numTasks; t++) {
							jobDuration += instance.duration(currentTask.job, t) ;
						}
						if (jobDuration > EST_LRPT) {
							EST_LRPT = jobDuration; 
							taskPrio = currentTask ;
						}
					}
				}
				break; 
			case SPT : 
				int SPT = Integer.MAX_VALUE ; 
				for(int i = 0;i<instance.numJobs;i++){
					if (nextTask[i] != instance.numTasks) {
						if(instance.duration(i, nextTask[i]) < SPT){
							SPT = instance.duration(i, nextTask[i]);
							taskPrio = new Task(i, nextTask[i]) ; 
						}
					}
				}
				break; 
			case LRPT:
				int LRPT = 0 ; 
				for (int i = 0; i<instance.numJobs; i++) {
					int jobDuration = 0;
					for(int j = nextTask[i]; j<instance.numTasks; j++) {
						jobDuration += instance.duration(i, j) ;
					}
					if (jobDuration > LRPT) {
						LRPT = jobDuration; 
						taskPrio = new Task(i, nextTask[i]) ;
					}
				}
				break; 
			}

			//update solution
			int machine = instance.machine(taskPrio) ;
			sol.tasksByMachine[machine][sol.nextFreeSlot[machine]] = taskPrio;
			sol.nextFreeSlot[machine]++; 
			endJobs[taskPrio.job] = instance.duration(taskPrio) + this.EST ; 
			releaseTimeOfMachine[machine] = endJobs[taskPrio.job] ; 


			//update realisable[]

			nextTask[taskPrio.job]++; 

			//check if there is still tasks to be done for each job
			hasRealisable = false; 
			for (int i = 0; i<instance.numJobs; i++) {
				if (nextTask[i] != instance.numTasks)
					hasRealisable = true; 
			}
		}
		return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
	}

	private Task[] tasksEST(Instance instance) {
		//calculation of est
		int minEST = Integer.MAX_VALUE ; 
		for (int j = 0; j<instance.numJobs; j++) {
			if (nextTask[j] != instance.numTasks) {
				int currentST = Math.max(endJobs[j], releaseTimeOfMachine[instance.machine(j, nextTask[j])]) ;
				if (currentST <= minEST) {
					minEST = currentST ; 
				}
			}
		}
		this.EST = minEST ; 
		//selection of all tasks with EST value
		Task[] tasksEST = new Task[instance.numJobs] ;
		for (int j = 0; j<instance.numJobs; j++) {
			if (nextTask[j] != instance.numTasks) {
				int currentEST = Math.max(endJobs[j], releaseTimeOfMachine[instance.machine(j, nextTask[j])]) ;
				if (currentEST == minEST) {
					tasksEST[j] = new Task(j, nextTask[j]); 
				}
			}
		}

		return tasksEST; 
	}
}
