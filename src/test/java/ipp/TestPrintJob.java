package ipp;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

public class TestPrintJob {

  public static void main(String[] args) throws Exception {
    URI printerURI = URI.create("ipp://localhost:631/printers/ColorJet_HP");
    String userDesktop = System.getProperty("user.home") + "/Desktop";
    File file = new File(userDesktop, "print.pdf");
    new PrintJob()
        .printDocument(printerURI, new FileInputStream(file));
  }

}