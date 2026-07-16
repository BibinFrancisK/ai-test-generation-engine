package com.testgen.github;

import com.testgen.model.GeneratedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubPrCreatorTest {

    private static final String BASE_URL = "http://localhost";
    private static final String OWNER = "owner";
    private static final String REPO = "repo";
    private static final String SOURCE_BRANCH = "feature/add-payment";
    private static final String SOURCE_SHA = "abc123sha";
    private static final int SOURCE_PR_NUMBER = 42;
    private static final String INSTALLATION_TOKEN = "installation-token-xyz";
    private static final String TEST_PR_URL = "https://github.com/owner/repo/pull/99";
    private static final String TEST_CODE = "class FooTest {}";

    @Test
    void deliversBranchCommitPrAndCommentInOrder() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();

        GitHubAppAuthenticator authenticator = mock(GitHubAppAuthenticator.class);
        when(authenticator.getInstallationToken()).thenReturn(INSTALLATION_TOKEN);

        GitHubPrCreator prCreator = new GitHubPrCreator(builder.build(), authenticator);

        List<String> callOrder = new ArrayList<>();
        String[] capturedBranchName = new String[1];
        String base64TestCode = Base64.getEncoder().encodeToString(TEST_CODE.getBytes(StandardCharsets.UTF_8));
        Pattern branchRefPattern = Pattern.compile("\"ref\":\"refs/heads/(testgen/[^\"]+)\"");

        mockServer.expect(requestTo(BASE_URL + "/repos/owner/repo/git/refs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + INSTALLATION_TOKEN))
                .andExpect(request -> {
                    callOrder.add("create-branch");
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    assertThat(body).contains("\"sha\":\"" + SOURCE_SHA + "\"");
                    var matcher = branchRefPattern.matcher(body);
                    assertThat(matcher.find()).isTrue();
                    capturedBranchName[0] = matcher.group(1);
                })
                .andRespond(withStatus(HttpStatus.CREATED));

        mockServer.expect(requestTo(BASE_URL + "/repos/owner/repo/contents/src/test/java/com/example/FooTest.java"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer " + INSTALLATION_TOKEN))
                .andExpect(request -> {
                    callOrder.add("commit-file");
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    assertThat(body).contains("\"branch\":\"" + capturedBranchName[0] + "\"");
                    assertThat(body).contains("\"content\":\"" + base64TestCode + "\"");
                })
                .andRespond(withStatus(HttpStatus.OK));

        mockServer.expect(requestTo(BASE_URL + "/repos/owner/repo/pulls"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + INSTALLATION_TOKEN))
                .andExpect(request -> {
                    callOrder.add("open-pr");
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    assertThat(body).contains("\"head\":\"" + capturedBranchName[0] + "\"");
                    assertThat(body).contains("\"base\":\"" + SOURCE_BRANCH + "\"");
                })
                .andRespond(withSuccess("{\"html_url\":\"" + TEST_PR_URL + "\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(BASE_URL + "/repos/owner/repo/issues/" + SOURCE_PR_NUMBER + "/comments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + INSTALLATION_TOKEN))
                .andExpect(request -> {
                    callOrder.add("post-comment");
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    assertThat(body).contains(TEST_PR_URL);
                })
                .andRespond(withStatus(HttpStatus.CREATED));

        GeneratedTest generatedTest = new GeneratedTest(
                "FooTest", "com.example", TEST_CODE, Path.of("tmp", "FooTest.java"), Instant.now());
        PrDeliveryRequest deliveryRequest = new PrDeliveryRequest(
                OWNER, REPO, SOURCE_BRANCH, SOURCE_SHA, SOURCE_PR_NUMBER,
                generatedTest, "SUCCESS", List.of("bar", "baz"));

        String result = prCreator.deliver(deliveryRequest);

        assertThat(result).isEqualTo(TEST_PR_URL);
        assertThat(callOrder).containsExactly("create-branch", "commit-file", "open-pr", "post-comment");
        assertThat(capturedBranchName[0]).matches("testgen/" + Pattern.quote(SOURCE_BRANCH) + "-[0-9a-f]{8}");
        mockServer.verify();
    }
}
