package tests;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import lib.ApiCoreRequests;
import lib.Assertions;
import lib.BaseTestCase;
import lib.DataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class UserDeleteTest extends BaseTestCase {

    private final ApiCoreRequests apiCoreRequests = new ApiCoreRequests();

    String cookie;
    String header;
    int userIdOnAuth;

    @Test
    @Description("This test delete system user")
    @DisplayName("Delete system user")
    public void testDeleteUserDefault() {
        // Авторизация пользователя
        Map<String, String> authData = new HashMap<>();
        authData.put("email", "vinkotov@example.com");
        authData.put("password", "1234");

        Response responseGetAuth = apiCoreRequests
                .makePostRequest("https://playground.learnqa.ru/api/user/login", authData);

        this.cookie = this.getCookie(responseGetAuth, "auth_sid");
        this.header = this.getHeader(responseGetAuth, "x-csrf-token");
        this.userIdOnAuth = this.getIntFromJson(responseGetAuth, "user_id");

        String userId = responseGetAuth.jsonPath().getString("user_id");
        System.out.println(userId);

        Response responseDeleteUser = apiCoreRequests
                .makeDeleteRequestUser("https://playground.learnqa.ru/api/user/" + userId,
                        this.header,
                        this.cookie);
        Assertions.assertResponseCodeEquals(responseDeleteUser, 400);
        Assertions.assertResponseTextError(responseDeleteUser.jsonPath().get("error"), "Please, do not delete test users with ID 1, 2, 3, 4 or 5.");
    }

    @Test
    @Description("This test Create a user, log in as him, delete him, then try to get his data by ID and make sure that the user is really deleted")
    @DisplayName("Creating and deleting a user")
    public void testDeleteUser() {
        // Создание пользователя
        Map<String, String> createUser = new HashMap<>();
        createUser.put("email", DataGenerator.getRandomEmail());
        createUser.put("password", "1234");
        createUser = DataGenerator.getRegistrationData(createUser);

        Response responseCreateUser = apiCoreRequests
                .makePostRequestCreateUser("https://playground.learnqa.ru/api/user/", createUser);

        Assertions.assertResponseCodeEquals(responseCreateUser, 200);
        Assertions.assertJsonHasField(responseCreateUser, "id");

        String userId = responseCreateUser.jsonPath().getString("id");
        String createdEmail = createUser.get("email");

        // Авторизация пользователя
        Map<String, String> authData = new HashMap<>();
        authData.put("email", createdEmail);
        authData.put("password", "1234");

        Response responseGetAuth = apiCoreRequests
                .makePostRequest("https://playground.learnqa.ru/api/user/login", authData);

        this.cookie = this.getCookie(responseGetAuth, "auth_sid");
        this.header = this.getHeader(responseGetAuth, "x-csrf-token");
        this.userIdOnAuth = this.getIntFromJson(responseGetAuth, "user_id");

        Response responseDeleteUser = apiCoreRequests
                .makeDeleteRequestUser("https://playground.learnqa.ru/api/user/" + userId,
                        this.header,
                        this.cookie);
        Assertions.assertResponseCodeEquals(responseDeleteUser, 200);
        Assertions.assertResponseTextError(responseDeleteUser.jsonPath().get("success"), "!");

        Response responseUserData = apiCoreRequests
                .makeGetRequest("https://playground.learnqa.ru/api/user/" + userId,
                        this.header,
                        this.cookie);
        Assertions.assertResponseTextEquals(responseUserData, "User not found");
    }

    @Test
    @Description("This test delete user while logged in by another user")
    @DisplayName("Delete user while logged in by another user")
    public void testDeleteAuthorizationAnotherUser() {
        // Создание нового пользователя
        Map<String, String> createUser = new HashMap<>();
        createUser.put("username", "Torvalds");
        createUser = DataGenerator.getRegistrationData(createUser);

        Response responseCreateUser = apiCoreRequests
                .makePostRequestCreateUser("https://playground.learnqa.ru/api/user/", createUser);

        Assertions.assertResponseCodeEquals(responseCreateUser, 200);
        Assertions.assertJsonHasField(responseCreateUser, "id");
        String userId = responseCreateUser.jsonPath().getString("id");

        // Авторизация пользователя
        Map<String, String> authData = new HashMap<>();
        authData.put("email", "vinkotov@example.com");
        authData.put("password", "1234");

        Response responseGetAuth = apiCoreRequests
                .makePostRequest("https://playground.learnqa.ru/api/user/login", authData);

        this.cookie = this.getCookie(responseGetAuth, "auth_sid");
        this.header = this.getHeader(responseGetAuth, "x-csrf-token");
        this.userIdOnAuth = this.getIntFromJson(responseGetAuth, "user_id");

        Response responseDeleteUser = apiCoreRequests
                .makeDeleteRequestUser("https://playground.learnqa.ru/api/user/" + userId,
                        this.header,
                        this.cookie);
        Assertions.assertResponseCodeEquals(responseDeleteUser, 400);
        Assertions.assertResponseTextError(responseDeleteUser.jsonPath().get("error"), "Please, do not delete test users with ID 1, 2, 3, 4 or 5.");
    }
}
