package ltd.fdsa.job.admin.controller;

import lombok.extern.slf4j.Slf4j;
import ltd.fdsa.job.admin.jpa.service.impl.SystemUserServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Slf4j
public class JobInfoControllerTest extends AbstractSpringMvcTest {

    private Cookie cookie;

    @Before
    public void login() throws Exception {
        MvcResult ret =
                mockMvc
                        .perform(
                                post("/login")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("userName", "admin")
                                        .param("password", "123456"))
                        .andReturn();
        cookie = ret.getResponse().getCookie(SystemUserServiceImpl.USER_LOGIN_IDENTITY);
    }

    @Test
    public void testAdd() throws Exception {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("jobGroup", "1");

        MvcResult ret =
                mockMvc
                        .perform(
                                post("/jobinfo/pageList")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        // .content(paramsJson)
                                        .params(parameters)
                                        .cookie(cookie))
                        .andReturn();

        log.info(ret.getResponse().getContentAsString());
    }
}
