package com.bigchaindb.smartchaindb.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProcessesRunner {
    public static void main(String[] args) {
        int numProcesses = getNumProcesses(args);
        
        try {
            startProcesses(numProcesses);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error while launching processes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> getCommand() {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-cp");
        command.add(System.getProperty("java.class.path")); // Use the same classpath
        command.add(BigchainDBJavaDriver.class.getName());
        return command;
    }

    public static void startProcesses(int numProcesses) throws IOException, InterruptedException {
        List<String> workerCmd = getCommand();
        List<Process> processes = new ArrayList<>(numProcesses);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Process process : processes) {
                if (process.isAlive()) {
                    process.destroy();
                    System.out.println("Terminated subprocess: " + process);
                }
            }
            System.out.println("All subprocesses terminated.");
        }));

        for (int i = 0; i < numProcesses; i++) {
            ProcessBuilder processBuilder = new ProcessBuilder(workerCmd);

            // Optionally, set the working directory if needed
            // processBuilder.directory(new File("path/to/working/directory"));

            // Redirect output and error streams to the parent process
            processBuilder.inheritIO();

            Process process = processBuilder.start();
            processes.add(process);

            System.out.println("Started BigchainDBJavaDriver process " + (i + 1) + " of " + numProcesses);
        }

        // Wait for all processes to complete
        for (int i = 0; i < processes.size(); i++) {
            int exitCode = processes.get(i).waitFor();
            System.out.println("BigchainDBJavaDriver process " + (i + 1) + " of " + numProcesses + " finished with exit code: " + exitCode);
        }

        System.out.println("All BigchainDBJavaDriver processes have completed.");
    }

    private static int getNumProcesses(String[] args) {
        int num = 1;
        if (args.length > 0) {
            try {
                int parsed = Integer.parseInt(args[0]);
                if (parsed > 0) {
                    num = parsed;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid number of processes, defaulting to 1");
            }
        }
        return num;
    }
}
