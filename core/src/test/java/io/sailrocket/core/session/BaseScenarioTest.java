package io.sailrocket.core.session;

import io.sailrocket.api.connection.HttpClientPool;
import io.sailrocket.api.config.Phase;
import io.sailrocket.api.http.HttpVersion;
import io.sailrocket.api.session.Session;
import io.sailrocket.core.api.PhaseInstance;
import io.sailrocket.core.builders.BenchmarkBuilder;
import io.sailrocket.core.builders.ScenarioBuilder;
import io.sailrocket.core.client.HttpClientProvider;
import io.sailrocket.core.impl.ConcurrentPoolImpl;
import io.sailrocket.core.impl.PhaseInstanceImpl;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Router;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class BaseScenarioTest {
   protected final Logger log = LoggerFactory.getLogger(getClass());

   protected Vertx vertx;
   protected HttpClientPool httpClientPool;
   protected Router router;

   protected List<Session> runScenario(ScenarioBuilder scenarioBuilder, int repeats) {
      // trigger any cross-definition effects
      scenarioBuilder.endScenario().endPhase().endSimulation().build();
      PhaseInstance phase;
      if (repeats <= 0) {
         phase = new PhaseInstanceImpl.Always(new Phase.Always("test", scenarioBuilder.build(), 0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Long.MAX_VALUE, -1, "test", 1));
      } else {
         phase = new PhaseInstanceImpl.Sequentially(new Phase.Sequentially("test", scenarioBuilder.build(), 0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Long.MAX_VALUE, -1, "test", repeats));
      }
      return runScenario(phase);
   }

   protected List<Session> runScenarioOnceParallel(ScenarioBuilder scenarioBuilder, int concurrency) {
      // trigger any cross-definition effects
      scenarioBuilder.endScenario().endPhase().endSimulation().build();
      PhaseInstance phase = new PhaseInstanceImpl.AtOnce(new Phase.AtOnce("test", scenarioBuilder.build(), 0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Long.MAX_VALUE, -1, "test", concurrency));
      return runScenario(phase);
   }

   private List<Session> runScenario(PhaseInstance phase) {
      CountDownLatch latch = new CountDownLatch(1);
      List<Session> sessionList = Collections.synchronizedList(new ArrayList<>());
      phase.setComponents(new ConcurrentPoolImpl<>(() -> {
         Session session = SessionFactory.create(phase.definition().scenario, 0);
         sessionList.add(session);
         session.attach(httpClientPool.next());
         return session;
      }), sessionList, (p, status) -> {
         if (status == PhaseInstance.Status.TERMINATED) {
            latch.countDown();;
         }
      });
      phase.reserveSessions();
      phase.start(httpClientPool.executors());
      try {
         if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new AssertionError("statusCondition timeout");
         }
      } catch (InterruptedException e) {
         throw new AssertionError(e);
      }
      if (phase.getError() != null) {
         throw new AssertionError(phase.getError());
      }
      return sessionList;
   }

   @Before
   public void before(TestContext ctx) throws Exception {
      httpClientPool = HttpClientProvider.netty.builder().host("localhost")
            .versions(HttpVersion.ALL_VERSIONS)
            .port(8080)
            .concurrency(concurrency())
            .threads(threads())
            .size(connections())
            .build();
      Async clientPoolAsync = ctx.async();
      vertx = Vertx.vertx();
      router = Router.router(vertx);
      initRouter();
      vertx.createHttpServer().requestHandler(router::accept).listen(8080, "localhost",
            ctx.asyncAssertSuccess(server -> {
               httpClientPool.start(clientPoolAsync::complete);
      }));

   }

   protected abstract void initRouter();

   @After
   public void after(TestContext ctx) {
      httpClientPool.shutdown();
      vertx.close(ctx.asyncAssertSuccess());
   }

   protected ScenarioBuilder scenarioBuilder() {
      return BenchmarkBuilder.builder().simulation()
            .http()
               .baseUrl("http://localhost:8080")
            .endHttp()
            .addPhase("test").atOnce(1).duration(1).scenario();
   }

   protected int threads() {
      return 3;
   }

   protected int connections() {
      return 10;
   }

   protected int concurrency() {
      return 1;
   }
}