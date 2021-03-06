package sparkles.support.javalin.testing;

import io.javalin.Javalin;
import okhttp3.Response;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JavalinTestRunner.class)
public class JavalinApplicationTest {

  @TestClient
  private HttpClient client;

  @TestApp
  private Javalin testApp() {
    return Javalin.create()
      .get("/", (ctx) -> ctx.res.getWriter().print("foobar"));
  }

  @Test
  public void itRuns() throws Exception {
    Response response = client.get("/").send();
    assertThat(response.body().string()).isEqualTo("foobar");
  }

}
