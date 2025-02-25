package se.alipsa.groovy.resolver

import groovy.transform.CompileStatic;

@CompileStatic
class Dependency {

  private String groupId
  private String artifactId
  private String version

  Dependency() {
  }

  Dependency(String groupId, String artifactId, String version) {
    this.groupId = groupId
    this.artifactId = artifactId
    this.version = version
  }

  Dependency(String dependencyString) {
    var parts = dependencyString.split(":")
    if (parts.length != 3) {
      throw new IllegalArgumentException("Incorrect format for dependency for " + dependencyString);
    }
    this.groupId = parts[0]
    this.artifactId = parts[1]
    this.version = parts[2]
  }

  void setGroupId(String groupId) {
    this.groupId = groupId
  }

  String getGroupId() {
    return groupId
  }

  void setArtifactId(String artifactId) {
    this.artifactId = artifactId
  }

  String getArtifactId() {
    return artifactId
  }

  void setVersion(String version) {
    this.version = version
  }

  String getVersion() {
    return version
  }

  @Override
  String toString() {
    return groupId + ":" + artifactId + ":" + version
  }
}
