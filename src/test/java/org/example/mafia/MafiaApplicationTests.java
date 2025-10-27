package org.example.mafia;

import org.example.mafia.controller.MafiaGameController;
import org.example.mafia.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MafiaApplicationTests {

    @Autowired
    private GameService gameService;

    @Autowired
    private MafiaGameController mafiaGameController;

    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
        assertNotNull(gameService);
        assertNotNull(mafiaGameController);
    }

}
