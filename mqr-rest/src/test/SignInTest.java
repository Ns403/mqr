import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.molicloud.mqr.RobotApplication;
import com.molicloud.mqr.plugin.signin.entity.RobotPluginSignIn;
import com.molicloud.mqr.plugin.signin.mapper.RobotPluginSignInMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RobotApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SignInTest {
    @Autowired
    private RobotPluginSignInMapper mapper;
    private Boolean getTodaySignInCount(String groupId, String qq) {
        RobotPluginSignIn robotPluginSignIn = mapper.selectOne(Wrappers.<RobotPluginSignIn>lambdaQuery()
                .eq(RobotPluginSignIn::getGroupId, groupId)
                .eq(RobotPluginSignIn::getQq, qq)
                .ge(RobotPluginSignIn::getCreateTime, LocalDate.now()));
        return robotPluginSignIn != null;
    }
    @Test
    public void testMain(){
        Boolean todaySignInCount = getTodaySignInCount("1090197879", "1097793866");
        System.out.println(todaySignInCount);

    }
}
