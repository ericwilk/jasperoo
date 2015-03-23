![https://lh3.googleusercontent.com/-M5TxrB6Fy28/TYOk8nG1jCI/AAAAAAAAABg/Xfh_fd3z11w/s1600/jasperoo_logo.png](https://lh3.googleusercontent.com/-M5TxrB6Fy28/TYOk8nG1jCI/AAAAAAAAABg/Xfh_fd3z11w/s1600/jasperoo_logo.png)

A Spring ROO addon to facilitate the integration of [Jasper Reports](http://jasperforge.org/projects/jasperreports) into a Spring ROO project.


---

## Sorry ##
Dear community,

I am sorry that I have been absent from this project for the past year. I had to undergo emergency surgery, and the recovery has taken much longer than expected.

I will address the issues identified as quickly as possible.

If you would like to join the project, send me an email (jasperoo at digitalface dot ca).

Thanks for your input and patience. Please keep it coming.

Waldo

---


Jasperoo configures the host project to support Jasper Reports, and creates generic reports for the entities specified. Currently, version 3.5.3 of Jasper Reports is supported.

Jasperoo will generate reports in **Portable Document Format**(pdf), **MS Excel**(xls), **Comma Separated Values**(csv), **Hypertext Markup Language**(html), **Open Document Text**(odt), **Extensible Markup Language**(xml), and **Rich Text Format**(rtf) formats. For each entity specified, jasperoo will create a **List** report and a **Detail** report.

The **List** reports present all the fields of the entity in a columnar fashion, and are accessed through icons on the list.jsp page. The **Detail** reports present all the fields for a single instance of the entity in a "one per row" fashion, and are accessed through icons on the Show.jsp page. By default, all fields are presented. The idea is that it is easier to delete unwanted fields from a report than to add missing fields. The reports generated can be easily edited with [iReport](http://jasperforge.org/projects/ireport) from jasperforge.org.

A custom Roo "finder" can be used to populate the dataset used to generate the reports.

<table width='100%' border='0px'>
<tr>
<td align='center'>
<a href='https://lh6.googleusercontent.com/-LxTwWJKkigY/TY0NmF1ECuI/AAAAAAAAACE/6h5QqZGcB3Y/s1600/Screenshot-DetailScreen.png'><img src='https://lh6.googleusercontent.com/-LxTwWJKkigY/TY0NmF1ECuI/AAAAAAAAACE/6h5QqZGcB3Y/s320/Screenshot-DetailScreen.png' /></a>

<i><b>A "show" screen with the report icons highlighted.</b></i>
</td><td align='center'>
<a href='https://lh5.googleusercontent.com/-eeetOe17dcc/TY0NmpAJUuI/AAAAAAAAACI/iERFRnN5RRg/s1600/Screenshot-ListScreen.png'><img src='https://lh5.googleusercontent.com/-eeetOe17dcc/TY0NmpAJUuI/AAAAAAAAACI/iERFRnN5RRg/s320/Screenshot-ListScreen.png' /></a>

<i><b>A "list" screen with the report icons highlighted.</b></i>
</td>
</tr>
<tr>
<td align='center'>
<a href='https://lh6.googleusercontent.com/-VTgWRWODEXg/TY0Nm6Ux7kI/AAAAAAAAACM/_6sRUkHfWgM/s1600/Screenshot-ownerReportDetail.pdf.png'><img src='https://lh6.googleusercontent.com/-VTgWRWODEXg/TY0Nm6Ux7kI/AAAAAAAAACM/_6sRUkHfWgM/s320/Screenshot-ownerReportDetail.pdf.png' /></a>

<i><b>A "Detail" report</b></i>
</td><td align='center'>
<a href='https://lh4.googleusercontent.com/-TombxdsVE6Q/TY0NnIz27wI/AAAAAAAAACQ/drfcdjRx3D4/s1600/Screenshot-ownerReportList.pdf.png'><img src='https://lh4.googleusercontent.com/-TombxdsVE6Q/TY0NnIz27wI/AAAAAAAAACQ/drfcdjRx3D4/s320/Screenshot-ownerReportList.pdf.png' /></a>

<i><b>A "List" report.</b></i>
</td>
</tr>
</table>

Special thanks to [Saeid Moradi](http://twitter.com/#!/smoradi) for the [initial building blocks](http://sid3.blogspot.com/2010/01/configure-jasperreports-3x-spring-3x.html).

There you have it... happy coding. I not only welcome your comments, but covet your feedback.

---

Usage instructions in the [Project Wiki](http://code.google.com/p/jasperoo/wiki/Usage).

Known issues in the [Project Issue Tracker](http://code.google.com/p/jasperoo/issues/list).
