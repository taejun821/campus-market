package com.example.campusmarket;

import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class CampusMarketApplicationTests {

    @MockitoBean
    Firestore firestore;

    @Test
    void contextLoads() {
    }

}
