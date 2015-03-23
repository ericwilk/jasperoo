# Introduction #

Jasperoo is powered by Jasper Reports, so for detailed instructions it would be wise to Google [Jasper Reports Instructions](http://www.google.com/search?sourceid=chrome&client=ubuntu&channel=cs&ie=UTF-8&q=jasper+reports+instructions). There are two main editing tasks I will discuss here though: **Changing the Dataset**, and **Editing The Layout**.


# Details #

## Changing the Dataset ##
The dataset is determined by specifying a Roo finder in the request mapping method of the Report Controller. (By default this can be found at: ~.web.ReportController.java)

```
	@RequestMapping(value ="/ownerList/{format}", method = RequestMethod.GET)
	public String reportOwnerList(ModelMap modelMap, @PathVariable("format") String format) {
		JRBeanCollectionDataSource jrDataSource = new JRBeanCollectionDataSource(com.springsource.petclinic.domain.Owner.findAllOwners(),false);
		modelMap.put("reportData", jrDataSource);
		modelMap.put("format", format);
		return "ownerReportList";
	}
```

Just edit the finder defined in the creation of the jrDataSource object. So if you had a finder defined against your Owner class that would find Owners by last name, you would change the jrDataSource declaration to read something like:
```
JRBeanCollectionDataSource jrDataSource = new JRBeanCollectionDataSource(com.springsource.petclinic.domain.Owner.findOwnersByLastName("someLastName"),false);
```
I won't go into how you would pass the last name here.

## Editing The Layout ##
Jasper Report layouts are all defined in xml files with a **jrxml** extension. The easiest way to edit these files is with the use of iReport from Jasper Forge. http://jasperforge.org/projects/ireport.

iReport is a powerful xml editor tailored specifically to Jasper Reports. You can however edit the **jrxml** file by hand.

The files can be found in the reports folder under WEB-INF. There is one for each entity against which **jasperoo add** has been called.

The tags you'll be most likely interested in are the 

&lt;columnHeader&gt;

**and**

&lt;detail&gt;

**tags.**

The syntax is pretty self evident, and beyond the scope of this page, but again for more detailed help I would direct you to [Jasper Reports](http://jasperforge.org/projects/jasperreports).