import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {"classpath:features"},
        plugin = {"pretty", "json:target/cucumber/cucumber.json", "html:target/cucumber-html"},
        tags = {"@test" },
        glue = "com.payments.testing.bdd.api.steps"
)
public class RunCucumberTest {

}
