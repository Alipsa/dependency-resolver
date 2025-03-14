package se.alipsa.groovy.resolver

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.SAXException

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

@CompileStatic
class MavenRepoLookup {

  private static final Logger log = LoggerFactory.getLogger(MavenRepoLookup.class)

  /**
   * @param dependency in the "short form" i.e. groupid:artifact:id:version
   * @param repositoryUrl e.g. <a href="https://repo1.maven.org/maven2/">https://repo1.maven.org/maven2/</a>
   * @return a Dependency representing the artifact
   */
  static Dependency fetchLatestArtifact(String dependency, String repositoryUrl) throws ParserConfigurationException, IOException, SAXException {
    String[] depExp = dependency.split(":")
    String groupId = depExp[0]
    String artifactId = depExp[1]
   return fetchLatestArtifact(groupId, artifactId, repositoryUrl)
  }

  static String fetchLatestArtifactShortString(String dependency, String repositoryUrl) {
    try {
      Dependency artifact = fetchLatestArtifact(dependency, repositoryUrl)
      return toShortDependency(artifact.getGroupId(),artifact.getArtifactId(), artifact.getVersion());
    } catch (ParserConfigurationException | IOException | SAXException e) {
      return dependency
    }
  }

  static Dependency fetchLatestArtifact(String groupId, String artifactId, String repositoryUrl) throws ParserConfigurationException, IOException, SAXException {
    String url = metaDataUrl(groupId,artifactId, repositoryUrl)

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance()
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder()
    Document doc = docBuilder.parse(url)
    Element versioning = (Element)doc.getDocumentElement().getElementsByTagName("versioning").item(0)
    Element release = (Element)versioning.getElementsByTagName("release").item(0)
    String version = release.getTextContent()

    return new Dependency(groupId, artifactId, version)
  }

  static List<String> fetchVersions(String groupId, String artifactId, String repositoryUrl) throws ParserConfigurationException, IOException, SAXException {
    String url = metaDataUrl(groupId,artifactId, repositoryUrl)

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance()
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder()
    Document doc = docBuilder.parse(url)
    Element versioning = (Element)doc.getDocumentElement().getElementsByTagName("versioning").item(0)
    Element versions = (Element)versioning.getElementsByTagName("versions").item(0)
    NodeList nodeList = versions.getElementsByTagName("version")
    List<String> versionList = new ArrayList<>(nodeList.getLength())
    for (int i = 0; i < nodeList.getLength(); i++) {
      var node = (Element)nodeList.item(i)
      versionList.add(node.getTextContent())
    }

    return versionList
  }

  static String artifactUrl(String groupId, String artifactId, String version, String repositoryUrl) {
    return repositoryUrl + subDir(groupId,artifactId,version) + jarFile(artifactId, version)
  }

  static String artifactUrl(Dependency dependency, String repositoryUrl) {
    return artifactUrl(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), repositoryUrl)
  }

  static String pomUrl(String groupId, String artifactId, String version, String repositoryUrl) {
    return repositoryUrl + subDir(groupId,artifactId,version) + pomFile(artifactId, version)
  }

  static String pomUrl(Dependency dependency, String repositoryUrl) {
    return pomUrl(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), repositoryUrl)
  }

  static String jarFile(String artifactId, String version) {
    return artifactId + "-" + version + ".jar"
  }

  static String jarFile(Dependency dependency) {
    return jarFile(dependency.getArtifactId(), dependency.getVersion())
  }

  static String pomFile(String artifactId, String version) {
    return artifactId + "-" + version + ".pom"
  }

  static String pomFile(Dependency dependency) {
    return pomFile(dependency.getArtifactId(), dependency.getVersion())
  }

  static String subDir(String groupId, String artifactId, String version) {
    return groupUrlPart(groupId) + artifactId + "/" + version + "/"
  }

  static String subDir(Dependency dependency) {
    return subDir(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
  }

  static String groupUrlPart(String groupId) {
    return groupId.replace('.', '/') + "/"
  }

  static String metaDataUrl(String groupId, String artifactId, String repositoryUrl) {
    return repositoryUrl + groupUrlPart(groupId) + artifactId + "/maven-metadata.xml"
  }

  static String toShortDependency(String groupId, String artifactId, String version) {
    return groupId + ":" + artifactId + ":" + version
  }
}
