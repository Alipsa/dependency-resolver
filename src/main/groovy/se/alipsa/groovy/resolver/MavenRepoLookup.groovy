package se.alipsa.groovy.resolver

import groovy.transform.CompileStatic
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import se.alipsa.mavenutils.ArtifactLookup

import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

@CompileStatic
class MavenRepoLookup {

  private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl"
  private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities"
  private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities"
  private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd"

  /**
   * @param dependency in short form, e.g. groupId:artifactId[:type[:classifier]]
   * @param repositoryUrl e.g. <a href="https://repo1.maven.org/maven2/">https://repo1.maven.org/maven2/</a>
   * @return a Dependency representing the artifact
   */
  static Dependency fetchLatestArtifact(String dependency, String repositoryUrl) {
    String[] depExp = dependency.split(":")
    if (depExp.length < 2) {
      throw new IllegalArgumentException("dependency must be in the format groupId:artifactId")
    }
    String groupId = depExp[0]
    String artifactId = depExp[1]
    return fetchLatestArtifact(groupId, artifactId, repositoryUrl)
  }

  static String fetchLatestArtifactShortString(String dependency, String repositoryUrl) {
    try {
      Dependency artifact = fetchLatestArtifact(dependency, repositoryUrl)
      return toShortDependency(artifact.groupId, artifact.artifactId, artifact.version)
    } catch (RuntimeException e) {
      return dependency
    }
  }

  static Dependency fetchLatestArtifact(String groupId, String artifactId, String repositoryUrl) {
    ArtifactLookup lookup = new ArtifactLookup(repositoryUrl)
    String version = lookup.fetchLatestVersion(groupId, artifactId)
    return new Dependency(groupId, artifactId, version)
  }

  static List<String> fetchVersions(String groupId, String artifactId, String repositoryUrl) throws ParserConfigurationException, IOException, SAXException {
    String url = metaDataUrl(groupId, artifactId, repositoryUrl)

    Document doc = parseXml(url)
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
    return "$repositoryUrl${subDir(groupId, artifactId, version)}${jarFile(artifactId, version)}"
  }

  static String artifactUrl(Dependency dependency, String repositoryUrl) {
    return artifactUrl(dependency.groupId, dependency.artifactId, dependency.version, repositoryUrl)
  }

  static String pomUrl(String groupId, String artifactId, String version, String repositoryUrl) {
    return "$repositoryUrl${subDir(groupId, artifactId, version)}${pomFile(artifactId, version)}"
  }

  static String pomUrl(Dependency dependency, String repositoryUrl) {
    return pomUrl(dependency.groupId, dependency.artifactId, dependency.version, repositoryUrl)
  }

  static String jarFile(String artifactId, String version) {
    return "$artifactId-${version}.jar"
  }

  static String jarFile(Dependency dependency) {
    return jarFile(dependency.artifactId, dependency.version)
  }

  static String pomFile(String artifactId, String version) {
    return "$artifactId-${version}.pom"
  }

  static String pomFile(Dependency dependency) {
    return pomFile(dependency.artifactId, dependency.version)
  }

  static String subDir(String groupId, String artifactId, String version) {
    return "${groupUrlPart(groupId)}$artifactId/$version/"
  }

  static String subDir(Dependency dependency) {
    return subDir(dependency.groupId, dependency.artifactId, dependency.version)
  }

  static String groupUrlPart(String groupId) {
    return "${groupId.replace('.', '/')}/"
  }

  static String metaDataUrl(String groupId, String artifactId, String repositoryUrl) {
    return "$repositoryUrl${groupUrlPart(groupId)}$artifactId/maven-metadata.xml"
  }

  static String toShortDependency(String groupId, String artifactId, String version) {
    return "$groupId:$artifactId:$version"
  }

  private static Document parseXml(String url) throws ParserConfigurationException, IOException, SAXException {
    return createSecureDocumentBuilder().parse(url)
  }

  private static DocumentBuilder createSecureDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance()
    docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    docFactory.setFeature(DISALLOW_DOCTYPE_DECL, true)
    docFactory.setFeature(EXTERNAL_GENERAL_ENTITIES, false)
    docFactory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false)
    docFactory.setFeature(LOAD_EXTERNAL_DTD, false)
    docFactory.setXIncludeAware(false)
    docFactory.setExpandEntityReferences(false)
    docFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
    docFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    return docFactory.newDocumentBuilder()
  }
}
