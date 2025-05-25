package lib;

import io.restassured.response.Response;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Assertions {

    public static void assertJsonByName(Response Response, String name, int expectedValue) {
        Response.then().assertThat().body("$",hasKey(name));

        int value = Response.jsonPath().getInt(name);
        assertEquals(expectedValue, value, "JSON value is not equal to expected value");
    }

    public static void assertJsonByName(Response Response, String name, String expectedValue) {
        Response.then().assertThat().body("$",hasKey(name));

        String value = Response.jsonPath().getString(name);
        assertEquals(expectedValue, value, "JSON value is not equal to expected value");
    }

    public static void assertResponseTextEquals(Response Response, String expectedAnswer) {
        assertEquals(expectedAnswer, Response.asString(), "Response text is not as expected");
    }

    public static void assertResponseCodeEquals(Response Response, int expectedStatusCode) {
        assertEquals(expectedStatusCode, Response.statusCode(), "Response code is not as expected");
    }

    public static void assertJsonHasField(Response Response, String expectedFieldName) {
        Response.then().assertThat().body("$",hasKey(expectedFieldName));
    }

    public static void assertJsonHasNotField(Response Response, String unexpectedFieldName) {
        Response.then().assertThat().body("$", not(hasKey(unexpectedFieldName)));

    }

    public static void assertJsonHasFields(Response Response, String[] unexpectedFieldNames) {
        for (String unexpectedFieldName : unexpectedFieldNames) {
            Assertions.assertJsonHasField(Response, unexpectedFieldName);
        }
    }

    public static void assertInvalidEmail(Response Response, String expectedEmail) {
        assertEquals(expectedEmail, Response.asString(), "The response indicates that the email address lacks the '@' sign.");
    }

    public static void assertParamsAndMissed(String responseBody, String expectedEmail) {
        assertTrue(responseBody.contains("The following required params are missed: " + expectedEmail),
                "Expected missing param message not found in response: " + responseBody);
    }

    public static void assertValueFieldShort(String responseBody, String fieldName) {
        assertTrue(
                responseBody.contains(fieldName),
                "Expected a message about a field being too short '" + fieldName + "'. Actual answer: " + responseBody
        );
    }

    public static void assertValueFieldLong(String responseBody, String fieldName) {
        assertTrue(
                responseBody.contains(fieldName),
                "Expected a message about a field being too long '" + fieldName + "'. Actual answer: " + responseBody
        );
    }
}
