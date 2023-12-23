package com.library.steps;

import com.library.pages.BookPage;
import com.library.pages.LoginPage;
import com.library.utility.BrowserUtil;
import com.library.utility.ConfigurationReader;
import com.library.utility.DB_Util;
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
import org.junit.Assert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * US 03 RELATED
     */


    @Given("Request Content Type header is {string}")
    public void request_content_type_header_is(String contentType) {
        givenPart.contentType(contentType);

    }
    Map<String,Object> randomDataMap;
    @Given("I create a random {string} as request body")
    public void i_create_a_random_as_request_body(String randomData) {
        Map<String,Object> requestDataMap;


        switch (randomData){
            case "user":
                requestDataMap=LibraryAPI_Util.getRandomUserMap();
                break;
            case "book":
                requestDataMap=LibraryAPI_Util.getRandomBookMap();
                break;
            default:
                throw new RuntimeException("Unexpected Value :"+randomData);
        }
        System.out.println("requestDataMap = " + requestDataMap);

        /*

        Since Content Type is x-application/x-www-form-urlencoded we should send body by using
        formParam.Each requestDataMap has more than one value as key - value.That is why we used
        into here formParams to send all data in one shot

         */
        randomDataMap=requestDataMap;
        givenPart.formParams(requestDataMap);


    }


    @When("I send POST request to {string} endpoint")
    public void i_send_post_request_to_endpoint(String endpoint) {

         response = givenPart.when()
                .post(ConfigurationReader.getProperty("library.baseUri") + endpoint)
                .prettyPeek();





        thenPart = response.then();


    }
    @Then("the field value for {string} path should be equal to {string}")
    public void the_field_value_for_path_should_be_equal_to(String path, String value) {
        thenPart.body(path,is((value)));
    }
    @Then("{string} field should not be null")
    public void field_should_not_be_null(String path) {
        thenPart.body(path,is(notNullValue()));

    }


    /**
     *  US 03 SC 2  RELATED
     */


    @Then("UI, Database and API created book information must match")
    public void ui_database_and_api_created_book_information_must_match() {
        String id = response.path("book_id");

        // You can use data from randomData too
        // API DATA -> EXPECTED DATA
        Response apiResponse = given().log().uri()
                .header("x-library-token", LibraryAPI_Util.getToken("librarian"))
                .pathParam("id", id)
                .when().get(ConfigurationReader.getProperty("library.baseUri") + "/get_book_by_id/{id}")
                .prettyPeek();

        JsonPath jp = apiResponse.jsonPath();
        System.out.println("------API DATA--------");
        Map<String,Object> apiBook=new LinkedHashMap<>();
        String name = jp.getString("name");
        apiBook.put("name",name);
        apiBook.put("isbn",jp.getString("isbn"));
        apiBook.put("year",jp.getString("year"));
        apiBook.put("author",jp.getString("author"));
        apiBook.put("book_category_id",jp.getString("book_category_id"));
        apiBook.put("description",jp.getString("description"));
        System.out.println("apiBook Map= " + apiBook);


        // DB DATA -> ACTUAL
        DB_Util.runQuery("select * from books where id='"+id+"'");
        Map<String, Object> dbBook = DB_Util.getRowMap(1);
        System.out.println("------DB DATA--------");
        dbBook.remove("id");
        dbBook.remove("added_date");
        System.out.println("dbBook Map= " + dbBook);


        // UI DATA -> ACTUAL
        /*
            Normally to find book we can use ISBN but for this example we will use bookName to
            find same book from UI. Also you should have bookName as unique to make your test case
            successfully

         */
        // Get me bookName from API Request while POSTing bookName
        String bookName = (String) randomDataMap.get("name");
        System.out.println("bookName = " + bookName);

        BookPage bookPage=new BookPage();
        bookPage.search.sendKeys(bookName);
        BrowserUtil.waitFor(3);

        bookPage.editBook(bookName).click();
        BrowserUtil.waitFor(3);

        // Get the book that we created
        System.out.println("------UI DATA--------");

        Map<String,Object> uiBook=new LinkedHashMap<>();

        String uiBookName = bookPage.bookName.getAttribute("value");
        uiBook.put("name",uiBookName);

        String uiISBN = bookPage.isbn.getAttribute("value");
        uiBook.put("isbn", uiISBN);

        String uiYear = bookPage.year.getAttribute("value");
        uiBook.put("year",uiYear);

        String uiAuthor = bookPage.author.getAttribute("value");
        uiBook.put("author",uiAuthor);

        // Get me category id
        // We have category name into UI.To retrieve bookCategory ID we used category name with DB Query
        // for finding related book_category_id
        String selectedBookCategoryName = BrowserUtil.getSelectedOption(bookPage.categoryDropdown);
        DB_Util.runQuery("select id from book_categories where name='"+selectedBookCategoryName+"'");
        String uiCategoryID= DB_Util.getFirstRowFirstColumn();
        uiBook.put("book_category_id",uiCategoryID);

        String uiDesc = bookPage.description.getAttribute("value");
        uiBook.put("description",uiDesc);
        System.out.println("uiBook MAP= " + uiBook);

        // Assertions
        Assert.assertEquals(apiBook,uiBook);
        Assert.assertEquals(apiBook,dbBook);


    }

    /**
     *  US 04 RELATED
     */

    @Then("created user information should match with Database")
    public void created_user_information_should_match_with_database() {
      // Get me user_id
        String user_id=response.path("user_id");
        System.out.println("User is generated with "+ user_id);

      // Database Data  --> user_id
        DB_Util.runQuery("select full_name,email,user_group_id,status,start_date,end_date,address from users where id="+user_id);
        Map<String, Object> dbUser = DB_Util.getRowMap(1);
        System.out.println("dbUser = " + dbUser);

      // Compare against API Data
        System.out.println("--- API POST DATA ------");
        System.out.println(randomDataMap);
        String password= (String) randomDataMap.remove("password");

        Assert.assertEquals(randomDataMap,dbUser);
        randomDataMap.put("password",password);

    }

    @Then("created user should be able to login Library UI")
    public void created_user_should_be_able_to_login_library_ui() {

        LoginPage loginPage=new LoginPage();
        String email = (String) randomDataMap.get("email");
        System.out.println("email = " + email);
        String password = (String) randomDataMap.get("password");
        System.out.println("password = " + password);
        loginPage.login(email,password);
        BrowserUtil.waitFor(2);

    }

    @Then("created user name should appear in Dashboard Page")
    public void created_user_name_should_appear_in_dashboard_page() {

        BookPage bookPage=new BookPage();
        BrowserUtil.waitFor(2);

        String uiFullName = bookPage.accountHolderName.getText();
        String apiFullName = (String) randomDataMap.get("full_name");

        Assert.assertEquals(apiFullName,uiFullName);


    }

    /**
     * US 05
     */
    String token;
    @Given("I logged Library api with credentials {string} and {string}")
    public void i_logged_library_api_with_credentials_and(String email, String password) {
        token = LibraryAPI_Util.getToken(email, password);
        givenPart = given().log().uri();
    }
    @Given("I send token information as request body")
    public void i_send_token_information_as_request_body() {
        givenPart.formParam("token",token);
    }


}
