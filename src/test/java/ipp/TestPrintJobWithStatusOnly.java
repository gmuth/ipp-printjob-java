package ipp;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

public class TestPrintJobWithStatusOnly {

  public static void main(String[] args) throws Exception {
    URI printerURI = URI.create("http://localhost:631/printers/ColorJet_HP");
    String userDesktop = System.getProperty("user.home") + "/Desktop";
    File file = new File(userDesktop, "print.pdf");
    short status = new PrintJobWithStatusOnly()
        .printDocument(printerURI, new FileInputStream(file));
    System.out.println(String.format("ipp status: %04X", status));
  }

}