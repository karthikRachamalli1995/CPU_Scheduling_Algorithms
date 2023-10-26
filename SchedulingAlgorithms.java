import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

public class SchedulingAlgorithms {
    public static void main(String[] args) {
        try {
            // The below condition checks the input args are correct or not.
            if (args.length != 1 || !args[0].contains(".txt")) {
                System.out.println("Please provide a valid file name with extension .txt");
                return;
            }

            // Reading the test cases from the file.
            File testCaseFileName = new File(args[0]);
            Scanner sc = new Scanner(testCaseFileName);

            int testCasesCount = sc.nextInt();
            List<TestCase> testCases = new ArrayList<>();

            // Reads all the test cases one by one.
            for (int i = 0; i < testCasesCount; i++) {

                int processesCount = sc.nextInt();
                int[] burstTimes = new int[processesCount];
                int[] arrivalTimes = new int[processesCount];
                int[] priorities = new int[processesCount];

                for (int j = 0; j < processesCount; j++) {
                    burstTimes[j] = sc.nextInt();
                }

                for (int j = 0; j < processesCount; j++) {
                    arrivalTimes[j] = sc.nextInt();
                }

                for (int j = 0; j < processesCount; j++) {
                    priorities[j] = sc.nextInt();
                }

                int quantum = sc.nextInt();

                testCases.add(new TestCase(processesCount, burstTimes, arrivalTimes, priorities, quantum));
            }

            sc.close();

            try {
                // This below block of code perform the algorithms and calculates the metrics.
                FileOutputStream fileOutputStream = new FileOutputStream("output.txt");
                PrintWriter filePrintWriter = new PrintWriter(fileOutputStream);
                Map<Integer, Map<String, List<EvaluationCriteria>>> ecMap = new HashMap<>();
                // Iterates over testcase by testcase.
                for (int i = 0; i < testCases.size(); i++) {
                    int processesCount = testCases.get(i).getProcessesCount();
                    int[] burstTimes = testCases.get(i).getBurstTimes();
                    int[] arrivalTimes = testCases.get(i).getArrivalTimes();
                    int[] priorities = testCases.get(i).getPriorities();
                    int quantum = testCases.get(i).getQuantum();

                    // Evaluation criteria for the algorithms
                    EvaluationCriteria fcfsCriteria = scheduleFirstComeFirstServe(processesCount,
                            Arrays.copyOf(burstTimes, processesCount), Arrays.copyOf(arrivalTimes, processesCount));
                    EvaluationCriteria sjfCriteria = scheduleShortestJobFirst(processesCount,
                            Arrays.copyOf(burstTimes, processesCount), Arrays.copyOf(arrivalTimes, processesCount));
                    EvaluationCriteria priorityCriteria = schedulePriority(processesCount,
                            Arrays.copyOf(burstTimes, processesCount), Arrays.copyOf(arrivalTimes, processesCount), Arrays.copyOf(priorities, processesCount));
                    EvaluationCriteria rrCriteria = scheduleRoundRobin(processesCount,
                            Arrays.copyOf(burstTimes, processesCount), Arrays.copyOf(arrivalTimes, processesCount), quantum);
                    EvaluationCriteria priorityRrCriteria = schedulePriorityRoundRobin(processesCount,
                            Arrays.copyOf(burstTimes, processesCount), Arrays.copyOf(arrivalTimes, processesCount),
                            Arrays.copyOf(priorities, processesCount), quantum);

                    // compareAndPrint method compares the evaluation criteria and writes to both CLI and output file.
                    compareAndPrint(i, processesCount, fcfsCriteria, sjfCriteria, priorityCriteria, rrCriteria,
                            priorityRrCriteria,
                            filePrintWriter);
                    // Below code builds the overall evaluation criteria per each processes set such as 5, 10, 15,
                    // 20, 25.
                    Map<String, List<EvaluationCriteria>> ec = ecMap.getOrDefault(processesCount, new HashMap<>());

                    List<EvaluationCriteria> fcfsList = ec.getOrDefault("fcfs", new ArrayList<>());
                    fcfsList.add(fcfsCriteria);
                    ec.put("fcfs", fcfsList);

                    List<EvaluationCriteria> sjfList = ec.getOrDefault("sjf", new ArrayList<>());
                    sjfList.add(sjfCriteria);
                    ec.put("sjf", sjfList);

                    List<EvaluationCriteria> priorityList = ec.getOrDefault("priority", new ArrayList<>());
                    priorityList.add(priorityCriteria);
                    ec.put("priority", priorityList);

                    List<EvaluationCriteria> rrList = ec.getOrDefault("rr", new ArrayList<>());
                    rrList.add(rrCriteria);
                    ec.put("rr", rrList);

                    List<EvaluationCriteria> priorityrrList = ec.getOrDefault("priorityrr", new ArrayList<>());
                    priorityrrList.add(priorityRrCriteria);
                    ec.put("priorityrr", priorityrrList);

                    ecMap.put(processesCount, ec);
                }
                // printAndSaveAverages finds the averages of averages of each processes set.
                printAndSaveAverages(ecMap, filePrintWriter);
                filePrintWriter.close();
                fileOutputStream.close();
            } catch (Exception e) {
                System.out.println("Failed to Print and Write to the file! due to: " + e.getLocalizedMessage());
            }

        } catch (FileNotFoundException e) {
            System.out.println("TestCases file not found!");
        }
    }

