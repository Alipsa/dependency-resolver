package test.alipsa.groovy.resolver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.xml.sax.SAXException
import se.alipsa.groovy.resolver.Dependency
import se.alipsa.groovy.resolver.MavenRepoLookup

import java.nio.charset.StandardCharsets
import java.net.InetSocketAddress

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class MavenRepoLookupTest {

  @Test
  void testFetchLatestArtifactDelegatesToArtifactLookup() {
    String metadata = """<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>com.example</groupId>
  <artifactId>demo</artifactId>
  <versioning>
    <release>1.2.3</release>
    <versions>
      <version>1.0.0</version>
      <version>1.2.3</version>
    </versions>
  </versioning>
</metadata>
"""
    HttpServer server = startMetadataServer(metadata, 200)
    try {
      Dependency dependency = MavenRepoLookup.fetchLatestArtifact("com.example", "demo", repositoryUrl(server))
      assertEquals("com.example", dependency.groupId)
      assertEquals("demo", dependency.artifactId)
      assertEquals("1.2.3", dependency.version)
    } finally {
      server.stop(0)
    }
  }

  @Test
  void testFetchLatestArtifactFallsBackToLatestWhenReleaseMissing() {
    String metadata = """<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>com.example</groupId>
  <artifactId>demo</artifactId>
  <versioning>
    <latest>2.0.0</latest>
    <versions>
      <version>1.0.0</version>
      <version>2.0.0</version>
    </versions>
  </versioning>
</metadata>
"""
    HttpServer server = startMetadataServer(metadata, 200)
    try {
      Dependency dependency = MavenRepoLookup.fetchLatestArtifact("com.example", "demo", repositoryUrl(server))
      assertEquals("2.0.0", dependency.version)
    } finally {
      server.stop(0)
    }
  }

  @Test
  void testFetchLatestArtifactShortStringReturnsInputOnLookupFailure() {
    HttpServer server = startMetadataServer("Not found", 404)
    try {
      String dep = "com.example:demo:1.0.0"
      assertEquals(dep, MavenRepoLookup.fetchLatestArtifactShortString(dep, repositoryUrl(server)))
    } finally {
      server.stop(0)
    }
  }

  @Test
  void testFetchLatestArtifactShortStringReturnsInputOnCorruptMetadata() {
    String corruptMetadata = "<metadata><versioning><release>1.2.3</release></versioning>"
    HttpServer server = startMetadataServer(corruptMetadata, 200)
    try {
      String dep = "com.example:demo:1.0.0"
      assertEquals(dep, MavenRepoLookup.fetchLatestArtifactShortString(dep, repositoryUrl(server)))
    } finally {
      server.stop(0)
    }
  }

  @Test
  void testFetchVersions() {
    String metadata = """<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>com.example</groupId>
  <artifactId>demo</artifactId>
  <versioning>
    <versions>
      <version>1.0.0</version>
      <version>1.2.3</version>
    </versions>
  </versioning>
</metadata>
"""
    HttpServer server = startMetadataServer(metadata, 200)
    try {
      assertEquals(["1.0.0", "1.2.3"], MavenRepoLookup.fetchVersions("com.example", "demo", repositoryUrl(server)))
    } finally {
      server.stop(0)
    }
  }

  @Test
  void testFetchVersionsRejectsDoctypeInMetadata() {
    String metadata = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE metadata [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<metadata>
  <groupId>com.example</groupId>
  <artifactId>demo</artifactId>
  <versioning>
    <versions>
      <version>&xxe;</version>
    </versions>
  </versioning>
</metadata>
"""
    HttpServer server = startMetadataServer(metadata, 200)
    try {
      assertThrows(SAXException) {
        MavenRepoLookup.fetchVersions("com.example", "demo", repositoryUrl(server))
      }
    } finally {
      server.stop(0)
    }
  }

  @Test
  void testFetchVersionsThrowsOnCorruptMetadata() {
    String corruptMetadata = "<metadata><versioning><versions><version>1.0.0</version></versions>"
    HttpServer server = startMetadataServer(corruptMetadata, 200)
    try {
      assertThrows(SAXException) {
        MavenRepoLookup.fetchVersions("com.example", "demo", repositoryUrl(server))
      }
    } finally {
      server.stop(0)
    }
  }

  private static HttpServer startMetadataServer(String responseBody, int statusCode) {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/com/example/demo/maven-metadata.xml") { HttpExchange exchange ->
      byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8)
      exchange.responseHeaders.set("Content-Type", "application/xml")
      exchange.sendResponseHeaders(statusCode, payload.length)
      exchange.responseBody.withCloseable { it.write(payload) }
    }
    server.start()
    return server
  }

  private static String repositoryUrl(HttpServer server) {
    return "http://127.0.0.1:${server.address.port}/"
  }
}
