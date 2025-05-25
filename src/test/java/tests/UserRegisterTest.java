package tests;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lib.ApiCoreRequests;
import lib.Assertions;
import lib.BaseTestCase;
import lib.DataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class UserRegisterTest extends BaseTestCase {

    private final ApiCoreRequests apiCoreRequests = new ApiCoreRequests();

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
}
