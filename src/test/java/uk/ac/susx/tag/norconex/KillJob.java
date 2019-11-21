package uk.ac.susx.tag.norconex;

import com.enioka.jqm.api.JqmClientFactory;
import org.junit.Test;
import uk.ac.casm.jqm.manager.SubmissionService;

import java.util.Properties;

public class KillJob {

    @Test
    public void KillJob(){
        Properties props = new Properties();
        props.put("com.enioka.jqm.ws.url", "http://localhost:49910/ws/client");

//        props.put("com.enioka.jqm.ws.url", "https://jqm.casmconsulting.co.uk/ws/client");

        JqmClientFactory.setProperties(props);
        JqmClientFactory.getClient().killJob(1568);
    }


}
