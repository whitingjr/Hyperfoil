package io.hyperfoil.core.statistics;

import static io.hyperfoil.core.builders.StepCatalog.SC;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.hyperfoil.api.http.HttpMethod;
import io.hyperfoil.api.statistics.StatisticsSnapshot;
import io.hyperfoil.core.session.BaseScenarioTest;
import io.hyperfoil.core.steps.PathMetricSelector;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PathMetricsTest extends BaseScenarioTest {
   @Override
   protected void initRouter() {
      router.route("/foo.js").handler(ctx -> ctx.response().setStatusCode(200).end());
      router.route("/bar.php").handler(ctx -> ctx.response().setStatusCode(200).end());
      router.route("/goo.css").handler(ctx -> ctx.response().setStatusCode(200).end());
   }

   @Test
   public void test() {
      AtomicInteger counter = new AtomicInteger(0);
      PathMetricSelector selector = new PathMetricSelector();
      selector.nextItem(".*\\.js");
      selector.nextItem("(.*\\.php).* -> $1");
      selector.nextItem("-> others");
      scenario(3).initialSequence("test")
            .step(SC).httpRequest(HttpMethod.GET)
            .path(s -> {
               switch (counter.getAndIncrement()) {
                  case 0:
                     return "/foo.js";
                  case 1:
                     return "/bar.php?foo=bar";
                  case 2:
                     return "/goo.css";
                  default:
                     throw new IllegalStateException();
               }
            })
            .metric(selector)
            .endStep();

      Map<String, List<StatisticsSnapshot>> stats = runScenario();
      verifyRequest(stats.get("/foo.js"));
      verifyRequest(stats.get("/bar.php"));
      verifyRequest(stats.get("others"));
   }

   private void verifyRequest(List<StatisticsSnapshot> stats) {
      assertThat(stats).isNotNull();
      assertThat(stats.size()).isEqualTo(1);
      assertThat(stats.iterator().next().requestCount).isEqualTo(1);
   }
}