    private static EvaluationCriteria scheduleFirstComeFirstServe(int processesCount, int[] burstTimes, int[] arrivalTimes) {
        // Initialising the waitingTime, turnaroundTime, responseTime and completionTime
        int[] waitingTimes = new int[processesCount];
        int[] turnaroundTimes = new int[processesCount];
        int[] responseTimes = new int[processesCount];
        int[] completionTimes = new int[processesCount];

        // Initialising the Total Burst Time and present time.
        int totalBurstTime = 0;
        int presentTime = 0;

        // Calculating the total burst time and initializing the response times to -1.
        for (int i = 0; i < processesCount; i++) {
            totalBurstTime += burstTimes[i];
            responseTimes[i] = -1;
        }

        // The below is the main loop that iterates and executes the processes in First Come First Serve basis.
        while (totalBurstTime > 0) {
            // Initializing the minimumArrivalTime and nextProcess to Max value and -1 respectively.
            int minimumArrivalTime = Integer.MAX_VALUE;
            int nextProcess = -1;

            // The below 'for' loop finds the next process based on the minimum arrival time
            for (int i = 0; i < processesCount; i++) {
                // The below condition checks whether the process
                // * Arrived before the presentTime
                // * Process has some burst time greater than 0
                // * It's arrival time is lesser than minimum arrival time as of that time.
                if (arrivalTimes[i] <= presentTime && burstTimes[i] > 0 && arrivalTimes[i] < minimumArrivalTime) {
                    // If the process is in the next position, update the minimum arrival time with it's arrival time
                    // and next process with process number.
                    minimumArrivalTime = arrivalTimes[i];
                    nextProcess = i;
                }
            }

            // If the nextProcess is '-1', that means no process has arrived yet.
            if (nextProcess == -1) {
                // If there is no process has arrived yet, simply increment the present timer.
                presentTime++;
            } else {
                // The below if condition updates the response time.
                if (responseTimes[nextProcess] == -1) {
                    responseTimes[nextProcess] = presentTime - arrivalTimes[nextProcess];
                }

                int currentProcessBurstTime = burstTimes[nextProcess];

                // Updates the present time with burst times of the process that being executed.
                presentTime = presentTime + currentProcessBurstTime;
                // Decrementing the total burst time, as process is executed.
                totalBurstTime = totalBurstTime - currentProcessBurstTime;
                // updating the completion time of the current process.
                completionTimes[nextProcess] = presentTime;
                // Calculating the waiting time.
                waitingTimes[nextProcess] = presentTime - arrivalTimes[nextProcess] - currentProcessBurstTime;
                // Calculating the Turnaround time.
                turnaroundTimes[nextProcess] = completionTimes[nextProcess] - arrivalTimes[nextProcess];
                burstTimes[nextProcess] = 0;
            }
        }

        // Calculate average values
        double avgTurnaroundTime = Arrays.stream(turnaroundTimes).asDoubleStream().average().orElse(0.0);
        double avgResponseTime = Arrays.stream(responseTimes).asDoubleStream().average().orElse(0.0);
        double avgWaitingTime = Arrays.stream(waitingTimes).asDoubleStream().average().orElse(0.0);

        return new EvaluationCriteria("First Come First Serve", avgWaitingTime, avgResponseTime, avgTurnaroundTime);
    }

