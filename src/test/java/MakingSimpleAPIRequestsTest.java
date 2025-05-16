import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MakingSimpleAPIRequestsTest {

    public static final String STATUS_READY = "Job is ready";
    public static final String STATUS_NOT_READY = "Job is NOT ready";

    @Test
    public void testRestAssuredPars() {
        Map<String, String> params = new HashMap<>();
        params.put("message", "And this is a second message");
        JsonPath response = RestAssured
                .given()
                .queryParams(params)
                .get("https://playground.learnqa.ru/api/get_json_homework")
                .jsonPath();
        response.prettyPrint();

        String secondMessage = response.getString("messages[1].message");
        System.out.println("Второе сообщение: " + secondMessage);
    }

    @Test
    public void testRestAssuredRedirect() {
        Response response = RestAssured
                .given()
                .redirects()
                .follow(false)
                .when()
                .get("https://playground.learnqa.ru/api/long_redirect")
                .andReturn();
        response.prettyPrint();

        String locationHeader = response.getHeader("Location");
        System.out.println(locationHeader);
    }

    @Test
    public void testRestAssuredLongRedirect() {
        String currentUrl = "https://playground.learnqa.ru/api/long_redirect";
        int redirectCount = 0;

        while (true) {
            Response response = RestAssured
                    .given()
                    .redirects().follow(false)
                    .when()
                    .get(currentUrl)
                    .andReturn();

            String location = response.getHeader("Location");

            if (location == null) {
                System.out.println("Финальный URL: " + currentUrl);
                System.out.println("Общее количество редиректов: " + redirectCount);
                break;
            }

            // Переход на следующий редирект
            currentUrl = location;
            redirectCount++;
        }
    }

    @Test
    public void testRestAssuredLongTokenRedirect() throws InterruptedException {
        JsonPath createTask = RestAssured
                .get("https://playground.learnqa.ru/ajax/api/longtime_job")
                .jsonPath();
        createTask.prettyPrint();

        String token = createTask.getString("token");
        int wait = createTask.getInt("seconds");

        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        Response createTaskWithToken = RestAssured
                .given()
                .queryParams(params)
                .get("https://playground.learnqa.ru/ajax/api/longtime_job")
                .andReturn();
        createTaskWithToken.prettyPrint();
        String status = createTaskWithToken.jsonPath().getString("status");
        assertEquals(status, STATUS_NOT_READY, "Ожидается статус " + STATUS_NOT_READY + "до выполнения задачи");

        System.out.println("Ждём " + wait + " секунд...");
        Thread.sleep(wait * 1000L);

        Response createTaskReady = RestAssured
                .given()
                .queryParams(params)
                .get("https://playground.learnqa.ru/ajax/api/longtime_job")
                .andReturn();
        createTaskReady.prettyPrint();

        String statusReady = createTaskReady.jsonPath().getString("status");
        int result = createTaskReady.jsonPath().getInt("result");
        assertEquals(statusReady, STATUS_READY, "Ожидается статус " + STATUS_READY + "после выполнения задачи");
        assertEquals(result, 42, "Результат 42 после выполнения задачи");
    }
}
