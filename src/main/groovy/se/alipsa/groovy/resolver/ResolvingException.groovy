package se.alipsa.groovy.resolver

class ResolvingException extends Exception {
  ResolvingException() {
    super()
  }

  ResolvingException(String message) {
    super(message)
  }

  ResolvingException(String message, Throwable cause) {
    super(message, cause)
  }
}
