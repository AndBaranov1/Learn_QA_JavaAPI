import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MakingSimpleAPIRequestsTest {

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
}
