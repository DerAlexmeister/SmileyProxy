package Blatt2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

/**
 * Proxy-Server to receive a GET-Request by the Client and send off the manipulated respond back to the Client.
 */
public class Proxy {

    private static String[] keywords = {"InformatikerInnen", "Informatiker", "Informatikerin", "Software", "Linux", "Windows",
            "Studierende", "Studentin", "Student", "Informatik", "Debugger", "CISC", "RISC", "Computer",
            "Java", "MMIX"};
    private static final int PORT = 8082;
    private static Socket clientSocket = null;
    private static String urlFromSocket = null;
    private static StringBuilder builder = null;
    private static ServerSocket myServerSkt;
    private static String protokol;
    private static String currentHost = "";
    public static final String syst = "SYSTEM >> ";
    static String httpTyp = "http://";


    /**
     * Main-Method will just call the serverUP-Method to start the server and catches the Exceptions.
     *
     * @param args - never Used parameter.
     */
    public static void main(String[] args) {
        try {
            setMyServerSkt(new ServerSocket(getPort()));
            System.out.println(syst + "Server up");
            setClientSocket(myServerSkt.accept());
            System.out.println(syst + "Connection established to client");
        } catch (BindException be) {
            System.out.println(syst + "On this port is already a service running - Server will stop");
            System.exit(0);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        serverUP();
    }

    /**
     * Extracts the ServerURL from the GET Request and calls the method httpConnector.
     */
    private static void serverUP() {
        try {
            try {
                setUrlFromSocket(hostURLExtractor(getClientSocket()));
            } catch (Exception e) {
                serverUP();
            }
            System.out.println(syst + "Generating URL");
            httpConnector(getUrlFromSocket());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    /**
     * Method to setup the Connection to the TargetHost.
     *
     * @param urlFromSocket Url of the foreign Socket.
     */
    private static void httpConnector(String urlFromSocket) {
        try {
            URL url = new URL(httpTyp + urlFromSocket);
            HttpURLConnection httpsCon = null;
            try {
                System.out.println(syst + "Trying to connect to Host >> " + url);
                httpsCon = (HttpURLConnection) url.openConnection();
                httpsCon.setRequestMethod("GET");
                httpsCon.addRequestProperty("Host", getUrlFromSocket());
                httpsCon.addRequestProperty("Connection", "keep-alive");
                try {
                    responder(new BufferedReader(new InputStreamReader(httpsCon.getInputStream())));
                    try {
                        httpsCon.disconnect();
                        System.out.println(syst + "Trying to restarting Service");
                    } catch (Exception exception) {
                        System.out.println(syst + "Cannot restart the Service");
                    }
                } catch (Exception exception) {
                    System.out.println(syst + "Cannot resolve the Host");
                    setClientSocket(myServerSkt.accept());
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            System.out.println("\n \n");
            serverUP();
            setUrlFromSocket(hostURLExtractor(getClientSocket()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to prepare the respond and sent it to the Client. (changes the Pictures, adds yeah! )
     *
     * @param bufferedReader is the respond from the TH as BufferedReader
     */
    private static void responder(BufferedReader bufferedReader) {
        String inputLine = null;
        try {
            setBuilder(new StringBuilder());
            while ((inputLine = bufferedReader.readLine()) != null) {
                setBuilder(getBuilder().append(inputLine));
            }
            inputLine = stringConc(searchForIMGTags(getBuilder()).toString());
            BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(getClientSocket().getOutputStream()));
            toClient.write(getProtokol() + " 200 OK\r\n" + "Content-length: " + inputLine.length() + "\r\n" + "\r\n" + inputLine);
            toClient.flush();
            System.out.println(syst + "Get Request respond to Client");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Method to Check whether the string Contains a Word of the Array keyWord or there are IMG-Tags.
     *
     * @param inputLine will be the SourceCode of the Request as String
     * @return will return a new String with replaced words and IMG links
     */
    private static String stringConc(String inputLine) {
        for (int i = 0; i < getLengthOfKeyWord(); i++) {
            if (inputLine.toLowerCase().contains((CharSequence) getSingleWord(i).toLowerCase())) {
                inputLine = inputLine.replaceAll(getSingleWord(i), (getSingleWord(i) + " (yeah!)"));
            }
        }
        return inputLine;
    }

    /**
     * Method to change the PATH of the Image, so a smiley is given.
     *
     * @param inputLine Website as String
     * @return will return a String with changed href's
     */
    private static StringBuilder searchForIMGTags(StringBuilder inputLine) {
        String imgSubString;
        int end, imgTagStart, imgTagEnde;
        int start = inputLine.indexOf("<img");
        while (start != -1) {
            end = inputLine.indexOf(">", start);
            imgSubString = inputLine.substring(start, end);
            imgTagStart = imgSubString.indexOf("src=\"", 0);
            imgTagEnde = imgSubString.indexOf("\"", imgTagStart);
            imgSubString = imgSubString.substring(0, imgTagStart - 1) + " src=\"https://upload.wikimedia.org/wikipedia/commons/8/8d/Smiley_head_happy.svg\"" + imgSubString.substring(imgTagEnde + 1, imgSubString.length());
            inputLine.replace(start, start + imgTagEnde, imgSubString);
            start = inputLine.indexOf("<img", start + imgTagEnde);
        }
        return inputLine;
    }

    /**
     * Method to get the Address of the TH by Capturing the GET/Request and searching for the TH-Address.
     * Checks if ther is a valid URL.
     *
     * @param socket Current Socket to extract the data
     * @return will return the Address as String
     */
    public static String hostURLExtractor(Socket socket) {
        String message = null;
        try {
            BufferedReader buff = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            message = buff.readLine();
            try {
                message = message.replaceAll("GET /", "");
            } catch (Exception ex) {
                setClientSocket(myServerSkt.accept());
            }
            message = cleanHTTPProtocol(message);
            System.out.println(message);
            if (message.contains("www.") || message.contains("http://") || message.contains("https://")) {
                if (message.contains("https://")) {
                    message = message.replaceAll("https://", "");
                    httpTyp = "https://";
                } else if (message.contains("http://")) {
                    message = message.replaceAll("http://", "");
                    httpTyp = "http://";
                }
                message = message.replaceAll(" ", "");
                long count = message.chars().filter(ch -> ch == '.').count(); //check URL
                System.out.println(count);
                if ((int) count >= 2) {
                    setCurrentHost(message);
                } else {
                    message = getCurrentHost() + "/" + deleteWhitespaceAndSlashes(message); //try to build a valid URL
                }
            } else {
                message = getCurrentHost() + "/" + deleteWhitespaceAndSlashes(message);
            }
        } catch (IOException e) {
            setUrlFromSocket(hostURLExtractor(getClientSocket()));
        }
        return message;
    }

    /**
     * Helps to build a valid URL.
     *
     * @param currentMessage current URL
     * @return URL without whitespace and /
     */
    public static String deleteWhitespaceAndSlashes(String currentMessage) {
        String firstLetter = Character.toString(currentMessage.charAt(0));
        if (firstLetter.equals(" ") || firstLetter.equals("/")) {
            return deleteWhitespaceAndSlashes(currentMessage.substring(1));
        } else {
            return currentMessage;
        }
    }

    /**
     * Saves the Protocol in the attributes and cleans it out of the string.
     *
     * @param str Current String.
     * @return Cleaned string without Protocol.
     */
    public static String cleanHTTPProtocol(String str) {
        if (str.contains("HTTP/1.1")) {
            setProtokol("HTTP/1.1");
            str = str.replaceAll("HTTP/1.1", "");
        } else if (str.contains("HTTP/1.0")) {
            setProtokol("HTTP/1.0");
            str = str.replaceAll("HTTP/1.0", "");
        } else {
            setProtokol("HTTP/2.0");
            str = str.replaceAll("HTTP/2.0", "");
        }
        return str;
    }

    /**
     * Getter for the Length of the Array Keyword.
     *
     * @return will return the length of the Array as integer
     */
    public static int getLengthOfKeyWord() {
        return keywords.length;
    }

    /**
     * Will return the keyWord at number from Keyword.
     *
     * @param number number of the Element
     * @return will return the Keyword as String
     */
    public static String getSingleWord(int number) {
        return keywords[number];
    }

    /**
     * Getter for the Keywords as a String Array.
     *
     * @return will return a String Array
     */
    public String[] getKeywords() {
        return keywords;
    }

    /**
     * Setter for the Keyword.
     *
     * @param keywordsParam Array of new Keywords
     */
    public void setKeywords(String[] keywordsParam) {
        keywords = keywordsParam;
    }

    /**
     * Getter for the Port.
     *
     * @return will return the Port(8082) as Integer
     */
    public static int getPort() {
        return PORT;
    }

    /**
     * Getter for the ClientSocket.
     *
     * @return will return the Socket
     */
    public static Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Setter for the ServerSocket.
     *
     * @param clientSocket
     */
    public static void setClientSocket(Socket clientSocket) {
        Proxy.clientSocket = clientSocket;
    }

    /**
     * Getter for the StringBuilder.
     *
     * @return will return the StringBuilder
     */
    public static StringBuilder getBuilder() {
        return builder;
    }

    /**
     * Setter for the StringBuilder.
     *
     * @param builder StringBuilder
     */
    public static void setBuilder(StringBuilder builder) {
        Proxy.builder = builder;
    }

    /**
     * Getter for the ServerSocket.
     *
     * @return will return the ServerSocket
     */
    public static ServerSocket getMyServerSkt() {
        return myServerSkt;
    }

    /**
     * Setter for the ServerSocket.
     *
     * @param myServerSkt ServerSocket
     */
    public static void setMyServerSkt(ServerSocket myServerSkt) {
        Proxy.myServerSkt = myServerSkt;
    }

    /**
     * Getter for the URL.
     *
     * @return will return the URL as String
     */
    public static String getUrlFromSocket() {
        return urlFromSocket;
    }

    /**
     * Setter for the URL
     *
     * @param urlFromSocket URL as a String
     */
    public static void setUrlFromSocket(String urlFromSocket) {
        Proxy.urlFromSocket = urlFromSocket;
    }

    /**
     * Getter for the HTTP Protocol.
     *
     * @return Current Server HTTP Protocol
     */
    public static String getProtokol() {
        return protokol;
    }

    /**
     * Setter for Protocol in the Attributes.
     *
     * @param protokol Which Protocol hast to be set.
     */
    public static void setProtokol(String protokol) {
        Proxy.protokol = protokol;
    }

    /**
     * Getter for the Host Name.
     *
     * @return Host Name as String.
     */
    public static String getCurrentHost() {
        return currentHost;
    }

    /**
     * Setter for the Attribute currentHost.
     *
     * @param currentHost String of the current Host.
     */
    public static void setCurrentHost(String currentHost) {
        Proxy.currentHost = currentHost;
    }

}
