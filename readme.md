# Dependency Resolver

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