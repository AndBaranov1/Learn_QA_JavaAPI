import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @Test
    public void testRestAssuredPasswordSelection() throws IOException {
        // Получаем список паролей
        Document document = Jsoup.connect("https://en.wikipedia.org/wiki/List_of_the_most_common_passwords").get();
        Elements tableRows = document.select("#mw-content-text > div.mw-content-ltr.mw-parser-output > table:nth-child(11) > tbody");

        // Проходим по всем строкам таблицы, находим ячейки align=left и добавляем в коллекцию.
        // Коллекция содержит уникальные значения
        Set<String> passwordSet = new HashSet<>();
        for (Element row : tableRows) {
            Elements tdElements = row.select("td[align=left]");

            for (Element td : tdElements) {
                String password = td.text().trim();
                if (!password.isEmpty()) {
                    passwordSet.add(password);
                }
            }
        }

        // Каждый пароль подставляем в запрос и получаем cookie
       for (String password : passwordSet) {
           Map<String, String> credentials = new HashMap<>();
           credentials.put("login", "super_admin");
           credentials.put("password", password);
           // Запрос авторизации
           Response response = RestAssured
                   .given()
                   .body(credentials)
                   .when()
                   .post("https://playground.learnqa.ru/ajax/api/get_secret_password_homework")
                   .andReturn();
           String responseCookies= response.getCookie("auth_cookie");

           // Полученную cookie передаем в метод для проверки
           Map<String, String> cookies = new HashMap<>();
           cookies.put("auth_cookie", responseCookies);
           Response responseForCheck = RestAssured
                   .given()
                   .body(credentials)
                   .cookies(cookies)
                   .when()
                   .post("https://playground.learnqa.ru/api/check_auth_cookie")
                   .andReturn();

           String answer = responseForCheck.asString();
           if (!answer.equals("You are NOT authorized"))  {
               System.out.println("Верный пароль: " + password);
               System.out.println("Ответ: " + answer);
               break;
           }
        }
    }
}