    private static EvaluationCriteria scheduleShortestJobFirst(int processesCount, int[] burstTimes, int[] arrivalTimes) {
        // Initialising the waitingTime, turnaroundTime, responseTime and completionTime
        int[] waitingTimes = new int[processesCount];
        int[] turnaroundTimes = new int[processesCount];
        int[] responseTimes = new int[processesCount];
        int[] completionTimes = new int[processesCount];

        // Initialising the Total Burst Time and present time.
        int presentTime = 0;
        int totalBurstTime = 0;

        // Calculating the total burst time and initializing the response times to -1.
        for (int i = 0; i < processesCount; i++) {
            totalBurstTime += burstTimes[i];
            responseTimes[i] = -1;
        }

        // The below is the main loop that iterates and executes the processes that has shortest burst time first.
        while (totalBurstTime > 0) {
            // Initializing the shortestBurstTime and shortestJobProcessId to Max value and -1 respectively.
            int shortestJobProcessId = -1;
            int shortestBurstTime = Integer.MAX_VALUE;

            // The below 'for' loop finds the next process based on the shortest burst time
            for (int i = 0; i < processesCount; i++) {
                // The below condition checks whether the process
                // * Arrived before the presentTime
                // * Process has some burst time greater than 0
                // * It's burst time is lesser than minimum burst time as of that time.
                if (arrivalTimes[i] <= presentTime && burstTimes[i] > 0 && burstTimes[i] < shortestBurstTime) {
                    shortestJobProcessId = i;
                    shortestBurstTime = burstTimes[i];
                }
            }

            if (shortestJobProcessId == -1) {
                presentTime++;
            } else {
                // The below if condition checks and updates the response time.
                if (responseTimes[shortestJobProcessId] == -1) {
                    responseTimes[shortestJobProcessId] = presentTime - arrivalTimes[shortestJobProcessId];
                }

                int currentProcessBurstTime = burstTimes[shortestJobProcessId];

                // Updates the present time with burst times of the process that being executed.
                presentTime = presentTime + currentProcessBurstTime;
                // Decrementing the total burst time, as process is executed.
                totalBurstTime = totalBurstTime - currentProcessBurstTime;
                // updating the completion time of the current process.
                completionTimes[shortestJobProcessId] = presentTime;
                // Calculating the waiting time.
                waitingTimes[shortestJobProcessId] = presentTime - arrivalTimes[shortestJobProcessId] - currentProcessBurstTime;
                // Calculating the Turnaround time.
                turnaroundTimes[shortestJobProcessId] = completionTimes[shortestJobProcessId] - arrivalTimes[shortestJobProcessId];
                burstTimes[shortestJobProcessId] = 0;
            }
        }

        // Calculate average values
        double avgTurnaroundTime = Arrays.stream(turnaroundTimes).asDoubleStream().average().orElse(0.0);
        double avgResponseTime = Arrays.stream(responseTimes).asDoubleStream().average().orElse(0.0);
        double avgWaitingTime = Arrays.stream(waitingTimes).asDoubleStream().average().orElse(0.0);

        return new EvaluationCriteria("Shortest Job First", avgWaitingTime, avgResponseTime, avgTurnaroundTime);
    }

