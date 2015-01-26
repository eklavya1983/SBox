import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Context;
import models.Entry;
import org.junit.*;

import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.test.*;
import play.libs.F.*;

import java.io.IOException;
import java.util.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class IntegrationTest {
    final static int REQUEST_TIMEOUT_MS = 1000;
    static Random rand = new Random();
    static String tags[] = {"Hawaii trip", "With mom", "park", "napa trip"};
    static Comparator<Context> contextComparator = (c1, c2) -> c1.getName().compareTo(c2.getName());
    static Comparator<Entry> entryComparator = (e1, e2) -> new Long(e1.getEid()).compareTo(e2.getEid());
    static Comparator<Entry> entryChrnoComparator = (e1, e2) -> new Long(e1.getTimestamp()).compareTo(e2.getTimestamp());

    Hashtable<String, Context> contextTbl = new Hashtable<String, Context>();

    static Entry getFakeEntry() {
        Entry e = new Entry();
        e.setUserid("Rao");
        e.setTimestamp(rand.nextLong());
        e.setLocation(String.format("%d, %d", rand.nextInt(200), rand.nextInt(200)));
        e.setMimeType("application/text");
        e.setText("text from location: " + e.getLocation());
        /* Generate set of contexts */
        Set<Context> contextSet = new TreeSet<Context>((e1, e2) -> e1.getName().compareTo(e2.getName()));
        int numContexts = rand.nextInt(tags.length);
        for (int i = 0; i < numContexts; i++) {
            Context c = new Context();
            c.setName(tags[rand.nextInt(tags.length)]);
            contextSet.add(c);
        }
        e.setContexts(new ArrayList<Context>(contextSet));
        return e;
    }

    static boolean compareContexs(Context c1, Context c2, boolean recurse) {
        boolean ret = c1.getName().equals(c2.getName()) &&
                compareEntryLists(c1.getEntries(), c2.getEntries(), recurse);
        return ret;
    }

    static boolean compareEntryLists(List<Entry> el1, List<Entry> el2, boolean recurse)
    {
        boolean ret = el1.size() == el2.size();

        if (ret) {
            for (int i = 0; i < el1.size(); i++) {
                if (recurse) {
                    ret = compareEntries(el1.get(i), el2.get(i), false);
                } else {
                    ret = (el1.get(i).getEid() == el2.get(i).getEid());
                }
                if (!ret) {
                    return false;
                }
            }
        }
        return ret;

    }

    static boolean compareEntries(Entry e1, Entry e2, boolean recurse) {
        boolean ret = (e1.getEid() == e2.getEid() &&
                e1.getUserid().equals(e2.getUserid()) &&
                e1.getTimestamp() == e2.getTimestamp() &&
                e1.getLocation().equals(e2.getLocation()) &&
                e1.getMimeType().equals(e2.getMimeType()) &&
                e1.getText().equals(e2.getText()) &&
                e1.getContexts().size() == e2.getContexts().size());

        if (ret) {
            List<Context> cl1 = e1.getContexts();
            List<Context> cl2 = e2.getContexts();
            cl1.sort(contextComparator);
            cl2.sort(contextComparator);
            for (int i = 0; i < cl1.size(); i++) {
               if (recurse) {
                   ret = compareContexs(cl1.get(i), cl2.get(i), false);
               } else {
                   ret = cl1.get(i).getName().equals(cl2.get(i).getName());
               }
               if (!ret) {
                   return false;
               }
           }
        }
        return ret;
    }

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
            /* Put entry */
            Entry e1 = getFakeEntry();
            testPutEntry(e1);

            /* Get entry */
            Entry respE1 = testGetEntry(e1.getEid()) ;

            Assert.assertTrue(compareEntries(e1, respE1, false));

            testGetEntriesForMultiContexts();

        });
    }

    public void testPutEntry(Entry e)
    {
        JsonNode jsonEntry = Json.toJson(e);
        WSResponse wsResponse = WS.url("http://localhost:3333/entry")
                .put(jsonEntry)
                .get(REQUEST_TIMEOUT_MS);

        Assert.assertEquals(wsResponse.getStatus(), 200);

        JsonNode response = wsResponse.asJson();
        e.setEid(response.asInt());

        /* Update contextTbl */
        List<Context> eContexts = e.getContexts();
        for (Context ec : eContexts) {
            Context c = contextTbl.get(ec.getName());
            if (c == null) {
                c = new Context();
                c.setName(ec.getName());
                c.setEntries(new ArrayList<Entry>());
                contextTbl.put(ec.getName(), c);
            }
            c.getEntries().add(e);
        }
    }

    public Entry testGetEntry(long eid) {
        WSResponse wsResponse = WS.url("http://localhost:3333/entry")
                .setQueryParameter("eid", String.valueOf(eid))
                .get()
                .get(REQUEST_TIMEOUT_MS);

        Assert.assertEquals(wsResponse.getStatus(), 200);

        Entry entry = Json.fromJson(wsResponse.asJson(), Entry.class);
        return entry;
    }

    public List<Entry> testGetEntries(String cName) {
        WSResponse wsResponse = WS.url("http://localhost:3333/entries")
                .setQueryParameter("cName", cName)
                .get()
                .get(REQUEST_TIMEOUT_MS);

        Assert.assertEquals(wsResponse.getStatus(), 200);


        List<Entry> entryList = null;
        try {
            entryList = new ObjectMapper().readValue(wsResponse.asByteArray(), new TypeReference<List<Entry>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryList;
    }

    public void testGetEntriesForMultiContexts() {
        int entryCnt = 100;
        /* First put the entries */
        for (int i = 0; i < entryCnt; i++) {
            Entry e = getFakeEntry();
            testPutEntry(e);
        }

        /* Compare stored entries with what's queried from database */
        for (String cName : contextTbl.keySet()){
            List<Entry> retEntries = testGetEntries(cName);
            List<Entry> storedEntries = contextTbl.get(cName).getEntries();
            storedEntries.sort(entryChrnoComparator);

            Assert.assertTrue(compareEntryLists(storedEntries, retEntries, true));
        }
    }

}
