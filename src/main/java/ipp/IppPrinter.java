package ipp;

// Author: Gerhard Muth

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;

class IppPrinter {

  public static void main(final String[] args) {
    try {
      // printer uri for PrinterSimulator v87 (Apple)
      URI uri = new URI("ipp://localhost:8632/ipp/print/laser");
      File file = new File("demo/A4-blank.pdf");
      if (args.length > 0) uri = new URI(args[0]);
      if (args.length > 1) file = new File(args[1]);
      new IppPrinter(uri).printJob(file);
    } catch (Exception exception) {
      exception.printStackTrace(System.err);
    }
  }

  private URI uri;
  private DataOutputStream dataOutputStream;
  private DataInputStream dataInputStream;

  IppPrinter(URI uri) {
    this.uri = uri;
  }

  // https://tools.ietf.org/html/rfc8011#section-4.2.1
  public void printJob(final File file) throws IOException {
    System.out.printf("send %s to %s%n", file.getName(), uri);
    String httpScheme = uri.getScheme().replace("ipp", "http");
    URI httpUri = URI.create(String.format("%s:%s", httpScheme, uri.getRawSchemeSpecificPart()));
    HttpURLConnection httpUrlConnection = (HttpURLConnection) httpUri.toURL().openConnection();
    httpUrlConnection.setConnectTimeout(5000);
    httpUrlConnection.setDoOutput(true);
    httpUrlConnection.setRequestProperty("Content-Type", "application/ipp");

    // rfc 8010 syntax of encoding: https://tools.ietf.org/html/rfc8010#page-15
    dataOutputStream = new DataOutputStream(httpUrlConnection.getOutputStream());
    dataOutputStream.writeShort(0x0101); // ipp version 1.1
    // operation -> https://tools.ietf.org/html/rfc8011#section-5.4.15
    dataOutputStream.writeShort(0x0002); // operation Print-Job
    dataOutputStream.writeInt(0x0001); // request id
    dataOutputStream.writeByte(0x01); // operation group tag
    writeAttribute(0x47, "attributes-charset", "us-ascii"); // charset tag
    writeAttribute(0x48, "attributes-natural-language", "en"); // natural-language tag
    writeAttribute(0x45, "printer-uri", uri.toString()); // uri tag
    dataOutputStream.writeByte(0x03); // end tag
    new FileInputStream(file) {{
      transferTo(dataOutputStream);
      close();
    }};

    // check http response
    if (httpUrlConnection.getResponseCode() != 200) {
      System.err.println(new String(httpUrlConnection.getErrorStream().readAllBytes()));
      throw new IOException(String.format("post to %s failed with http status %d", uri, httpUrlConnection.getResponseCode()));
    }
    if (!"application/ipp".equals(httpUrlConnection.getHeaderField("Content-Type"))) {
      throw new IOException("response type is not ipp");
    }

    // decode ipp response
    dataInputStream = new DataInputStream(httpUrlConnection.getInputStream());
    System.out.printf("version %d.%d%n", dataInputStream.readByte(), dataInputStream.readByte());
    // status -> https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xhtml#ipp-registrations-11
    System.out.printf("status 0x%04X%n", dataInputStream.readShort());
    System.out.printf("requestId %d%n", dataInputStream.readInt());
    byte tag;
    do {
      tag = dataInputStream.readByte();
      // delimiter tag -> https://tools.ietf.org/html/rfc8010#section-3.5.1
      if (tag < 0x10) {
        System.out.printf("group %02X%n", tag);
        continue;
      }
      String name = readStringValue();
      Object value;
      // value tag -> https://tools.ietf.org/html/rfc8010#section-3.5.2
      switch (tag) {
        case 0x21: // integer
        case 0x23: // enum
          dataInputStream.readShort(); // value length: 4
          value = dataInputStream.readInt();
          break;
        case 0x41: // textWithoutLanguage
        case 0x44: // keyword
        case 0x45: // uri
        case 0x47: // charset
        case 0x48: // naturalLanguage
          value = readStringValue();
          break;
        default:
          readStringValue();
          value = "<decoding-not-implemented>";
      }
      System.out.printf(" %s (0x%02X) = %s%n", name, tag, value);
    } while (tag != (byte) 0x03); // end tag
    // job-state -> https://tools.ietf.org/html/rfc8011#section-5.3.7
    dataOutputStream.close();
    dataInputStream.close();
  }

  private void writeAttribute(final Integer tag, final String name, final String value) throws IOException {
    dataOutputStream.writeByte(tag);
    dataOutputStream.writeShort(name.length());
    dataOutputStream.write(name.getBytes());
    dataOutputStream.writeShort(value.length());
    dataOutputStream.write(value.getBytes());
  }

  private String readStringValue() throws IOException {
    byte[] valueBytes = dataInputStream.readNBytes(dataInputStream.readShort());
    return new String(valueBytes, "us-ascii");
  }
}