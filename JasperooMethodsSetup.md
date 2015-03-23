# Introduction #
The setup method is the first one to be called after installation of jasperoo.
```
jasperoo setup [--controllerPackage ~.web] [--formats "pdf,xls"]
```

## Parameters ##
The **setup** method accepts the following two parameters:

### controllerPackage ###
**optional**
The location of the Web Controllers. Default: "~.web".

### formats ###
**optional**
A comma separated list of report formats to support. Options: pdf, xls, csv, html, odt, xml, and rtf. Default: "pdf,xls".

[[Return to API](http://code.google.com/p/jasperoo/wiki/API)]