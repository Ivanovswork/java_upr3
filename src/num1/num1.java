package num1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class FileAnalyzer implements Runnable {
    private String filePath;
    private Map<String, Double> totalSalaries;
    private Map<String, Integer> employeeCounts;

    public FileAnalyzer(String filePath, Map<String, Double> totalSalaries, Map<String, Integer> employeeCounts) {
        this.filePath = filePath;
        this.totalSalaries = totalSalaries;
        this.employeeCounts = employeeCounts;
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            double totalSalary = 0.0;
            int count = 0;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    double salary = Double.parseDouble(parts[2].trim());

                    totalSalary += salary;
                    count++;
                }
            }

            synchronized (totalSalaries) {
                totalSalaries.put(filePath, totalSalary);
            }
            synchronized (employeeCounts) {
                employeeCounts.put(filePath, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class EmployeeSalaryAnalyzer {
    public static void main(String[] args) {
        String[] filePaths = {
                "C:\\Users\\desti\\IdeaProjects\\untitled2\\src\\num1\\file1.txt",
                "C:\\Users\\desti\\IdeaProjects\\untitled2\\src\\num1\\file2.txt",
                "C:\\Users\\desti\\IdeaProjects\\untitled2\\src\\num1\\file3.txt"};
        Map<String, Double> totalSalaries = new HashMap<>();
        Map<String, Integer> employeeCounts = new HashMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(filePaths.length);

        for (String filePath : filePaths) {
            FileAnalyzer analyzer = new FileAnalyzer(filePath, totalSalaries, employeeCounts);
            executorService.submit(analyzer);
        }

        executorService.shutdown();
        while (!executorService.isTerminated()) {}

        for (String filePath : totalSalaries.keySet()) {
            System.out.println("Файл: " + filePath);
            System.out.println("Общая зарплата: " + totalSalaries.get(filePath));
            System.out.println("Количество сотрудников: " + employeeCounts.get(filePath));
            System.out.println();
        }
    }
}