# Introduction #

Jasperoo has build-in support for **pdf**, **xls**, **csv**, **html**, **odt**, **xml**, and **rtf** formats. Since vesrion 0.1.3, the supported types can be specified as part of the **setup** call, or at any time with the **extend** call.

# Details #
## Setup ##
At setup, add the formats attribute and pass it a comma separated list from the above valid formats.
For example,
```
jasperoo setup --formats "pdf, odt, csv"
```
will display three report icons on the list and show pages of whatever entity is added with the **jasperoo add** command.

## Extend ##
To add support for a different report format after setup has been run, use the **extend** command like this:
```
jasperoo extend --formats "xls"
```

## Removing support for specific formats ##
Currently, there is no way to remove support from within the roo shell.
To remove support, edit the following two files, removing the "catch" tags that apply to the report format to be removed.
  * src/main/webapp/WEB-INF/tags/form/show.tagx
  * src/main/webapp/WEB-INF/tags/form/list.tagx