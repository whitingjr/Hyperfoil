package io.hyperfoil.core.session;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.hyperfoil.api.http.HttpMethod;
import io.hyperfoil.api.statistics.StatisticsSnapshot;
import io.hyperfoil.core.builders.StepCatalog;
import io.hyperfoil.core.generators.RandomCsvRowStep;
import io.hyperfoil.core.generators.TemplateStep;
import io.hyperfoil.core.steps.HttpRequestStep;
import io.hyperfoil.core.steps.HttpRequestStep.HeadersBuilder;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class RandomCsvRowTest extends BaseScenarioTest {

   @Override
   protected void initRouter() {
      router.route("/*").handler(routingContext -> {
         routingContext.response().setStatusCode(200).end("Well done.");
      });
   }

   @Test
   public void testSingleRequestSucceeds() {
      String response = null;
      benchmarkBuilder.http("localhost").addAddress("127.0.0.1");
      StepCatalog catalogue = scenario().initialSequence("randomcsvtest").step(SC);

      RandomCsvRowStep.Builder randomBuidler = catalogue.randomCsvRow();
      randomBuidler.file("src/test/resources/data/random-sample-test.csv").skipComments(true).removeQuotes(true);
      randomBuidler.columns().accept("0", "target-host");
      randomBuidler.columns().accept("1", "uri");

      TemplateStep.Builder templateBuilder = catalogue.template();
      templateBuilder.pattern("${target-host}:8099").toVar("target-authority");

      HttpRequestStep.Builder requestBuilder = catalogue.httpRequest(HttpMethod.GET);
      requestBuilder
            .path().fromVar("uri").end()
            .authority().fromVar("target-host").end();

      HeadersBuilder headersBuilder = requestBuilder.headers();
      headersBuilder.withKey("HOST").fromVar("target-host");
      headersBuilder.endHeaders().endStep()
      .step(SC).awaitAllResponses();

      Map<String, List<StatisticsSnapshot>> stats = runScenario();
      StatisticsSnapshot test1 = assertSingleItem(stats.get("echo-api"));
      Assert.assertEquals("The SUT did not process the request successfully.", 1, test1.status_2xx);
   }
}
