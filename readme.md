# Dependency Resolver

This is a dependency resolver similar to Grapes grab based on se.alipsa:maven-utils.
To use it add the following dependencies to your pom
```groovy
implementation('org.apache.groovy:groovy:5.0.4')
implementation('se.alipsa.groovy:dependency-resolver:1.0.2')
```
...or the equivalent for maven, ivy etc.

## Example usage
The following code:
```groovy
    String depScript = '''
    import se.alipsa.groovy.resolver.DependencyResolver
    def resolver = new DependencyResolver(this)
    resolver.addDependency('com.googlecode.libphonenumber:libphonenumber:8.13.26')    
    '''
    String script = '''
    import com.google.i18n.phonenumbers.PhoneNumberUtil
    def numberUtil = PhoneNumberUtil.getInstance()
    def phoneNumber = numberUtil.parse('+46 70 12 23 198', 'SE')
    println "Phone number is ${numberUtil.isValidNumber(phoneNumber) ? '' : 'NOT '}valid"
    '''
    def shell = new GroovyShell()
    shell.evaluate(depScript)
    shell.evaluate(script)
```
Produces:
```
Phone number is valid
```

## Tests
Run fast, hermetic tests (default):
```bash
./gradlew test
```

Run network-dependent integration tests:
```bash
./gradlew integrationTest
```
