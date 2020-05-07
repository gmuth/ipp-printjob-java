package ipp;

// --------------------
// Author: Gerhard Muth
// Date  : 21.3.2020
// --------------------

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;

class PrintJob {

  public static void main(final String[] args) {
    if (args.length < 2) {
      System.out.println("usage: java -jar printjob.jar <printer-uri> <file>");
      return;
    }
    try {
      URI printerUri = URI.create(args[0]);
      File file = new File(args[1]);
      new PrintJob().printDocument(printerUri, new FileInputStream(file));

    } catch (Exception exception) {
      exception.printStackTrace(System.err);
    }
  }

  private static Charset US_ASCII = Charset.forName("US-ASCII");
  private static String ippContentType = "application/ipp";

  public void printDocument(final URI uri, final InputStream documentInputStream) throws IOException {
    System.out.println(String.format("send ipp request to %s", uri.toString()));
    String httpScheme = uri.getScheme().replace("ipp", "http");
    URI httpUri = URI.create(String.format("%s:%s", httpScheme, uri.getSchemeSpecificPart()));
    HttpURLConnection httpUrlConnection = (HttpURLConnection) httpUri.toURL().openConnection();
    httpUrlConnection.setConnectTimeout(5000);
    httpUrlConnection.setDoOutput(true);
    httpUrlConnection.setRequestProperty("Content-Type", ippContentType);

    // encode ipp request 'Print-Job operation'
    DataOutputStream dataOutputStream = new DataOutputStream(httpUrlConnection.getOutputStream());
    dataOutputStream.writeShort(0x0101); // ipp version
    dataOutputStream.writeShort(0x0002); // print job operation
    dataOutputStream.writeInt(0x002A); // request id
    dataOutputStream.writeByte(0x01); // operation group tag
    writeAttribute(dataOutputStream, 0x47, "attributes-charset", "us-ascii");
    writeAttribute(dataOutputStream, 0x48, "attributes-natural-language", "en");
    writeAttribute(dataOutputStream, 0x45, "printer-uri", uri.toString());
    dataOutputStream.writeByte(0x03); // end tag
    // append document
    // byte[] buffer = new byte[4096]; // Java <9
    // for (int length; (length = documentInputStream.read(buffer)) != -1; outputStream.write(buffer, 0, length));
    documentInputStream.transferTo(dataOutputStream); // Java >= 9
    dataOutputStream.close();

    // check http response
    if (httpUrlConnection.getResponseCode() != 200) {
      System.err.println(new String(httpUrlConnection.getErrorStream().readAllBytes()));
      throw new IOException(String.format("post to %s failed with http status %d", uri, httpUrlConnection.getResponseCode()));
    }
    if (!ippContentType.equals(httpUrlConnection.getHeaderField("Content-Type"))) {
      throw new IOException(String.format("response from %s is not '%s'", uri, ippContentType));
    }

    // decode ipp response
    DataInputStream dataInputStream = new DataInputStream(httpUrlConnection.getInputStream());
    System.out.println(String.format("ipp version %d.%s", dataInputStream.readByte(), dataInputStream.readByte()));
    System.out.println(String.format("ipp status %04X", dataInputStream.readShort()));
    dataInputStream.readInt(); // ignore request id
    byte tag;
    do {
      tag = dataInputStream.readByte();
      if (tag < 0x10) {
        System.out.println(String.format("group %02X", tag));
        continue;
      }
      // attribute tag
      String name = new String(readValue(dataInputStream), US_ASCII);
      Object value;
      switch (tag) {
        case 0x21:
        case 0x23:
          dataInputStream.readShort();
          value = dataInputStream.readInt();
          break;

        case 0x41:
        case 0x44:
        case 0x45:
        case 0x47:
        case 0x48:
          value = new String(readValue(dataInputStream), US_ASCII);
          break;

        default:
          readValue(dataInputStream);
          value = String.format("<decoding-tag-%02X-not-implemented>", tag);
      }
      System.out.println(String.format("   %s (%02X) = %s", name, tag, value));
    } while (tag != (byte) 0x03);
  }

  protected void writeAttribute(DataOutputStream dataOutputStream, Integer tag, String name, String value) throws IOException {
    dataOutputStream.writeByte(tag);
    dataOutputStream.writeShort(name.length());
    dataOutputStream.write(name.getBytes(US_ASCII));
    dataOutputStream.writeShort(value.length());
    dataOutputStream.write(value.getBytes(US_ASCII));
  }

  protected byte[] readValue(DataInputStream dataInputStream) throws IOException {
    byte[] valueBytes = new byte[dataInputStream.readShort()];
    if (valueBytes.length == dataInputStream.read(valueBytes)) {
      return valueBytes;
    } else {
      throw new IOException("failed to read valueBytes");
    }
    // Java 11: return dataInputStream.readNBytes(dataInputStream.readShort());
  }

}