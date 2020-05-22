package jobshop;

import java.io.IOException;
import java.nio.file.Paths;

import jobshop.encodings.ResourceOrder;
import jobshop.solvers.GreedyPriority;
import jobshop.solvers.GreedySolver;

public class DebuggingMain {

	public static void main(String[] args) {
		try {
			Instance instance = Instance.fromFile(Paths.get("instances/ft06"));
			/*int dureeMax = 0; 
			for (int j = 0; j<instance.numJobs; j++) {
				for (int t = 0; t<instance.numTasks; t++) {
					dureeMax += instance.duration(j, t);
				}
			}
			System.out.println(dureeMax);
			/*GreedySolver solver = new GreedySolver(GreedyPriority.SPT) ; 			
			Result result = solver.solve(instance, System.currentTimeMillis()+10) ; 
			ResourceOrder order = new ResourceOrder(result.schedule) ; 
			System.out.println(order);
			System.out.println(result.schedule.makespan());*/
			


			/*Result result = solver.solve(instance, System.currentTimeMillis() + 10);
             System.out.println(result.schedule.toString());
             System.out.println(result.schedule.makespan());*/
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}
}
