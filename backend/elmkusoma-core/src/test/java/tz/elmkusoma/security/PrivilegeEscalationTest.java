package tz.elmkusoma.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class PrivilegeEscalationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void studentCannotAccessAdminEndpoints() throws Exception {
        String studentToken = TestTokens.studentToken();

        mockMvc.perform(get("/v1/admin")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/admin/people")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/admin/settings")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/admin/roles")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotAccessInstitutionAdminEndpoints() throws Exception {
        String teacherToken = TestTokens.teacherToken();

        mockMvc.perform(get("/v1/admin/people")
                .header("Authorization", "Bearer " + teacherToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/admin/roles")
                .header("Authorization", "Bearer " + teacherToken)
                .contentType("application/json")
                .content("{\"name\":\"test\",\"displayName\":\"Test\",\"permissions\":[]}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotPromoteToAdmin() throws Exception {
        String studentToken = TestTokens.studentToken();
        String adminUserId = TestDataSeeder.ADMIN_USER_ID.toString();

        mockMvc.perform(put("/v1/admin/people/" + adminUserId + "/role")
                .header("Authorization", "Bearer " + studentToken)
                .param("newRole", "ADMIN"))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotCreateCustomRole() throws Exception {
        String studentToken = TestTokens.studentToken();

        mockMvc.perform(post("/v1/admin/roles")
                .header("Authorization", "Bearer " + studentToken)
                .contentType("application/json")
                .content("{\"name\":\"EVIL_ROLE\",\"displayName\":\"Evil\",\"permissions\":[\"*\"]}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotAccessPlatformAdminEndpoints() throws Exception {
        String studentToken = TestTokens.studentToken();

        mockMvc.perform(get("/v1/platform-admin")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/platform-admin/users")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotDeleteOtherTeacher() throws Exception {
        String teacherToken = TestTokens.teacherToken();
        String otherTeacherId = "11111111-1111-1111-1111-111111111111";

        mockMvc.perform(delete("/v1/teachers/" + otherTeacherId)
                .header("Authorization", "Bearer " + teacherToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotModifyOtherStudentData() throws Exception {
        String studentToken = TestTokens.studentToken();
        String otherStudentId = TestDataSeeder.OTHER_STUDENT_USER_ID.toString();

        mockMvc.perform(put("/v1/students/" + otherStudentId)
                .header("Authorization", "Bearer " + studentToken)
                .contentType("application/json")
                .content("{\"firstName\":\"Hacked\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotEnableService() throws Exception {
        String studentToken = TestTokens.studentToken();

        mockMvc.perform(post("/v1/admin/services/live_classes/enable")
                .header("Authorization", "Bearer " + studentToken)
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotAccessActuator() throws Exception {
        String studentToken = TestTokens.studentToken();

        mockMvc.perform(get("/actuator/health")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }
}