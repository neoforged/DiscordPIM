package net.neoforged.discord.bots.pim.dba;

import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;
import net.neoforged.discord.bots.pim.dba.model.RoleRemovalJob;
import org.junit.jupiter.api.*;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DBATest {
    private DBA dba;
    private static final String TEST_DB_PATH = "data/pim_test.db";

    @BeforeAll
    void setup() {
        // Remove test DB if exists
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) dbFile.delete();
        dba = new DBA("jdbc:sqlite:" + TEST_DB_PATH);
    }

    @AfterAll
    void cleanup() {
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) dbFile.delete();
    }

    @Test
    void testRoleConfigurationCRUD() {
        dba.createRoleConfiguration("TestRole", 1L, true, 3600, 123L, 456L);
        RoleConfiguration config = dba.getRoleConfiguration("TestRole", 1L);
        assertNotNull(config);
        assertEquals("TestRole", config.getName());
        config.setGrantedTimeInSeconds(7200);
        dba.updateRoleConfiguration(config);
        RoleConfiguration updated = dba.getRoleConfiguration("TestRole", 1L);
        assertEquals(7200, updated.getGrantedTimeInSeconds());
        dba.deleteRoleConfiguration(updated.getId());
        assertNull(dba.getRoleConfiguration("TestRole", 1L));
    }

    @Test
    void testPendingRoleRequestCRUD() {
        PendingRoleRequest req = dba.createPendingRoleRequest("RoleA", 2L, "reason", 1L);
        assertNotNull(req.getId());
        PendingRoleRequest fetched = dba.getPendingRoleRequest("RoleA", 2L, 1L);
        assertNotNull(fetched);
        fetched.setReason("new reason");
        dba.updatePendingRoleRequest(fetched);
        PendingRoleRequest updated = dba.getPendingRoleRequest("RoleA", 2L, 1L);
        assertEquals("new reason", updated.getReason());
        dba.deletePendingRoleRequest(updated);
        assertNull(dba.getPendingRoleRequest("RoleA", 2L, 1L));
    }

    @Test
    void testRoleRemovalJobCRUD() {
        dba.createRoleRemovalJob("RoleB", 1L, 10L, 2L, 3600);
        List<RoleRemovalJob> jobs = dba.getOpenRemovalJobs();
        assertFalse(jobs.isEmpty());
        RoleRemovalJob job = jobs.get(0);
        dba.removeRemovalJob(job);
        assertTrue(dba.getOpenRemovalJobs().isEmpty());
    }
}