    private static EvaluationCriteria schedulePriority(int processesCount, int[] burstTimes, int[] arrivalTimes, int[] priorities) {
        // Initialising the waitingTime, turnaroundTime, responseTime and completionTime
        int[] waitingTimes = new int[processesCount];
        int[] turnaroundTimes = new int[processesCount];
        int[] responseTimes = new int[processesCount];
        int[] completionTimes = new int[processesCount];

        // Initialising the Total Burst Time and present time.
        int presentTime = 0;
        int totalBurstTime = 0;

        // Calculating the total burst time and initializing the response times to -1.
        for (int i = 0; i < processesCount; i++) {
            totalBurstTime += burstTimes[i];
            responseTimes[i] = -1;
        }

        // The below is the main loop that iterates and executes the processes that has highest priority.
        while (totalBurstTime > 0) {
            int highestPriorityProcessId = -1;
            int highestPriority = Integer.MAX_VALUE;

            for (int i = 0; i < processesCount; i++) {
                // The below condition checks whether the process
                // * Arrived before the presentTime
                // * Process has some burst time greater than 0
                // * It's priority is lesser than highest priority as of that time.
                if (arrivalTimes[i] <= presentTime && burstTimes[i] > 0 && priorities[i] < highestPriority) {
                    highestPriorityProcessId = i;
                    highestPriority = priorities[i];
                }
            }

            if (highestPriorityProcessId == -1) {
                presentTime++;
            } else {
                // The below if condition checks and updates the response time.
                if (responseTimes[highestPriorityProcessId] == -1) {
                    responseTimes[highestPriorityProcessId] = presentTime - arrivalTimes[highestPriorityProcessId];
                }

                int currentProcessBurstTime = burstTimes[highestPriorityProcessId];

                // Updates the present time with burst times of the process that being executed.
                presentTime = presentTime + currentProcessBurstTime;
                // Decrementing the total burst time, as process is executed.
                totalBurstTime = totalBurstTime - currentProcessBurstTime;
                // updating the completion time of the current process.
                completionTimes[highestPriorityProcessId] = presentTime;
                // Calculating the waiting time.
                waitingTimes[highestPriorityProcessId] = presentTime - arrivalTimes[highestPriorityProcessId] - currentProcessBurstTime;
                // Calculating the Turnaround time.
                turnaroundTimes[highestPriorityProcessId] = completionTimes[highestPriorityProcessId] - arrivalTimes[highestPriorityProcessId];
                burstTimes[highestPriorityProcessId] = 0;
            }
        }

        // Calculate average values
        double avgTurnaroundTime = Arrays.stream(turnaroundTimes).asDoubleStream().average().orElse(0.0);
        double avgResponseTime = Arrays.stream(responseTimes).asDoubleStream().average().orElse(0.0);
        double avgWaitingTime = Arrays.stream(waitingTimes).asDoubleStream().average().orElse(0.0);

        return new EvaluationCriteria("Priority", avgWaitingTime, avgResponseTime, avgTurnaroundTime);
    }

