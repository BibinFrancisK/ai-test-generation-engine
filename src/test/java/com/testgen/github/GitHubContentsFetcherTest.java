package com.testgen.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubContentsFetcherTest {

    private static final String BASE_URL = "http://localhost";

    private MockRestServiceServer mockServer;
    private GitHubContentsFetcher fetcher;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        fetcher = new GitHubContentsFetcher(builder.build());
    }

    @Test
    void decodesBase64ContentFromContentsApi() {
        String decodedSource = "package com.example;\n\npublic class Foo {}\n";
        String base64Content = Base64.getEncoder().encodeToString(decodedSource.getBytes());

        mockServer.expect(requestTo(BASE_URL + "/repos/owner/repo/contents/src/main/java/com/example/Foo.java?ref=main"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"content\":\"" + base64Content + "\",\"encoding\":\"base64\"}",
                        MediaType.APPLICATION_JSON));

        Optional<String> result = fetcher.fetchFileContent("owner", "repo", "src/main/java/com/example/Foo.java", "main");

        assertThat(result).contains(decodedSource);
    }

    @Test
    void returnsEmptyOn404() {
        mockServer.expect(requestTo(BASE_URL + "/repos/owner/repo/contents/src/main/java/com/example/Missing.java?ref=main"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        Optional<String> result = fetcher.fetchFileContent("owner", "repo", "src/main/java/com/example/Missing.java", "main");

        assertThat(result).isEmpty();
    }

    @Test
    void findTestFileDerivesConventionalTestPath() {
        String decodedSource = "package com.example;\n\nclass FooTest {}\n";
        String base64Content = Base64.getEncoder().encodeToString(decodedSource.getBytes());

        mockServer.expect(requestTo(BASE_URL + "/repos/owner/repo/contents/src/test/java/com/example/FooTest.java?ref=main"))
                .andRespond(withSuccess(
                        "{\"content\":\"" + base64Content + "\",\"encoding\":\"base64\"}",
                        MediaType.APPLICATION_JSON));

        Optional<String> result = fetcher.findTestFile("owner", "repo", "src/main/java/com/example/Foo.java", "main");

        assertThat(result).contains(decodedSource);
    }

    @Test
    void fetchDependencySourcesFiltersOutFrameworkImportsAndRespectsMaxFiles() {
        String source = """
                package com.example;

                import java.util.List;
                import org.springframework.stereotype.Component;
                import org.junit.jupiter.api.Test;
                import com.example.dep.Alpha;
                import com.example.dep.Beta;
                import com.example.dep.Gamma;

                class Foo {}
                """;

        String alphaSource = "class Alpha {}";
        String betaSource = "class Beta {}";
        String alphaBase64 = Base64.getEncoder().encodeToString(alphaSource.getBytes());
        String betaBase64 = Base64.getEncoder().encodeToString(betaSource.getBytes());

        mockServer.expect(requestTo(BASE_URL + "/repos/owner/repo/contents/src/main/java/com/example/dep/Alpha.java?ref=main"))
                .andRespond(withSuccess("{\"content\":\"" + alphaBase64 + "\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(BASE_URL + "/repos/owner/repo/contents/src/main/java/com/example/dep/Beta.java?ref=main"))
                .andRespond(withSuccess("{\"content\":\"" + betaBase64 + "\"}", MediaType.APPLICATION_JSON));

        List<String> result = fetcher.fetchDependencySources("owner", "repo", source, "main", 2);

        assertThat(result).containsExactly(alphaSource, betaSource);
        mockServer.verify();
    }
}
