import com.fasterxml.jackson.databind.JsonNode;
import models.Entry;
import org.junit.*;

import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

import static org.fluentlenium.core.filter.FilterConstructor.*;

public class IntegrationTest {
    final static int REQUEST_TIMEOUT_MS = 1000;

    /**
     * add your integration test here
     * in this example we just check if the welcome page is being shown
     */
    @Test
    @Ignore
    public void test() {
        running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                browser.goTo("http://localhost:3333");
                assertThat(browser.pageSource()).contains("Your new application is ready.");
            }
        });
    }
    @Test
    public void testInServer() {
        running(testServer(3333, fakeApplication(inMemoryDatabase())), () -> {
            System.out.println("Started");
            try {
                /* Load entry e1 */
                JsonNode jsonEntry = Json.parse(new FileInputStream(new File("test-data/entry1.json")));
                Entry e1 = Json.fromJson(jsonEntry, Entry.class);

                /* Put entry */
                JsonNode response = WS.url("http://localhost:3333/entry")
                        .put(jsonEntry)
                        .get(REQUEST_TIMEOUT_MS)
                        .asJson();
                e1.setEid(response.asInt());

                /* Get entry */
                response = WS.url("http://localhost:3333/entry")
                        .setQueryParameter("eid", String.valueOf(e1.getEid()))
                        .get()
                        .get(REQUEST_TIMEOUT_MS)
                        .asJson();
                Entry respE1 = Json.fromJson(response, Entry.class);

                /* Ensure put entry matches get entry */
                Assert.assertTrue(e1.getEid() == respE1.getEid());
                Assert.assertTrue(e1.getContexts().size() == respE1.getContexts().size());
                // TODO(ekl): match other attributes as well


                // TODO(ekl): test get entries

            } catch (Exception e) {
                Assert.fail();
            }
        });
    }

}
