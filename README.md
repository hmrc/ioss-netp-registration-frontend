
# ioss-netp-registration-frontend

This is a repository for Import One Stop Shop NETP Registration Frontend

Backend: https://github.com/hmrc/ioss-netp-registration

Stub: https://github.com/hmrc/ioss-netp-registration-stub

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application locally via Service Manager

```
sm2 --start IMPORT_ONE_STOP_SHOP_ALL
```

### To run the application locally from the repository, execute the following:

The service needs to run in testOnly mode in order to access the testOnly get-passcodes endpoint which will generate a passcode for email verification.
```
sm2 --stop IOSS_NETP_REGISTRATION_FRONTEND
```
and
```
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

### Running correct version of mongo
Mongo 6 with a replica set is required to run the service. Please refer to the MDTP Handbook for instructions on how to run this

Unit and Integration Tests
------------

To run the unit and integration tests, you will need to open an sbt session on the browser.

### Unit Tests

To run all tests, run the following command in your sbt session:
```
test
```

To run a single test, run the following command in your sbt session:
```
testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
testOnly *<SpecName>
```

### Integration Tests

To run all tests, run the following command in your sbt session:
```
it:test
```

To run a single test, run the following command in your sbt session:
```
it:testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
it:testOnly *<SpecName>
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").