# Introduction #
The **all** method adds support for reporting to all Roo entities in the host project. [setup](JasperooMethodsSetup.md) must be called prior to calling **all**.

_**WARNING:** Currently, if **all** is called after **add**, there will be duplicate methods in the Report Controller that will cause it to fail to compile._
```
jasperoo all [--entityPackage ~.domain] [--controllerPackage ~.web]
```

## Parameters ##
The **all** method accepts the following two parameters:
### entityPackage ###
**optional**
The location of the Entities. Default: "~.domain".

### controllerPackage ###
**optional**
The location of the Web Controllers. Default: "~.web".

[[Return to API](http://code.google.com/p/jasperoo/wiki/API)]