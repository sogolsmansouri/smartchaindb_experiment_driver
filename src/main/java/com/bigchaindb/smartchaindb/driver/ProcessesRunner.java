package com.bigchaindb.smartchaindb.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProcessesRunner {

    public static void main(String[] args) {
        String className = "com.bigchaindb.smartchaindb.driver.BigchainDBJavaDriver";
        int numberOfProcesses = 3; // Number of SimulationRunner instances to launch

        try {
            runMultipleProcesses(className, numberOfProcesses);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error while launching processes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Launches multiple Java processes to run the specified class.
     *
     * @param className         The fully-qualified name of the class to run.
     * @param numberOfProcesses The number of processes to launch.
     * @throws IOException          If an I/O error occurs when starting the processes.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    public static void runMultipleProcesses(String className, int numberOfProcesses)
            throws IOException, InterruptedException {

        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty.");
        }

        if (numberOfProcesses <= 0) {
            throw new IllegalArgumentException("Number of processes must be greater than zero.");
        }

        List<Process> processes = new ArrayList<>(numberOfProcesses);

        // Build the command for launching the SimulationRunner
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-cp");
        command.add("."); // Current directory as classpath
        command.add(className);

        // Launch each process
        for (int i = 0; i < numberOfProcesses; i++) {
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Optionally, set the working directory if needed
            // processBuilder.directory(new File("path/to/working/directory"));

            // Redirect output and error streams to the parent process
            processBuilder.inheritIO();

            Process process = processBuilder.start();
            processes.add(process);

            System.out.println("Started SimulationRunner process " + (i + 1));
        }

        // Wait for all processes to complete
        for (int i = 0; i < processes.size(); i++) {
            Process process = processes.get(i);
            int exitCode = process.waitFor();
            System.out.println("SimulationRunner process " + (i + 1) + " finished with exit code: " + exitCode);
        }

        System.out.println("All SimulationRunner processes have completed.");
    }
}