    private static EvaluationCriteria scheduleRoundRobin(int processesCount, int[] burstTimes, int[] arrivalTimes, int quantum) {
        // Initialising the waitingTime, turnaroundTime, responseTime, completionTime, etc..
        int[] waitingTimes = new int[processesCount];
        int[] turnaroundTimes = new int[processesCount];
        int[] responseTimes = new int[processesCount];
        int[] completionTimes = new int[processesCount];
        boolean[] completed = new boolean[processesCount];
        int[] remainingBurstTimes = new int[processesCount];

        // Initialising the presentTime and completedCount.
        int presentTime = 0;
        int completedCount = 0;

        // Initialising the remaining burst times and responseTimes
        for (int i = 0; i < processesCount; i++) {
            remainingBurstTimes[i] = burstTimes[i];
            responseTimes[i] = -1;
        }

        // The below is the main loop that iterates and executes the processes in Round Robin.
        while (completedCount < processesCount) {
            boolean noExecution = true;
            // The below for loop will execute all the processes for a quantum time.
            for (int i = 0; i < processesCount; i++) {
                if (!completed[i] && arrivalTimes[i] <= presentTime) {
                    if (remainingBurstTimes[i] <= quantum) {
                        // The below condition checks and updates the response time.
                        responseTimes[i] = (responseTimes[i] == -1) ? presentTime - arrivalTimes[i] : responseTimes[i];
                        // Updates the present time with burst times of the process that being executed.
                        presentTime = presentTime + remainingBurstTimes[i];
                        // updating the completion time of the current process.
                        completionTimes[i] = presentTime;
                        // Calculating the Turnaround time.
                        turnaroundTimes[i] = completionTimes[i] - arrivalTimes[i];
                        // Calculating the waiting time.
                        waitingTimes[i] = completionTimes[i] - arrivalTimes[i] - burstTimes[i];
                        remainingBurstTimes[i] = 0;
                        completed[i] = true;
                        completedCount++;
                        noExecution = false;
                    } else {
                        // Updating the response times.
                        responseTimes[i] = (responseTimes[i] == -1) ? presentTime - arrivalTimes[i] : responseTimes[i];
                        // Updating the present time with quantum as burst time is greater than quantum
                        presentTime = presentTime + quantum;
                        // Reducing the remaining burst time by quantum
                        remainingBurstTimes[i] = remainingBurstTimes[i] - quantum;
                        noExecution = false;
                    }
                }
            }

            if (noExecution) {
                presentTime++;
            }
        }

        // Calculate average values
        double avgTurnaroundTime = Arrays.stream(turnaroundTimes).asDoubleStream().average().orElse(0.0);
        double avgResponseTime = Arrays.stream(responseTimes).asDoubleStream().average().orElse(0.0);
        double avgWaitingTime = Arrays.stream(waitingTimes).asDoubleStream().average().orElse(0.0);

        return new EvaluationCriteria("Round Robin", avgWaitingTime, avgResponseTime, avgTurnaroundTime);
    }

