package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lib.ApiCoreRequests;
import lib.Assertions;
import lib.BaseTestCase;
import lib.DataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@Feature("Edit User")
@Link(name = "Update user (must be logged in as this user)",
        url = "https://playground.learnqa.ru/api/user/{id}")
public class UserEditTest extends BaseTestCase {

    private final ApiCoreRequests apiCoreRequests = new ApiCoreRequests();

    String cookie;
    String header;
    int userIdOnAuth;

    @Test
    public void testEditJustCreatedTest() {
        // GENERATE USER
        Map<String, String> userData = DataGenerator.getRegistrationData();

        JsonPath responseCreateAuth = RestAssured
                .given()
                .body(userData)
                .post("https://playground.learnqa.ru/api/user/")
                .jsonPath();

        String userId = responseCreateAuth.getString("id");

        // LOGIN
        Map<String, String> authData = new HashMap<>();
        authData.put("email", userData.get("email"));
        authData.put("password", userData.get("password"));

        Response responseGetAuth = RestAssured
                .given()
                .body(authData)
                .post("https://playground.learnqa.ru/api/user/login")
                .andReturn();

        // EDIT
        String newName = "Changed Name";
        Map<String, String> editData = new HashMap<>();
        editData.put("firstName", newName);

        Response responseEditUser = RestAssured
                .given()
                .header("x-csrf-token", this.getHeader(responseGetAuth, "x-csrf-token"))
                .cookies("auth_sid", this.getCookie(responseGetAuth, "auth_sid"))
                .body(editData)
                .put("https://playground.learnqa.ru/api/user/" + userId)
                .andReturn();

        // GET
        Response responseUserData = RestAssured
                .given()
                .header("x-csrf-token", this.getHeader(responseGetAuth, "x-csrf-token"))
                .cookie("auth_sid", this.getCookie(responseGetAuth, "auth_sid"))
                .get("https://playground.learnqa.ru/api/user/" + userId)
                .andReturn();
        Assertions.assertJsonByName(responseUserData, "firstName", newName);
    }

    @Test
    @Description("This test change user data without authorization")
    @DisplayName("Change user data without authorization")
    @Severity(SeverityLevel.NORMAL)
    public void testEditNotAuthorization() {
        Map<String, String> createUser = new HashMap<>();
        createUser.put("email", DataGenerator.getRandomEmail());
        createUser = DataGenerator.getRegistrationData(createUser);

        Response responseCreateUser = apiCoreRequests
                .makePostRequestCreateUser("https://playground.learnqa.ru/api/user/", createUser);
        Assertions.assertResponseCodeEquals(responseCreateUser, 200);
        Assertions.assertJsonHasField(responseCreateUser, "id");

        String userId = responseCreateUser.jsonPath().getString("id");
        String newName = "QA";
        Map<String, String> editData = new HashMap<>();
        editData.put("firstName", newName);

        Response responseEditUser = apiCoreRequests
                .makePutRequestEditeUser("https://playground.learnqa.ru/api/user/" + userId, editData);
        System.out.println(responseEditUser.asString());
        Assertions.assertResponseCodeEquals(responseEditUser, 400);
        Assertions.assertResponseTextError(responseEditUser.jsonPath().get("error"), "Auth token not supplied");
    }

    @Test
    @Description("This test change user data while logged in by another user")
    @DisplayName("Change user data while logged in by another user")
    @Severity(SeverityLevel.CRITICAL)
    public void testEditAuthorizationAnotherUser() {
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

        // Изменение username пользователя
        String newName = "nameTestLearnQA";
        Map<String, String> editData = new HashMap<>();
        editData.put("username", newName);

        Response responseEditUser = apiCoreRequests
                .makePutRequestEdit("https://playground.learnqa.ru/api/user/" + userId,
                        this.header,
                        this.cookie,
                        editData);
        Assertions.assertResponseCodeEquals(responseEditUser, 400);
        Assertions.assertResponseTextError(responseEditUser.jsonPath().get("error"), "Please, do not edit test users with ID 1, 2, 3, 4 or 5.");

        Response responseUserData = apiCoreRequests
                .makeGetRequest("https://playground.learnqa.ru/api/user/" + userId,
                        this.header,
                        this.cookie);
        Assertions.assertResponseCodeEquals(responseUserData, 200);
        Assertions.assertJsonByName(responseUserData, "username", "Torvalds");
    }

    @Test
    @Description("Change user email while logged in by the same user to a new email without the @ symbol")
    @DisplayName("Change email without the @ symbol")
    @Severity(SeverityLevel.NORMAL)
    public void testEditWithoutATAuthorizationThisUser() {
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

        String newEmail = "EditUserexample.com";
        Map<String, String> editData = new HashMap<>();
        editData.put("email", newEmail);

        Response responseEditUser = apiCoreRequests
                .makePutRequestEdit("https://playground.learnqa.ru/api/user/" + userId,
                        this.header,
                        this.cookie,
                        editData);
        Assertions.assertResponseCodeEquals(responseEditUser, 400);
        Assertions.assertResponseTextError(responseEditUser.jsonPath().get("error"), "Invalid email format");
    }

    @Test
    @Description("Сhange the firstName of a user, while logged in by the same user, to a very short value of one character")
    @DisplayName("Change user's firstName to short value, same user")
    @Severity(SeverityLevel.NORMAL)
    public void testEditAuthorizationThisrUserShortFirstName() {
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

        // Изменить firstName пользователя
        String newFirstName = "a";
        Map<String, String> editData = new HashMap<>();
        editData.put("firstName", newFirstName);

        Response responseEditUser = apiCoreRequests
                .makePutRequestEdit("https://playground.learnqa.ru/api/user/" + userId,
                        this.header,
                        this.cookie,
                        editData);
        Assertions.assertResponseCodeEquals(responseEditUser, 400);
        Assertions.assertResponseTextError(responseEditUser.jsonPath().get("error"), "The value for field `firstName` is too short");
    }
}
