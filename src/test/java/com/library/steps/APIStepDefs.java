package com.library.steps;

import com.github.javafaker.Faker;
import com.library.utility.ConfigurationReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.Assert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.library.utility.LibraryAPI_Util.getToken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class APIStepDefs {
    String token;
    String idValue;
    String baseURI = ConfigurationReader.getProperty("library.baseUri");
    RequestSpecification requestSpecification;
    ValidatableResponse validatableResponse;
    Response response;
    JsonPath jp;
    Faker faker;

    String createdBookIDtoDelete;

    @Given("I logged Library api as a {string}")
    public void i_logged_library_api_as_a(String userType) {
        token = getToken(userType);

       requestSpecification = RestAssured.given().header("x-library-token",token);

    }
    @Given("Accept header is {string}")
    public void accept_header_is(String contentType) {

        requestSpecification = requestSpecification.given().accept(contentType);

    }
    @When("I send GET request to {string} endpoint")
    public void i_send_get_request_to_endpoint(String endpoint) {

        response = requestSpecification.get(baseURI + endpoint);

    }
    @Then("status code should be {int}")
    public void status_code_should_be(Integer statusCode) {

        validatableResponse = response.then().statusCode(statusCode);

    }
    @Then("Response Content type is {string}")
    public void response_content_type_is(String contentType) {

        validatableResponse = response.then().contentType(contentType);
    }

    @Given("Path param is {string}")
    public void path_param_is(String pathParam) {

        requestSpecification.pathParam("id",pathParam);

        this.idValue = pathParam;

    }
    @Then("{string} field should be same with path param")
    public void field_should_be_same_with_path_param(String fieldKey) {

        validatableResponse = response.then().body(fieldKey, Matchers.is(idValue));

    }
    @Then("following fields should not be null")
    public void following_fields_should_not_be_null(List<String> fieldNames) {

        jp = response.jsonPath();

        for (int i=0; i<fieldNames.size(); i++) {
            assertNotNull(jp.getString(fieldNames.get(i)));
        }
    }

    @Then("{string} field should not be null")
    public void field_should_not_be_null(String fieldName) {
        jp = response.jsonPath();

        assertNotNull(jp.getString(fieldName));
    }

    @Given("Request Content Type header is {string}")
    public void request_content_type_header_is(String headerValue) {

        requestSpecification = requestSpecification.header("Content-Type", headerValue);

    }
    @Given("I create a random {string} as request body")
    public void i_create_a_random_as_request_body(String randomRequestBody) {

        faker = new Faker();

        if (randomRequestBody.equals("book")) {

            Map<String,String> bookBodyMap = new LinkedHashMap<>();
            bookBodyMap.put("id", "" +faker.numerify("4####"));
            bookBodyMap.put("name", faker.book().title());
            bookBodyMap.put("isbn", "" + faker.numerify("6###########"));
            bookBodyMap.put("year", "" + faker.numerify("19##"));
            bookBodyMap.put("author", faker.name().fullName());
            bookBodyMap.put("book_category_id", "5");
            bookBodyMap.put("description", faker.howIMetYourMother().catchPhrase());
            bookBodyMap.put("added_date", "2022-07-06 00:00:00");

        requestSpecification = requestSpecification.formParams(bookBodyMap);



        } else if (randomRequestBody.equals("user")) {



        }else{
            throw new RuntimeException("Wow wow slow down bully, you broke something.");
        }


    }
    @When("I send POST request to {string} endpoint")
    public void i_send_post_request_to_endpoint(String endpoint) {

        response = requestSpecification.post(baseURI + endpoint);

    }
    @Then("the field value for {string} path should be equal to {string}")
    public void the_field_value_for_path_should_be_equal_to(String fieldKey, String expectedMessage) {

        jp = response.jsonPath();

       String actualMessage = jp.getString(fieldKey);

        assertEquals(expectedMessage,actualMessage);

    }

}