    private static EvaluationCriteria schedulePriorityRoundRobin(int processesCount, int[] burstTimes, int[] arrivalTimes, int[] priorities, int quantum) {
        // Initialising the waitingTime, turnaroundTime, responseTime, remainingBurstTimes, etc..
        int[] waitingTimes = new int[processesCount];
        int[] turnaroundTimes = new int[processesCount];
        int[] responseTimes = new int[processesCount];
        int[] remainingBurstTimes = new int[processesCount];
        boolean[] executed = new boolean[processesCount];
        Map<Integer, List<Integer>> prioritiesMap = new HashMap<>();

        // Build a map which has processes with same priority.
        for (int i = 0; i < priorities.length; i++) {
            List<Integer> processes = prioritiesMap.getOrDefault(priorities[i], new ArrayList<>());
            processes.add(i);
            prioritiesMap.put(priorities[i], processes);
        }

        // Initialising the remaining burst times and response times.
        for (int i = 0; i < processesCount; i++) {
            remainingBurstTimes[i] = burstTimes[i];
            responseTimes[i] = -1;
        }

        int presentTime = 0;
        int completedProcessesCount = 0;

        // Below is the main while loop that will execute processes on priority and Round Robin on equal priority.
        while (completedProcessesCount < processesCount) {
            int highestPriorityProcessId = -1;
            int highestPriority = Integer.MAX_VALUE;

            // The below for will find the highest priority at this point of time.
            for (int i = 0; i < processesCount; i++) {
                if (!executed[i] && arrivalTimes[i] <= presentTime && priorities[i] < highestPriority) {
                    highestPriorityProcessId = i;
                    highestPriority = priorities[i];
                }
            }

            // If highestPriorityProcessId == -1 indicates that no process is present to execute i.e., nothing has
            // arrived.
            if (highestPriorityProcessId == -1) {
                presentTime++;
            } else {
                // If we have process that has highest priority and not executed.
                int presentPriority = priorities[highestPriorityProcessId];

                List<Integer> processesList = prioritiesMap.get(presentPriority);
                // The below code executes, if the size of the processes with priority is greater than 1. So that it
                // runs the processes with same priority in Round Robin.
                if (processesList.size() > 1) {
                    int completedSamePriorityProcessesCount = 0;
                    while (completedSamePriorityProcessesCount < processesList.size()) {
                        boolean noExecution = true;
                        // This below for loop executes the processes with same priority one by one for a quantum
                        // amount of time.
                        for (Integer processId : processesList) {
                            if (!executed[processId] && arrivalTimes[processId] <= presentTime) {
                                // The below condition checks and updates the response time.
                                responseTimes[processId] = (responseTimes[processId] == -1) ? presentTime - arrivalTimes[processId] : responseTimes[processId];

                                if (remainingBurstTimes[processId] <= quantum) {
                                    presentTime = presentTime + remainingBurstTimes[processId];
                                    turnaroundTimes[processId] = presentTime - arrivalTimes[processId];
                                    waitingTimes[processId] = turnaroundTimes[processId] - burstTimes[processId];
                                    remainingBurstTimes[processId] = 0;
                                    executed[processId] = true;
                                    completedProcessesCount++;
                                    completedSamePriorityProcessesCount++;
                                    noExecution = false;
                                } else {
                                    presentTime += quantum;
                                    remainingBurstTimes[processId] = remainingBurstTimes[processId] - quantum;
                                    noExecution = false;
                                }
                            }
                        }

                        if (noExecution) {
                            break;
                        }
                    }
                } else {
                    // This else block will execute, when there is only one process that has a unique priority.
                    // The below condition checks and updates the response time.
                    responseTimes[highestPriorityProcessId] = (responseTimes[highestPriorityProcessId] == -1) ? presentTime - arrivalTimes[highestPriorityProcessId] : responseTimes[highestPriorityProcessId];
                    // Updates the present time with burst times of the process that being executed.
                    presentTime = presentTime + remainingBurstTimes[highestPriorityProcessId];
                    // Calculating the Turnaround time.
                    turnaroundTimes[highestPriorityProcessId] = presentTime - arrivalTimes[highestPriorityProcessId];
                    // Calculating the waiting time.
                    waitingTimes[highestPriorityProcessId] = presentTime - arrivalTimes[highestPriorityProcessId] - burstTimes[highestPriorityProcessId];
                    remainingBurstTimes[highestPriorityProcessId] = 0;
                    executed[highestPriorityProcessId] = true;
                    completedProcessesCount++;
                }
            }
        }

        // Calculate average values
        double avgTurnaroundTime = Arrays.stream(turnaroundTimes).asDoubleStream().average().orElse(0.0);
        double avgResponseTime = Arrays.stream(responseTimes).asDoubleStream().average().orElse(0.0);
        double avgWaitingTime = Arrays.stream(waitingTimes).asDoubleStream().average().orElse(0.0);

        return new EvaluationCriteria("Priority Round Robin", avgWaitingTime, avgResponseTime, avgTurnaroundTime);
    }

