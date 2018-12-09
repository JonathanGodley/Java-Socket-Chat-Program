// ChatClient.java
// Chat client utilising Java Sockets
// Jonathan Godley - c3188072
// Last Modified - 1/9/2018

import java.io.*;
import java.net.*;

public class ChatClient
{
    public static void main(String[] args) throws IOException
    {

        InetAddress hostName   = InetAddress.getByName("localhost"); // default
        int         portNumber = 3400; // default
        String      userName;
        String      ServerResponse;
        String      userInput;

        // check number of provided arguments
        if (args.length != 2)
        {
            System.err.println("Usage: java ChatClient <host name> <port number>");
        }

        // check that args are supplied and valid, use defaults where necessary
        if (args.length < 1)
        {
            InetAddress.getByName("localhost");
            System.err.println("Hostname not specified, using default hostname: localhost");
        }
        else
        {
            try
            {
                hostName = InetAddress.getByName(args[0]);
            }
            catch (UnknownHostException e)
            {
                System.err.println("Invalid Hostname - Unknown Host Exception");
                System.exit(1);
            }
        }
        if (args.length < 2)
        {
            portNumber = 3400;
            System.err.println("Port not specified, using default port: 3400");
        }
        else
        {
            try
            {
                portNumber = Integer.parseInt(args[1]);
                if (portNumber < 1023 || portNumber > 65535)
                {
                    System.err.println("Invalid Port - Range must be between 1023-65535");
                    System.exit(1);
                }
            }
            catch (NumberFormatException e)
            {
                System.err.println("Invalid Port - Range must be between 1023-65535");
                System.exit(1);
            }
        }
        // create our try with resources to run the socket
        try (Socket clientSocket = new Socket(hostName, portNumber);
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in)))
        {
            System.out.println("SCP Chat Client starting...");
            System.out.print("Enter a username: ");

            // get username from user
            userName = stdIn.readLine();

            // prepare our connection message
            userInput = ConnectionRequest(hostName.getHostAddress(), portNumber, userName);

            // send our connection message
            System.out.println(
                    "Connecting to " + hostName.getHostAddress() + ":" + portNumber + " with username: \"" + userName +
                    "\"");
            out.println(userInput);
            System.out.print("sending CONNECT message...");

            // now we check for a SCP ACCEPT or SCP REJECT message

            if ((ServerResponse = (in.readLine()).trim()).equals("SCP ACCEPT"))
            {
                System.out.print(" received ACCEPT message... ");
                if ((ServerResponse = in.readLine().trim()).startsWith("USERNAME") && ServerResponse.contains(userName))
                {
                    if ((ServerResponse = in.readLine().trim()).startsWith("CLIENTADDRESS") &&
                        ServerResponse.length() > 14)
                    {
                        if ((ServerResponse = in.readLine().trim()).startsWith("CLIENTPORT") &&
                            ServerResponse.contains(portNumber + ""))
                        {
                            if ((ServerResponse = in.readLine().trim()).equals("SCP END"))
                            {
                                System.out.println("Connected");

                                // send acknowledgement
                                System.out.print("sending ACKNOWLEDGE message... ");
                                out.println("SCP ACKNOWLEDGE\n" + "USERNAME \"" + userName + "\"\n" + "SERVERADDRESS " +
                                            hostName + "\n" + "SERVERPORT " + portNumber + "\n" + "SCP END");

                            }
                            else
                            {
                                System.err.println("Unexpected Message Received:");
                                System.err.println("Expected \"SCP END\" Received: \"" + ServerResponse + "\"");
                                clientSocket.close();
                                System.exit(1);
                            }
                        }
                        else
                        {
                            System.err.println("Unexpected Message Received:");
                            System.err.println(
                                    "Expected \"CLIENTPORT " + portNumber + "\" Received: \"" + ServerResponse + "\"");
                            clientSocket.close();
                            System.exit(1);
                        }
                    }
                    else
                    {
                        System.err.println("Unexpected Message Received:");
                        System.err.println(
                                "Expected \"CLIENTADDRESS <localAddress>\" Received: \"" + ServerResponse + "\"");
                        clientSocket.close();
                        System.exit(1);
                    }
                }
                else
                {
                    System.err.println("Unexpected Message Received:");
                    System.err
                            .println("Expected \"USERNAME \"" + userName + "\"\" Received: \"" + ServerResponse + "\"");
                    clientSocket.close();
                    System.exit(1);
                }


            }
            else if (ServerResponse.equals("SCP REJECT"))
            {
                System.out.println("received REJECT message");
                if ((ServerResponse = in.readLine().trim()).startsWith("TIMEDIFFERENTIAL") &&
                    ServerResponse.length() > 17)
                {
                    int serverDifferential = Integer.parseInt(ServerResponse.trim().substring(17));

                    if ((ServerResponse = in.readLine().trim()).startsWith("REMOTEADDRESS") &&
                        ServerResponse.length() > 14)
                    {
                        String serverHostname = ServerResponse.trim().substring(14);

                        if ((ServerResponse = in.readLine().trim()).equals("SCP END"))
                        {
                            System.err.println("Server Rejected Connection - Time Differential: " + serverDifferential +
                                               " at Hostname: " + serverHostname);
                            System.err.println("Time Differential must be less than 5 seconds");
                            System.exit(1);
                        }
                        else
                        {
                            System.err.println("Unexpected Message Received:");
                            System.err.println("Expected \"SCP END\" Received: \"" + ServerResponse + "\"");
                            clientSocket.close();
                            System.exit(1);
                        }

                    }
                    else
                    {
                        System.err.println("Unexpected Message Received:");
                        System.err.println("Expected \"REMOTEADDRESS <address>\" Received: \"" + ServerResponse + "\"");
                        clientSocket.close();
                        System.exit(1);
                    }

                }
                else
                {
                    System.err.println("Unexpected Message Received:");
                    System.err
                            .println("Expected \"TIMEDIFFERENTIAL <difference>\" Received: \"" + ServerResponse + "\"");
                    clientSocket.close();
                    System.exit(1);
                }
            }
            else
            {
                System.err.println("Unexpected Message Received:");
                System.err.println("Expected \"SCP ACCEPT\" or \"SCP REJECT\" Received: \"" + ServerResponse + "\"");
                clientSocket.close();
                System.exit(1);
            }

            // we've now established our connection and will start trading CHAT messages
            while ((ServerResponse = in.readLine()) != null)
            {
                if (ServerResponse.trim().equals("SCP CHAT"))
                {
                    System.out.println("received CHAT message");
                    if ((ServerResponse = in.readLine().trim()).startsWith("REMOTEADDRESS") &&
                        ServerResponse.length() > 14)
                    {
                        if ((ServerResponse = in.readLine().trim()).startsWith("REMOTEPORT") &&
                            ServerResponse.contains("" + portNumber))
                        {
                            if ((ServerResponse = in.readLine().trim()).startsWith("MESSAGECONTENT"))
                            {
                                if ((ServerResponse = in.readLine().trim()).equals(""))
                                {
                                    if ((ServerResponse = in.readLine().trim()).equals(""))
                                    {
                                        while (!((ServerResponse = in.readLine().trim()).equals("SCP END")) &&
                                               ServerResponse != null)
                                        {
                                            System.out.println("Server: " + ServerResponse.trim());
                                        }
                                        if (ServerResponse.trim().equals("SCP END"))
                                        {
                                            System.out.print("Enter a message: ");
                                            userInput = stdIn.readLine();

                                            // Disconnect from Server
                                            if (userInput.equals("DISCONNECT"))
                                            {
                                                out.println("SCP DISCONNECT \n" + "SCP END");
                                                System.out.println("sending DISCONNECT message");
                                                System.out.println("Disconnecting...");
                                            }
                                            else
                                            {
                                                out.println(
                                                        chatMessage(hostName.getHostAddress(), portNumber, userInput));
                                                System.out.println("sending CHAT message");
                                                System.out.println(userName + ": " + userInput);
                                                System.out.println("Other user is typing... ");
                                            }
                                        }
                                        else
                                        {
                                            System.err.println("Unexpected Message Received:");
                                            System.err.println(
                                                    "Expected \"SCP END\" Received: \"" + ServerResponse + "\"");
                                            clientSocket.close();
                                            System.exit(1);
                                        }
                                    }
                                    else
                                    {
                                        System.err.println("Unexpected Message Received:");
                                        System.err.println("Expected \"\" Received: \"" + ServerResponse + "\"");
                                        clientSocket.close();
                                        System.exit(1);
                                    }
                                }
                                else
                                {
                                    System.err.println("Unexpected Message Received:");
                                    System.err.println("Expected \"\" Received: \"" + ServerResponse + "\"");
                                    clientSocket.close();
                                    System.exit(1);
                                }
                            }
                            else
                            {
                                System.err.println("Unexpected Message Received:");
                                System.err.println("Expected \"MESSAGECONTENT\" Received: \"" + ServerResponse + "\"");
                                clientSocket.close();
                                System.exit(1);
                            }
                        }
                        else
                        {
                            System.err.println("Unexpected Message Received:");
                            System.err.println(
                                    "Expected \"REMOTEPORT " + portNumber + "\" Received: \"" + ServerResponse + "\"");
                            clientSocket.close();
                            System.exit(1);
                        }
                    }
                    else
                    {
                        System.err.println("Unexpected Message Received:");
                        System.err.println(
                                "Expected \"REMOTEADDRESS <local hostname>\" Received: \"" + ServerResponse + "\"");
                        clientSocket.close();
                        System.exit(1);
                    }

                }
                else if (ServerResponse.trim().equals("SCP ACKNOWLEDGE"))
                {
                    if ((ServerResponse = in.readLine().trim()).equals("SCP END"))
                    {
                        System.out.println("received ACKNOWLEDGE message");
                        System.out.println("Server disconnected, exiting...");
                        clientSocket.close();
                        System.exit(1);
                    }
                    else
                    {
                        System.err.println("Unexpected Message Received:");
                        System.err.println("Expected \"SCP END\" Received: \"" + ServerResponse + "\"");
                        clientSocket.close();
                        System.exit(1);
                    }
                }
                else if (ServerResponse.trim().equals("SCP DISCONNECT"))
                {
                    if ((ServerResponse = in.readLine().trim()).equals("SCP END"))
                    {
                        System.out.println("received DISCONNECT message");
                        System.out.println("sending ACKNOWLEDGE message");
                        System.out.println("Server disconnected, exiting...");


                        out.println("SCP ACKNOWLEDGE \n" + "SCP END");


                        clientSocket.close();
                        System.exit(0);
                    }
                    else
                    {
                        System.err.println("Unexpected Message Received:");
                        System.err.println("Expected \"SCP END\" Received: \"" + ServerResponse + "\"");
                        clientSocket.close();
                        System.exit(1);
                    }
                }
                else
                {
                    System.err.println("Unexpected Message Received:");
                    System.err.println(
                            "Expected \"SCP CHAT\" or \"SCP ACKNOWLEDGE\" or \"SCP DISCONNECT\" \nReceived: \"" +
                            ServerResponse + "\"");
                    clientSocket.close();
                    System.exit(1);

                }
            }
        }
        catch (UnknownHostException e)
        {
            System.err.println("Unknown Host: " + hostName);
            System.exit(1);
        }
        catch (IOException e)
        {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }

    private static String ConnectionRequest(String hostName, int portNumber, String userName)
    {
        //  SCP CONNECT
        // 	SERVERADDRESS <server hostname>
        //  SERVERPORT <server port>
        // 	REQUESTCREATED <time since epoch>
        // 	USERNAME <username as String with quotes>
        // 	SCP END


        long epoch = java.time.Instant.now().getEpochSecond();
        return "SCP CONNECT \n" + "SERVERADDRESS " + hostName + "\n" + "SERVERPORT " + portNumber + "\n" +
               "REQUESTCREATED " + epoch + "\n" + "USERNAME \"" + userName + "\" \n" + "SCP END";
    }

    private static String chatMessage(String clientHostName, int clientPort, String messageContent)
    {
        return "SCP CHAT\n" + "REMOTEADDRESS " + clientHostName + "\n" + "REMOTEPORT " + clientPort + "\n" +
               "MESSAGECONTENT \n" + "\n\n" + messageContent + "\n" + "SCP END";

    }

}