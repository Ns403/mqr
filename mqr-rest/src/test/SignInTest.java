import com.molicloud.mqr.RobotApplication;
import com.molicloud.mqr.mapper.GroupMuteAllConfigMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RobotApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Profile("local")
public class SignInTest {
    @Autowired
    private GroupMuteAllConfigMapper mapper;

    @Test
    public void testMain() {
        String all = mapper.getAll();
        System.out.println(all);
    }
}
