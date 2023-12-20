package com.library.steps;

import com.library.utility.ConfigurationReader;
import com.library.utility.LibraryAPI_Util;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;

import java.util.List;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class APIStepDefs {
    RequestSpecification givenPart;
    Response response;
    ValidatableResponse thenPart;
    /**
     * US 01
     *      */

    @Given("I logged Library api as a {string}")
    public void i_logged_library_api_as_a(String userType) {
        givenPart = given().log().uri()
                .header("x-library-token",
                        LibraryAPI_Util.getToken(userType));
    }
    @Given("Accept header is {string}")
    public void accept_header_is(String contentType) {
        givenPart.accept(contentType);

    }
    @When("I send GET request to {string} endpoint")
    public void i_send_get_request_to_endpoint(String endpoint) {
         response = givenPart.when()
                .get(ConfigurationReader.getProperty("library.baseUri") + endpoint)
                .prettyPeek();

         thenPart = response.then();
    }
    @Then("status code should be {int}")
    public void status_code_should_be(Integer statusCode) {
        thenPart.statusCode(statusCode);
    }
    @Then("Response Content type is {string}")
    public void response_content_type_is(String contentType) {
        thenPart.contentType(contentType);

    }
    @Then("Each {string} field should not be null")
    public void each_field_should_not_be_null(String path) {

        thenPart.body(path, everyItem(notNullValue()));

        // HOW TO GET SPESIFIC VALUE FROM RESPONSE
        // response or thenPart.extract().jsonPath();

    }


    /**
     * US02 RELATED
     *
     */

    String id;
    @Given("Path param {string} is {string}")
    public void path_param_is(String pathParam, String value) {
        givenPart.pathParam(pathParam,value);
        id=value;

    }
    @Then("{string} field should be same with path param")
    public void field_should_be_same_with_path_param(String path) {
        thenPart.body(path,is(id));
    }
    @Then("following fields should not be null")
    public void following_fields_should_not_be_null(List<String> paths) {
        /*
        thenPart.body(paths.get(0),is(notNullValue()))
                .body(paths.get(1),is(notNullValue()))
                .body(paths.get(1),is(notNullValue()));

         */

        for (String eachPath : paths) {
            thenPart.body(eachPath,is(notNullValue()));
        }

    }


}
