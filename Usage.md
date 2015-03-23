# Introduction #
Jasperoo is designed to behave like any other Spring Roo addon. After adding it to the OSGI list, just call **jasperoo setup** and **jasperoo add** to create your first Jasper Report.


# Details #
## Installation ##
1. If you have already installed a previous version of jasperoo, remove it first with:
```
addon remove --bundleSymbolicName ca.digitalface.jasperoo
```
2. Install and start the jasperoo with:
```
osgi start --url http://s.digitalface.ca/jasperoo-latest
```

## Setup ##
Setup jasperoo with:
```
jasperoo setup
```

## Add ##
Here we will assume that you are working with the Petclinic example. If you want to follow along, enter create a new roo workspace and run:
```
script --file clinic.roo
jasperoo setup
```

Default is to add a report for an entity in the "~.domain" package with a Controller in the "~.web" package.
```
jasperoo add --type ~.domain.Owner
```

Alternatively, you could enable reports for all Roo entities in the host project with the following:
```
jasperoo all
```

## More ##
> Refer to the [API](API.md) for the most up-to-date and complete options.