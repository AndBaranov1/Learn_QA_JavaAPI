import io.restassured.RestAssured;
import io.restassured.http.Headers;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserAuthTest {

    String cookie;
    String header;
    int userIdOnAuth;

    @BeforeEach
    public void loginUser() {
        // Авторизация пользователя
        Map<String, String> authData = new HashMap<>();
        authData.put("email", "vinkotov@example.com");
        authData.put("password", "1234");

        Response responseGetAuth = RestAssured
                .given()
                .body(authData)
                .post("https://playground.learnqa.ru/api/user/login")
                .andReturn();
        this.cookie = responseGetAuth.getCookie("auth_sid");
        this.header = responseGetAuth.getHeader("x-csrf-token");
        this.userIdOnAuth = responseGetAuth.jsonPath().getInt("user_id");
    }


    @Test
    public void testAuthUser() {
        // Проверка успешной авторизации пользователя
        JsonPath responseGCheckAuth = RestAssured
                .given()
                .header("x-csrf-token", this.header)
                .cookie("auth_sid", this.cookie)
                .get("https://playground.learnqa.ru/api/user/auth")
                .jsonPath();

        int userIdOnCheck = responseGCheckAuth.getInt("user_id");
        assertTrue(userIdOnCheck > 0, "Unexpected user id " + userIdOnCheck);
        assertEquals(userIdOnAuth, userIdOnCheck, "user id from auth request is not equal to user_id from check request");
    }

    @ParameterizedTest
    @ValueSource(strings = {"cookie", "headers"})
    public void testNegativeAuthUser(String condition) {
        RequestSpecification spec = RestAssured.given();
        spec.baseUri("https://playground.learnqa.ru/api/user/auth");

        if (condition.equals("cookie")) {
            spec.cookie("auth_sid", this.cookie);
        } else if (condition.equals("headers")) {
            spec.header("x-csrf-token", this.header);
        } else {
            throw new IllegalArgumentException("Condition value is know: " + condition);
        }

        JsonPath responseForCheck = spec.get().jsonPath();
        assertEquals(0, responseForCheck.getInt("user_id"), "user_id should be 0 for auth request");
    }

    @Test
    public void testMethodCookie() {
        Response response = RestAssured
                .given()
                .get("https://playground.learnqa.ru/api/homework_cookie")
                .andReturn();
        assertEquals(200, response.getStatusCode(), "Unexpected status code");

        Map<String, String> cookies = response.getCookies();
        System.out.println("Вывод Cookies: " + cookies);
        assertTrue(cookies.containsValue("hw_value"), "Response doesn't have 'hw_value' cookie");
    }

    @Test
    public void testMethodHeader() {
        Response response = RestAssured
                .given()
                .get("https://playground.learnqa.ru/api/homework_header")
                .andReturn();
        assertEquals(200, response.getStatusCode(), "Unexpected status code");

        Headers headers = response.headers();
        System.out.println("Вывод headers: \n" + headers);

        String secretHeaderValue = response.getHeader("x-secret-homework-header");
        assertNotNull(secretHeaderValue, "Response doesn't have 'x-secret-homework-header'");
        assertEquals("Some secret value", secretHeaderValue, "Header 'x-secret-homework-header' does not have the expected value");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mozilla/5.0 (iPad; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"})
    public void testUserAgent(String user_agent) {
        Map<String, String> header = new HashMap<>();

        if (user_agent.length() > 0) {
            header.put("User-agent", user_agent);
        }

        JsonPath response = RestAssured
                .given()
                .headers(header)
                .get("https://playground.learnqa.ru/ajax/api/user_agent_check")
                .jsonPath();
        response.prettyPrint();

        String platform = response.getString("platform");
        if (user_agent.contains("iPad")) {
            assertEquals("Mobile", platform, "Ожидалось значение Mobile для iOS устройства");
        } else if (user_agent.contains("Windows")) {
            assertEquals("Web", platform, "Ожидалось значение Web для Windows устройства");
        }

        String browser = response.getString("browser");
        if (user_agent.contains("iPad")) {
            assertEquals("No", browser, "Ожидалось значение No для iOS устройства");
        } else if (user_agent.contains("Windows")) {
            assertEquals("Chrome", browser, "Ожидалось значение Chrome для Windows устройства");
        }

        String device = response.getString("device");
        if (user_agent.contains("iPad")) {
            assertEquals("iPhone", device, "Ожидалось значение iPhone для iOS устройства");
        } else if (user_agent.contains("Windows")) {
            assertEquals("No", device, "Ожидалось значение No для Windows устройства");
        }
    }
}
