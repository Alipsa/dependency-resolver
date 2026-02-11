package se.alipsa.groovy.resolver

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.maven.model.building.ModelBuildingException
import org.apache.maven.settings.building.SettingsBuildingException
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.alipsa.mavenutils.DependenciesResolveException
import se.alipsa.mavenutils.MavenUtils

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@CompileStatic
class DependencyResolver {

  private static final Logger log = LoggerFactory.getLogger(DependencyResolver)
  private static final int DOWNLOAD_CONNECT_TIMEOUT_MS = 10_000
  private static final int DOWNLOAD_READ_TIMEOUT_MS = 30_000

  private URLClassLoader classLoader = null

  DependencyResolver() {
  }

  DependencyResolver(GroovyClassLoader classLoader) {
    this((URLClassLoader) classLoader)
  }

  DependencyResolver(URLClassLoader classLoader) {
    this()
    this.classLoader = classLoader
  }

  DependencyResolver(Class callingClass) {
    this()

    if (!(callingClass.classLoader instanceof GroovyClassLoader)) {
      log.warn("Expected a GroovyClassloader but was {}", callingClass.classLoader)
      throw new IllegalArgumentException("The calling class must be loaded by the groovy classloader")
    }
    this.classLoader = (GroovyClassLoader)callingClass.classLoader
  }

  DependencyResolver(Object caller) {
    this(caller.class)
  }

  void addDependency(String groupId, String artifactId, String version) throws ResolvingException {
    addDependency(new Dependency(groupId, artifactId, version))
  }

  void addDependency(String dependency) throws ResolvingException {
    addDependency(new Dependency(dependency))
  }

  void addDependency(Dependency dep) throws ResolvingException {
    List<File> artifacts = resolve(dep.groupId, dep.artifactId, dep.version)
    if (classLoader == null) {
      log.error("No classloader available, you must add a URLClassLoader (e.g. GroovyClassLoader) before adding dependencies")
      throw new ResolvingException("You must add a URLClassLoader (e.g. GroovyClassLoader) before adding dependencies")
    }
    try {
      for (File artifact : artifacts) {
        addURLToClassLoader(artifact.toURI().toURL())
      }
    } catch (MalformedURLException e) {
      log.warn("Failed to convert the downloaded file to a URL", e)
      throw new ResolvingException("Failed to convert the downloaded file to a URL", e)
    }
  }

  @CompileDynamic
  private void addURLToClassLoader(URL url) {
    classLoader.addURL(url)
  }

  List<File> resolve(String groupId, String artifactId, String version) throws ResolvingException {
    List<File> jarFiles = []
    resolve(new Dependency(groupId, artifactId, version), jarFiles)
    jarFiles
  }

  List<File> resolve(String dependency) throws ResolvingException {
    List<File> jarFiles = []
    resolve(new Dependency(dependency), jarFiles)
    jarFiles
  }

  void resolve(Dependency dependency, List<File> jarFiles) throws ResolvingException {
    MavenUtils mavenUtils = new MavenUtils()
    File artifact
    try {
      artifact = mavenUtils.resolveArtifact(
          dependency.groupId,
          dependency.artifactId,
          null,
          "jar",
          dependency.version
      )
    } catch (SettingsBuildingException | ArtifactResolutionException e) {
      log.warn("Failed to resolve artifact {}", dependency)
      throw new ResolvingException("Failed to resolve artifact $dependency")
    }
    jarFiles << artifact
    File pomFile = new File(artifact.parent, artifact.name.replace(".jar", ".pom"))
    if (!pomFile.exists()) {
      String url
      for (RemoteRepository remoteRepository : mavenUtils.remoteRepositories) {
        url = MavenRepoLookup.pomUrl(dependency, remoteRepository.url)
        log.debug("Trying {}", url)
        try {
          download(url, pomFile)
          log.debug("Download of {} successful", url)
          break
        } catch (IOException ignored) {
          log.debug("Download of {} failed", url)
        }
      }
      if (!pomFile.exists()) {
        throw new ResolvingException("Failed to resolve pom file ($pomFile) for $dependency")
      }
    }
    try {
      for (File file : mavenUtils.resolveDependencies(pomFile)) {
        if (file.name.toLowerCase().endsWith(".jar")) {
          jarFiles << file
        }
      }
    } catch (SettingsBuildingException | ModelBuildingException | DependenciesResolveException e) {
      log.warn("Failed to resolve pom file {}", pomFile)
      throw new ResolvingException("Failed to resolve pom file $pomFile")
    }
  }

  private void download(String urlString, File localFilename) throws IOException {
    URL url = new URL(urlString)
    URLConnection connection = url.openConnection()
    connection.setConnectTimeout(DOWNLOAD_CONNECT_TIMEOUT_MS)
    connection.setReadTimeout(DOWNLOAD_READ_TIMEOUT_MS)
    File tempFile = File.createTempFile(localFilename.name, ".part", localFilename.parentFile)
    try {
      try (
          InputStream inputStream = connection.getInputStream()
          ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream)
          FileOutputStream fileOutputStream = new FileOutputStream(tempFile)
          FileChannel fileChannel = fileOutputStream.getChannel()) {
        fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
      }
      try {
        Files.move(tempFile.toPath(), localFilename.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(tempFile.toPath(), localFilename.toPath(), StandardCopyOption.REPLACE_EXISTING)
      }
    } finally {
      if (tempFile.exists()) {
        tempFile.delete()
      }
    }
  }
}
