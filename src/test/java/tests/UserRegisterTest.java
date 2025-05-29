package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lib.ApiCoreRequests;
import lib.Assertions;
import lib.BaseTestCase;
import lib.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Feature("User registration")
@Link(name = "Create user", url = "https://playground.learnqa.ru/api/user/")
public class UserRegisterTest extends BaseTestCase {

    String cookie;
    String header;
    int userIdOnAuth;
    private final ApiCoreRequests apiCoreRequests = new ApiCoreRequests();

    @BeforeEach
    public void loginUser() {
        // Авторизация пользователя
        Map<String, String> authData = new HashMap<>();
        authData.put("email", "vinkotov@example.com");
        authData.put("password", "1234");

        Response responseGetAuth = apiCoreRequests
                .makePostRequest("https://playground.learnqa.ru/api/user/login", authData);

        this.cookie = this.getCookie(responseGetAuth, "auth_sid");
        this.header = this.getHeader(responseGetAuth, "x-csrf-token");
        this.userIdOnAuth = this.getIntFromJson(responseGetAuth, "user_id");
    }

    @Test
    public void testCreateUserWithExistingEmail() {
        String email = "vinkotov@example.com";

        Map<String, String> userData = new HashMap<>();
        userData.put("email", email);
        userData = DataGenerator.getRegistrationData(userData);

        Response responseCreateAuth = RestAssured
                .given()
                .body(userData)
                .post("https://playground.learnqa.ru/api/user/")
                .andReturn();

        Assertions.assertResponseCodeEquals(responseCreateAuth, 400);
        Assertions.assertResponseTextEquals(responseCreateAuth, "Users with email '" + email + "' already exists");
    }

    @Test
    public void testCreateUserSuccessfullyEmail() {
        String email = DataGenerator.getRandomEmail();

        Map<String, String> userData = DataGenerator.getRegistrationData();

        Response responseCreateAuth = RestAssured
                .given()
                .body(userData)
                .post("https://playground.learnqa.ru/api/user/")
                .andReturn();
        Assertions.assertResponseCodeEquals(responseCreateAuth, 200);
        Assertions.assertJsonHasField(responseCreateAuth, "id");
    }

    @Test
    @Description("This test creating user with mail without @")
    @DisplayName("Test negative creating user")
    public void testCreateUserWrongEmail() {
        String email = "vinkotovexample.com";

        Map<String, String> createUser = new HashMap<>();
        createUser.put("email", email);
        createUser = DataGenerator.getRegistrationData(createUser);
        Response responseCreateAuth = apiCoreRequests
                .makePostRequestCreateUser("https://playground.learnqa.ru/api/user/", createUser);

        Assertions.assertResponseCodeEquals(responseCreateAuth, 400);
        Assertions.assertInvalidEmail(responseCreateAuth, "Invalid email format");
    }

    @ParameterizedTest
    @MethodSource("userFieldVariants")
    @Description("This test creating user with any empty field")
    @DisplayName("Test negative creating user any empty field")
    @Severity(SeverityLevel.NORMAL)
    public void testCreateUserWithoutFillingOneFields(String username, String firstName, String lastName, String expectedMissingField) {
        String email = DataGenerator.getRandomEmail();
        Map<String, String> createUser = new HashMap<>();

        createUser.put("email", email);
        createUser.put("password", "123");

        if (username != null) createUser.put("username", username);
        if (firstName != null) createUser.put("firstName", firstName);
        if (lastName != null) createUser.put("lastName", lastName);

        Response responseCreateAuth = apiCoreRequests
                .makePostRequestCreateUser("https://playground.learnqa.ru/api/user/", createUser);

        Assertions.assertResponseCodeEquals(responseCreateAuth, 400);
        Assertions.assertParamsAndMissed(responseCreateAuth.asString(), expectedMissingField);
    }

     static Stream<Arguments> userFieldVariants() {
        return Stream.of(
                Arguments.of(null, "learnqa", "learnqa", "username"),
                Arguments.of("learnqa", null, "learnqa", "firstName"),
                Arguments.of("learnqa", "learnqa", null, "lastName")
        );
    }

    @Test
    @Description("This test creating user with short name")
    @DisplayName("Test negative creating user short name field")
    @Severity(SeverityLevel.MINOR)
    public void testCreateUserShortName() {
        String username = "v";

        Map<String, String> createUser = new HashMap<>();
        createUser.put("username", username);
        createUser = DataGenerator.getRegistrationData(createUser);

        Response responseCreateAuth = apiCoreRequests
                .makePostRequestCreateUser("https://playground.learnqa.ru/api/user/", createUser);

        Assertions.assertResponseCodeEquals(responseCreateAuth, 400);
        Assertions.assertValueFieldShort(responseCreateAuth.asString(), "username");
    }

    @Test
    @Description("This test creating user with long name")
    @DisplayName("Test negative creating user long name field")
    @Severity(SeverityLevel.MINOR)
    public void testCreateUserLongName() {
        String username = "asdsadqwertycfvbgmklpiobcsdvgbnhmkasdrty" +
                "nfvdxsccasdsadqwertycfvbgmklpiobcsdvgbnhmkasdrtynf" +
                "vdxsccasdsadqwertycfvbgmklpiobcsdvgbnhmkasdrtynfvd" +
                "xsccasdsadqwertycfvbgmklpiobcsdvgbnhmkasdrtynfvdxs" +
                "ccasdsadqwertycfvbgmklpiobcsdvgbnhmkasdrtynfvfdvdtyikqwxcvbpm";

        Map<String, String> createUser = new HashMap<>();
        createUser.put("username", username);
        createUser = DataGenerator.getRegistrationData(createUser);
        Response responseCreateAuth = apiCoreRequests
                .makePostRequestCreateUser("https://playground.learnqa.ru/api/user/", createUser);

        Assertions.assertResponseCodeEquals(responseCreateAuth, 400);
        Assertions.assertValueFieldLong(responseCreateAuth.asString(), "username");
    }

    @Test
    @Description("Authorization by user 1, create user 2, get user 2 from authorization data of user 1")
    @DisplayName("Test authorization by one user, but receives data from another")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateUserAnotherUserData() {
        // Создание нового пользователя
        Map<String, String> createUser = new HashMap<>();
        createUser.put("email", DataGenerator.getRandomEmail());
        createUser = DataGenerator.getRegistrationData(createUser);

        Response responseCreateUser = apiCoreRequests
                .makePostRequestCreateUser("https://playground.learnqa.ru/api/user/", createUser);
        Assertions.assertResponseCodeEquals(responseCreateUser, 200);
        Assertions.assertJsonHasField(responseCreateUser, "id");

        // Проверка успешной авторизации пользователя
        Response responseGetCheckAuth = apiCoreRequests
                .makeGetRequest("https://playground.learnqa.ru/api/user/auth",
                        this.header,
                        this.cookie);
        Assertions.assertJsonByName(responseGetCheckAuth, "user_id", this.userIdOnAuth);

        String userId = responseCreateUser.jsonPath().getString("id");
        // Получение пользователя
        Response responseUserData = apiCoreRequests
                .makeGetRequest("https://playground.learnqa.ru/api/user/" + userId,
                        this.header,
                        this.cookie);
        Assertions.assertJsonHasField(responseUserData, "username");
        Assertions.assertJsonHasNotField(responseUserData, "email");
        Assertions.assertJsonHasNotField(responseUserData, "firstName");
        Assertions.assertJsonHasNotField(responseUserData, "lastName");
    }
}
