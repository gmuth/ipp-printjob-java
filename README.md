
# ipp-printjob-java
A minimal ipp protocol implementation (100 lines) to submit a document to a printer.

![Java CI with Gradle](https://github.com/gmuth/ipp-printjob-java/workflows/Java%20CI%20with%20Gradle/badge.svg)

### General

The printjob source code should be useful for use cases of driverless printing.
Especially automated processes without user interaction should benefit from simple solutions like this.
This code is not and will never become a full-fledged ipp implementation.
I provide more control over print jobs, monitoring of printers and print jobs in my other project
[ipp-client-kotlin](https://github.com/gmuth/ipp-client-kotlin).

### Distribution

For the impatient [binary releases](https://github.com/gmuth/ipp-printjob-java/releases) are provided. 
Directory `demo` also contains the `printjob.jar` and a test script called `go` that submits a blank PDF to Apple's Printer Simulator.
To avoid real printing, registered Apple developers can download
[Additional Tools for Xcode](https://download.developer.apple.com/Developer_Tools/Additional_Tools_for_Xcode_11/Additional_Tools_for_Xcode_11.dmg)
containing the Printer Simulator.

### Usage

The tool takes two arguments: *printer-uri* and *file-name*. 
If you don't know the printer uri try `ippfind`. 

    java -jar printjob.jar ipp://colorjet:631/ipp/printer A4-blank.pdf
    
    send ipp request to ipp://colorjet:631/ipp/printer
    ipp version 1.1
    ipp response status: 0000
    group 01
       attributes-charset (47) = utf-8
       attributes-natural-language (48) = en
    group 02
       job-uri (45) = ipp://colorjet:631/jobs/352
       job-id (21) = 352
       job-state (23) = 3
       job-state-reasons (44) = none
    group 03
    
The equivalent java code is:

    new PrintJob().printDocument(
        URI.create("ipp://colorjet:631/ipp/printer"),
        FileInputStream(File("A4-blank.pdf"))
    )

### Document Format

The operation attributes group does not include a value for `document-format` by default.
This should be equivalent to `application/octet-stream` indicating the printer has to auto sense the document format.
You have to make sure the printer supports the document format you send - PDF is usually a good option.
If required by your printer, you can set the document format programmatically by adding it e.g. after the `printer-uri` attribute.

    writeAttribute(dataOutputStream, 0x49, "document-format", "application/pdf");
    
### Issues

If you use an unsupported `printer-uri` you will get a response similar to this one:

    send ipp request to ipp://localhost:8632/ipp/norona
    ipp version 1.1
    ipp status 0400
    group 01
       attributes-charset (47) = utf-8
       attributes-natural-language (48) = en
       status-message (41) = Bad printer-uri "ipp://localhost:8632/ipp/norona".
    group 03

You can use `ippfind` or `dns-sd -Z _ipp._tcp` (look at the rp value) to discover your printer's uri.
If you have other issues contact me.

### Build

To build `printjob.jar` into `build/libs` you need an installed JDK.

    ./gradlew

### Community

I'd be happy to see this minimal ipp implementation being ported to all kinds of programming languages.
On request I'll explain developers without experience in jvm based languages what the jvm runtime library is used for.
