import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

public class Tests {
    static HashMap<String, HashMap<String, Object>> id_list = new HashMap<>();
    static ArrayList<String> strictly_ids = new ArrayList<>();

    // TEST BUILDING BLOCKS

    // Create One To-do
    private static String createTodo(HashMap<String, Object> commandBody){
        final Response body = given().body(commandBody).
                when().post("/todos").
                then().
                statusCode(201).
                contentType(ContentType.JSON).
                and().extract().response();

        return body.jsonPath().get("id");
        //return "";
    }

    // Delete One To-do
    private static void deleteOneTodo(String id){
        given().body("").
                when().delete("/todos/" + id).
                then().
                statusCode(200).
                contentType(ContentType.JSON).
                and().extract().response();
    }

    // Update One To-do (Put)
    private static void updateOneTodo_Put(String id, HashMap<String, Object> update_contents){
        given().body(update_contents).
                when().put("/todos/" + id).
                then().
                statusCode(200).
                contentType(ContentType.JSON).
                and().extract().response();
    }

    // Update One To-do (Post)
    private static void updateOneTodo_Post(String id, HashMap<String, Object> updateBody){
        given().body(updateBody).
                when().post("/todos/" + id).
                then().
                statusCode(200).
                contentType(ContentType.JSON).
                and().extract().response();
    }

    // ENVIRONMENT SETUP
    private static void setup_n_todos(int n){
        // Clear to-do's
        RestAssured.baseURI = "http://localhost:4567";

        when().post("/admin/data/thingifier")
                .then().statusCode(200);

        final JsonPath clearedData = when().get("/todos")
                .then().statusCode(200).extract().body().jsonPath();

        final int newNumberOfTodos = clearedData.getList("todos").size();

        Assertions.assertEquals(0, newNumberOfTodos);

        id_list.clear();
        strictly_ids.clear();

        // Create n todos
        HashMap<String, Object> create_hash;
        for (int i=0; i<n; i++){
            create_hash = new HashMap<>();
            create_hash.put("title", "baseline_title_" + i);
            create_hash.put("doneStatus", i % 2 == 0);
            create_hash.put("description", "baseline_description_" + i);
            String id = createTodo(create_hash);
            id_list.put(id, create_hash);
            strictly_ids.add(id);
        }
    }

    // n operation functionality
    private static String[] create_n_todos(ArrayList<HashMap<String, Object>> todoContents){
        // Get start time
        // long startTime = System.nanoTime();
        LocalDateTime startTime = LocalDateTime.now();

        // Create n todos
        for (HashMap<String, Object> contents : todoContents)
            createTodo(contents);

        // Get end time
        // long endTime = System.nanoTime();
        LocalDateTime endTime = LocalDateTime.now();

        // return elapsed time
        // return endTime - startTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        String formattedStartTime = startTime.format(formatter);
        String formattedEndTime = endTime.format(formatter);

        return new String[]{formattedStartTime, formattedEndTime};

        // TODO: Convey information about created todos (id's)
    }

    private static String[] delete_n_todos(ArrayList<String> idsToDelete){
        // Get start time
        // long startTime = System.nanoTime();
        LocalDateTime startTime = LocalDateTime.now();

        // delete n todos
        for (String id : idsToDelete)
            deleteOneTodo(id);

        // Get end time
        // long endTime = System.nanoTime();
        LocalDateTime endTime = LocalDateTime.now();

        // return elapsed time

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        String formattedStartTime = startTime.format(formatter);
        String formattedEndTime = endTime.format(formatter);

        return new String[]{formattedStartTime, formattedEndTime};
    }

    private static String[] update_n_todos_put(ArrayList<String> idsToUpdate, HashMap<String, HashMap<String, Object>> todoContents){
        // Get start time
        // long startTime = System.nanoTime();
        LocalDateTime startTime = LocalDateTime.now();

        // delete n todos
        for (String id : idsToUpdate)
            updateOneTodo_Put(id, todoContents.get(id));

        // Get end time
        // long endTime = System.nanoTime();
        LocalDateTime endTime = LocalDateTime.now();

        // return elapsed time
        // return endTime - startTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        String formattedStartTime = startTime.format(formatter);
        String formattedEndTime = endTime.format(formatter);

        return new String[]{formattedStartTime, formattedEndTime};
    }

