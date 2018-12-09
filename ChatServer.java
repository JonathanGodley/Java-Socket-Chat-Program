// ChatServer.java
// Chat Server utilising Java Sockets
// Jonathan Godley - c3188072
// Last Modified - 1/9/2018

import java.net.*;
import java.io.*;

public class ChatServer
{
    public static void main(String[] args) throws IOException
    {

        InetAddress hostName       = InetAddress.getByName("localhost"); // default
        int         portNumber     = 3400; // default
        String      welcomeMessage = "WELCOME MESSAGE"; // default
        boolean     reset;
        boolean     quit           = false;

        // check number of provided arguments
        if (args.length < 3)
        {
            System.err.println("Usage: java ChatServer <host name> <port number> <welcome message>");
        }

        // check that args are supplied and valid, use defaults where necessary
        if (args.length < 1)
        {
            hostName = InetAddress.getByName("localhost");
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

        if (args.length < 3)
        {
            welcomeMessage = "Welcome to SCP";
            System.err.println("Welcome Message not specified, using default welcome message: \"Welcome to SCP\"");
        }
        else
        {
            // need to piece together the welcome message
            // if there's only 1 word in the welcome message;
            if (args.length < 4)
            {
                welcomeMessage = args[2];
            }
            else
            {
                // 1st find length of args
                int l = args.length;
                welcomeMessage = args[2];
                for (int i = 3; i != l; i++)
                {
                    welcomeMessage += " " + args[i];
                }
            }
        }

        System.out.println("SCP Server started on port: " + portNumber);

        // start an infiniate loop to keep the server running after a disconnect
        // when the "reset" loop ends, the "quit" loop will immediately restart the server back to a fresh state
        while (quit == false)
        {
            reset = false;
            System.out.println("Waiting for client connection...");

            String  clientHostname;
            int     clientPort     = 3400; // default port 3400
            long    clientEpoch;
            String  clientUserName = "default";
            boolean connected      = false;
            boolean accepted       = false;
            boolean acknowledged   = false;
            boolean cancelled      = false;
            String  userInput;
            String  inputLine;

            // create our try with resources to run the socket
            try (ServerSocket serverSocket = new ServerSocket(portNumber, 50, hostName);
                 Socket clientSocket = serverSocket.accept();
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())))
            {
                // get the address of our connected client
                clientHostname =
                        (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress());

                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

                // loop until reset flag is set to true, keep pulling the next inputLine while there is one available.
                while (reset == false && (inputLine = in.readLine()) != null)
                {
                    if (inputLine.trim().equals("SCP CONNECT") && !connected)
                    {
                        System.out.print("received CONNECT message... ");
                        if ((inputLine = in.readLine().trim()).startsWith("SERVERADDRESS") &&
                            inputLine.contains(hostName.getHostAddress()))
                        {

                            if ((inputLine = in.readLine().trim()).startsWith("SERVERPORT") &&
                                inputLine.contains(portNumber + ""))
                            {

                                clientPort = Integer.parseInt(inputLine.trim().substring(11));
                                if ((inputLine = in.readLine().trim()).startsWith("REQUESTCREATED") &&
                                    inputLine.length() > 15)
                                {

                                    clientEpoch = Long.parseLong(inputLine.trim().substring(15));
                                    if ((inputLine = in.readLine().trim()).startsWith("USERNAME") &&
                                        inputLine.length() > 9)
                                    {

                                        clientUserName = inputLine.trim().substring(9);
                                        clientUserName = clientUserName.substring(1, clientUserName.length() - 1);

                                        if ((in.readLine().trim()).equals("SCP END"))
                                        {
                                            connected = true;

                                            // packet confirmed as valid, now processing
                                            // check if epoch time.
                                            long epoch = java.time.Instant.now().getEpochSecond();

                                            // using abs. value so that if server is behind client in time, it still must be within 5 seconds
                                            long diff = Math.abs(clientEpoch - (epoch));
                                            if (diff > 5)
                                            {

                                                out.println("SCP REJECT\n" + "TIMEDIFFERENTIAL " + diff + "\n" +
                                                            "REMOTEADDRESS " + clientHostname + "\n" + "SCP END");
                                                System.out.println("sending REJECT message");

                                                // then close socket
                                                clientSocket.close();

                                                System.err.println(
                                                        "Connection Terminated - Time Differential: " + diff +
                                                        " with client: " + clientHostname +
                                                        " was out of allowable bounds");

                                                reset = true;


                                            }
                                            else
                                            {
                                                out.println("SCP ACCEPT\n" + "USERNAME \"" + clientUserName + "\"\n" +
                                                            "CLIENTADDRESS " + clientHostname + "\n" + "CLIENTPORT " +
                                                            clientPort + "\n" + "SCP END");
                                                System.out.println("sending ACCEPT message ... ");
                                                System.out.print("Client: \"" + clientUserName + "\" at " +
                                                                 clientHostname + ":" + clientPort + " connected... ");
                                                accepted = true;
                                            }
                                        }
                                        else
                                        {
                                            System.err.println("Unexpected Message Received:");
                                            System.err.println("Expected \"SCP END\" Received: \"" + inputLine + "\"");
                                            clientSocket.close();
                                            serverSocket.close();
                                            System.exit(1);
                                        }

                                    }
                                    else
                                    {
                                        System.err.println("Unexpected Message Received:");
                                        System.err.println(
                                                "Expected \"USERNAME \"username\"\" Received: \"" + inputLine + "\"");
                                        clientSocket.close();
                                        serverSocket.close();
                                        System.exit(1);
                                    }

                                }
                                else
                                {
                                    System.err.println("Unexpected Message Received:");
                                    System.err.println(
                                            "Expected \"REQUESTCREATED <epoch>\" Received: \"" + inputLine + "\"");
                                    clientSocket.close();
                                    serverSocket.close();
                                    System.exit(1);
                                }

                            }
                            else
                            {
                                System.err.println("Unexpected Message Received:");
                                System.err.println(
                                        "Expected \"SERVERPORT " + portNumber + "\" Received: \"" + inputLine + "\"");
                                clientSocket.close();
                                serverSocket.close();
                                System.exit(1);
                            }

                        }
                        else
                        {
                            System.err.println("Unexpected Message Received:");
                            System.err.println(
                                    "Expected \"SERVERADDRESS " + hostName.getHostAddress() + "\" Received: \"" +
                                    inputLine + "\"");
                            clientSocket.close();
                            serverSocket.close();
                            System.exit(1);
                        }
                    }
                    else if (accepted && !acknowledged)
                    {
                        // now we've received an connect, and responded
                        // now we wait for acknowledge

                        if ((inputLine.trim().equals("SCP ACKNOWLEDGE")))
                        {
                            System.out.println("received ACKNOWLEDGE message... ");
                            if ((inputLine = in.readLine().trim()).startsWith("USERNAME") &&
                                inputLine.contains(clientUserName))
                            {
                                if ((inputLine = in.readLine().trim()).startsWith("SERVERADDRESS") &&
                                    inputLine.contains(hostName.getHostAddress()))
                                {
                                    if ((inputLine = in.readLine().trim()).startsWith("SERVERPORT") &&
                                        inputLine.contains(portNumber + ""))
                                    {
                                        if ((inputLine = in.readLine().trim()).equals("SCP END"))
                                        {
                                            acknowledged = true;

                                            out.println(chatMessage(clientHostname, clientPort, welcomeMessage));
                                            System.out.println("sending Welcome CHAT message... ");

                                            System.out.println("Server: " + welcomeMessage);
                                            System.out.println("Other user is typing...");

                                        }
                                        else
                                        {
                                            System.err.println("Unexpected Message Received:");
                                            System.err.println("Expected \"SCP END\" Received: \"" + inputLine + "\"");
                                            clientSocket.close();
                                            serverSocket.close();
                                            System.exit(1);
                                        }
                                    }
                                    else
                                    {
                                        System.err.println("Unexpected Message Received:");
                                        System.err.println(
                                                "Expected \"SERVERPORT " + portNumber + "\" Received: \"" + inputLine +
                                                "\"");
                                        clientSocket.close();
                                        serverSocket.close();
                                        System.exit(1);
                                    }
                                }
                                else
                                {
                                    System.err.println("Unexpected Message Received:");
                                    System.err.println("Expected \"SERVERADDRESS " + hostName.getHostAddress() +
                                                       "\" Received: \"" + inputLine + "\"");
                                    clientSocket.close();
                                    serverSocket.close();
                                    System.exit(1);
                                }
                            }
                            else
                            {
                                System.err.println("Unexpected Message Received:");
                                System.err.println(
                                        "Expected \"USERNAME \"" + clientUserName + "\"\" Received: \"" + inputLine +
                                        "\"");
                                clientSocket.close();
                                serverSocket.close();
                                System.exit(1);
                            }
                        }
                        else
                        {
                            System.err.println("Unexpected Message Received:");
                            System.err.println("Expected \"SCP ACKNOWLEDGE\" Received: \"" + inputLine + "\"");
                            clientSocket.close();
                            serverSocket.close();
                            System.exit(1);
                        }
                    }
                    else if (accepted && acknowledged && !cancelled)
                    {
                        // chat loop
                        if (inputLine.trim().equals("SCP CHAT"))
                        {
                            System.out.println("received CHAT message");
                            if ((inputLine = in.readLine().trim()).startsWith("REMOTEADDRESS") &&
                                inputLine.contains(hostName.getHostAddress()))
                            {
                                if ((inputLine = in.readLine().trim()).startsWith("REMOTEPORT") &&
                                    inputLine.contains(portNumber + ""))
                                {

                                    if ((inputLine = in.readLine().trim()).startsWith("MESSAGECONTENT"))
                                    {
                                        if ((inputLine = in.readLine().trim()).equals(""))
                                        {
                                            if ((inputLine = in.readLine().trim()).equals(""))
                                            {
                                                while (!((inputLine = in.readLine().trim()).equals("SCP END")) &&
                                                       inputLine != null)
                                                {
                                                    System.out.println(clientUserName + ": " + inputLine.trim());
                                                }
                                                if (inputLine.trim().equals("SCP END"))
                                                {
                                                    System.out.print("Enter a message: ");
                                                    userInput = stdIn.readLine();

                                                    // disconnect
                                                    if (userInput.equals("DISCONNECT"))
                                                    {
                                                        out.println("SCP DISCONNECT \n" + "SCP END");
                                                        System.out.println("Disconnecting...");
                                                        System.out.println("sending DISCONNECT message");
                                                        cancelled = true;
                                                    }
                                                    else
                                                    {
                                                        out.println(chatMessage(clientHostname, clientPort, userInput));
                                                        System.out.println("sending CHAT message");
                                                        System.out.println("Server: " + userInput);
                                                        System.out.println("Other user is typing...");
                                                    }

                                                }
                                                else
                                                {
                                                    System.err.println("Unexpected Message Received:");
                                                    System.err.println(
                                                            "Expected \"SCP END\" Received: \"" + inputLine + "\"");
                                                    clientSocket.close();
                                                    serverSocket.close();
                                                    System.exit(1);
                                                }
                                            }
                                            else
                                            {
                                                System.err.println("Unexpected Message Received:");
                                                System.err.println("Expected \"\" Received: \"" + inputLine + "\"");
                                                clientSocket.close();
                                                serverSocket.close();
                                                System.exit(1);
                                            }
                                        }
                                        else
                                        {
                                            System.err.println("Unexpected Message Received:");
                                            System.err.println("Expected \"\" Received: \"" + inputLine + "\"");
                                            clientSocket.close();
                                            serverSocket.close();
                                            System.exit(1);
                                        }
                                    }
                                    else
                                    {
                                        System.err.println("Unexpected Message Received:");
                                        System.err
                                                .println("Expected \"MESSAGECONTENT\" Received: \"" + inputLine + "\"");
                                        clientSocket.close();
                                        serverSocket.close();
                                        System.exit(1);
                                    }
                                }
                                else
                                {
                                    System.err.println("Unexpected Message Received:");
                                    System.err.println(
                                            "Expected \"REMOTEPORT " + portNumber + "\" Received: \"" + inputLine +
                                            "\"");
                                    clientSocket.close();
                                    serverSocket.close();
                                    System.exit(1);
                                }
                            }
                            else
                            {
                                System.err.println("Unexpected Message Received:");
                                System.err.println(
                                        "Expected \"REMOTEADDRESS " + hostName.getHostAddress() + "\" Received: \"" +
                                        inputLine + "\"");
                                clientSocket.close();
                                serverSocket.close();
                                System.exit(1);
                            }

                        }
                        else if (inputLine.trim().equals("SCP DISCONNECT"))
                        {
                            if ((inputLine = in.readLine().trim()).equals("SCP END"))
                            {
                                System.out.println("received DISCONNECT message");
                                System.out.println("Client disconnected, waiting for new connection...");

                                out.println("SCP ACKNOWLEDGE \n" + "SCP END");
                                System.out.println("sending ACKNOWLEDGE message");

                                clientSocket.close();
                                reset = true;
                            }
                            else
                            {
                                System.err.println("Unexpected Message Received:");
                                System.err.println("Expected: \"SCP END\" Received: \"" + inputLine + "\"");
                                clientSocket.close();
                                serverSocket.close();
                                System.exit(1);
                            }
                        }
                        else
                        {
                            System.err.println("Unexpected Message Received:");
                            System.err.println(
                                    "Expected: \"SCP CHAT\" or \"SCP DISCONNECT\" \nReceived: \"" + inputLine + "\"");
                            clientSocket.close();
                            serverSocket.close();
                            System.exit(1);
                        }
                    }
                    else if (cancelled)
                    {
                        if (inputLine.trim().equals("SCP ACKNOWLEDGE"))
                        {
                            if ((inputLine = in.readLine().trim()).equals("SCP END"))
                            {
                                System.out.println("received ACKNOWLEDGE message");
                                System.out.println("Client disconnected, waiting for new connection...");
                                clientSocket.close();
                                reset = true;
                            }
                            else
                            {
                                System.err.println("Unexpected Message Received:");
                                System.err.println("Expected \"SCP END\" Received: \"" + inputLine + "\"");
                                clientSocket.close();
                                serverSocket.close();
                                System.exit(1);
                            }


                        }
                        else
                        {
                            System.err.println("Unexpected Message Received:");
                            System.err.println("Expected \"SCP ACKNOWLEDGE\" Received: \"" + inputLine + "\"");
                            clientSocket.close();
                            serverSocket.close();
                            System.exit(1);
                        }
                    }
                    else
                    {
                        System.err.println("Unexpected Message Received:");
                        System.err.println("Expected \"SCP CONNECT\" Received: \"" + inputLine + "\"");
                        clientSocket.close();
                        serverSocket.close();
                        System.exit(1);
                    }
                }
            }
            catch (IOException e)
            {
                System.out.println("Exception caught when trying to listen on port " + portNumber +
                                   " or listening for a connection");
                System.out.println(e.getMessage());
            }
        }
    }

    private static String chatMessage(String clientHostName, int clientPort, String messageContent)
    {
        return "SCP CHAT\n" + "REMOTEADDRESS " + clientHostName + "\n" + "REMOTEPORT " + clientPort + "\n" +
               "MESSAGECONTENT \n" + "\n\n" + messageContent + "\n" + "SCP END";

    }
}