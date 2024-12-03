import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataParsing {
    private static void createTimeElapsedFile(String testLogFile, String timeElapsedFilename) throws FileNotFoundException {
        File logFile = new File(testLogFile);
        Scanner sc = new Scanner(logFile);
        sc.nextLine();

        File timeElapsedFile = new File(timeElapsedFilename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(timeElapsedFilename, true))) {
            writer.write("Label,TodoCount,TimeElapsed");
            writer.newLine();

            while (sc.hasNextLine()){
                String[] line = sc.nextLine().split(",");

                String entry = String.format("%s,%s,%s", line[0], line[1], computeTimeElapsed(line[2], line[3]));
                writer.write(entry);
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println("An error occurred while handling the CSV file: " + e.getMessage());
        }
    }

    private static void writeResultsToFile(HashMap<String, ArrayList<String[]>> intermediate, String memFileName, String cpuFileName) throws IOException {
        writeResultsHelper(intermediate, cpuFileName, true);
        writeResultsHelper(intermediate, memFileName, false);
    }

    private static void writeResultsHelper(HashMap<String, ArrayList<String[]>> intermediate, String fileName, boolean isCPU) throws IOException {
        int counter = 0;
        File file = new File(fileName + counter + ".csv");
        boolean fileExists = file.exists();
        while (fileExists){
            counter++;
            file = new File(fileName + counter + ".csv");
            fileExists = file.exists();
        }

        String statLabel = "Average_CPU_Usage";
        int indexOfConcern = 1;
        if (!isCPU){
            statLabel = "AverageBytesAvailable";
            indexOfConcern = 2;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName + counter + ".csv", true))) {
            if (!fileExists) {
                // If file does not exist, create it with headers
                StringBuilder sb = new StringBuilder("Label");

                for (String[] entry : intermediate.get("Delete")){
                    sb.append("," + statLabel + "_" + entry[0]);
                }

                writer.write(String.valueOf(sb));
                writer.newLine();
            }

            for (String label : intermediate.keySet()){
                StringBuilder sb = new StringBuilder(label);

                for (String[] elem : intermediate.get(label)){
                    sb.append("," + elem[indexOfConcern]);
                }

                writer.write(String.valueOf(sb));
                writer.newLine();

            }
        } catch (IOException e) {
            System.err.println("An error occurred while handling the CSV file: " + e.getMessage());
        }
    }

    private static HashMap<String, ArrayList<String[]>> processRelevantChunks(HashMap<String, List<String>> processedChunks) {
        // <Label> , <to-do Count, avgCpuUse, avgBytesAvail>
        HashMap<String, ArrayList<String[]>> intermediateHash = new HashMap<>();

        for (String key : processedChunks.keySet()) {
            String[] keySplit = key.split(",");
            String label = keySplit[0];
            String todoNum = keySplit[1];

            long totalBytesAvail = 0;
            double totalCpuUsed = 0;

            for (String line : processedChunks.get(key)){
                String[] lineSplit = line.split(",");
                totalBytesAvail += Long.parseLong(lineSplit[4].strip());
                totalCpuUsed += Double.parseDouble(lineSplit[1].substring(1, lineSplit[1].length()-1));
            }

            double averageBytesAvail = (double) totalBytesAvail / processedChunks.get(key).size();
            double averageCpuUsed = totalCpuUsed / processedChunks.get(key).size();

            if (!intermediateHash.containsKey(label)) intermediateHash.put(label, new ArrayList<>());

            intermediateHash.get(label).add(new String[]{todoNum, String.valueOf(averageCpuUsed), String.valueOf(averageBytesAvail)});
        }

        for (String key : intermediateHash.keySet()){
            Collections.sort(intermediateHash.get(key), new Comparator<String[]>() {
                @Override
                public int compare(String[] o1, String[] o2) {
                    // Parse the first element of each String[] as an integer
                    return Integer.parseInt(o1[0]) - Integer.parseInt(o2[0]);
                }
            });
        }

        // Print intermediate hash
        for (String key : intermediateHash.keySet()){
            System.out.println(key);
            System.out.print("\t");
            for (String[] elem : intermediateHash.get(key)){
                System.out.print(Arrays.toString(elem));
                System.out.print(" ");
            }
            System.out.println();
        }

        return intermediateHash;
    }

    private static HashMap<String, List<String>> findCorrespondingChunks(String logFile, String analysisFile) throws FileNotFoundException {
        HashMap<String, List<String>> resultsHash = new HashMap<>();

        ArrayList<String> analysisLines = new ArrayList<>();
        // Collect all the lines of the analysis file into an arraylist
        File analysisFileObj = new File(analysisFile);
        Scanner sc = new Scanner(analysisFileObj);
        sc.nextLine();
        while (sc.hasNextLine())
            analysisLines.add(sc.nextLine());

        File logFileObj = new File(logFile);
        sc = new Scanner(logFileObj);
        sc.nextLine();
        while (sc.hasNextLine()){
            String entry = sc.nextLine();
            // entry = entry.split(",")[0] + "," + entry.split(",")[1];
            String[] entrySplit = entry.split(",");
            resultsHash.put(entrySplit[0] + "," + entrySplit[1], findRelevantChunk(entrySplit[2], entrySplit[3], analysisLines));
        }

        return resultsHash;
    }

    private static List<String> findRelevantChunk(String startTime, String endTime, ArrayList<String> dataAnalysisLines){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        // Parse the input strings into LocalDateTime objects
        long minStartDiff = Long.MAX_VALUE;
        int minStartDiffIndex = -1;
        long minEndDiff = Long.MAX_VALUE;
        int minEndDiffIndex = -1;

        for (int i=0; i<dataAnalysisLines.size(); i++){
            String entry = dataAnalysisLines.get(i).split(",")[0].strip();

            long diffFromStart = Math.abs(computeTimeElapsed(startTime, entry));
            long diffFromEnd = Math.abs(computeTimeElapsed(endTime, entry));


            if (diffFromStart < minStartDiff){
                minStartDiff = diffFromStart;
                minStartDiffIndex = i;
            }

            if (diffFromEnd < minEndDiff){
                minEndDiff = diffFromEnd;
                minEndDiffIndex = i;
            }
        }

        return dataAnalysisLines.subList(minStartDiffIndex, minEndDiffIndex+1);
    }

    private static long computeTimeElapsed(String s1, String s2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        // Parse the input strings into LocalDateTime objects
        LocalDateTime dateTime1 = LocalDateTime.parse(s1, formatter);
        LocalDateTime dateTime2 = LocalDateTime.parse(s2, formatter);

        // Compute the duration between the two dates
        Duration duration = Duration.between(dateTime1, dateTime2);

        // Return the duration in microseconds
        return duration.toNanos() / 1000;
    }

    private static void createTimePerTodo(String testLogFile, String timePerTodoFilename) throws FileNotFoundException {
        File logFile = new File(testLogFile);
        Scanner sc = new Scanner(logFile);
        sc.nextLine();

        File timeElapsedFile = new File(timePerTodoFilename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(timePerTodoFilename, true))) {
            writer.write("Label,TodoCount,TimePerTodo");
            writer.newLine();

            while (sc.hasNextLine()){
                String[] line = sc.nextLine().split(",");

                String entry = String.format("%s,%s,%f", line[0], line[1], (float) computeTimeElapsed(line[2], line[3]) / Integer.parseInt(line[1]));
                writer.write(entry);
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println("An error occurred while handling the CSV file: " + e.getMessage());
        }
    }

    private static void populateIntermediateResults() throws IOException {
        String base = "/Users/danielarturi/Desktop/McGill Fall 2024/ECSE 429/Project/PartC/ECSE429_PartC/";

        String dynamicAnalysisBase = base + "intermediate_findings/dynamicAnalysisLog/";
        String[] dynamicAnalysisFileNames = new String[]{"raw_log_data4.csv", "raw_log_data5.csv", "raw_log_data6.csv",
                "raw_log_data7.csv", "raw_log_data8.csv"};
        String runLogBase = base + "intermediate_findings/run_log/";
        String[] runLogFileNames = new String[]{"log11.csv", "log12.csv", "log14.csv", "log15.csv", "log16.csv"};

        String timeElapsed = base + "intermediate_findings/intermediate_time_elapsed/timeElapsed_";
        String timePerTodo = base + "intermediate_findings/intermediate_time_per_todo/time_per_todo_";
        String cpuUsage = base + "intermediate_findings/intermediate_cpu_usage/cpu_usage_";
        String memUsage = base + "intermediate_findings/intermediate_mem_usage/mem_usage_";

        for (int i=0; i<5; i++){
            String dynamicAnalysisFile = dynamicAnalysisBase + dynamicAnalysisFileNames[i];
            String logFile = runLogBase + runLogFileNames[i];

            createTimeElapsedFile(logFile, timeElapsed + i + ".csv");
            createTimePerTodo(logFile, timePerTodo + i + ".csv");

            HashMap<String, List<String>> result = findCorrespondingChunks(logFile, dynamicAnalysisFile);
            writeResultsToFile(processRelevantChunks(result),
                    memUsage, cpuUsage);
        }

        System.out.println("Done");
    }

    private static void createFinalFindingsFiles() throws FileNotFoundException {
        String base = "/Users/danielarturi/Desktop/McGill Fall 2024/ECSE 429/Project/PartC/ECSE429_PartC/";
        String intermediate_base = base + "intermediate_findings/";
        String cpu_usage_base = intermediate_base + "intermediate_cpu_usage/cpu_usage_";
        String mem_usage_base = intermediate_base + "intermediate_mem_usage/mem_usage_";
        String time_per_base = intermediate_base + "intermediate_time_per_todo/time_per_todo_";
        String time_elapsed_base = intermediate_base + "intermediate_time_elapsed/timeElapsed_";

        finalFindingsCPUUsage(cpu_usage_base, base + "final_findings/final_cpu_usage.csv");
        finalFindingsMemUsage(mem_usage_base, base + "final_findings/final_mem_usage.csv");
        finalTimePerFindings(time_per_base, base + "final_findings/final_time_per_todo.csv");
        finalTimeElapsedFindings(time_elapsed_base, base + "final_findings/final_time_elapsed.csv");
    }

    private static void finalFindingsCPUUsage(String cpu_usage_base, String destination) throws FileNotFoundException {
        HashMap<String, double[]> summedAggregate = new HashMap<>();

        String header = "";
        int itemCount = 1;

        // Read in data
        for (int i=0; i<5; i++){
            File file = new File(cpu_usage_base + i + ".csv");
            Scanner sc = new Scanner(file);
            header = sc.nextLine();

            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split(",");

                String label = line[0];
                itemCount = line.length - 1;
                if (!summedAggregate.containsKey(label)) summedAggregate.put(label, new double[itemCount]);

                for (int j = 1; j < line.length; j++)
                    summedAggregate.get(label)[j - 1] += Double.parseDouble(line[j]);
            }
        }

        // Transform to get averages
        for (String key : summedAggregate.keySet()){
            double[] elem = summedAggregate.get(key);
            for (int i=0; i<elem.length; i++){
                elem[i] = elem[i] / 5;
            }
        }

        // Write data to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination, true))) {
            writer.write(header);
            writer.newLine();

            for (String label : summedAggregate.keySet()){
                StringBuilder sb = new StringBuilder(label);

                for (double d : summedAggregate.get(label)){
                    sb.append(",");
                    sb.append(String.format("%.2f", d));
                }

                writer.write(String.valueOf(sb));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("An error occurred while handling the CSV file: " + e.getMessage());
        }
    }

    private static long parseOddBytes(String oddMemory){
        String[] oddMemSplit = oddMemory.split("\\.");
        String[] secondBit = oddMemSplit[1].split("E");
        return (long) (Long.parseLong(oddMemSplit[0]) * Math.pow(10, Integer.parseInt(secondBit[1])) + Long.parseLong(secondBit[0]));
    }

    private static void finalFindingsMemUsage(String mem_usage_base, String destination) throws FileNotFoundException {
        HashMap<String, long[]> summedAggregate = new HashMap<>();

        String header = "";
        int itemCount = 1;

        // Read in data
        for (int i=0; i<5; i++){
            File file = new File(mem_usage_base + i + ".csv");
            Scanner sc = new Scanner(file);
            header = sc.nextLine();

            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split(",");

                String label = line[0];
                itemCount = line.length - 1;
                if (!summedAggregate.containsKey(label)) summedAggregate.put(label, new long[itemCount]);

                for (int j = 1; j < line.length; j++)
                    summedAggregate.get(label)[j - 1] += parseOddBytes(line[j]);
            }
        }

        // Transform to get averages
        for (String key : summedAggregate.keySet()){
            long[] elem = summedAggregate.get(key);
            for (int i=0; i<elem.length; i++){
                elem[i] = elem[i] / 5;
            }
        }

        // Write data to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination, true))) {
            writer.write(header);
            writer.newLine();

            for (String label : summedAggregate.keySet()){
                StringBuilder sb = new StringBuilder(label);

                for (long d : summedAggregate.get(label)){
                    sb.append(",");
                    sb.append(d);
                }

                writer.write(String.valueOf(sb));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("An error occurred while handling the CSV file: " + e.getMessage());
        }
    }

    private static void finalTimeElapsedFindings(String time_elapsed_base, String destination) throws FileNotFoundException {
        // <label, <to_do_count, summed_time>>
        HashMap<String, HashMap<String, Long>> aggregate = new HashMap<>();

        String header = "";
        int itemCount = 1;

        // Read in data
        for (int i=0; i<5; i++){
            File file = new File(time_elapsed_base + i + ".csv");
            Scanner sc = new Scanner(file);
            header = sc.nextLine();

            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split(",");
                if (!aggregate.containsKey(line[0])) aggregate.put(line[0], new HashMap<>());
                if (!aggregate.get(line[0]).containsKey(line[1])) aggregate.get(line[0]).put(line[1], 0L);

                aggregate.get(line[0]).put(line[1], Long.parseLong(line[2]) + aggregate.get(line[0]).get(line[1]));
            }
        }

        // Transform to get averages
        HashMap<String, ArrayList<Long[]>> finalHash = new HashMap<>();
        for (String key : aggregate.keySet()){
            finalHash.put(key, new ArrayList<>());
            for (String subKey : aggregate.get(key).keySet()){
                finalHash.get(key).add(new Long[]{Long.parseLong(subKey), aggregate.get(key).get(subKey) / 5});
            }

            Collections.sort(finalHash.get(key), new Comparator<Long[]>() {
                @Override
                public int compare(Long[] o1, Long[] o2) {
                    // Parse the first element of each String[] as an integer
                    return o1[0].compareTo(o2[0]);
                }
            });
        }

        // Write to destination
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination, true))) {
            writer.write(header);
            writer.newLine();

            for (String label : finalHash.keySet()){
                StringBuilder sb = new StringBuilder(label);

                for (Long[] d : finalHash.get(label)){
                    sb.append(",");
                    sb.append(String.format("%d", d[1]));
                }

                writer.write(String.valueOf(sb));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("An error occurred while handling the CSV file: " + e.getMessage());
        }

    }

    private static void finalTimePerFindings(String time_per_base, String destination) throws FileNotFoundException {
        // <label, <to_do_count, summed_time>>
        HashMap<String, HashMap<String, Double>> aggregate = new HashMap<>();

        String header = "";
        int itemCount = 1;

        // Read in data
        for (int i=0; i<5; i++){
            File file = new File(time_per_base + i + ".csv");
            Scanner sc = new Scanner(file);
            header = sc.nextLine();

            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split(",");
                if (!aggregate.containsKey(line[0])) aggregate.put(line[0], new HashMap<>());
                if (!aggregate.get(line[0]).containsKey(line[1])) aggregate.get(line[0]).put(line[1], 0.);

                aggregate.get(line[0]).put(line[1], Double.parseDouble(line[2]) + aggregate.get(line[0]).get(line[1]));
            }
        }

        // Transform to get averages
        HashMap<String, ArrayList<Double[]>> finalHash = new HashMap<>();
        for (String key : aggregate.keySet()){
            finalHash.put(key, new ArrayList<>());
            for (String subKey : aggregate.get(key).keySet()){
                finalHash.get(key).add(new Double[]{Double.parseDouble(subKey), aggregate.get(key).get(subKey) / 5});
            }

            Collections.sort(finalHash.get(key), new Comparator<Double[]>() {
                @Override
                public int compare(Double[] o1, Double[] o2) {
                    // Parse the first element of each String[] as an integer
                    return o1[0].compareTo(o2[0]);
                }
            });
        }

        // Write to destination
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination, true))) {
            writer.write(header);
            writer.newLine();

            for (String label : finalHash.keySet()){
                StringBuilder sb = new StringBuilder(label);

                for (Double[] d : finalHash.get(label)){
                    sb.append(",");
                    sb.append(String.format("%.2f", d[1]));
                }

                writer.write(String.valueOf(sb));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("An error occurred while handling the CSV file: " + e.getMessage());
        }

    }

    public static void main(String[] args) throws IOException {
        createFinalFindingsFiles();
    }


}