    private static String[] update_n_todos_post(ArrayList<String> idsToUpdate, HashMap<String, HashMap<String, Object>> todoContents){
        // Get start time
        // long startTime = System.nanoTime();
        LocalDateTime startTime = LocalDateTime.now();

        // delete n todos
        for (String id : idsToUpdate)
            updateOneTodo_Post(id, todoContents.get(id));

        // Get end time
        // long endTime = System.nanoTime();
        LocalDateTime endTime = LocalDateTime.now();

        // return elapsed time
        // return endTime - startTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        String formattedStartTime = startTime.format(formatter);
        String formattedEndTime = endTime.format(formatter);

        return new String[]{formattedStartTime, formattedEndTime};
    }
    private static String[] create_n_todos_test(int n, boolean allFields){
        // Clear todos
        setup_n_todos(0);

        ArrayList<HashMap<String, Object>> todoContents = new ArrayList<>();
        HashMap<String, Object> create_hash;
        for (int i=0; i<n; i++){
            create_hash = new HashMap<>();
            create_hash.put("title", generateRandomString(10));
            if (allFields){
                create_hash.put("doneStatus", i % 2 == 0);
                create_hash.put("description", generateRandomString(10));
            }
            todoContents.add(create_hash);
        }

        // Return time taken to create all n todos
        return create_n_todos(todoContents);
    }
    private static String[] delete_n_todos_test(int n){
        // Create n todos
        setup_n_todos(n);

        // Return time taken to delete all n todos
        return delete_n_todos(strictly_ids);
    }
    private static String[] update_n_todos_put_test(int n, boolean allFields){
        // Create n todos
        setup_n_todos(n);

        // Create update bodies
        HashMap<String, HashMap<String, Object>> todoContents = new HashMap<>();
        HashMap<String, Object> create_hash;
        for (int i=0; i<n; i++){
            create_hash = new HashMap<>();
            create_hash.put("title", generateRandomString(10));
            if (allFields){
                create_hash.put("doneStatus", i % 2 == 0);
                create_hash.put("description", generateRandomString(10));
            }
            todoContents.put(strictly_ids.get(i), create_hash);
        }

        // Return time taken to update all n todos with put
        return update_n_todos_put(strictly_ids, todoContents);
    }
    private static String[] update_n_todos_post_test(int n, boolean allFields){
        // Create n todos
        setup_n_todos(n);

        // Create update bodies
        HashMap<String, HashMap<String, Object>> todoContents = new HashMap<>();
        HashMap<String, Object> create_hash;
        for (int i=0; i<n; i++){
            create_hash = new HashMap<>();
            create_hash.put("title", generateRandomString(10));
            if (allFields){
                create_hash.put("doneStatus", i % 2 == 0);
                create_hash.put("description", generateRandomString(10));
            }
            todoContents.put(strictly_ids.get(i), create_hash);
        }

        // Return time taken to update all n todos with post
        return update_n_todos_post(strictly_ids, todoContents);
    }
    private static ArrayList<String[]> aggregateCreate(ArrayList<Integer> pointsToTest, boolean allFields, int sleepBetweenCases) throws InterruptedException {
        TimeUnit.SECONDS.sleep(sleepBetweenCases);
        ArrayList<String[]> createResults = new ArrayList<>();
        for (int i : pointsToTest) {
            createResults.add(create_n_todos_test(i, allFields));
            TimeUnit.SECONDS.sleep(sleepBetweenCases);
        }
        return createResults;
    }
    private static ArrayList<String[]> aggregateDelete(ArrayList<Integer> pointsToTest, int sleepBetweenCases) throws InterruptedException {
        TimeUnit.SECONDS.sleep(sleepBetweenCases);
        ArrayList<String[]> createResults = new ArrayList<>();
        for (int i : pointsToTest) {
            createResults.add(delete_n_todos_test(i));
            TimeUnit.SECONDS.sleep(sleepBetweenCases);
        }
        return createResults;
    }
    private static ArrayList<String[]> aggregateUpdate_put(ArrayList<Integer> pointsToTest, boolean allFields, int sleepBetweenCases) throws InterruptedException {
        TimeUnit.SECONDS.sleep(sleepBetweenCases);
        ArrayList<String[]> createResults = new ArrayList<>();
        for (int i : pointsToTest) {
            createResults.add(update_n_todos_put_test(i, allFields));
            TimeUnit.SECONDS.sleep(sleepBetweenCases);
        }
        return createResults;
    }
    private static ArrayList<String[]> aggregateUpdate_post(ArrayList<Integer> pointsToTest, boolean allFields, int sleepBetweenCases) throws InterruptedException {
        ArrayList<String[]> createResults = new ArrayList<>();
        TimeUnit.SECONDS.sleep(sleepBetweenCases);
        for (int i : pointsToTest) {
            createResults.add(update_n_todos_post_test(i, allFields));
            TimeUnit.SECONDS.sleep(sleepBetweenCases);
        }
        return createResults;
    }
    private static HashMap<String, ArrayList<String[]>> performAllAggregatedTests(ArrayList<Integer> pointsToTest, int sleepBetweenCases) throws InterruptedException {
        HashMap<String, ArrayList<String[]>> allResults = new HashMap<>();
        // Create with only title
        allResults.put("Create_OnlyTitle", aggregateCreate(pointsToTest, false, sleepBetweenCases));
        // Create with all fields
        allResults.put("Create_AllFields", aggregateCreate(pointsToTest, true, sleepBetweenCases));
        // Delete
        allResults.put("Delete", aggregateDelete(pointsToTest, sleepBetweenCases));
        // Update, put, title only
        allResults.put("Update_Put_TitleOnly", aggregateUpdate_put(pointsToTest, false, sleepBetweenCases));
        // Update, put, all fields
        allResults.put("Update_Put_AllFields", aggregateUpdate_put(pointsToTest, true, sleepBetweenCases));
        // Update, post, title only
        allResults.put("Update_Post_TitleOnly", aggregateUpdate_post(pointsToTest, false, sleepBetweenCases));
        // Update, post, all fields
        allResults.put("Update_Post_AllFields", aggregateUpdate_post(pointsToTest, true, sleepBetweenCases));

        return allResults;
    }
    private static String generateRandomString(int length){
        String possibleChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder rString = new StringBuilder();
        Random r = new Random();
        while (rString.length() < length){
            int index = (int) (r.nextFloat() * possibleChars.length());
            rString.append(possibleChars.charAt(index));
        }
        return rString.toString();
    }
    private static void writeAggregateResultsToLog(String filename, ArrayList<Integer> pointsToTest, int sleepBetweenCases) throws InterruptedException {
        HashMap<String, ArrayList<String[]>> aggregateResults = performAllAggregatedTests(pointsToTest, sleepBetweenCases);

        int counter = 0;
        File file = new File(filename + counter + ".csv");
        boolean fileExists = file.exists();
        while (fileExists){
            counter++;
            file = new File(filename + counter + ".csv");
            fileExists = file.exists();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename + counter + ".csv", true))) {
            if (!fileExists) {
                // If file does not exist, create it with headers
                writer.write("Label,TodoCount,StartTime,EndTime");
                writer.newLine();
            }

            for (String label : aggregateResults.keySet()){
                ArrayList<String[]> currTimeBlock = aggregateResults.get(label);
                for (int i=0; i<pointsToTest.size(); i++){
                    String entry = String.format("%s,%d,%s,%s",
                            label, pointsToTest.get(i), currTimeBlock.get(i)[0], currTimeBlock.get(i)[1]);
                    writer.write(entry);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("An error occurred while handling the CSV file: " + e.getMessage());
        }
    }
    public static void main(String[] args) throws InterruptedException {
        ArrayList<Integer> pointsToTest = new ArrayList<>();
        pointsToTest.add(1);
        pointsToTest.add(5);
        pointsToTest.add(10);
        pointsToTest.add(25);
        pointsToTest.add(50);
        pointsToTest.add(100);
        pointsToTest.add(200);

        int timeBetweenCases = 3;
        String filename = "/Users/danielarturi/Desktop/McGill Fall 2024/ECSE 429/Project/PartC/ECSE429_PartC/intermediate_findings/run_log/log1";

        RestAssured.baseURI = "http://localhost:4567";

        when().post("/admin/data/thingifier")
                .then().statusCode(200);

        final JsonPath clearedData = when().get("/todos")
                .then().statusCode(200).extract().body().jsonPath();

        final int newNumberOfTodos = clearedData.getList("todos").size();

        Assertions.assertEquals(0, newNumberOfTodos);

        writeAggregateResultsToLog(filename, pointsToTest, timeBetweenCases);

        System.out.println("log file written");
    }
}
