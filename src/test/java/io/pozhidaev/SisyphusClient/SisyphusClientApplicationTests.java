package io.pozhidaev.SisyphusClient;

import io.pozhidaev.SisyphusClient.services.TusExecutorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SisyphusClientApplicationTests {

	@MockBean
	TusExecutorService tusExecutorService;

	@Test
	public void contextLoads() {
	}

}