    private static void compareAndPrint(int testCaseNumber, int processesCount, EvaluationCriteria fcfsCriteria, EvaluationCriteria sjfCriteria, EvaluationCriteria priorityCriteria, EvaluationCriteria rrCriteria, EvaluationCriteria priorityRrCriteria, PrintWriter filePrintWriter) {
        // Finds minimum avg response time among averageResponseTimes.
        List<Double> averageResponseTimes = Arrays.asList(fcfsCriteria.getAverageResponseTime(), sjfCriteria.getAverageResponseTime(), priorityCriteria.getAverageResponseTime(), rrCriteria.getAverageResponseTime(), priorityRrCriteria.getAverageResponseTime());
        Double minAverageResponseTime = Collections.min(averageResponseTimes);

        // Finds minimum avg turn around time among averageTurnAroundTimes.
        List<Double> averageTurnAroundTimes = Arrays.asList(fcfsCriteria.getAverageTurnAroundTime(), sjfCriteria.getAverageTurnAroundTime(), priorityCriteria.getAverageTurnAroundTime(), rrCriteria.getAverageTurnAroundTime(), priorityRrCriteria.getAverageTurnAroundTime());
        Double minAverageTurnAroundTime = Collections.min(averageTurnAroundTimes);

        // Finds minimum avg waiting time among averageWaitingTimes.
        List<Double> averageWaitingTimes = Arrays.asList(fcfsCriteria.getAverageWaitingTime(), sjfCriteria.getAverageWaitingTime(), priorityCriteria.getAverageWaitingTime(), rrCriteria.getAverageWaitingTime(), priorityRrCriteria.getAverageWaitingTime());
        Double minAverageWaitingTime = Collections.min(averageWaitingTimes);

        testCaseNumber = testCaseNumber + 1;

        if(testCaseNumber == 0) {
            printAndSave("********************************************************************* Overall Test Cases Average Start *********************************************************************",
                    filePrintWriter);
        } else {
            printAndSave("********************************************************************* Test Case " + testCaseNumber + " for " + processesCount + " processes Start *********************************************************************", filePrintWriter);
            printAndSave("Test Case " + testCaseNumber, filePrintWriter);
            printAndSave("\nNumber of Processes: " + processesCount, filePrintWriter);
        }

        // Printing the performance metrics individually.
        printAndSave("\nPerformance Metrics:\n", filePrintWriter);
        List<EvaluationCriteria> evaluationCriteriaList = Arrays.asList(fcfsCriteria, sjfCriteria, priorityCriteria, rrCriteria, priorityRrCriteria);
        for (EvaluationCriteria evaluationCriteria : evaluationCriteriaList) {
            printAndSave("\n" + evaluationCriteria.getAlgorithm() + " Scheduling Algorithm:\n", filePrintWriter);
            printAndSave("Average Waiting Time: " + evaluationCriteria.getAverageWaitingTime(), filePrintWriter);
            printAndSave("Average Turnaround Time: " + evaluationCriteria.getAverageTurnAroundTime(), filePrintWriter);
            printAndSave("Average Response Time: " + evaluationCriteria.getAverageResponseTime(), filePrintWriter);
        }

        printAndSave("\nBest Performing Algorithm(s) based on Metrics:\n", filePrintWriter);

        // The below code prints the best algorithm that has minimum avg response time, turnaround time, waiting time.

        printAndSave("\nAverage Response Time:\n", filePrintWriter);
        if (fcfsCriteria.getAverageResponseTime() == minAverageResponseTime) {
            printAndSave("\t* FCFS (First-Come-First-Serve)", filePrintWriter);
        }
        if (sjfCriteria.getAverageResponseTime() == minAverageResponseTime) {
            printAndSave("\t* SJF (Shortest Job First)", filePrintWriter);
        }
        if (priorityCriteria.getAverageResponseTime() == minAverageResponseTime) {
            printAndSave("\t* Priority Scheduling", filePrintWriter);
        }
        if (rrCriteria.getAverageResponseTime() == minAverageResponseTime) {
            printAndSave("\t* Round Robin", filePrintWriter);
        }
        if (priorityRrCriteria.getAverageResponseTime() == minAverageResponseTime) {
            printAndSave("\t* Priority Round Robin", filePrintWriter);
        }

        printAndSave("\nAverage Waiting Time:\n", filePrintWriter);
        if (fcfsCriteria.getAverageWaitingTime() == minAverageWaitingTime) {
            printAndSave("\t* FCFS (First-Come-First-Serve)", filePrintWriter);
        }
        if (sjfCriteria.getAverageWaitingTime() == minAverageWaitingTime) {
            printAndSave("\t* SJF (Shortest Job First)", filePrintWriter);
        }
        if (priorityCriteria.getAverageWaitingTime() == minAverageWaitingTime) {
            printAndSave("\t* Priority Scheduling", filePrintWriter);
        }
        if (rrCriteria.getAverageWaitingTime() == minAverageWaitingTime) {
            printAndSave("\t* Round Robin", filePrintWriter);
        }
        if (priorityRrCriteria.getAverageWaitingTime() == minAverageWaitingTime) {
            printAndSave("\t* Priority Round Robin", filePrintWriter);
        }

        printAndSave("\nAverage Turnaround Time:\n", filePrintWriter);
        if (fcfsCriteria.getAverageTurnAroundTime() == minAverageTurnAroundTime) {
            printAndSave("\t* FCFS (First-Come-First-Serve)", filePrintWriter);
        }
        if (sjfCriteria.getAverageTurnAroundTime() == minAverageTurnAroundTime) {
            printAndSave("\t* SJF (Shortest Job First)", filePrintWriter);
        }
        if (priorityCriteria.getAverageTurnAroundTime() == minAverageTurnAroundTime) {
            printAndSave("\t* Priority Scheduling", filePrintWriter);
        }
        if (rrCriteria.getAverageTurnAroundTime() == minAverageTurnAroundTime) {
            printAndSave("\t* Round Robin", filePrintWriter);
        }
        if (priorityRrCriteria.getAverageTurnAroundTime() == minAverageTurnAroundTime) {
            printAndSave("\t* Priority Round Robin", filePrintWriter);
        }

        if(testCaseNumber == 0) {
            printAndSave("********************************************************************* Overall Test Cases " +
                            "Average Finish *********************************************************************",
                    filePrintWriter);
        } else {
            printAndSave("********************************************************************* Test Case " + testCaseNumber + " for " + processesCount + " processes Finish *********************************************************************", filePrintWriter);
        }
    }

