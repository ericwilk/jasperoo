# Introduction #
The **expand** method adds support for additional formats of reports from a predetermined collection of valid formats. This can also be accomplished by referencing the formats parameter during **setup**. [setup](JasperooMethodsSetup.md) must be called prior to calling **expand**.
```
jasperoo expand --formats "pdf,xls"
```

## Parameters ##
The **expand** method expects the following parameter:

### formats ###
**required**
A comma separated list of report formats to support. Options: pdf, xls, csv, html, odt, xml, and rtf. Default: "pdf,xls".