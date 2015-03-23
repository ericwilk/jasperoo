# Introduction #
The **add** method adds support for reporting to a single Roo entity. [setup](JasperooMethodsSetup.md) must be called prior to calling **add**.
```
jasperoo add --type ~.domain.SomeType [-- finder findSomeTypeBySomething] [--entityPackage ~.domain] [--controllerPackage ~.web]
```

## Parameters ##
The **add** method accepts the following four parameters:
### type ###
**required**
The entity upon which to report.
### finder ###
**optional**
If specified, this finder will produce the resultset to be passed to the List report.

### entityPackage ###
**optional**
The location of the Entities. Default: "~.domain".

### controllerPackage ###
**optional**
The location of the Web Controllers. Default: "~.web".

[[Return to API](http://code.google.com/p/jasperoo/wiki/API)]