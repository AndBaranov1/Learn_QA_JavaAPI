import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
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
}