    private static void printAndSave(String message, PrintWriter filePrintWriter) {
        filePrintWriter.println(message);
        System.out.println(message);
    }

    private static void printAndSaveAverages(Map<Integer, Map<String, List<EvaluationCriteria>>> ecMap, PrintWriter filePrintWriter) {
        Map<String, EvaluationCriteria> overallEvaluationCriterias = new HashMap<>();
        for (Map.Entry<Integer, Map<String, List<EvaluationCriteria>>> algos : ecMap.entrySet()) {
            for (Map.Entry<String, List<EvaluationCriteria>> ec : algos.getValue().entrySet()) {
                double averageWaitingTime = ec.getValue().stream().map(EvaluationCriteria::getAverageWaitingTime).mapToDouble(Double::doubleValue).average().orElse(0.0);
                double averageResponseTime = ec.getValue().stream().map(EvaluationCriteria::getAverageResponseTime).mapToDouble(Double::doubleValue).average().orElse(0.0);
                double averageTurnAroundTime = ec.getValue().stream().map(EvaluationCriteria::getAverageTurnAroundTime).mapToDouble(Double::doubleValue).average().orElse(0.0);

                overallEvaluationCriterias.put(ec.getKey(), new EvaluationCriteria(ec.getKey(), averageWaitingTime, averageResponseTime, averageTurnAroundTime));
            }
        }

        compareAndPrint(-1, 0, overallEvaluationCriterias.get("fcfs"), overallEvaluationCriterias.get("sjf"),
                overallEvaluationCriterias.get("priority"), overallEvaluationCriterias.get("rr"), overallEvaluationCriterias.get("priorityrr"), filePrintWriter);
    }
}

class TestCase {
    private final int processesCount;
    private final int[] burstTimes;
    private final int[] arrivalTimes;
    private final int[] priorities;
    private final int quantum;

    public TestCase(int processesCount, int[] burstTimes, int[] arrivalTimes, int[] priorities, int quantum) {
        this.processesCount = processesCount;
        this.burstTimes = burstTimes;
        this.arrivalTimes = arrivalTimes;
        this.priorities = priorities;
        this.quantum = quantum;
    }

    public int getProcessesCount() {
        return processesCount;
    }

    public int[] getBurstTimes() {
        return burstTimes;
    }

    public int[] getArrivalTimes() {
        return arrivalTimes;
    }

    public int[] getPriorities() {
        return priorities;
    }

    public int getQuantum() {
        return quantum;
    }
}

class EvaluationCriteria {

    private final String algorithm;
    private final double averageWaitingTime;
    private final double averageResponseTime;
    private final double averageTurnAroundTime;

    public EvaluationCriteria(String algorithm, double averageWaitingTime, double averageResponseTime, double averageTurnAroundTime) {
        this.algorithm = algorithm;
        this.averageWaitingTime = averageWaitingTime;
        this.averageResponseTime = averageResponseTime;
        this.averageTurnAroundTime = averageTurnAroundTime;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public double getAverageWaitingTime() {
        return averageWaitingTime;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public double getAverageTurnAroundTime() {
        return averageTurnAroundTime;
    }
}