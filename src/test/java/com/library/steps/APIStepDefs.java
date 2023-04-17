package com.library.steps;

import com.github.javafaker.Faker;
import com.library.pages.BookPage;
import com.library.pages.LoginPage;
import com.library.utility.BrowserUtil;
import com.library.utility.ConfigurationReader;
import com.library.utility.Driver;
import org.junit.Before;
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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.library.utility.DB_Util.getCellValue;
import static com.library.utility.DB_Util.runQuery;
import static com.library.utility.LibraryAPI_Util.getToken;
import static io.restassured.RestAssured.given;
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
    String createdBookTitle;
    String createdBookID;
    BookPage bookPage;
    String encryptedPassword;
    Map<String, String> userBodyMap;
    String createdUserEmail;
    String newToken;


    @Given("I logged Library api as a {string}")
    public void i_logged_library_api_as_a(String userType) {
        token = getToken(userType);

        requestSpecification = given().header("x-library-token", token);
    }

    @Given("Accept header is {string}")
    public void accept_header_is(String contentType) {

        if (requestSpecification == null) {
            requestSpecification = given();
        }
        requestSpecification = requestSpecification.accept(contentType);
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

        requestSpecification.pathParam("id", pathParam);

        this.idValue = pathParam;

    }

    @Then("{string} field should be same with path param")
    public void field_should_be_same_with_path_param(String fieldKey) {

        validatableResponse = response.then().body(fieldKey, Matchers.is(idValue));

    }

    @Then("following fields should not be null")
    public void following_fields_should_not_be_null(List<String> fieldNames) {

        jp = response.jsonPath();

        for (int i = 0; i < fieldNames.size(); i++) {
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

            createdBookTitle = faker.name().firstName() + " travels to " + faker.country().name();

            Map<String, String> bookBodyMap = new LinkedHashMap<>();
            bookBodyMap.put("id", "" + faker.numerify("4####"));
            bookBodyMap.put("name", createdBookTitle);
            bookBodyMap.put("isbn", "" + faker.numerify("6###########"));
            bookBodyMap.put("year", "" + faker.numerify("19##"));
            bookBodyMap.put("author", faker.name().fullName());
            bookBodyMap.put("book_category_id", "5");
            bookBodyMap.put("description", faker.howIMetYourMother().catchPhrase());
            bookBodyMap.put("added_date", "2022-07-06 00:00:00");

            requestSpecification = requestSpecification.formParams(bookBodyMap);


        } else if (randomRequestBody.equals("user")) {

            this.createdUserEmail = faker.internet().emailAddress();

            userBodyMap = new LinkedHashMap<>();
            userBodyMap.put("full_name", faker.name().fullName());
            userBodyMap.put("email", createdUserEmail);
            userBodyMap.put("password", faker.address().cityName());
            userBodyMap.put("user_group_id", "" + 2);
            userBodyMap.put("start_date", "2022-03-11");
            userBodyMap.put("end_date", "2023-03-11");
            userBodyMap.put("address", faker.country().capital());

            requestSpecification = requestSpecification.formParams(userBodyMap);

        } else {
            throw new RuntimeException("Wow wow slow down bully, you broke something. Slow down ");
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

        assertEquals(expectedMessage, actualMessage);

    }

    @Then("UI, Database and API created book information must match")
    public void ui_database_and_api_created_book_information_must_match() {
        Map<String, String> bookUImap = new LinkedHashMap<>();
        Map<String, String> bookDBmap = new LinkedHashMap<>();
        Map<String, String> bookAPImap = new LinkedHashMap<>();


        // get the info from UI
        bookPage = new BookPage();
        BrowserUtil.waitFor(2);
        bookPage.search.sendKeys(createdBookTitle);
        BrowserUtil.waitFor(2);
        bookPage.editBookButton.click();
        BrowserUtil.waitFor(2); // ps: Synchronization issues suck!


        bookUImap.put("title", bookPage.bookName.getAttribute("value"));
        bookUImap.put("author", bookPage.author.getAttribute("value"));
        bookUImap.put("isbn", bookPage.isbn.getAttribute("value"));

        System.out.println("---------------------------------------");

        //get the info from DB
        runQuery("select name, author, isbn  from books\n" +
                "where name = '" + createdBookTitle + "'");

        bookDBmap.put("title", getCellValue(1, "name"));
        bookDBmap.put("author", getCellValue(1, "author"));
        bookDBmap.put("isbn", getCellValue(1, "isbn"));

        System.out.println("---------------------------------------");
        //get the info from API
        createdBookID = jp.getString("book_id");

        //get the json body for created book for comparison
        JsonPath jsonPath = given().accept(ContentType.JSON)
                .header("x-library-token", token)
                .pathParam("id", createdBookID)
                .when().get(baseURI + "/get_book_by_id/{id}")
                .then().statusCode(200).extract().jsonPath();

        bookAPImap.put("title", jsonPath.getString("name"));
        bookAPImap.put("author", jsonPath.getString("author"));
        bookAPImap.put("isbn", jsonPath.getString("isbn"));


        System.out.println("-------------------------------------------");

        //compare all 3 layers

        assertEquals(bookUImap, bookAPImap);
        assertEquals(bookUImap, bookDBmap);
        assertEquals(bookAPImap, bookDBmap);

    }

    @Then("created user information should match with Database")
    public void created_user_information_should_match_with_database() {

        runQuery("select full_name, email, password, user_group_id, start_date,end_date,address\n" +
                "from users where full_name = '" + userBodyMap.get("full_name") + "'");

         encryptedPassword = getCellValue(1, "password");

        Map<String, String> dbInformation = new LinkedHashMap<>();
        dbInformation.put("full_name", getCellValue(1, "full_name"));
        dbInformation.put("email", getCellValue(1, "email"));
        dbInformation.put("password", userBodyMap.get("password"));
        dbInformation.put("user_group_id", getCellValue(1, "user_group_id"));
        dbInformation.put("start_date", getCellValue(1, "start_date"));
        dbInformation.put("end_date", getCellValue(1, "end_date"));
        dbInformation.put("address", getCellValue(1, "address"));

        assertEquals(dbInformation,userBodyMap);

    }

    @Then("created user should be able to login Library UI")
    public void created_user_should_be_able_to_login_library_ui() {

        LoginPage loginPage = new LoginPage();

        loginPage.login(createdUserEmail, userBodyMap.get("password"));

    }

    @Then("created user name should appear in Dashboard Page")
    public void created_user_name_should_appear_in_dashboard_page() {

        bookPage = new BookPage();

        WebDriverWait wait = new WebDriverWait(Driver.getDriver(),20);
        wait.until(ExpectedConditions.visibilityOf(bookPage.accountHolderName));

        String apiUserName = userBodyMap.get("full_name");
        String uiUserName = bookPage.accountHolderName.getText();

        System.out.println("apiUserName = " + apiUserName);
        System.out.println("uiUserName = " + uiUserName);


        assertEquals(apiUserName,uiUserName);
    }

    @Given("I logged Library api with credentials {string} and {string}")
    public void i_logged_library_api_with_credentials_and(String email, String password) {

        Map<String,String> credentials = new LinkedHashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        given().accept(ContentType.URLENC)
                .formParams(credentials);

        newToken = getToken(email, password);

    }
    @Given("I send token information as request body")
    public void i_send_token_information_as_request_body() {

        Map<String,String> tokenMap = new HashMap<>();
        tokenMap.put("token",newToken);

        requestSpecification = given().formParam("token", newToken);

    }

}
