package cucumber.examples.java.calculator;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import java.util.List;

public class ShoppingStepdefs {
  @When("^I have short step$")
  public void I_have_short_step() throws Throwable {
  }

  @When("^I have short step with extension$")
  public void I_have_short_step_with_extension(int arg1, int arg2) throws Throwable {
  }
}
