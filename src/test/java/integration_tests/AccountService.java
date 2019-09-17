package integration_tests;

import org.jsmart.zerocode.core.domain.JsonTestCase;
import org.jsmart.zerocode.core.domain.TargetEnv;
import org.jsmart.zerocode.core.runner.ZeroCodeUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@TargetEnv("server_host.properties")
@RunWith(ZeroCodeUnitRunner.class)
public class AccountService {

    @Test
    @JsonTestCase("load_tests/create_get_and_transfer_get_account.json")
    public void testTransfer() throws Exception {

    }



}
